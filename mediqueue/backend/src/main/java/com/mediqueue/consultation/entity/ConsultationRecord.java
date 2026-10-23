package com.mediqueue.consultation.entity;

import com.mediqueue.appointment.entity.Appointment;
import com.mediqueue.consultation.enums.ConsultationStatus;
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
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "consultation_records",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_cr_appointment", columnNames = {"appointment_id"}),
                @UniqueConstraint(name = "uk_cr_reference", columnNames = {"consultation_reference"})
        },
        indexes = {
                @Index(name = "idx_cr_patient_status", columnList = "patient_id, status"),
                @Index(name = "idx_cr_doctor_status", columnList = "doctor_id, status"),
                @Index(name = "idx_cr_start_time", columnList = "actual_start_timestamp")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsultationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "consultation_reference", length = 64, nullable = false, unique = true)
    private String consultationReference;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    @Builder.Default
    private ConsultationStatus status = ConsultationStatus.SCHEDULED;

    @Column(name = "actual_start_timestamp")
    private Instant actualStartTimestamp;

    @Column(name = "actual_end_timestamp")
    private Instant actualEndTimestamp;

    @Column(name = "cancellation_reason", length = 255)
    private String cancellationReason;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = ConsultationStatus.SCHEDULED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
