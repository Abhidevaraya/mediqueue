package com.mediqueue.appointment.entity;

import com.mediqueue.appointment.enums.AppointmentStatus;
import com.mediqueue.common.BaseEntity;
import com.mediqueue.department.entity.Department;
import com.mediqueue.doctor.entity.Doctor;
import com.mediqueue.patient.entity.Patient;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "appointments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Appointment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "appointment_reference", nullable = false, unique = true, length = 50)
    private String appointmentReference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(name = "start_date_time", nullable = false)
    private LocalDateTime startDateTime;

    @Column(name = "end_date_time", nullable = false)
    private LocalDateTime endDateTime;

    @Column(name = "slot_duration_minutes", nullable = false)
    private Integer slotDurationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 25)
    @Builder.Default
    private AppointmentStatus status = AppointmentStatus.BOOKED;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "booking_timestamp", nullable = false)
    @Builder.Default
    private Instant bookingTimestamp = Instant.now();

    @Column(name = "cancellation_timestamp")
    private Instant cancellationTimestamp;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "rescheduled_from_reference", length = 50)
    private String rescheduledFromReference;

    @Column(name = "rescheduled_timestamp")
    private Instant rescheduledTimestamp;

    public boolean isActive() {
        return this.status == AppointmentStatus.BOOKED
                || this.status == AppointmentStatus.CHECKED_IN
                || this.status == AppointmentStatus.IN_PROGRESS;
    }
}
