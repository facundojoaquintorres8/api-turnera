package com.f8.turnera.domain.dtos;

import lombok.Data;

@Data
public class AppointmentSaveDTO {

    private SmallAgendaDTO agenda;
    private CustomerDTO customer;

}
