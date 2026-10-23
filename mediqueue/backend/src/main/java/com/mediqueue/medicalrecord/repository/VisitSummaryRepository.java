package com.mediqueue.medicalrecord.repository;

import com.mediqueue.medicalrecord.entity.VisitSummary;
import com.mediqueue.medicalrecord.enums.VisitSummaryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VisitSummaryRepository extends JpaRepository<VisitSummary, Long> {

    Optional<VisitSummary> findBySummaryReference(String summaryReference);

    Optional<VisitSummary> findByConsultationId(Long consultationId);

    Page<VisitSummary> findByPatientIdAndStatusOrderByCreatedAtDesc(
            Long patientId,
            VisitSummaryStatus status,
            Pageable pageable
    );

    Page<VisitSummary> findByAuthoringDoctorIdOrderByCreatedAtDesc(
            Long doctorId,
            Pageable pageable
    );

    Page<VisitSummary> findByPatientIdOrderByCreatedAtDesc(
            Long patientId,
            Pageable pageable
    );
}
