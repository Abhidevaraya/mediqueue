package com.mediqueue.queue.entity;

import com.mediqueue.appointment.entity.Appointment;
import com.mediqueue.department.entity.Department;
import com.mediqueue.doctor.entity.Doctor;
import com.mediqueue.patient.entity.Patient;
import com.mediqueue.queue.enums.QueuePriority;
import com.mediqueue.queue.enums.QueueStatus;
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
import java.time.LocalDate;

@Entity
@Table(
        name = "queue_entries",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_qe_appointment", columnNames = {"appointment_id"}),
                @UniqueConstraint(name = "uk_qe_doc_date_token", columnNames = {"doctor_id", "queue_date", "token_number"})
        },
        indexes = {
                @Index(name = "idx_qe_doc_date_status", columnList = "doctor_id, queue_date, status"),
                @Index(name = "idx_qe_patient_date", columnList = "patient_id, queue_date"),
                @Index(name = "idx_qe_order", columnList = "doctor_id, queue_date, status, check_in_timestamp")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QueueEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entry_reference", length = 64, nullable = false, unique = true)
    private String entryReference;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(name = "queue_date", nullable = false)
    private LocalDate queueDate;

    @Column(name = "token_number", nullable = false)
    private Integer tokenNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private QueueStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority_category", length = 30, nullable = false)
    @Builder.Default
    private QueuePriority priorityCategory = QueuePriority.STANDARD;

    @Column(name = "check_in_timestamp", nullable = false)
    private Instant checkInTimestamp;

    @Column(name = "called_timestamp")
    private Instant calledTimestamp;

    @Column(name = "consultation_start_timestamp")
    private Instant consultationStartTimestamp;

    @Column(name = "consultation_end_timestamp")
    private Instant consultationEndTimestamp;

    @Column(name = "skip_timestamp")
    private Instant skipTimestamp;

    @Column(name = "skip_reason", length = 255)
    private String skipReason;

    @Column(name = "cancellation_timestamp")
    private Instant cancellationTimestamp;

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
        if (this.priorityCategory == null) {
            this.priorityCategory = QueuePriority.STANDARD;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
