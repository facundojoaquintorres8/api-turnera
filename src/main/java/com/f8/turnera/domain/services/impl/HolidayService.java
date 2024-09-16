package com.f8.turnera.domain.services.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

import com.f8.turnera.domain.dtos.HolidayDTO;
import com.f8.turnera.domain.dtos.HolidayFilterDTO;
import com.f8.turnera.domain.dtos.OrganizationDTO;
import com.f8.turnera.domain.dtos.ResponseDTO;
import com.f8.turnera.domain.entities.Holiday;
import com.f8.turnera.domain.entities.Organization;
import com.f8.turnera.domain.repositories.IHolidayRepository;
import com.f8.turnera.domain.services.IHolidayService;
import com.f8.turnera.domain.services.IOrganizationService;
import com.f8.turnera.exception.BadRequestException;
import com.f8.turnera.exception.NoContentException;
import com.f8.turnera.util.Constants;
import com.f8.turnera.util.MapperHelper;
import com.f8.turnera.util.OrganizationHelper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class HolidayService implements IHolidayService {

    @Autowired
    private IHolidayRepository holidayRepository;

    @Autowired
    private IOrganizationService organizationService;

    @Autowired
    private EntityManager em;

    @Override
    public ResponseDTO findAllByFilter(String token, HolidayFilterDTO filter) throws Exception {
        filter.setOrganizationId(
                OrganizationHelper.getOrganizationId(token));

        Page<Holiday> holidays = findByCriteria(filter);
        return new ResponseDTO(HttpStatus.OK.value(),
                holidays.map(holiday -> MapperHelper.modelMapper().map(holiday, HolidayDTO.class)));
    }

    private Page<Holiday> findByCriteria(HolidayFilterDTO filter) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Holiday> cq = cb.createQuery(Holiday.class);

        List<Predicate> predicates = new ArrayList<>();

        Root<Holiday> root = cq.from(Holiday.class);
        predicates.add(cb.equal(root.join("organization", JoinType.LEFT), filter.getOrganizationId()));
        if (filter.getDate() != null) {
            Predicate predicate = cb.equal(root.get("date"), filter.getDate());
            predicates.add(predicate);
        }
        if (filter.getDescription() != null) {
            Predicate predicate = cb.like(cb.lower(root.get("description")),
                    "%" + filter.getDescription().toLowerCase() + "%");
            predicates.add(predicate);
        }
        if (filter.getUseInAgenda() != null) {
            Predicate predicate = cb.equal(root.get("useInAgenda"), filter.getUseInAgenda());
            predicates.add(predicate);
        }
        if (filter.getActive() != null) {
            Predicate predicate = cb.equal(root.get("active"), filter.getActive());
            predicates.add(predicate);
        }

        cq.where(predicates.toArray(new Predicate[0]));

        List<Holiday> result = em.createQuery(cq).getResultList();
        if (filter.getSort() != null) {
            if (filter.getSort().get(0).equals("ASC")) {
                switch (filter.getSort().get(1)) {
                    case "date":
                        result.sort(Comparator.comparing(Holiday::getDate));
                        break;
                    case "description":
                        result.sort(Comparator.comparing(Holiday::getDescription, String::compareToIgnoreCase));
                        break;
                    case "useInAgenda":
                        result.sort(Comparator.comparing(Holiday::getUseInAgenda));
                        break;
                    case "active":
                        result.sort(Comparator.comparing(Holiday::getActive));
                        break;
                    default:
                        break;
                }
            } else if (filter.getSort().get(0).equals("DESC")) {
                switch (filter.getSort().get(1)) {
                    case "date":
                        result.sort(Comparator.comparing(Holiday::getDate).reversed());
                        break;
                    case "description":
                        result.sort(
                                Comparator.comparing(Holiday::getDescription, String::compareToIgnoreCase).reversed());
                        break;
                    case "useInAgenda":
                        result.sort(Comparator.comparing(Holiday::getUseInAgenda).reversed());
                        break;
                    case "active":
                        result.sort(Comparator.comparing(Holiday::getActive).reversed());
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
        return new PageImpl<Holiday>(result.subList(fromIndex, toIndex), pageable, count);
    }

    @Override
    public ResponseDTO findById(String token, Long id) throws Exception {
        Optional<Holiday> holiday = holidayRepository.findByIdAndOrganizationId(id, OrganizationHelper.getOrganizationId(token));
        if (!holiday.isPresent()) {
            throw new NoContentException("Feriado no encontrado - " + id);
        }

        return new ResponseDTO(HttpStatus.OK.value(), MapperHelper.modelMapper().map(holiday.get(), HolidayDTO.class));
    }

    @Override
    public ResponseDTO create(String token, HolidayDTO holidayDTO) throws Exception {
        OrganizationDTO organization = (OrganizationDTO) organizationService.findById(token).getData();

        Optional<Holiday> existingHoliday = holidayRepository.findByDateAndOrganizationId(holidayDTO.getDate(),
                organization.getId());
        if (existingHoliday.isPresent()) {
            throw new BadRequestException("El feriado ingresado ya está registrado.");
        }

        Holiday holiday = MapperHelper.modelMapper().map(holidayDTO, Holiday.class);
        holiday.setCreatedDate(LocalDateTime.now());
        holiday.setActive(true);
        holiday.setOrganization(MapperHelper.modelMapper().map(organization, Organization.class));

        holidayRepository.save(holiday);

        return new ResponseDTO(HttpStatus.OK.value(), MapperHelper.modelMapper().map(holiday, HolidayDTO.class));
    }

    @Override
    public ResponseDTO update(String token, HolidayDTO holidayDTO) throws Exception {
        Long orgId = OrganizationHelper.getOrganizationId(token);
        Optional<Holiday> holiday = holidayRepository.findByIdAndOrganizationId(holidayDTO.getId(), orgId);
        if (!holiday.isPresent()) {
            throw new NoContentException("Feriado no encontrado - " + holidayDTO.getId());
        }

        Optional<Holiday> existingHoliday = holidayRepository.findByDateAndOrganizationId(holidayDTO.getDate(), orgId);
        if (existingHoliday.isPresent() && !existingHoliday.get().getId().equals(holidayDTO.getId())) {
            throw new BadRequestException("El feriado ingresado ya está registrado.");
        }

        holiday.ifPresent(r -> {
            r.setActive(holidayDTO.getActive());
            r.setDate(holidayDTO.getDate());
            r.setDescription(holidayDTO.getDescription());
            r.setUseInAgenda(holidayDTO.getUseInAgenda());

            holidayRepository.save(r);
        });

        return new ResponseDTO(HttpStatus.OK.value(), MapperHelper.modelMapper().map(holiday.get(), HolidayDTO.class));
    }

    @Override
    public ResponseDTO deleteById(String token, Long id) throws Exception {
        Optional<Holiday> holiday = holidayRepository.findByIdAndOrganizationId(id, OrganizationHelper.getOrganizationId(token));
        if (!holiday.isPresent()) {
            throw new NoContentException("Feriado no encontrado - " + id);
        }

        holidayRepository.delete(holiday.get());

        return new ResponseDTO(HttpStatus.OK.value(), "Borrado exitoso!");
    }

    @Override
    public List<LocalDate> findByDateBetweenToAgenda(String token, LocalDate start, LocalDate end) throws Exception {
        List<Holiday> holidays = holidayRepository.findByOrganizationIdAndActiveTrueAndUseInAgendaTrueAndDateBetween(
                OrganizationHelper.getOrganizationId(token), start, end);

        if (!holidays.isEmpty()) {
            return holidays.stream().map(Holiday::getDate).collect(Collectors.toList());
        }
        return new ArrayList<LocalDate>();
    }
}
