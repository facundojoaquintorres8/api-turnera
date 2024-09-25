package com.f8.turnera.domain.dtos;

import java.util.List;

import lombok.Data;

@Data
public class FullAppointmentDTO {
    private Long id;
    private CustomerDTO customer;
    private List<AppointmentStatusDTO> appointmentStatus;
}
