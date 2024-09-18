package com.f8.turnera.domain.dtos;

import java.time.LocalDate;
import java.time.LocalTime;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import lombok.Data;

@Data
public class UpdateAgendaDTO {

    @NotNull
    private Long id;
    @NotNull
    private ResourceDTO resource;
    @NotNull
    private ResourceTypeDTO resourceType;
    @NotNull
    private LocalDate startDate;
    @NotNull
    private LocalTime startHour;
    @NotNull
    private LocalDate endDate;
    @NotNull
    private LocalTime endHour;
    @NotBlank
    private String zoneId;
    @NotNull
    private Boolean active;

}
