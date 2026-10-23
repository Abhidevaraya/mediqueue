package com.mediqueue.queue.repository;

import com.mediqueue.queue.entity.DoctorQueueSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DoctorQueueSessionRepository extends JpaRepository<DoctorQueueSession, Long> {

    Optional<DoctorQueueSession> findByDoctorIdAndQueueDate(Long doctorId, LocalDate queueDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM DoctorQueueSession s WHERE s.doctor.id = :doctorId AND s.queueDate = :queueDate")
    Optional<DoctorQueueSession> findByDoctorIdAndQueueDateWithLock(
            @Param("doctorId") Long doctorId,
            @Param("queueDate") LocalDate queueDate
    );

    @Modifying
    @Query(value = "INSERT IGNORE INTO doctor_queue_sessions (doctor_id, queue_date, last_token_number, is_paused, version, created_at, updated_at) " +
            "VALUES (:doctorId, :queueDate, 0, false, 0, NOW(6), NOW(6))", nativeQuery = true)
    int insertIgnoreDoctorQueueSession(
            @Param("doctorId") Long doctorId,
            @Param("queueDate") LocalDate queueDate
    );
}
