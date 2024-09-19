package com.f8.turnera.domain.services;

import com.f8.turnera.domain.dtos.AppointmentChangeStatusDTO;
import com.f8.turnera.domain.dtos.AppointmentFilterDTO;
import com.f8.turnera.domain.dtos.AppointmentSaveDTO;
import com.f8.turnera.domain.dtos.ResponseDTO;

public interface IAppointmentService {

    ResponseDTO findAllByFilter(String token, AppointmentFilterDTO filter) throws Exception;

    ResponseDTO book(String token, AppointmentSaveDTO appointmentSaveDTO) throws Exception;

    ResponseDTO absent(String token, AppointmentChangeStatusDTO appointmentChangeStatusDTO) throws Exception;

    ResponseDTO cancel(String token, AppointmentChangeStatusDTO appointmentChangeStatusDTO) throws Exception;

    ResponseDTO attend(String token, AppointmentChangeStatusDTO appointmentChangeStatusDTO) throws Exception;

    ResponseDTO finalize(String token, AppointmentChangeStatusDTO appointmentChangeStatusDTO) throws Exception;
}
