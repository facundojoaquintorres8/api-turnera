package com.f8.turnera.domain.services.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import com.f8.turnera.config.SecurityConstants;
import com.f8.turnera.config.TokenUtil;
import com.f8.turnera.domain.dtos.OrganizationDTO;
import com.f8.turnera.domain.dtos.ResourceTypeDTO;
import com.f8.turnera.domain.dtos.ResourceTypeFilterDTO;
import com.f8.turnera.domain.entities.Organization;
import com.f8.turnera.domain.entities.ResourceType;
import com.f8.turnera.domain.repositories.IResourceTypeRepository;
import com.f8.turnera.domain.services.IOrganizationService;
import com.f8.turnera.domain.services.IResourceTypeService;
import com.f8.turnera.exception.NoContentException;
import com.f8.turnera.util.Constants;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    public Page<ResourceTypeDTO> findAllByFilter(String token, ResourceTypeFilterDTO filter) throws Exception {
        ModelMapper modelMapper = new ModelMapper();

        filter.setOrganizationId(Long.parseLong(TokenUtil.getClaimByToken(token, SecurityConstants.ORGANIZATION_KEY).toString()));

        Page<ResourceType> resourcesTypes = findByCriteria(filter);
        return resourcesTypes.map(resourceType -> modelMapper.map(resourceType, ResourceTypeDTO.class));
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

        List<ResourceType> result = em.createQuery(cq).getResultList();
        if (filter.getSort() != null) {
            if (filter.getSort().get(0).equals("ASC")) {
                switch (filter.getSort().get(1)) {
                case "description":
                    result.sort(Comparator.comparing(ResourceType::getDescription, String::compareToIgnoreCase));
                    break;
                default:
                    break;
                }
            } else if (filter.getSort().get(0).equals("DESC")) {
                switch (filter.getSort().get(1)) {
                case "description":
                    result.sort(
                            Comparator.comparing(ResourceType::getDescription, String::compareToIgnoreCase).reversed());
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
        return new PageImpl<ResourceType>(result.subList(fromIndex, toIndex), pageable, count);
    }

    @Override
    public ResourceTypeDTO findById(String token, Long id) throws Exception {
        Long orgId = Long.parseLong(TokenUtil.getClaimByToken(token, SecurityConstants.ORGANIZATION_KEY).toString());
        Optional<ResourceType> resourceType = resourceTypeRepository.findByIdAndOrganizationId(id, orgId);
        if (!resourceType.isPresent()) {
            throw new NoContentException("Tipo de Recurso no encontrado - " + id);
        }

        ModelMapper modelMapper = new ModelMapper();

        return modelMapper.map(resourceType.get(), ResourceTypeDTO.class);
    }

    @Override
    public ResourceTypeDTO create(String token, ResourceTypeDTO resourceTypeDTO) throws Exception {
        ModelMapper modelMapper = new ModelMapper();

        OrganizationDTO organization = organizationService.findById(token);

        ResourceType resourceType = modelMapper.map(resourceTypeDTO, ResourceType.class);
        resourceType.setCreatedDate(LocalDateTime.now());
        resourceType.setActive(true);
        resourceType.setOrganization(modelMapper.map(organization, Organization.class));

        resourceTypeRepository.save(resourceType);

        return modelMapper.map(resourceType, ResourceTypeDTO.class);
    }

    @Override
    public ResourceTypeDTO update(String token, ResourceTypeDTO resourceTypeDTO) throws Exception {
        Long orgId = Long.parseLong(TokenUtil.getClaimByToken(token, SecurityConstants.ORGANIZATION_KEY).toString());
        Optional<ResourceType> resourceType = resourceTypeRepository.findByIdAndOrganizationId(resourceTypeDTO.getId(), orgId);
        if (!resourceType.isPresent()) {
            throw new NoContentException("Tipo de Recurso no encontrado - " + resourceTypeDTO.getId());
        }

        if (resourceType.get().getActive() && !resourceTypeDTO.getActive()
                && resourceType.get().getResources().stream().filter(x -> x.getActive()).count() > 0) {
            throw new RuntimeException(
                    "Existen Recursos activos con este Tipo de Recurso asociado. Primero debe modificar los Recursos para continuar.");
        }

        ModelMapper modelMapper = new ModelMapper();

        resourceType.ifPresent(rt -> {
            rt.setActive(resourceTypeDTO.getActive());
            rt.setDescription(resourceTypeDTO.getDescription());

            resourceTypeRepository.save(rt);
        });

        return modelMapper.map(resourceType.get(), ResourceTypeDTO.class);
    }

    @Override
    public void deleteById(String token, Long id) throws Exception {
        Long orgId = Long.parseLong(TokenUtil.getClaimByToken(token, SecurityConstants.ORGANIZATION_KEY).toString());
        Optional<ResourceType> resourceType = resourceTypeRepository.findByIdAndOrganizationId(id, orgId);
        if (!resourceType.isPresent()) {
            throw new NoContentException("Tipo de Recurso no encontrado - " + id);
        }

        resourceTypeRepository.delete(resourceType.get());
    }
}
