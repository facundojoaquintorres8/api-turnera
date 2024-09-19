package com.f8.turnera.domain.dtos;

import lombok.Data;

@Data
public class SmallAppointmentDTO {

    private Long id;
    private String customerBusinessName;
    private AppointmentStatusDTO lastAppointmentStatus;

}
