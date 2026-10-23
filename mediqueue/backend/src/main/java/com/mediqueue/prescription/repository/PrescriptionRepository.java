package com.mediqueue.prescription.repository;

import com.mediqueue.prescription.entity.Prescription;
import com.mediqueue.prescription.enums.PrescriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    Optional<Prescription> findByPrescriptionReference(String prescriptionReference);

    Page<Prescription> findByPatientIdOrderByIssuedAtDesc(Long patientId, Pageable pageable);

    Page<Prescription> findByPatientIdAndStatusOrderByIssuedAtDesc(
            Long patientId,
            PrescriptionStatus status,
            Pageable pageable
    );

    Page<Prescription> findByDoctorIdOrderByIssuedAtDesc(Long doctorId, Pageable pageable);

    Page<Prescription> findByConsultationIdOrderByIssuedAtDesc(Long consultationId, Pageable pageable);
}
