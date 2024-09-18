package com.f8.turnera.domain.dtos;

import java.time.LocalDate;
import java.time.LocalTime;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import lombok.Data;

@Data
public class CreateAgendaDTO {

    @NotNull
    private ResourceDTO resource;
    @NotNull
    private ResourceTypeDTO resourceType;
    @NotNull
    private LocalDate startDate;
    @NotNull
    private LocalTime startHour;
    @NotNull
    private LocalTime endHour;
    @NotBlank
    private String zoneId;
    private Boolean segmented;
    private Long duration;
    private Boolean repeat;
    private LocalDate finalize;
    private RepeatTypeEnum repeatType;
    private Boolean sunday;
    private Boolean monday;
    private Boolean tuesday;
    private Boolean wednesday;
    private Boolean thursday;
    private Boolean friday;
    private Boolean saturday;
    private Boolean omitHolidays;

}
