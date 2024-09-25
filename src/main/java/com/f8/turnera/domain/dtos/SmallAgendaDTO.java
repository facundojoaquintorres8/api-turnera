package com.f8.turnera.domain.dtos;

import java.time.ZonedDateTime;

import lombok.Data;

@Data
public class SmallAgendaDTO {

    private Long id;
    private Boolean active;
    private ResourceDTO resource;
    private ResourceTypeDTO resourceType;
    private ZonedDateTime startDate;
    private ZonedDateTime endDate;
    private SmallAppointmentDTO lastAppointment;

}
