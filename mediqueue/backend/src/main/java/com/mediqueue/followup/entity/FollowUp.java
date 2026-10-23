package com.mediqueue.followup.entity;

import com.mediqueue.appointment.entity.Appointment;
import com.mediqueue.consultation.entity.ConsultationRecord;
import com.mediqueue.doctor.entity.Doctor;
import com.mediqueue.followup.enums.FollowUpReminderStatus;
import com.mediqueue.followup.enums.FollowUpStatus;
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
import java.time.LocalDate;

@Entity
@Table(
        name = "follow_ups",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_fu_reference", columnNames = {"follow_up_reference"})
        },
        indexes = {
                @Index(name = "idx_fu_patient_status", columnList = "patient_id, status"),
                @Index(name = "idx_fu_doctor_date", columnList = "doctor_id, recommended_date"),
                @Index(name = "idx_fu_reminder", columnList = "reminder_status, recommended_date")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowUp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "follow_up_reference", length = 64, nullable = false, unique = true)
    private String followUpReference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consultation_id", nullable = false)
    private ConsultationRecord consultation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(name = "recommended_date", nullable = false)
    private LocalDate recommendedDate;

    @Column(name = "instructions", length = 500)
    private String instructions;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    @Builder.Default
    private FollowUpStatus status = FollowUpStatus.SCHEDULED;

    @Enumerated(EnumType.STRING)
    @Column(name = "reminder_status", length = 30, nullable = false)
    @Builder.Default
    private FollowUpReminderStatus reminderStatus = FollowUpReminderStatus.PENDING;

    @Column(name = "reminder_sent_at")
    private Instant reminderSentAt;

    @Column(name = "completed_at")
    private Instant completedAt;

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
            this.status = FollowUpStatus.SCHEDULED;
        }
        if (this.reminderStatus == null) {
            this.reminderStatus = FollowUpReminderStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
