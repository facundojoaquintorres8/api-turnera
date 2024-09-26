package com.f8.turnera.domain.services.impl;

import java.util.Optional;

import com.f8.turnera.domain.dtos.OrganizationDTO;
import com.f8.turnera.domain.dtos.ResponseDTO;
import com.f8.turnera.domain.entities.Organization;
import com.f8.turnera.domain.repositories.IOrganizationRepository;
import com.f8.turnera.domain.services.IOrganizationService;
import com.f8.turnera.exception.NoContentException;
import com.f8.turnera.util.EmailValidation;
import com.f8.turnera.util.MapperHelper;
import com.f8.turnera.util.OrganizationHelper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrganizationService implements IOrganizationService {

    @Autowired
    private IOrganizationRepository organizationRepository;

    @Override
    public ResponseDTO findById(String token) throws Exception {
        Long id = OrganizationHelper.getOrganizationId(token);
        Optional<Organization> organization = organizationRepository.findById(id);
        if (!organization.isPresent()) {
            throw new NoContentException("Organización no encontrada - " + id);
        }

        return new ResponseDTO(MapperHelper.modelMapper().map(organization.get(), OrganizationDTO.class));
    }

    @Override
    public ResponseDTO update(String token, OrganizationDTO organizationDTO) throws Exception {
        Long id = OrganizationHelper.getOrganizationId(token);
        Optional<Organization> organization = organizationRepository.findById(id);
        if (!organization.isPresent()) {
            throw new NoContentException("Organización no encontrada - " + id);
        }

        EmailValidation.validateEmail(organizationDTO.getDefaultEmail());

        organization.ifPresent(o -> {
            o.setBusinessName(organizationDTO.getBusinessName());
            o.setBrandName(organizationDTO.getBrandName());
            o.setCuit(organizationDTO.getCuit());
            o.setAddress(organizationDTO.getAddress());
            o.setPhone1(organizationDTO.getPhone1());
            o.setPhone2(organizationDTO.getPhone2());
            o.setDefaultEmail(organizationDTO.getDefaultEmail());

            organizationRepository.save(o);
        });

        return new ResponseDTO(MapperHelper.modelMapper().map(organization.get(), OrganizationDTO.class));
    }

}