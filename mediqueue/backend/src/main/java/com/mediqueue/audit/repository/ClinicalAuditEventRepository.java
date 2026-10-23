package com.mediqueue.audit.repository;

import com.mediqueue.audit.entity.ClinicalAuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClinicalAuditEventRepository extends JpaRepository<ClinicalAuditEvent, Long> {

    Page<ClinicalAuditEvent> findByPatientIdOrderByEventTimestampDesc(Long patientId, Pageable pageable);

    Page<ClinicalAuditEvent> findByResourceTypeAndResourceIdOrderByEventTimestampDesc(
            String resourceType,
            Long resourceId,
            Pageable pageable
    );

    @org.springframework.data.jpa.repository.Query("SELECT e FROM ClinicalAuditEvent e " +
            "WHERE (:from IS NULL OR e.eventTimestamp >= :from) " +
            "AND (:to IS NULL OR e.eventTimestamp <= :to) " +
            "AND (:action IS NULL OR e.action = :action) " +
            "AND (:actorId IS NULL OR e.actorUserId = :actorId) " +
            "AND (:resourceType IS NULL OR e.resourceType = :resourceType) " +
            "AND (:resourceId IS NULL OR e.resourceId = :resourceId) " +
            "AND (:outcome IS NULL OR e.outcome = :outcome)")
    Page<ClinicalAuditEvent> findFilteredEvents(
            @org.springframework.data.repository.query.Param("from") java.time.Instant from,
            @org.springframework.data.repository.query.Param("to") java.time.Instant to,
            @org.springframework.data.repository.query.Param("action") String action,
            @org.springframework.data.repository.query.Param("actorId") Long actorId,
            @org.springframework.data.repository.query.Param("resourceType") String resourceType,
            @org.springframework.data.repository.query.Param("resourceId") Long resourceId,
            @org.springframework.data.repository.query.Param("outcome") String outcome,
            Pageable pageable
    );

    @org.springframework.data.jpa.repository.Query("SELECT e FROM ClinicalAuditEvent e " +
            "WHERE (:from IS NULL OR e.eventTimestamp >= :from) " +
            "AND (:to IS NULL OR e.eventTimestamp <= :to) " +
            "AND (:action IS NULL OR e.action = :action) " +
            "AND (:actorId IS NULL OR e.actorUserId = :actorId) " +
            "AND (:resourceType IS NULL OR e.resourceType = :resourceType) " +
            "AND (:resourceId IS NULL OR e.resourceId = :resourceId) " +
            "AND (:outcome IS NULL OR e.outcome = :outcome) " +
            "ORDER BY e.eventTimestamp DESC")
    java.util.List<ClinicalAuditEvent> findFilteredEventsList(
            @org.springframework.data.repository.query.Param("from") java.time.Instant from,
            @org.springframework.data.repository.query.Param("to") java.time.Instant to,
            @org.springframework.data.repository.query.Param("action") String action,
            @org.springframework.data.repository.query.Param("actorId") Long actorId,
            @org.springframework.data.repository.query.Param("resourceType") String resourceType,
            @org.springframework.data.repository.query.Param("resourceId") Long resourceId,
            @org.springframework.data.repository.query.Param("outcome") String outcome
    );
}
