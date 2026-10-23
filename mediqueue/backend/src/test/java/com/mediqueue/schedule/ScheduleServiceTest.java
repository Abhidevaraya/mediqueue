package com.mediqueue.schedule;

import com.mediqueue.doctor.entity.Doctor;
import com.mediqueue.doctor.repository.DoctorRepository;
import com.mediqueue.exception.ConflictException;
import com.mediqueue.schedule.dto.DoctorScheduleCreateRequest;
import com.mediqueue.schedule.dto.DoctorScheduleResponse;
import com.mediqueue.schedule.dto.ScheduleExceptionCreateRequest;
import com.mediqueue.schedule.dto.ScheduleExceptionResponse;
import com.mediqueue.schedule.entity.DoctorSchedule;
import com.mediqueue.schedule.entity.ScheduleException;
import com.mediqueue.schedule.enums.ScheduleExceptionType;
import com.mediqueue.schedule.repository.DoctorScheduleRepository;
import com.mediqueue.schedule.repository.ScheduleExceptionRepository;
import com.mediqueue.schedule.service.ScheduleService;
import com.mediqueue.security.AuthenticatedUser;
import com.mediqueue.user.entity.User;
import com.mediqueue.user.enums.AccountStatus;
import com.mediqueue.user.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private DoctorScheduleRepository doctorScheduleRepository;

    @Mock
    private ScheduleExceptionRepository scheduleExceptionRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @InjectMocks
    private ScheduleService scheduleService;

    private Doctor createSampleDoctor(Long doctorId, Long userId) {
        User user = User.builder()
                .id(userId)
                .fullName("Dr. Gregory House")
                .email("house@mediqueue.com")
                .role(UserRole.DOCTOR)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        return Doctor.builder()
                .id(doctorId)
                .user(user)
                .specialty("Diagnostics")
                .build();
    }

    @Test
    @DisplayName("Doctor successfully creates a weekly schedule")
    void testCreateScheduleSuccess() {
        Doctor doctor = createSampleDoctor(1L, 10L);
        AuthenticatedUser principal = new AuthenticatedUser(doctor.getUser());

        DoctorScheduleCreateRequest request = DoctorScheduleCreateRequest.builder()
                .doctorId(1L)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(17, 0))
                .slotDurationMinutes(30)
                .slotCapacity(1)
                .breakStartTime(LocalTime.of(12, 0))
                .breakEndTime(LocalTime.of(13, 0))
                .build();

        when(doctorRepository.findByUserId(10L)).thenReturn(Optional.of(doctor));
        when(doctorScheduleRepository.findOverlappingSchedules(eq(1L), eq(DayOfWeek.MONDAY), any(), any(), isNull()))
                .thenReturn(Collections.emptyList());
        when(doctorScheduleRepository.save(any(DoctorSchedule.class))).thenAnswer(inv -> {
            DoctorSchedule s = inv.getArgument(0);
            s.setId(100L);
            return s;
        });

        DoctorScheduleResponse response = scheduleService.createSchedule(principal, request);

        assertNotNull(response);
        assertEquals(DayOfWeek.MONDAY, response.getDayOfWeek());
        assertEquals(LocalTime.of(9, 0), response.getStartTime());
        assertEquals(LocalTime.of(17, 0), response.getEndTime());
        verify(doctorScheduleRepository).save(any(DoctorSchedule.class));
    }

    @Test
    @DisplayName("Reject schedule when start time is not before end time")
    void testCreateScheduleInvalidTimes() {
        Doctor doctor = createSampleDoctor(1L, 10L);
        AuthenticatedUser principal = new AuthenticatedUser(doctor.getUser());

        DoctorScheduleCreateRequest request = DoctorScheduleCreateRequest.builder()
                .doctorId(1L)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(17, 0))
                .endTime(LocalTime.of(9, 0))
                .slotDurationMinutes(30)
                .build();

        when(doctorRepository.findByUserId(10L)).thenReturn(Optional.of(doctor));

        assertThrows(ConflictException.class, () -> scheduleService.createSchedule(principal, request));
        verify(doctorScheduleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Reject schedule when break is outside working interval")
    void testCreateScheduleBreakOutsideWorkingHours() {
        Doctor doctor = createSampleDoctor(1L, 10L);
        AuthenticatedUser principal = new AuthenticatedUser(doctor.getUser());

        DoctorScheduleCreateRequest request = DoctorScheduleCreateRequest.builder()
                .doctorId(1L)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(17, 0))
                .slotDurationMinutes(30)
                .breakStartTime(LocalTime.of(8, 0))
                .breakEndTime(LocalTime.of(10, 0))
                .build();

        when(doctorRepository.findByUserId(10L)).thenReturn(Optional.of(doctor));

        assertThrows(ConflictException.class, () -> scheduleService.createSchedule(principal, request));
    }

    @Test
    @DisplayName("Reject schedule when it overlaps with an existing active schedule")
    void testCreateScheduleOverlappingConflict() {
        Doctor doctor = createSampleDoctor(1L, 10L);
        AuthenticatedUser principal = new AuthenticatedUser(doctor.getUser());

        DoctorScheduleCreateRequest request = DoctorScheduleCreateRequest.builder()
                .doctorId(1L)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(13, 0))
                .slotDurationMinutes(30)
                .build();

        DoctorSchedule existing = DoctorSchedule.builder()
                .id(50L)
                .doctor(doctor)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(14, 0))
                .build();

        when(doctorRepository.findByUserId(10L)).thenReturn(Optional.of(doctor));
        when(doctorScheduleRepository.findOverlappingSchedules(eq(1L), eq(DayOfWeek.MONDAY), any(), any(), isNull()))
                .thenReturn(List.of(existing));

        assertThrows(ConflictException.class, () -> scheduleService.createSchedule(principal, request));
    }

    @Test
    @DisplayName("Doctor cannot modify schedule of another doctor")
    void testDoctorCannotManageOtherDoctorSchedule() {
        Doctor doctor1 = createSampleDoctor(1L, 10L);
        AuthenticatedUser principal = new AuthenticatedUser(doctor1.getUser());

        DoctorScheduleCreateRequest request = DoctorScheduleCreateRequest.builder()
                .doctorId(2L) // Attempting to create for doctor 2
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(17, 0))
                .build();

        when(doctorRepository.findByUserId(10L)).thenReturn(Optional.of(doctor1));

        assertThrows(AccessDeniedException.class, () -> scheduleService.createSchedule(principal, request));
    }

    @Test
    @DisplayName("Create schedule exception for holiday/leave")
    void testCreateScheduleException() {
        Doctor doctor = createSampleDoctor(1L, 10L);
        AuthenticatedUser principal = new AuthenticatedUser(doctor.getUser());

        ScheduleExceptionCreateRequest request = ScheduleExceptionCreateRequest.builder()
                .doctorId(1L)
                .exceptionDate(LocalDate.of(2026, 12, 25))
                .exceptionType(ScheduleExceptionType.HOLIDAY)
                .reason("Christmas Day")
                .build();

        when(doctorRepository.findByUserId(10L)).thenReturn(Optional.of(doctor));
        when(scheduleExceptionRepository.save(any(ScheduleException.class))).thenAnswer(inv -> {
            ScheduleException ex = inv.getArgument(0);
            ex.setId(200L);
            return ex;
        });

        ScheduleExceptionResponse response = scheduleService.createException(principal, request);
        assertNotNull(response);
        assertEquals(ScheduleExceptionType.HOLIDAY, response.getExceptionType());
        assertEquals("Christmas Day", response.getReason());
    }
}
