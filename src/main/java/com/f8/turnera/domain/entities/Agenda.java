package com.f8.turnera.domain.entities;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import lombok.Data;

@Entity
@Table(name = "agendas")
@Data
public class Agenda {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    @NotNull
    private Long id;

    @Column(name = "active")
    @NotNull
    private Boolean active;

    @Column(name = "created_date")
    @NotNull
    private LocalDateTime createdDate;

    @ManyToOne(optional = false)
    @JoinColumn(name = "organization_id")
    @NotNull
    private Organization organization;

    @ManyToOne(optional = false)
    @JoinColumn(name = "resource_id")
    @NotNull
    private Resource resource;

    @ManyToOne(optional = false)
    @JoinColumn(name = "resource_type_id")
    @NotNull
    private ResourceType resourceType;

    @Column(name = "start_date")
    @NotNull
    private ZonedDateTime startDate;

    @Column(name = "end_date")
    @NotNull
    private ZonedDateTime endDate;

    @OneToOne()
    @JoinColumn(name = "last_appointment_id")
    private Appointment lastAppointment;

    public Agenda() {
    }

    public Agenda(LocalDateTime createdDate, Organization organization, Resource resource, ResourceType resourceType,
            ZonedDateTime startDate,
            ZonedDateTime endDate) {
        this.active = true;
        this.createdDate = createdDate;
        this.organization = organization;
        this.resource = resource;
        this.resourceType = resourceType;
        this.startDate = startDate;
        this.endDate = endDate;
    }
}
