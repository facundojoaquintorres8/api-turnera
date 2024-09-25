package com.f8.turnera.domain.dtos;

import lombok.Data;

@Data
public class AppointmentDTO {
    private Long id;
    private SmallAgendaDTO agenda;
    private CustomerDTO customer;
    private AppointmentStatusDTO lastAppointmentStatus;
}
