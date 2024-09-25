package com.f8.turnera.domain.entities;

import java.time.LocalDateTime;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import com.f8.turnera.domain.dtos.AppointmentStatusEnum;
import com.fasterxml.jackson.annotation.JsonBackReference;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "appointments")
@Setter
@Getter
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    @NotNull
    private Long id;

    @Column(name = "created_date")
    @NotNull
    private LocalDateTime createdDate;

    @ManyToOne(optional = false)
    @JoinColumn(name = "organization_id")
    @NotNull
    private Organization organization;

    @ManyToOne(optional = false)
    @JoinColumn(name = "agenda_id")
    @NotNull
    @JsonBackReference
    private Agenda agenda;

    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "last_appointment_status_id")
    private AppointmentStatus lastAppointmentStatus;

    @OneToMany(mappedBy = "appointment", cascade = CascadeType.ALL)
    private Set<AppointmentStatus> appointmentStatus;

    public void addStatus(AppointmentStatusEnum appointmentStatus, String observations) {
        AppointmentStatus newAppointmentStatus = new AppointmentStatus(LocalDateTime.now(), this, appointmentStatus, observations);
        setLastAppointmentStatus(newAppointmentStatus);
    }

    public Appointment(LocalDateTime createdDate, Organization organization, Agenda agenda, Customer customer) {
        this.createdDate = createdDate;
        this.organization = organization;
        this.agenda = agenda;
        this.customer = customer;
    }

    public Appointment() {
    }

}
