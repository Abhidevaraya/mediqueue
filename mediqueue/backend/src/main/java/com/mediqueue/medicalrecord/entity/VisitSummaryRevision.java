package com.mediqueue.medicalrecord.entity;

import com.mediqueue.doctor.entity.Doctor;
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

@Entity
@Table(
        name = "visit_summary_revisions",
        indexes = {
                @Index(name = "idx_vsr_summary", columnList = "visit_summary_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisitSummaryRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visit_summary_id", nullable = false)
    private VisitSummary visitSummary;

    @Column(name = "revision_number", nullable = false)
    private Integer revisionNumber;

    @Column(name = "chief_complaint", length = 500)
    private String chiefComplaint;

    @Column(name = "diagnosis", columnDefinition = "TEXT")
    private String diagnosis;

    @Column(name = "clinical_notes", columnDefinition = "TEXT")
    private String clinicalNotes;

    @Column(name = "treatment_plan", columnDefinition = "TEXT")
    private String treatmentPlan;

    @Column(name = "archived_at", nullable = false)
    private Instant archivedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "archived_by_doctor_id", nullable = false)
    private Doctor archivedByDoctor;

    @Column(name = "revision_reason", length = 255)
    private String revisionReason;

    @PrePersist
    protected void onCreate() {
        if (this.archivedAt == null) {
            this.archivedAt = Instant.now();
        }
    }
}
