package com.f8.turnera.controllers;

import com.f8.turnera.config.SecurityConstants;
import com.f8.turnera.domain.dtos.AppointmentChangeStatusDTO;
import com.f8.turnera.domain.dtos.AppointmentFilterDTO;
import com.f8.turnera.domain.dtos.AppointmentSaveDTO;
import com.f8.turnera.domain.dtos.ResponseDTO;
import com.f8.turnera.domain.services.IAppointmentService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AppointmentController {

    @Autowired
    private IAppointmentService service;

    @GetMapping("/appointments/findAllByFilter")
    @PreAuthorize("hasAuthority('agendas.read')")
    public ResponseEntity<ResponseDTO> findAllByFilter(
            @RequestHeader(name = SecurityConstants.HEADER_TOKEN) String token,
            AppointmentFilterDTO request) throws Exception {
        return new ResponseEntity<>(service.findAllByFilter(token, request), HttpStatus.OK);
    }

    @PostMapping("/appointments")
    @PreAuthorize("hasAuthority('appointments.book')")
    public ResponseEntity<ResponseDTO> book(
            @RequestHeader(name = SecurityConstants.HEADER_TOKEN) String token,
            @RequestBody AppointmentSaveDTO request) throws Exception {
        return new ResponseEntity<>(service.book(token, request), HttpStatus.OK);
    }

    @PostMapping("appointments/absent")
    @PreAuthorize("hasAuthority('appointments.absent')")
    public ResponseEntity<ResponseDTO> absent(
            @RequestHeader(name = SecurityConstants.HEADER_TOKEN) String token,
            @RequestBody AppointmentChangeStatusDTO request) throws Exception {
        return new ResponseEntity<>(service.absent(token, request), HttpStatus.OK);
    }

    @PostMapping("appointments/cancel")
    @PreAuthorize("hasAuthority('appointments.cancel')")
    public ResponseEntity<ResponseDTO> cancel(
            @RequestHeader(name = SecurityConstants.HEADER_TOKEN) String token,
            @RequestBody AppointmentChangeStatusDTO request) throws Exception {
        return new ResponseEntity<>(service.cancel(token, request), HttpStatus.OK);
    }

    @PostMapping("appointments/attend")
    @PreAuthorize("hasAuthority('appointments.attend')")
    public ResponseEntity<ResponseDTO> attend(
            @RequestHeader(name = SecurityConstants.HEADER_TOKEN) String token,
            @RequestBody AppointmentChangeStatusDTO request) throws Exception {
        return new ResponseEntity<>(service.attend(token, request), HttpStatus.OK);
    }

    @PostMapping("appointments/finalize")
    @PreAuthorize("hasAuthority('appointments.finalize')")
    public ResponseEntity<ResponseDTO> finalize(
            @RequestHeader(name = SecurityConstants.HEADER_TOKEN) String token,
            @RequestBody AppointmentChangeStatusDTO request) throws Exception {
        return new ResponseEntity<>(service.finalize(token, request), HttpStatus.OK);
    }

}
