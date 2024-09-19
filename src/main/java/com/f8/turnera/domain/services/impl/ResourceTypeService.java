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

import com.f8.turnera.domain.dtos.OrganizationDTO;
import com.f8.turnera.domain.dtos.ResourceTypeDTO;
import com.f8.turnera.domain.dtos.ResourceTypeFilterDTO;
import com.f8.turnera.domain.dtos.ResponseDTO;
import com.f8.turnera.domain.entities.Organization;
import com.f8.turnera.domain.entities.ResourceType;
import com.f8.turnera.domain.repositories.IResourceTypeRepository;
import com.f8.turnera.domain.services.IOrganizationService;
import com.f8.turnera.domain.services.IResourceTypeService;
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
public class ResourceTypeService implements IResourceTypeService {

    @Autowired
    private IResourceTypeRepository resourceTypeRepository;
    
    @Autowired
    private IOrganizationService organizationService;

    @Autowired
    private EntityManager em;

    @Override
    public ResponseDTO findAllByFilter(String token, ResourceTypeFilterDTO filter) throws Exception {
        filter.setOrganizationId(OrganizationHelper.getOrganizationId(token));

        Page<ResourceType> resourcesTypes = findByCriteria(filter);
        return new ResponseDTO(HttpStatus.OK.value(), resourcesTypes
                .map(resourceType -> MapperHelper.modelMapper().map(resourceType, ResourceTypeDTO.class)));
    }

    private Page<ResourceType> findByCriteria(ResourceTypeFilterDTO filter) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<ResourceType> cq = cb.createQuery(ResourceType.class);

        List<Predicate> predicates = new ArrayList<>();

        Root<ResourceType> root = cq.from(ResourceType.class);
        predicates.add(cb.equal(root.join("organization", JoinType.LEFT), filter.getOrganizationId()));
        if (filter.getDescription() != null) {
            Predicate predicate = cb.like(cb.lower(root.get("description")),
                    "%" + filter.getDescription().toLowerCase() + "%");
            predicates.add(predicate);
        }
        if (filter.getActive() != null) {
            Predicate predicate = cb.equal(root.get("active"), filter.getActive());
            predicates.add(predicate);
        }

        cq.where(predicates.toArray(new Predicate[0]));

        if (filter.getSort() != null) {
            @SuppressWarnings("rawtypes")
            Expression sort = root.get("id");
            switch (filter.getSort().get(1)) {
                case "description":
                    sort = cb.lower(root.get("description"));
                    break;
                case "active":
                    sort = root.get("active");
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

        List<ResourceType> result = em.createQuery(cq).getResultList();

        int count = result.size();
        int fromIndex = Constants.ITEMS_PER_PAGE * (filter.getPage());
        int toIndex = fromIndex + Constants.ITEMS_PER_PAGE > count ? count : fromIndex + Constants.ITEMS_PER_PAGE;
        Pageable pageable = PageRequest.of(filter.getPage(), Constants.ITEMS_PER_PAGE);
        return new PageImpl<ResourceType>(result.subList(fromIndex, toIndex), pageable, count);
    }

    @Override
    public ResponseDTO findById(String token, Long id) throws Exception {
        Optional<ResourceType> resourceType = resourceTypeRepository.findByIdAndOrganizationId(id, OrganizationHelper.getOrganizationId(token));
        if (!resourceType.isPresent()) {
            throw new NoContentException("Tipo de Recurso no encontrado - " + id);
        }

        return new ResponseDTO(HttpStatus.OK.value(), MapperHelper.modelMapper().map(resourceType.get(), ResourceTypeDTO.class));
    }

    @Override
    public ResponseDTO create(String token, ResourceTypeDTO resourceTypeDTO) throws Exception {
        OrganizationDTO organization = (OrganizationDTO) organizationService.findById(token).getData();

        ResourceType resourceType = MapperHelper.modelMapper().map(resourceTypeDTO, ResourceType.class);
        resourceType.setCreatedDate(LocalDateTime.now());
        resourceType.setActive(true);
        resourceType.setOrganization(MapperHelper.modelMapper().map(organization, Organization.class));

        resourceTypeRepository.save(resourceType);

        return new ResponseDTO(HttpStatus.OK.value(), MapperHelper.modelMapper().map(resourceType, ResourceTypeDTO.class));
    }

    @Override
    public ResponseDTO update(String token, ResourceTypeDTO resourceTypeDTO) throws Exception {
        Optional<ResourceType> resourceType = resourceTypeRepository.findByIdAndOrganizationId(resourceTypeDTO.getId(), OrganizationHelper.getOrganizationId(token));
        if (!resourceType.isPresent()) {
            throw new NoContentException("Tipo de Recurso no encontrado - " + resourceTypeDTO.getId());
        }

        // TODO: revisar si es necesario, o quizás mostrar mensaje que se desactivarán los recursos o no hacer nada con los recursos
        // if (resourceType.get().getActive() && !resourceTypeDTO.getActive()
        //         && resourceType.get().getResources().stream().filter(x -> x.getActive()).count() > 0) {
        //     throw new RuntimeException(
        //             "Existen Recursos activos con este Tipo de Recurso asociado. Primero debe modificar los Recursos para continuar.");
        // }

        resourceType.ifPresent(rt -> {
            rt.setActive(resourceTypeDTO.getActive());
            rt.setDescription(resourceTypeDTO.getDescription());

            resourceTypeRepository.save(rt);
        });

        return new ResponseDTO(HttpStatus.OK.value(), MapperHelper.modelMapper().map(resourceType.get(), ResourceTypeDTO.class));
    }

    @Override
    public ResponseDTO deleteById(String token, Long id) throws Exception {
        Optional<ResourceType> resourceType = resourceTypeRepository.findByIdAndOrganizationId(id, OrganizationHelper.getOrganizationId(token));
        if (!resourceType.isPresent()) {
            throw new NoContentException("Tipo de Recurso no encontrado - " + id);
        }

        resourceTypeRepository.delete(resourceType.get());

        return new ResponseDTO(HttpStatus.OK.value(), "Borrado exitoso!");
    }
}
