package com.f8.turnera.domain.services.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import com.f8.turnera.domain.dtos.OrganizationDTO;
import com.f8.turnera.domain.dtos.ResourceDTO;
import com.f8.turnera.domain.dtos.ResourceFilterDTO;
import com.f8.turnera.domain.dtos.ResourceTypeDTO;
import com.f8.turnera.domain.dtos.ResponseDTO;
import com.f8.turnera.domain.entities.Organization;
import com.f8.turnera.domain.entities.Resource;
import com.f8.turnera.domain.entities.ResourceType;
import com.f8.turnera.domain.repositories.IResourceRepository;
import com.f8.turnera.domain.services.IOrganizationService;
import com.f8.turnera.domain.services.IResourceService;
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
public class ResourceService implements IResourceService {

    @Autowired
    private IResourceRepository resourceRepository;

    @Autowired
    private IOrganizationService organizationService;

    @Autowired
    private EntityManager em;

    @Override
    public ResponseDTO findAllByFilter(String token, ResourceFilterDTO filter) throws Exception {
        filter.setOrganizationId(OrganizationHelper.getOrganizationId(token));

        Page<Resource> resources = findByCriteria(filter);
        return new ResponseDTO(HttpStatus.OK.value(),
                resources.map(resource -> MapperHelper.modelMapper().map(resource, ResourceDTO.class)));
    }

    private Page<Resource> findByCriteria(ResourceFilterDTO filter) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Resource> cq = cb.createQuery(Resource.class);

        List<Predicate> predicates = new ArrayList<>();

        Root<Resource> root = cq.from(Resource.class);
        predicates.add(cb.equal(root.join("organization", JoinType.LEFT), filter.getOrganizationId()));
        if (filter.getDescription() != null) {
            Predicate predicate = cb.like(cb.lower(root.get("description")),
                    "%" + filter.getDescription().toLowerCase() + "%");
            predicates.add(predicate);
        }
        if (filter.getCode() != null) {
            Predicate predicate = cb.like(cb.lower(root.get("code")), "%" + filter.getCode().toLowerCase() + "%");
            predicates.add(predicate);
        }
        if (filter.getResourceTypeId() != null) {
            Predicate predicate = cb.equal(root.join("resourcesTypes", JoinType.LEFT), filter.getResourceTypeId());
            predicates.add(predicate);
        }
        if (filter.getActive() != null) {
            Predicate predicate = cb.equal(root.get("active"), filter.getActive());
            predicates.add(predicate);
        }

        cq.where(predicates.toArray(new Predicate[0]));

        List<Resource> result = em.createQuery(cq).getResultList();
        if (filter.getSort() != null) {
            if (filter.getSort().get(0).equals("ASC")) {
                switch (filter.getSort().get(1)) {
                case "description":
                    result.sort(Comparator.comparing(Resource::getDescription, String::compareToIgnoreCase));
                    break;
                case "code":
                    result.sort(Comparator.comparing(Resource::getCode,
                            Comparator.nullsFirst(String::compareToIgnoreCase)));
                    break;
                default:
                    break;
                }
            } else if (filter.getSort().get(0).equals("DESC")) {
                switch (filter.getSort().get(1)) {
                case "description":
                    result.sort(Comparator.comparing(Resource::getDescription, String::compareToIgnoreCase).reversed());
                    break;
                case "code":
                    result.sort(
                            Comparator.comparing(Resource::getCode, Comparator.nullsFirst(String::compareToIgnoreCase))
                                    .reversed());
                    break;
                default:
                    break;
                }
            }
        }
        int count = result.size();
        int fromIndex = Constants.ITEMS_PER_PAGE * (filter.getPage());
        int toIndex = fromIndex + Constants.ITEMS_PER_PAGE > count ? count : fromIndex + Constants.ITEMS_PER_PAGE;
        Pageable pageable = PageRequest.of(filter.getPage(), Constants.ITEMS_PER_PAGE);
        return new PageImpl<Resource>(result.subList(fromIndex, toIndex), pageable, count);
    }

    @Override
    public ResponseDTO findById(String token, Long id) throws Exception {
        Optional<Resource> resource = resourceRepository.findByIdAndOrganizationId(id, OrganizationHelper.getOrganizationId(token));
        if (!resource.isPresent()) {
            throw new NoContentException("Recurso no encontrado - " + id);
        }

        return new ResponseDTO(HttpStatus.OK.value(), MapperHelper.modelMapper().map(resource.get(), ResourceDTO.class));
    }

    @Override
    public ResponseDTO create(String token, ResourceDTO request) throws Exception {
        OrganizationDTO organization = (OrganizationDTO) organizationService.findById(token).getData();

        Resource resource = MapperHelper.modelMapper().map(request, Resource.class);
        resource.setCreatedDate(LocalDateTime.now());
        resource.setActive(true);
        resource.setOrganization(MapperHelper.modelMapper().map(organization, Organization.class));
        resource.setResourcesTypes(addResourcesTypes(request));

        resourceRepository.save(resource);

        return new ResponseDTO(HttpStatus.OK.value(), MapperHelper.modelMapper().map(resource, ResourceDTO.class));
    }

    @Override
    public ResponseDTO update(String token, ResourceDTO request) throws Exception {
        Optional<Resource> resource = resourceRepository.findByIdAndOrganizationId(request.getId(), OrganizationHelper.getOrganizationId(token));
        if (!resource.isPresent()) {
            throw new NoContentException("Recurso no encontrado - " + request.getId());
        }

        resource.ifPresent(r -> {
            r.setActive(request.getActive());
            r.setDescription(request.getDescription());
            r.setCode(request.getCode());
            r.setResourcesTypes(addResourcesTypes(request));

            resourceRepository.save(r);
        });

        return new ResponseDTO(HttpStatus.OK.value(), MapperHelper.modelMapper().map(resource.get(), ResourceDTO.class));
    }

    @Override
    public ResponseDTO deleteById(String token, Long id) throws Exception {
        Optional<Resource> resource = resourceRepository.findByIdAndOrganizationId(id, OrganizationHelper.getOrganizationId(token));
        if (!resource.isPresent()) {
            throw new NoContentException("Recurso no encontrado - " + id);
        }

        resourceRepository.delete(resource.get());

        return new ResponseDTO(HttpStatus.OK.value(), "Borrado exitoso!");
    }

    private Set<ResourceType> addResourcesTypes(ResourceDTO resource) {
        Set<ResourceType> newResourcesTypes = new HashSet<>();
        for (ResourceTypeDTO resourceType : resource.getResourcesTypes()) {
            newResourcesTypes.add(MapperHelper.modelMapper().map(resourceType, ResourceType.class));
        }
        return newResourcesTypes;
    }
}
