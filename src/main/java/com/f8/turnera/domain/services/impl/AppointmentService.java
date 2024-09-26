package com.f8.turnera.domain.services.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import com.f8.turnera.domain.dtos.AgendaDTO;
import com.f8.turnera.domain.dtos.AppointmentChangeStatusDTO;
import com.f8.turnera.domain.dtos.AppointmentDTO;
import com.f8.turnera.domain.dtos.SmallAppointmentDTO;
import com.f8.turnera.domain.dtos.AppointmentFilterDTO;
import com.f8.turnera.domain.dtos.AppointmentSaveDTO;
import com.f8.turnera.domain.dtos.AppointmentStatusEnum;
import com.f8.turnera.domain.dtos.OrganizationDTO;
import com.f8.turnera.domain.dtos.ResponseDTO;
import com.f8.turnera.domain.entities.Agenda;
import com.f8.turnera.domain.entities.Appointment;
import com.f8.turnera.domain.entities.Customer;
import com.f8.turnera.domain.entities.Organization;
import com.f8.turnera.domain.repositories.IAppointmentRepository;
import com.f8.turnera.domain.services.IAgendaService;
import com.f8.turnera.domain.services.IAppointmentService;
import com.f8.turnera.domain.services.ICustomerService;
import com.f8.turnera.domain.services.IEmailService;
import com.f8.turnera.domain.services.IOrganizationService;
import com.f8.turnera.exception.NoContentException;
import com.f8.turnera.util.Constants;
import com.f8.turnera.util.MapperHelper;
import com.f8.turnera.util.OrganizationHelper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class AppointmentService implements IAppointmentService {

    @Autowired
    private IAppointmentRepository appointmentRepository;

    @Autowired
    private IOrganizationService organizationService;

    @Autowired
    private IAgendaService agendaService;

    @Autowired
    private ICustomerService customerService;

    @Autowired
    private IEmailService emailService;

    @Autowired
    private EntityManager em;

    @Override
    public ResponseDTO findAllByFilter(String token, AppointmentFilterDTO filter) throws Exception {
        filter.setOrganizationId(OrganizationHelper.getOrganizationId(token));

        Page<Appointment> appointments = findByCriteria(filter);
        return new ResponseDTO(appointments.map(appointment -> MapperHelper.modelMapper().map(appointment, AppointmentDTO.class)));
    }

    private Page<Appointment> findByCriteria(AppointmentFilterDTO filter) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Appointment> cq = cb.createQuery(Appointment.class);

        List<Predicate> predicates = new ArrayList<>();

        Root<Appointment> root = cq.from(Appointment.class);
        predicates.add(cb.equal(root.join("organization", JoinType.LEFT), filter.getOrganizationId()));
        if (filter.getCustomerId() != null) {
            Predicate predicate = cb.equal(root.join("customer", JoinType.LEFT), filter.getCustomerId());
            predicates.add(predicate);
        }
        if (filter.getResourceId() != null) {
            Predicate predicate = cb.equal(root.join("agenda", JoinType.LEFT).get("resource"), filter.getResourceId());
            predicates.add(predicate);
        }

        cq.where(predicates.toArray(new Predicate[0]));

        if (filter.getSort() != null) {
            @SuppressWarnings("rawtypes")
            Expression sort = root.get("id");
            switch (filter.getSort().get(1)) {
                case "startDate":
                    sort = root.get("agenda").get("startDate");
                    break;
                case "endDate":
                    sort = root.get("agenda").get("endDate");
                    break;
                case "resource":
                    sort = cb.lower(root.get("agenda").get("resource").get("description"));
                    break;
                case "resourceType":
                    sort = cb.lower(root.get("agenda").get("resourceType").get("description"));
                    break;
                default:
                    break;
            }
            if (filter.getSort().get(0).equals("ASC")) {
                cq.orderBy(cb.asc(sort));
            } else if (filter.getSort().get(0).equals("DESC")) {
                cq.orderBy(cb.desc(sort));
            }
        }

        List<Appointment> result = em.createQuery(cq).getResultList();

        int count = result.size();
        int fromIndex = Constants.ITEMS_PER_PAGE * (filter.getPage());
        int toIndex = fromIndex + Constants.ITEMS_PER_PAGE > count ? count : fromIndex + Constants.ITEMS_PER_PAGE;
        Pageable pageable = PageRequest.of(filter.getPage(), Constants.ITEMS_PER_PAGE);
        return new PageImpl<Appointment>(result.subList(fromIndex, toIndex), pageable, count);
    }

    @Override
    public ResponseDTO book(String token, AppointmentSaveDTO appointmentSaveDTO) throws Exception {
        OrganizationDTO organizationDTO = (OrganizationDTO) organizationService.findById(token).getData();
        Organization organization = MapperHelper.modelMapper().map(organizationDTO, Organization.class);
        AgendaDTO agenda = (AgendaDTO) agendaService.findById(token, appointmentSaveDTO.getAgenda().getId()).getData();

        Customer customer = null;
        if (appointmentSaveDTO.getCustomer().getId() != null) {
            customer = MapperHelper.modelMapper().map(appointmentSaveDTO.getCustomer(), Customer.class);
        } else {
            customer = customerService.createQuick(appointmentSaveDTO.getCustomer(), organization);
        }

        Appointment appointment = new Appointment(LocalDateTime.now(), organization,
                MapperHelper.modelMapper().map(agenda, Agenda.class), customer);
        appointment.addStatus(AppointmentStatusEnum.BOOKED, null);

        appointmentRepository.save(appointment);
        agenda.setLastAppointment(MapperHelper.modelMapper().map(appointment, SmallAppointmentDTO.class));
        agendaService.setLastAppointment(token, agenda);

        emailService.sendBookedAppointmentEmail(appointment);

        return new ResponseDTO(MapperHelper.modelMapper().map(appointment, SmallAppointmentDTO.class));
    }

    @Override
    public ResponseDTO absent(String token, AppointmentChangeStatusDTO appointmentChangeStatusDTO) throws Exception {
        Optional<Appointment> appointment = appointmentRepository.findByIdAndOrganizationId(appointmentChangeStatusDTO.getId(), OrganizationHelper.getOrganizationId(token));
        if (!appointment.isPresent()) {
            throw new NoContentException("Turno no encontrado - " + appointmentChangeStatusDTO.getId());
        }

        appointment.get().addStatus(AppointmentStatusEnum.ABSENT, appointmentChangeStatusDTO.getObservations());

        appointmentRepository.save(appointment.get());

        return new ResponseDTO(MapperHelper.modelMapper().map(appointment.get(), SmallAppointmentDTO.class));
    }

    @Override
    public ResponseDTO cancel(String token, AppointmentChangeStatusDTO appointmentChangeStatusDTO) throws Exception {
        Optional<Appointment> appointment = appointmentRepository.findByIdAndOrganizationId(appointmentChangeStatusDTO.getId(), OrganizationHelper.getOrganizationId(token));
        if (!appointment.isPresent()) {
            throw new NoContentException("Turno no encontrado - " + appointmentChangeStatusDTO.getId());
        }

        appointment.get().addStatus(AppointmentStatusEnum.CANCELLED, appointmentChangeStatusDTO.getObservations());

        emailService.sendCanceledAppointmentEmail(appointment.get());

        appointmentRepository.save(appointment.get());

        return new ResponseDTO(MapperHelper.modelMapper().map(appointment.get(), SmallAppointmentDTO.class));
    }

    @Override
    public ResponseDTO attend(String token, AppointmentChangeStatusDTO appointmentChangeStatusDTO) throws Exception {
        Optional<Appointment> appointment = appointmentRepository.findByIdAndOrganizationId(appointmentChangeStatusDTO.getId(), OrganizationHelper.getOrganizationId(token));
        if (!appointment.isPresent()) {
            throw new NoContentException("Turno no encontrado - " + appointmentChangeStatusDTO.getId());
        }

        appointment.get().addStatus(AppointmentStatusEnum.IN_ATTENTION, appointmentChangeStatusDTO.getObservations());

        appointmentRepository.save(appointment.get());

        return new ResponseDTO(MapperHelper.modelMapper().map(appointment.get(), SmallAppointmentDTO.class));
    }

    @Override
    public ResponseDTO finalize(String token, AppointmentChangeStatusDTO appointmentChangeStatusDTO) throws Exception {
        Optional<Appointment> appointment = appointmentRepository.findByIdAndOrganizationId(appointmentChangeStatusDTO.getId(), OrganizationHelper.getOrganizationId(token));
        if (!appointment.isPresent()) {
            throw new NoContentException("Turno no encontrado - " + appointmentChangeStatusDTO.getId());
        }

        appointment.get().addStatus(AppointmentStatusEnum.FINALIZED, appointmentChangeStatusDTO.getObservations());

        emailService.sendFinalizeAppointmentEmail(appointment.get());

        appointmentRepository.save(appointment.get());

        return new ResponseDTO(MapperHelper.modelMapper().map(appointment.get(), SmallAppointmentDTO.class));
    }
}
