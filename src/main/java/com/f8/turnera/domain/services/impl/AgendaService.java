package com.f8.turnera.domain.services.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.f8.turnera.domain.dtos.AgendaDTO;
import com.f8.turnera.domain.dtos.CreateAgendaDTO;
import com.f8.turnera.domain.dtos.AppointmentFilterDTO;
import com.f8.turnera.domain.dtos.AppointmentStatusEnum;
import com.f8.turnera.domain.dtos.RepeatTypeEnum;
import com.f8.turnera.domain.dtos.ResponseDTO;
import com.f8.turnera.domain.dtos.UpdateAgendaDTO;
import com.f8.turnera.domain.entities.Agenda;
import com.f8.turnera.domain.entities.Appointment;
import com.f8.turnera.domain.entities.Organization;
import com.f8.turnera.domain.entities.Resource;
import com.f8.turnera.domain.entities.ResourceType;
import com.f8.turnera.domain.repositories.IAgendaRepository;
import com.f8.turnera.domain.services.IAgendaService;
import com.f8.turnera.domain.services.IHolidayService;
import com.f8.turnera.domain.services.IOrganizationService;
import com.f8.turnera.domain.services.IResourceService;
import com.f8.turnera.domain.services.IResourceTypeService;
import com.f8.turnera.exception.BadRequestException;
import com.f8.turnera.exception.NoContentCustomException;
import com.f8.turnera.exception.NoContentException;
import com.f8.turnera.util.Constants;
import com.f8.turnera.util.MapperHelper;
import com.f8.turnera.util.OrganizationHelper;

@Service
public class AgendaService implements IAgendaService {

    @Autowired
    private IAgendaRepository agendaRepository;

    @Autowired
    private IHolidayService holidayService;

    @Autowired
    private IOrganizationService organizationService;

    @Autowired
    private IResourceService resourceService;

    @Autowired
    private IResourceTypeService resourceTypeService;

    @Autowired
    private EntityManager em;

    @Override
    public ResponseDTO findAllByFilter(String token, AppointmentFilterDTO filter) throws Exception {
        filter.setOrganizationId(OrganizationHelper.getOrganizationId(token));

        Page<Agenda> agendas = findByCriteria(filter);
        return new ResponseDTO(HttpStatus.OK.value(),
                agendas.map(agenda -> MapperHelper.modelMapper().map(agenda, AgendaDTO.class)));
    }

    private Page<Agenda> findByCriteria(AppointmentFilterDTO filter) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Agenda> cq = cb.createQuery(Agenda.class);

        List<Predicate> predicates = new ArrayList<>();

        Root<Agenda> root = cq.from(Agenda.class);
        predicates.add(cb.equal(root.join("organization", JoinType.LEFT), filter.getOrganizationId()));
        if (filter.getResourceId() != null) {
            Predicate predicate = cb.equal(root.join("resource", JoinType.LEFT), filter.getResourceId());
            predicates.add(predicate);
        }
        if (filter.getResourceTypeId() != null) {
            Predicate predicate = cb.equal(root.join("resourceType", JoinType.LEFT), filter.getResourceTypeId());
            predicates.add(predicate);
        }
        if (filter.getCustomerId() != null) {
            Predicate predicate = cb.equal(root.join("lastAppointment", JoinType.LEFT).get("customer"),
                    filter.getCustomerId());
            predicates.add(predicate);
        }
        if (filter.getStatus() != null) {
            if (filter.getStatus() == AppointmentStatusEnum.FREE) {
                Predicate predicate1 = root.join("lastAppointment", JoinType.LEFT).isNull();
                Predicate predicate2 = cb.equal(root.join("lastAppointment", JoinType.LEFT)
                        .join("lastAppointmentStatus", JoinType.LEFT).get("status"), AppointmentStatusEnum.CANCELLED);
                predicates.add(cb.or(predicate1, predicate2));

            } else {
                Predicate predicate = cb.equal(root.join("lastAppointment", JoinType.LEFT)
                        .join("lastAppointmentStatus", JoinType.LEFT).get("status"), filter.getStatus());
                predicates.add(predicate);
            }
        }
        ZoneId zoneId = ZoneId.of(filter.getZoneId());
        if (filter.getFrom() != null) {
            ZonedDateTime startDate = ZonedDateTime.of(filter.getFrom(), LocalTime.MIN, zoneId);
            Predicate predicate = cb.greaterThanOrEqualTo(root.get("startDate"), startDate);
            predicates.add(predicate);
        }
        if (filter.getTo() != null) {
            ZonedDateTime endDate = ZonedDateTime.of(filter.getTo().plusDays(1), LocalTime.MIN, zoneId);
            Predicate predicate = cb.lessThanOrEqualTo(root.get("endDate"), endDate);
            predicates.add(predicate);
        }
        if (filter.getActive() != null) {
            Predicate predicate = cb.equal(root.get("active"), filter.getActive());
            predicates.add(predicate);
        }

        cq.where(predicates.toArray(new Predicate[0]));

        List<Agenda> result = em.createQuery(cq).getResultList();
        if (filter.getSort() != null) {
            if (filter.getSort().get(0).equals("ASC")) {
                switch (filter.getSort().get(1)) {
                    case "startDate":
                        result.sort(Comparator.comparing(Agenda::getStartDate));
                        break;
                    case "endDate":
                        result.sort(Comparator.comparing(Agenda::getEndDate));
                        break;
                    case "resourceDescription":
                        result.sort(
                                Comparator.comparing(x -> x.getResource().getDescription(),
                                        String::compareToIgnoreCase));
                        break;
                    default:
                        break;
                }
            } else if (filter.getSort().get(0).equals("DESC")) {
                switch (filter.getSort().get(1)) {
                    case "startDate":
                        result.sort(Comparator.comparing(Agenda::getStartDate).reversed());
                        break;
                    case "endDate":
                        result.sort(Comparator.comparing(Agenda::getEndDate).reversed());
                        break;
                    case "resourceDescription":
                        result.sort(Comparator.comparing(x -> x.getResource().getDescription(),
                                Comparator.nullsFirst(String::compareToIgnoreCase).reversed()));
                        break;
                    default:
                        break;
                }
            }
        }
        int count = result.size();
        int fromIndex = Constants.ITEMS_PER_PAGE * (filter.getPage());
        int toIndex = fromIndex + Constants.ITEMS_PER_PAGE > count ? count : fromIndex + Constants.ITEMS_PER_PAGE;
        if (filter.getIgnorePaginated() != null && filter.getIgnorePaginated()) {
            fromIndex = 0;
            toIndex = count;
        }
        Pageable pageable = PageRequest.of(filter.getPage(), Constants.ITEMS_PER_PAGE);
        return new PageImpl<Agenda>(result.subList(fromIndex, toIndex), pageable, count);
    }

    @Override
    public ResponseDTO findById(String token, Long id) throws Exception {
        Optional<Agenda> agenda = agendaRepository.findByIdAndOrganizationId(id, OrganizationHelper.getOrganizationId(token));
        if (!agenda.isPresent()) {
            throw new NoContentException("Disponibilidad no encontrada - " + id);
        }

        return new ResponseDTO(HttpStatus.OK.value(), MapperHelper.modelMapper().map(agenda.get(), AgendaDTO.class));
    }

    @Override
    public ResponseDTO create(String token, CreateAgendaDTO agendaSaveDTO) throws Exception {
        Organization organization = MapperHelper.modelMapper().map(organizationService.findById(token).getData(),
                Organization.class);
        Resource resource = MapperHelper.modelMapper()
                .map(resourceService.findById(token, agendaSaveDTO.getResource().getId()).getData(), Resource.class);
        ResourceType resourceType = MapperHelper.modelMapper()
                .map(resourceTypeService.findById(token, agendaSaveDTO.getResourceType().getId()).getData(), ResourceType.class);

        Boolean isSegmented = BooleanUtils.isTrue(agendaSaveDTO.getSegmented());

        // Validations start 
        if (isSegmented && agendaSaveDTO.getDuration() != null
                && agendaSaveDTO.getDuration() < 5) {
            throw new BadRequestException("La duración debe ser mayor o igual a 5 minutos.");
        }
        if (agendaSaveDTO.getRepeat() != null && agendaSaveDTO.getRepeat()) {
            if (agendaSaveDTO.getFinalize() == null) {
                throw new BadRequestException("Finaliza es requerido.");
            }
            if (agendaSaveDTO.getStartDate().isAfter(agendaSaveDTO.getFinalize())) {
                throw new BadRequestException("La fecha de inicio deber ser menor o igual a la de finaliza.");
            }
            if (agendaSaveDTO.getRepeatType() == null) {
                throw new BadRequestException("Cada es requerido.");
            } else if (agendaSaveDTO.getRepeatType().equals(RepeatTypeEnum.WEEKLY)
                    && (agendaSaveDTO.getSunday() == null || !agendaSaveDTO.getSunday())
                    && (agendaSaveDTO.getMonday() == null || !agendaSaveDTO.getMonday())
                    && (agendaSaveDTO.getTuesday() == null || !agendaSaveDTO.getTuesday())
                    && (agendaSaveDTO.getWednesday() == null || !agendaSaveDTO.getWednesday())
                    && (agendaSaveDTO.getThursday() == null || !agendaSaveDTO.getThursday())
                    && (agendaSaveDTO.getFriday() == null || !agendaSaveDTO.getFriday())
                    && (agendaSaveDTO.getSaturday() == null || !agendaSaveDTO.getSaturday())) {
                throw new BadRequestException("Debe seleccionar al menos un día de la semana.");
            }
        }
        // Validations end 

        List<Agenda> agendas = new ArrayList<>();
        LocalDateTime createdDate = LocalDateTime.now();
        ZoneId zoneId = ZoneId.of(agendaSaveDTO.getZoneId());
        Integer plusDays = agendaSaveDTO.getStartHour().isBefore(agendaSaveDTO.getEndHour()) ? 0 : 1;
        ZonedDateTime tempStart = ZonedDateTime.of(agendaSaveDTO.getStartDate(), agendaSaveDTO.getStartHour(), zoneId);
        ZonedDateTime tempEnd = ZonedDateTime.of(agendaSaveDTO.getStartDate().plusDays(plusDays), agendaSaveDTO.getEndHour(), zoneId);
        if (BooleanUtils.isTrue(agendaSaveDTO.getRepeat())) {
            ZonedDateTime finalize = ZonedDateTime.of(agendaSaveDTO.getFinalize().plusDays(1), LocalTime.MIN, zoneId);
            switch (agendaSaveDTO.getRepeatType()) {
                case DAILY:
                    if (isSegmented) {
                        Long daysCount = 0L;
                        while (!tempStart.isAfter(finalize) && !tempStart.isEqual(finalize)) {
                            agendas.addAll(createAgendasSegmented(resource, resourceType, organization, createdDate,
                                    tempStart, tempEnd, agendaSaveDTO.getDuration()));
                            daysCount++;
                            tempStart = ZonedDateTime
                                    .of(agendaSaveDTO.getStartDate(), agendaSaveDTO.getStartHour(), zoneId)
                                    .plusDays(daysCount);
                            tempEnd = ZonedDateTime
                                    .of(agendaSaveDTO.getStartDate().plusDays(plusDays), agendaSaveDTO.getEndHour(), zoneId)
                                    .plusDays(daysCount);
                        }
                    } else {
                        while (!tempStart.isAfter(finalize) && !tempStart.isEqual(finalize)) {
                            agendas.add(new Agenda(createdDate, organization, resource, resourceType, tempStart, tempEnd));
                            tempStart = tempStart.plusDays(1);
                            tempEnd = tempEnd.plusDays(1);
                        }
                    }
                    break;
                case WEEKLY:
                    if (isSegmented) {
                        agendas.addAll(createAgendasSegmentedWeekly(agendaSaveDTO, resource, resourceType, organization, createdDate,
                                tempStart, tempEnd, finalize, zoneId, plusDays));
                    } else {
                        agendas.addAll(createAgendasWeekly(agendaSaveDTO, resource, resourceType, organization, createdDate, tempStart, tempEnd, finalize));
                    }
                    break;
                case MONTHLY:
                    Long monthsCount = 0L;
                    if (isSegmented) {
                        while (!tempStart.isAfter(finalize) && !tempStart.isEqual(finalize)) {
                            agendas.addAll(createAgendasSegmented(resource, resourceType, organization, createdDate,
                                    tempStart, tempEnd, agendaSaveDTO.getDuration()));
                            monthsCount++;
                            tempStart = ZonedDateTime
                                    .of(agendaSaveDTO.getStartDate(), agendaSaveDTO.getStartHour(), zoneId)
                                    .plusMonths(monthsCount);
                            tempEnd = ZonedDateTime
                                    .of(agendaSaveDTO.getStartDate().plusDays(plusDays), agendaSaveDTO.getEndHour(), zoneId)
                                    .plusMonths(monthsCount);
                        }
                    } else {
                        while (!tempStart.isAfter(finalize) && !tempStart.isEqual(finalize)) {
                            agendas.add(new Agenda(createdDate, organization, resource, resourceType, tempStart, tempEnd));
                            monthsCount++;
                            tempStart = ZonedDateTime
                                    .of(agendaSaveDTO.getStartDate(), agendaSaveDTO.getStartHour(), zoneId)
                                    .plusMonths(monthsCount);
                            tempEnd = ZonedDateTime
                                    .of(agendaSaveDTO.getStartDate().plusDays(plusDays), agendaSaveDTO.getEndHour(),
                                            zoneId)
                                    .plusMonths(monthsCount);
                        }
                    }
                    break;
                default:
                    break;
            }
        } else {
            if (isSegmented) {
                agendas = createAgendasSegmented(resource, resourceType, organization, createdDate,
                        tempStart, tempEnd, agendaSaveDTO.getDuration());
            } else {
                agendas.add(new Agenda(createdDate, organization, resource, resourceType, tempStart, tempEnd));
            }
        }

        // filter holidays
        if (BooleanUtils.isTrue(agendaSaveDTO.getOmitHolidays())) {
            LocalDate end = BooleanUtils.isTrue(agendaSaveDTO.getRepeat()) ? agendaSaveDTO.getFinalize() : agendaSaveDTO.getStartDate();
            List<LocalDate> holidayDates = holidayService.findByDateBetweenToAgenda(token, agendaSaveDTO.getStartDate(), end);
            agendas = agendas.stream().filter(
                    x -> !holidayDates.contains(x.getStartDate().toLocalDate())).collect(Collectors.toList());
        }

        agendaRepository.saveAll(agendas);

        if (agendas.isEmpty()) {
            throw new NoContentCustomException("No se generaron Disponibilidades");
        }
        return new ResponseDTO(HttpStatus.OK.value(), "Se generaron " + agendas.size() + " Disponibilidades");
    }

    private List<Agenda> createAgendasWeekly(CreateAgendaDTO agendaSaveDTO,
            Resource resource, ResourceType resourceType, Organization organization, LocalDateTime createdDate,
            ZonedDateTime tempStart, ZonedDateTime tempEnd, ZonedDateTime finalize) {
        List<Agenda> agendas = new ArrayList<>();
        while (!tempStart.isAfter(finalize) && !tempStart.isEqual(finalize)) {
            switch (tempStart.getDayOfWeek()) {
                case SUNDAY:
                    if (BooleanUtils.isTrue(agendaSaveDTO.getSunday())) {
                        agendas.add(new Agenda(createdDate, organization, resource, resourceType, tempStart, tempEnd));
                    }
                    break;
                case MONDAY:
                    if (BooleanUtils.isTrue(agendaSaveDTO.getMonday())) {
                        agendas.add(new Agenda(createdDate, organization, resource, resourceType, tempStart, tempEnd));
                    }
                    break;
                case TUESDAY:
                    if (BooleanUtils.isTrue(agendaSaveDTO.getTuesday())) {
                        agendas.add(new Agenda(createdDate, organization, resource, resourceType, tempStart, tempEnd));
                    }
                    break;
                case WEDNESDAY:
                    if (BooleanUtils.isTrue(agendaSaveDTO.getWednesday())) {
                        agendas.add(new Agenda(createdDate, organization, resource, resourceType, tempStart, tempEnd));
                    }
                    break;
                case THURSDAY:
                    if (BooleanUtils.isTrue(agendaSaveDTO.getThursday())) {
                        agendas.add(new Agenda(createdDate, organization, resource, resourceType, tempStart, tempEnd));
                    }
                    break;
                case FRIDAY:
                    if (BooleanUtils.isTrue(agendaSaveDTO.getFriday())) {
                        agendas.add(new Agenda(createdDate, organization, resource, resourceType, tempStart, tempEnd));
                    }
                    break;
                case SATURDAY:
                    if (BooleanUtils.isTrue(agendaSaveDTO.getSaturday())) {
                        agendas.add(new Agenda(createdDate, organization, resource, resourceType, tempStart, tempEnd));
                    }
                    break;
            }
            tempStart = tempStart.plusDays(1);
            tempEnd = tempEnd.plusDays(1);
        }
        return agendas;
    }

    private List<Agenda> createAgendasSegmentedWeekly(CreateAgendaDTO agendaSaveDTO,
            Resource resource, ResourceType resourceType, Organization organization, LocalDateTime createdDate,
            ZonedDateTime tempStart, ZonedDateTime tempEnd, ZonedDateTime finalize,
            ZoneId zoneId, Integer plusDays) {
        List<Agenda> agendas = new ArrayList<>();
        Long daysCount = 0L;
        while (!tempStart.isAfter(finalize) && !tempStart.isEqual(finalize)) {
            switch (tempStart.getDayOfWeek()) {
                case SUNDAY:
                    if (BooleanUtils.isTrue(agendaSaveDTO.getSunday())) {
                        agendas.addAll(createAgendasSegmented(resource, resourceType, organization, createdDate,
                                tempStart, tempEnd, agendaSaveDTO.getDuration()));
                    }
                    break;
                case MONDAY:
                    if (BooleanUtils.isTrue(agendaSaveDTO.getMonday())) {
                        agendas.addAll(createAgendasSegmented(resource, resourceType, organization, createdDate, tempStart,
                        tempEnd, agendaSaveDTO.getDuration()));
                    }
                    break;
                case TUESDAY:
                    if (BooleanUtils.isTrue(agendaSaveDTO.getTuesday())) {
                        agendas.addAll(createAgendasSegmented(resource, resourceType, organization, createdDate, tempStart,
                        tempEnd, agendaSaveDTO.getDuration()));
                    }
                    break;
                case WEDNESDAY:
                    if (BooleanUtils.isTrue(agendaSaveDTO.getWednesday())) {
                        agendas.addAll(createAgendasSegmented(resource, resourceType, organization, createdDate, tempStart,
                        tempEnd, agendaSaveDTO.getDuration()));
                    }
                    break;
                case THURSDAY:
                    if (BooleanUtils.isTrue(agendaSaveDTO.getThursday())) {
                        agendas.addAll(createAgendasSegmented(resource, resourceType, organization, createdDate, tempStart,
                        tempEnd, agendaSaveDTO.getDuration()));
                    }
                    break;
                case FRIDAY:
                    if (BooleanUtils.isTrue(agendaSaveDTO.getFriday())) {
                        agendas.addAll(createAgendasSegmented(resource, resourceType, organization, createdDate, tempStart,
                        tempEnd, agendaSaveDTO.getDuration()));
                    }
                    break;
                case SATURDAY:
                    if (BooleanUtils.isTrue(agendaSaveDTO.getSaturday())) {
                        agendas.addAll(createAgendasSegmented(resource, resourceType, organization, createdDate, tempStart,
                        tempEnd, agendaSaveDTO.getDuration()));
                    }
                    break;
            }
            daysCount++;
            tempStart = ZonedDateTime
                    .of(agendaSaveDTO.getStartDate(), agendaSaveDTO.getStartHour(), zoneId)
                    .plusDays(daysCount);
            tempEnd = ZonedDateTime
                    .of(agendaSaveDTO.getStartDate().plusDays(plusDays), agendaSaveDTO.getEndHour(), zoneId)
                    .plusDays(daysCount);
        }
        return agendas;
    }

    private List<Agenda> createAgendasSegmented(Resource resource, ResourceType resourceType, Organization organization, LocalDateTime createdDate,
            ZonedDateTime tempStart, ZonedDateTime end, Long duration) {
        List<Agenda> agendas = new ArrayList<>();
        ZonedDateTime tempSegmentEnd = tempStart.plusMinutes(duration);
        while (!tempStart.isAfter(end) && !tempStart.isEqual(end)) {
            agendas.add(new Agenda(createdDate, organization, resource, resourceType, tempStart, tempSegmentEnd));
            tempStart = tempSegmentEnd;
            tempSegmentEnd = tempSegmentEnd.plusMinutes(duration);
        }
        return agendas;
    }

    @Override
    public ResponseDTO updateAgenda(String token, UpdateAgendaDTO request) throws Exception {
        Optional<Agenda> agenda = agendaRepository.findByIdAndOrganizationId(request.getId(), OrganizationHelper.getOrganizationId(token));
        if (!agenda.isPresent()) {
            throw new NoContentException("Disponibilidad no encontrada - " + request.getId());
        }
        Resource resource = MapperHelper.modelMapper()
                .map(resourceService.findById(token, request.getResource().getId()).getData(), Resource.class);
        ResourceType resourceType = MapperHelper.modelMapper()
                .map(resourceTypeService.findById(token, request.getResourceType().getId()).getData(), ResourceType.class);

                
        ZoneId zoneId = ZoneId.of(request.getZoneId());
        ZonedDateTime startDate = ZonedDateTime.of(request.getStartDate(), request.getStartHour(), zoneId);
        ZonedDateTime endDate = ZonedDateTime.of(request.getEndDate(), request.getEndHour(), zoneId);
        if (startDate.isEqual(endDate) || startDate.isAfter(endDate)) {
            throw new BadRequestException("La fecha de inicio no puede ser igual o mayor a la de fin.");
        }

        agenda.ifPresent(a -> {
            a.setActive(request.getActive());
            a.setResource(resource);
            a.setResourceType(resourceType);
            a.setStartDate(startDate);
            a.setEndDate(endDate);

            agendaRepository.save(a);
        });

        // TODO: enviar email de actualización de turno

        return new ResponseDTO(HttpStatus.OK.value(),
                MapperHelper.modelMapper().map(agenda.get(), AgendaDTO.class));
    }

    @Override
    public ResponseDTO setLastAppointment(String token, AgendaDTO agendaDTO) throws Exception {
        Optional<Agenda> agenda = agendaRepository.findByIdAndOrganizationId(agendaDTO.getId(), OrganizationHelper.getOrganizationId(token));
        if (!agenda.isPresent()) {
            throw new NoContentException("Disponibilidad no encontrada - " + agendaDTO.getId());
        }

        agenda.ifPresent(a -> {
            a.setLastAppointment(MapperHelper.modelMapper().map(agendaDTO.getLastAppointment(), Appointment.class));
            agendaRepository.save(a);
        });

        return new ResponseDTO(HttpStatus.OK.value(), MapperHelper.modelMapper().map(agenda.get(), AgendaDTO.class));
    }

    @Override
    public ResponseDTO deleteById(String token, Long id) throws Exception {
        Optional<Agenda> agenda = agendaRepository.findByIdAndOrganizationId(id, OrganizationHelper.getOrganizationId(token));
        if (!agenda.isPresent()) {
            throw new NoContentException("Disponibilidad no encontrada - " + id);
        }

        agendaRepository.delete(agenda.get());

        return new ResponseDTO(HttpStatus.OK.value(), "Borrado exitoso!");
    }

    @Override
    public ResponseDTO desactivate(String token, Long id) throws Exception {
        Optional<Agenda> agenda = agendaRepository.findByIdAndOrganizationId(id, OrganizationHelper.getOrganizationId(token));
        if (!agenda.isPresent()) {
            throw new NoContentException("Disponibilidad no encontrada - " + id);
        }

        agenda.ifPresent(a -> {
            a.setActive(false);
            agendaRepository.save(a);
        });

        return new ResponseDTO(HttpStatus.OK.value(), MapperHelper.modelMapper().map(agenda.get(), AgendaDTO.class));
    }
    
    @Override
    public ResponseDTO activate(String token, Long id) throws Exception {
        Optional<Agenda> agenda = agendaRepository.findByIdAndOrganizationId(id, OrganizationHelper.getOrganizationId(token));
        if (!agenda.isPresent()) {
            throw new NoContentException("Disponibilidad no encontrada - " + id);
        }

        agenda.ifPresent(a -> {
            a.setActive(true);
            agendaRepository.save(a);
        });

        return new ResponseDTO(HttpStatus.OK.value(), MapperHelper.modelMapper().map(agenda.get(), AgendaDTO.class));
    }

}
