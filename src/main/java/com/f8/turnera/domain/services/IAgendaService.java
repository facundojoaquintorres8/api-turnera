package com.f8.turnera.domain.services;


import com.f8.turnera.domain.dtos.AgendaDTO;
import com.f8.turnera.domain.dtos.AgendaSaveDTO;
import com.f8.turnera.domain.dtos.AppointmentFilterDTO;
import com.f8.turnera.domain.dtos.ResponseDTO;

public interface IAgendaService {

    ResponseDTO findAllByFilter(String token, AppointmentFilterDTO filter) throws Exception;

    ResponseDTO findById(String token, Long id) throws Exception;

    ResponseDTO create(String token, AgendaSaveDTO agendaDTO) throws Exception;

    ResponseDTO setLastAppointment(String token, AgendaDTO agendaDTO) throws Exception;

    ResponseDTO deleteById(String token, Long id) throws Exception;

    ResponseDTO desactivate(String token, Long id) throws Exception;

    ResponseDTO activate(String token, Long id) throws Exception;
}
