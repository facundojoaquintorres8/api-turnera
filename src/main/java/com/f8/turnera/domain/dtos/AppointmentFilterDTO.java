package com.f8.turnera.domain.dtos;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AppointmentFilterDTO extends DefaultFilterDTO {

    private Long resourceTypeId;
    private Long resourceId;
    private Long customerId;
    private AppointmentStatusEnum status;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate from;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate to;
    private String zoneId;

}
