package com.mediqueue.prediction.entity;

import com.mediqueue.queue.entity.QueueEntry;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
        name = "prediction_logs",
        indexes = {
                @Index(name = "idx_pred_qe", columnList = "queue_entry_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PredictionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "queue_entry_id", nullable = false)
    private QueueEntry queueEntry;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @Column(name = "queue_date", nullable = false)
    private LocalDate queueDate;

    @Column(name = "eligible_patients_ahead", nullable = false)
    private Integer eligiblePatientsAhead;

    @Column(name = "predicted_wait_minutes", nullable = false)
    private Integer predictedWaitMinutes;

    @Column(name = "prediction_method", length = 50, nullable = false)
    private String predictionMethod;

    @Column(name = "uses_fallback", nullable = false)
    private Boolean usesFallback;

    @Column(name = "actual_wait_minutes")
    private Integer actualWaitMinutes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}
