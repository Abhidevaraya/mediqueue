package com.mediqueue.medicaldocument.repository;

import com.mediqueue.medicaldocument.entity.MedicalDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MedicalDocumentRepository extends JpaRepository<MedicalDocument, Long> {

    Optional<MedicalDocument> findByDocumentReference(String documentReference);

    Optional<MedicalDocument> findByStorageKey(String storageKey);

    Page<MedicalDocument> findByPatientIdAndIsDeletedFalseOrderByCreatedAtDesc(Long patientId, Pageable pageable);

    Page<MedicalDocument> findByConsultationIdAndIsDeletedFalseOrderByCreatedAtDesc(Long consultationId, Pageable pageable);
}
