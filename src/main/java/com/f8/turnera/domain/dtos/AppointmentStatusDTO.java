package com.f8.turnera.domain.dtos;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class AppointmentStatusDTO {

    private Long id;
    private AppointmentStatusEnum status;
    private String observations;
    private LocalDateTime createdDate;
}