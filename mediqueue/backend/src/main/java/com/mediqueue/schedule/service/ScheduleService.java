package com.mediqueue.schedule.service;

import com.mediqueue.doctor.entity.Doctor;
import com.mediqueue.doctor.enums.DoctorVerificationStatus;
import com.mediqueue.doctor.repository.DoctorRepository;
import com.mediqueue.exception.ConflictException;
import com.mediqueue.exception.ResourceNotFoundException;
import com.mediqueue.schedule.dto.DoctorScheduleCreateRequest;
import com.mediqueue.schedule.dto.DoctorScheduleResponse;
import com.mediqueue.schedule.dto.DoctorScheduleUpdateRequest;
import com.mediqueue.schedule.dto.ScheduleExceptionCreateRequest;
import com.mediqueue.schedule.dto.ScheduleExceptionResponse;
import com.mediqueue.schedule.entity.DoctorSchedule;
import com.mediqueue.schedule.entity.ScheduleException;
import com.mediqueue.schedule.enums.ScheduleExceptionType;
import com.mediqueue.schedule.repository.DoctorScheduleRepository;
import com.mediqueue.schedule.repository.ScheduleExceptionRepository;
import com.mediqueue.security.AuthenticatedUser;
import com.mediqueue.user.enums.AccountStatus;
import com.mediqueue.user.enums.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final DoctorScheduleRepository doctorScheduleRepository;
    private final ScheduleExceptionRepository scheduleExceptionRepository;
    private final DoctorRepository doctorRepository;

    @Transactional
    public DoctorScheduleResponse createSchedule(AuthenticatedUser principal, DoctorScheduleCreateRequest request) {
        Doctor doctor = resolveDoctorForPrincipal(principal, request.getDoctorId());
        validateWorkingHoursAndBreaks(request.getStartTime(), request.getEndTime(),
                request.getSlotDurationMinutes(), request.getBreakStartTime(), request.getBreakEndTime());

        List<DoctorSchedule> overlapping = doctorScheduleRepository.findOverlappingSchedules(
                doctor.getId(),
                request.getDayOfWeek(),
                request.getStartTime(),
                request.getEndTime(),
                null
        );

        if (!overlapping.isEmpty()) {
            throw new ConflictException("Doctor already has an active schedule overlapping with this time interval on " + request.getDayOfWeek());
        }

        DoctorSchedule schedule = DoctorSchedule.builder()
                .doctor(doctor)
                .dayOfWeek(request.getDayOfWeek())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .slotDurationMinutes(request.getSlotDurationMinutes())
                .slotCapacity(request.getSlotCapacity())
                .breakStartTime(request.getBreakStartTime())
                .breakEndTime(request.getBreakEndTime())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        DoctorSchedule saved = doctorScheduleRepository.save(schedule);
        return DoctorScheduleResponse.from(saved);
    }

    @Transactional
    public DoctorScheduleResponse updateSchedule(AuthenticatedUser principal, Long scheduleId, DoctorScheduleUpdateRequest request) {
        DoctorSchedule schedule = doctorScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with ID: " + scheduleId));

        validateDoctorOwnership(principal, schedule.getDoctor());
        validateWorkingHoursAndBreaks(request.getStartTime(), request.getEndTime(),
                request.getSlotDurationMinutes(), request.getBreakStartTime(), request.getBreakEndTime());

        List<DoctorSchedule> overlapping = doctorScheduleRepository.findOverlappingSchedules(
                schedule.getDoctor().getId(),
                request.getDayOfWeek(),
                request.getStartTime(),
                request.getEndTime(),
                scheduleId
        );

        if (!overlapping.isEmpty()) {
            throw new ConflictException("Another active schedule overlaps with this time interval on " + request.getDayOfWeek());
        }

        schedule.setDayOfWeek(request.getDayOfWeek());
        schedule.setStartTime(request.getStartTime());
        schedule.setEndTime(request.getEndTime());
        schedule.setSlotDurationMinutes(request.getSlotDurationMinutes());
        schedule.setSlotCapacity(request.getSlotCapacity());
        schedule.setBreakStartTime(request.getBreakStartTime());
        schedule.setBreakEndTime(request.getBreakEndTime());
        if (request.getActive() != null) {
            schedule.setActive(request.getActive());
        }

        DoctorSchedule updated = doctorScheduleRepository.save(schedule);
        return DoctorScheduleResponse.from(updated);
    }

    @Transactional(readOnly = true)
    public List<DoctorScheduleResponse> getDoctorSchedules(Long doctorId) {
        List<DoctorSchedule> schedules = doctorScheduleRepository.findByDoctorIdAndActiveTrue(doctorId);
        return schedules.stream().map(DoctorScheduleResponse::from).toList();
    }

    @Transactional
    public ScheduleExceptionResponse createException(AuthenticatedUser principal, ScheduleExceptionCreateRequest request) {
        Doctor doctor = resolveDoctorForPrincipal(principal, request.getDoctorId());

        if (request.getExceptionType() == ScheduleExceptionType.PARTIAL_UNAVAILABILITY ||
                request.getExceptionType() == ScheduleExceptionType.CUSTOM_HOURS) {
            if (request.getStartTime() == null || request.getEndTime() == null) {
                throw new ConflictException("Start and end times are required for partial unavailability or custom hours exceptions");
            }
            if (!request.getStartTime().isBefore(request.getEndTime())) {
                throw new ConflictException("Exception start time must be before end time");
            }
        }

        ScheduleException exception = ScheduleException.builder()
                .doctor(doctor)
                .exceptionDate(request.getExceptionDate())
                .exceptionType(request.getExceptionType())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .slotDurationMinutes(request.getSlotDurationMinutes())
                .slotCapacity(request.getSlotCapacity())
                .reason(request.getReason() != null ? request.getReason().trim() : null)
                .build();

        ScheduleException saved = scheduleExceptionRepository.save(exception);
        return ScheduleExceptionResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ScheduleExceptionResponse> getExceptions(AuthenticatedUser principal, Long doctorId) {
        Doctor doctor = resolveDoctorForPrincipal(principal, doctorId);
        List<ScheduleException> list = scheduleExceptionRepository.findByDoctorId(doctor.getId());
        return list.stream().map(ScheduleExceptionResponse::from).toList();
    }

    private void validateWorkingHoursAndBreaks(LocalTime startTime, LocalTime endTime, int slotDurationMinutes,
                                               LocalTime breakStart, LocalTime breakEnd) {
        if (!startTime.isBefore(endTime)) {
            throw new ConflictException("Working start time must be strictly before end time");
        }

        long workingMinutes = java.time.Duration.between(startTime, endTime).toMinutes();
        if (slotDurationMinutes <= 0 || slotDurationMinutes > workingMinutes) {
            throw new ConflictException("Slot duration must be positive and fit within working interval");
        }

        if (breakStart != null || breakEnd != null) {
            if (breakStart == null || breakEnd == null) {
                throw new ConflictException("Both break start and end times must be provided together");
            }
            if (!breakStart.isBefore(breakEnd)) {
                throw new ConflictException("Break start time must be strictly before break end time");
            }
            if (breakStart.isBefore(startTime) || breakEnd.isAfter(endTime)) {
                throw new ConflictException("Break interval must be fully contained within working hours");
            }
        }
    }

    private Doctor resolveDoctorForPrincipal(AuthenticatedUser principal, Long requestedDoctorId) {
        if (principal.getRole() == UserRole.ADMIN) {
            if (requestedDoctorId == null) {
                throw new ConflictException("Doctor ID must be specified by administrator");
            }
            return doctorRepository.findById(requestedDoctorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + requestedDoctorId));
        }

        if (principal.getRole() == UserRole.DOCTOR) {
            Doctor doctor = doctorRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("No doctor profile associated with authenticated user"));

            if (requestedDoctorId != null && !doctor.getId().equals(requestedDoctorId)) {
                throw new AccessDeniedException("Doctors can only manage their own schedules");
            }

            if (doctor.getUser().getAccountStatus() != AccountStatus.ACTIVE) {
                throw new AccessDeniedException("Doctor account is not active");
            }
            return doctor;
        }

        throw new AccessDeniedException("Only doctors and administrators may manage schedules");
    }

    private void validateDoctorOwnership(AuthenticatedUser principal, Doctor doctor) {
        if (principal.getRole() == UserRole.ADMIN) {
            return;
        }
        if (principal.getRole() == UserRole.DOCTOR) {
            Doctor currentDoc = doctorRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Doctor profile not found"));
            if (!currentDoc.getId().equals(doctor.getId())) {
                throw new AccessDeniedException("Doctors can only modify their own schedules");
            }
            return;
        }
        throw new AccessDeniedException("Access denied: insufficient schedule management permissions");
    }
}
