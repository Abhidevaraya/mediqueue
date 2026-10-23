package com.mediqueue.appointment.repository;

import com.mediqueue.appointment.entity.Appointment;
import com.mediqueue.appointment.enums.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Query("SELECT a FROM Appointment a JOIN FETCH a.patient p JOIN FETCH p.user JOIN FETCH a.doctor d JOIN FETCH d.user JOIN FETCH a.department WHERE a.appointmentReference = :reference")
    Optional<Appointment> findByAppointmentReference(@Param("reference") String reference);

    @Query(value = "SELECT a FROM Appointment a JOIN FETCH a.patient p JOIN FETCH p.user JOIN FETCH a.doctor d JOIN FETCH d.user JOIN FETCH a.department " +
                   "WHERE p.id = :patientId " +
                   "AND (:status IS NULL OR a.status = :status)",
           countQuery = "SELECT count(a) FROM Appointment a WHERE a.patient.id = :patientId AND (:status IS NULL OR a.status = :status)")
    Page<Appointment> findByPatientIdAndStatus(
            @Param("patientId") Long patientId,
            @Param("status") AppointmentStatus status,
            Pageable pageable
    );

    @Query("SELECT a FROM Appointment a JOIN FETCH a.patient p JOIN FETCH p.user JOIN FETCH a.doctor d JOIN FETCH d.user JOIN FETCH a.department " +
           "WHERE d.id = :doctorId " +
           "AND a.startDateTime >= :startOfDay AND a.startDateTime <= :endOfDay " +
           "ORDER BY a.startDateTime ASC")
    List<Appointment> findDoctorDailyAppointments(
            @Param("doctorId") Long doctorId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.doctor.id = :doctorId " +
           "AND a.startDateTime = :startDateTime " +
           "AND a.status IN :activeStatuses")
    int countActiveBookings(
            @Param("doctorId") Long doctorId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("activeStatuses") Collection<AppointmentStatus> activeStatuses
    );

    @Query("SELECT COUNT(a) > 0 FROM Appointment a WHERE a.patient.id = :patientId " +
           "AND a.status IN :activeStatuses " +
           "AND (:excludeId IS NULL OR a.id != :excludeId) " +
           "AND (a.startDateTime < :endTime AND a.endDateTime > :startTime)")
    boolean hasConflictingActiveAppointment(
            @Param("patientId") Long patientId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("activeStatuses") Collection<AppointmentStatus> activeStatuses,
            @Param("excludeId") Long excludeId
    );

    @Query(value = "SELECT a FROM Appointment a JOIN FETCH a.patient p JOIN FETCH p.user pu JOIN FETCH a.doctor d JOIN FETCH d.user du JOIN FETCH a.department dept " +
                   "WHERE (:doctorId IS NULL OR d.id = :doctorId) " +
                   "AND (:patientId IS NULL OR p.id = :patientId) " +
                   "AND (:departmentId IS NULL OR dept.id = :departmentId) " +
                   "AND (:status IS NULL OR a.status = :status) " +
                   "AND (:startFrom IS NULL OR a.startDateTime >= :startFrom) " +
                   "AND (:startTo IS NULL OR a.startDateTime <= :startTo)",
           countQuery = "SELECT count(a) FROM Appointment a " +
                        "WHERE (:doctorId IS NULL OR a.doctor.id = :doctorId) " +
                        "AND (:patientId IS NULL OR a.patient.id = :patientId) " +
                        "AND (:departmentId IS NULL OR a.department.id = :departmentId) " +
                        "AND (:status IS NULL OR a.status = :status) " +
                        "AND (:startFrom IS NULL OR a.startDateTime >= :startFrom) " +
                        "AND (:startTo IS NULL OR a.startDateTime <= :startTo)")
    Page<Appointment> findAdminAppointments(
            @Param("doctorId") Long doctorId,
            @Param("patientId") Long patientId,
            @Param("departmentId") Long departmentId,
            @Param("status") AppointmentStatus status,
            @Param("startFrom") LocalDateTime startFrom,
            @Param("startTo") LocalDateTime startTo,
            Pageable pageable
    );

    long countByStartDateTimeBetween(LocalDateTime start, LocalDateTime end);

    long countByStartDateTimeBetweenAndStatus(LocalDateTime start, LocalDateTime end, AppointmentStatus status);

    @Query("SELECT a FROM Appointment a " +
           "WHERE a.startDateTime >= :start AND a.startDateTime <= :end " +
           "AND (:doctorId IS NULL OR a.doctor.id = :doctorId) " +
           "AND (:departmentId IS NULL OR a.department.id = :departmentId)")
    List<Appointment> findAppointmentsForAnalytics(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("doctorId") Long doctorId,
            @Param("departmentId") Long departmentId
    );
}
