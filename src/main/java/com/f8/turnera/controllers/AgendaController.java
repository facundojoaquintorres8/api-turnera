package com.f8.turnera.controllers;

import com.f8.turnera.config.SecurityConstants;
import com.f8.turnera.domain.dtos.AgendaSaveDTO;
import com.f8.turnera.domain.dtos.AppointmentFilterDTO;
import com.f8.turnera.domain.dtos.ResponseDTO;
import com.f8.turnera.domain.services.IAgendaService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AgendaController {

    @Autowired
    private IAgendaService service;

    @GetMapping("/agendas/findAllByFilter")
    @PreAuthorize("hasAuthority('agendas.read')")
    public ResponseEntity<ResponseDTO> findAllByFilter(
            @RequestHeader(name = SecurityConstants.HEADER_TOKEN) String token,
            AppointmentFilterDTO request) throws Exception {
        return new ResponseEntity<>(service.findAllByFilter(token, request), HttpStatus.OK);
    }

    @PostMapping("/agendas")
    @PreAuthorize("hasAuthority('agendas.write')")
    public ResponseEntity<ResponseDTO> createAgenda(
            @RequestHeader(name = SecurityConstants.HEADER_TOKEN) String token,
            @RequestBody AgendaSaveDTO request) throws Exception {
        return new ResponseEntity<>(service.create(token, request), HttpStatus.OK);
    }

    @DeleteMapping("agendas/{id}")
    @PreAuthorize("hasAuthority('agendas.delete')")
    public ResponseEntity<ResponseDTO> deleteAgenda(
            @RequestHeader(name = SecurityConstants.HEADER_TOKEN) String token,
            @PathVariable Long id) throws Exception {
        return new ResponseEntity<>(service.deleteById(token, id), HttpStatus.OK);
    }

    @PutMapping("agendas/{id}/desactivate")
    @PreAuthorize("hasAuthority('agendas.write')")
    public ResponseEntity<ResponseDTO> desactivate(
            @RequestHeader(name = SecurityConstants.HEADER_TOKEN) String token,
            @PathVariable Long id) throws Exception {
        return new ResponseEntity<>(service.desactivate(token, id), HttpStatus.OK);
    }
    
    @PutMapping("agendas/{id}/activate")
    @PreAuthorize("hasAuthority('agendas.write')")
    public ResponseEntity<ResponseDTO> activate(
            @RequestHeader(name = SecurityConstants.HEADER_TOKEN) String token,
            @PathVariable Long id) throws Exception {
        return new ResponseEntity<>(service.activate(token, id), HttpStatus.OK);
    }

}
