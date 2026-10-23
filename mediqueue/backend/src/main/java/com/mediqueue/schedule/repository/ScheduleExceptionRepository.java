package com.mediqueue.schedule.repository;

import com.mediqueue.schedule.entity.ScheduleException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ScheduleExceptionRepository extends JpaRepository<ScheduleException, Long> {

    List<ScheduleException> findByDoctorId(Long doctorId);

    List<ScheduleException> findByDoctorIdAndExceptionDate(Long doctorId, LocalDate exceptionDate);

    @Query("SELECT e FROM ScheduleException e WHERE e.doctor.id = :doctorId " +
           "AND e.exceptionDate BETWEEN :startDate AND :endDate ORDER BY e.exceptionDate ASC")
    List<ScheduleException> findByDoctorIdAndDateRange(
            @Param("doctorId") Long doctorId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
