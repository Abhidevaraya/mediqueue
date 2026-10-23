package com.mediqueue.schedule.repository;

import com.mediqueue.schedule.entity.DoctorSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, Long> {

    List<DoctorSchedule> findByDoctorId(Long doctorId);

    List<DoctorSchedule> findByDoctorIdAndActiveTrue(Long doctorId);

    List<DoctorSchedule> findByDoctorDepartmentId(Long departmentId);

    List<DoctorSchedule> findByDoctorIdAndDayOfWeekAndActiveTrue(Long doctorId, DayOfWeek dayOfWeek);

    @Query("SELECT s FROM DoctorSchedule s WHERE s.doctor.id = :doctorId " +
           "AND s.dayOfWeek = :dayOfWeek AND s.active = true " +
           "AND (:excludeId IS NULL OR s.id != :excludeId) " +
           "AND (s.startTime < :endTime AND s.endTime > :startTime)")
    List<DoctorSchedule> findOverlappingSchedules(
            @Param("doctorId") Long doctorId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeId") Long excludeId
    );
}
