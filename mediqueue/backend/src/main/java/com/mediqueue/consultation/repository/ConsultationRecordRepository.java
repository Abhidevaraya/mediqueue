package com.mediqueue.consultation.repository;

import com.mediqueue.consultation.entity.ConsultationRecord;
import com.mediqueue.consultation.enums.ConsultationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConsultationRecordRepository extends JpaRepository<ConsultationRecord, Long> {

    Optional<ConsultationRecord> findByAppointmentId(Long appointmentId);

    Optional<ConsultationRecord> findByConsultationReference(String consultationReference);

    Page<ConsultationRecord> findByPatientIdOrderByCreatedAtDesc(Long patientId, Pageable pageable);

    Page<ConsultationRecord> findByDoctorIdOrderByCreatedAtDesc(Long doctorId, Pageable pageable);

    Optional<ConsultationRecord> findFirstByDoctorIdAndStatus(Long doctorId, ConsultationStatus status);

    @Query("SELECT c FROM ConsultationRecord c WHERE c.doctor.id = :doctorId AND c.status = 'COMPLETED' " +
            "AND c.actualStartTimestamp IS NOT NULL AND c.actualEndTimestamp IS NOT NULL " +
            "ORDER BY c.actualEndTimestamp DESC")
    List<ConsultationRecord> findRecentCompletedConsultations(
            @Param("doctorId") Long doctorId,
            Pageable pageable
    );

    List<ConsultationRecord> findByActualStartTimestampBetweenAndStatus(
            java.time.Instant start, java.time.Instant end, ConsultationStatus status
    );

    List<ConsultationRecord> findByDoctorIdAndActualStartTimestampBetweenAndStatus(
            Long doctorId, java.time.Instant start, java.time.Instant end, ConsultationStatus status
    );
}
