package com.f8.turnera.security.domain.services.impl;

import java.util.List;
import java.util.Optional;

import com.f8.turnera.security.domain.dtos.PermissionDTO;
import com.f8.turnera.security.domain.dtos.ResponseDTO;
import com.f8.turnera.security.domain.entities.Permission;
import com.f8.turnera.security.domain.repositories.IPermissionRepository;
import com.f8.turnera.security.domain.services.IPermissionService;
import com.f8.turnera.util.MapperHelper;

import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PermissionService implements IPermissionService {

    @Autowired
    private IPermissionRepository repository;

    @Override
    public ResponseDTO findAll() throws Exception {
        List<Permission> permissions = repository.findAll();
        return new ResponseDTO(MapperHelper.modelMapper().map(permissions, new TypeToken<List<PermissionDTO>>() { }.getType()));
    }

    @Override
    public PermissionDTO findByCode(String code) throws Exception {
        Optional<Permission> permission = repository.findByCode(code);
        if (!permission.isPresent()) {
            return null;
        }

        return MapperHelper.modelMapper().map(permission.get(), PermissionDTO.class);
    }
}
