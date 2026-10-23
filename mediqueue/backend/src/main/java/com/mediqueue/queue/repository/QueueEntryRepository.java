package com.mediqueue.queue.repository;

import com.mediqueue.queue.entity.QueueEntry;
import com.mediqueue.queue.enums.QueueStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface QueueEntryRepository extends JpaRepository<QueueEntry, Long> {

    Optional<QueueEntry> findByEntryReference(String entryReference);

    Optional<QueueEntry> findByAppointmentId(Long appointmentId);

    boolean existsByAppointmentId(Long appointmentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM QueueEntry q WHERE q.id = :id")
    Optional<QueueEntry> findByIdWithLock(@Param("id") Long id);

    List<QueueEntry> findByDoctorIdAndQueueDateOrderByCheckInTimestampAsc(Long doctorId, LocalDate queueDate);

    List<QueueEntry> findByDoctorIdAndQueueDateAndStatusInOrderByCheckInTimestampAsc(
            Long doctorId,
            LocalDate queueDate,
            Collection<QueueStatus> statuses
    );

    @Query("SELECT q FROM QueueEntry q WHERE q.patient.user.id = :userId AND q.queueDate = :queueDate AND q.status IN :statuses ORDER BY q.checkInTimestamp DESC")
    List<QueueEntry> findActiveByPatientUserId(
            @Param("userId") Long userId,
            @Param("queueDate") LocalDate queueDate,
            @Param("statuses") Collection<QueueStatus> statuses
    );

    @Query("SELECT COUNT(q) FROM QueueEntry q WHERE q.doctor.id = :doctorId AND q.queueDate = :queueDate " +
            "AND q.status = 'WAITING' AND q.checkInTimestamp < :checkInTimestamp")
    int countWaitingAhead(
            @Param("doctorId") Long doctorId,
            @Param("queueDate") LocalDate queueDate,
            @Param("checkInTimestamp") Instant checkInTimestamp
    );

    Optional<QueueEntry> findFirstByDoctorIdAndQueueDateAndStatus(
            Long doctorId,
            LocalDate queueDate,
            QueueStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM QueueEntry q WHERE q.doctor.id = :doctorId AND q.queueDate = :queueDate " +
            "AND q.status = 'WAITING' ORDER BY q.checkInTimestamp ASC, q.tokenNumber ASC LIMIT 1")
    Optional<QueueEntry> findNextWaitingWithLock(
            @Param("doctorId") Long doctorId,
            @Param("queueDate") LocalDate queueDate
    );

    @Query("SELECT q FROM QueueEntry q WHERE q.doctor.id = :doctorId AND q.status = 'COMPLETED' " +
            "AND q.consultationStartTimestamp IS NOT NULL AND q.consultationEndTimestamp IS NOT NULL " +
            "ORDER BY q.consultationEndTimestamp DESC")
    List<QueueEntry> findRecentCompletedConsultations(
            @Param("doctorId") Long doctorId,
            org.springframework.data.domain.Pageable pageable
    );

    List<QueueEntry> findByQueueDate(LocalDate queueDate);

    List<QueueEntry> findByQueueDateBetween(LocalDate from, LocalDate to);

    long countByQueueDateAndStatus(LocalDate queueDate, QueueStatus status);
}
