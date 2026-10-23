package com.mediqueue.schedule;

import com.mediqueue.appointment.repository.AppointmentRepository;
import com.mediqueue.doctor.entity.Doctor;
import com.mediqueue.doctor.enums.DoctorVerificationStatus;
import com.mediqueue.doctor.repository.DoctorRepository;
import com.mediqueue.schedule.dto.SlotResponse;
import com.mediqueue.schedule.entity.DoctorSchedule;
import com.mediqueue.schedule.entity.ScheduleException;
import com.mediqueue.schedule.enums.ScheduleExceptionType;
import com.mediqueue.schedule.repository.DoctorScheduleRepository;
import com.mediqueue.schedule.repository.ScheduleExceptionRepository;
import com.mediqueue.schedule.service.AvailabilityService;
import com.mediqueue.user.entity.User;
import com.mediqueue.user.enums.AccountStatus;
import com.mediqueue.user.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private DoctorScheduleRepository doctorScheduleRepository;

    @Mock
    private ScheduleExceptionRepository scheduleExceptionRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @InjectMocks
    private AvailabilityService availabilityService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(availabilityService, "hospitalTimezone", "Asia/Kolkata");
    }

    private Doctor createSampleDoctor() {
        User user = User.builder()
                .id(10L)
                .fullName("Dr. Gregory House")
                .role(UserRole.DOCTOR)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        return Doctor.builder()
                .id(1L)
                .user(user)
                .verificationStatus(DoctorVerificationStatus.VERIFIED)
                .build();
    }

    @Test
    @DisplayName("Generate slots excluding break interval")
    void testGenerateSlotsExcludingBreak() {
        Doctor doctor = createSampleDoctor();
        // A future date
        LocalDate futureDate = LocalDate.now().plusDays(10);
        DayOfWeek dayOfWeek = futureDate.getDayOfWeek();

        DoctorSchedule schedule = DoctorSchedule.builder()
                .id(1L)
                .doctor(doctor)
                .dayOfWeek(dayOfWeek)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(12, 0))
                .slotDurationMinutes(60)
                .slotCapacity(1)
                .breakStartTime(LocalTime.of(10, 0))
                .breakEndTime(LocalTime.of(11, 0))
                .active(true)
                .build();

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(scheduleExceptionRepository.findByDoctorIdAndExceptionDate(1L, futureDate))
                .thenReturn(Collections.emptyList());
        when(doctorScheduleRepository.findByDoctorIdAndDayOfWeekAndActiveTrue(1L, dayOfWeek))
                .thenReturn(List.of(schedule));
        when(appointmentRepository.countActiveBookings(eq(1L), any(), any())).thenReturn(0);

        List<SlotResponse> slots = availabilityService.generateSlots(1L, futureDate);

        // Expected slots: 09:00-10:00, (10:00-11:00 break skipped), 11:00-12:00 -> exactly 2 slots
        assertEquals(2, slots.size());
        assertEquals(LocalTime.of(9, 0), slots.get(0).getStartDateTime().toLocalTime());
        assertEquals(LocalTime.of(11, 0), slots.get(1).getStartDateTime().toLocalTime());
    }

    @Test
    @DisplayName("Full day leave returns zero available slots")
    void testFullDayLeaveReturnsZeroSlots() {
        Doctor doctor = createSampleDoctor();
        LocalDate futureDate = LocalDate.now().plusDays(5);

        ScheduleException leave = ScheduleException.builder()
                .doctor(doctor)
                .exceptionDate(futureDate)
                .exceptionType(ScheduleExceptionType.FULL_DAY_LEAVE)
                .build();

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(scheduleExceptionRepository.findByDoctorIdAndExceptionDate(1L, futureDate))
                .thenReturn(List.of(leave));

        List<SlotResponse> slots = availabilityService.generateSlots(1L, futureDate);
        assertTrue(slots.isEmpty());
    }

    @Test
    @DisplayName("Partial unavailability exception removes overlapping slots")
    void testPartialUnavailabilityException() {
        Doctor doctor = createSampleDoctor();
        LocalDate futureDate = LocalDate.now().plusDays(7);
        DayOfWeek dayOfWeek = futureDate.getDayOfWeek();

        DoctorSchedule schedule = DoctorSchedule.builder()
                .id(1L)
                .doctor(doctor)
                .dayOfWeek(dayOfWeek)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(12, 0))
                .slotDurationMinutes(60)
                .slotCapacity(1)
                .active(true)
                .build();

        ScheduleException partialLeave = ScheduleException.builder()
                .doctor(doctor)
                .exceptionDate(futureDate)
                .exceptionType(ScheduleExceptionType.PARTIAL_UNAVAILABILITY)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .build();

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(scheduleExceptionRepository.findByDoctorIdAndExceptionDate(1L, futureDate))
                .thenReturn(List.of(partialLeave));
        when(doctorScheduleRepository.findByDoctorIdAndDayOfWeekAndActiveTrue(1L, dayOfWeek))
                .thenReturn(List.of(schedule));
        when(appointmentRepository.countActiveBookings(eq(1L), any(), any())).thenReturn(0);

        List<SlotResponse> slots = availabilityService.generateSlots(1L, futureDate);

        // 09:00-10:00 (present), 10:00-11:00 (excluded), 11:00-12:00 (present)
        assertEquals(2, slots.size());
        assertEquals(LocalTime.of(9, 0), slots.get(0).getStartDateTime().toLocalTime());
        assertEquals(LocalTime.of(11, 0), slots.get(1).getStartDateTime().toLocalTime());
    }

    @Test
    @DisplayName("Custom hours exception overrides regular weekly schedule")
    void testCustomHoursOverridesSchedule() {
        Doctor doctor = createSampleDoctor();
        LocalDate futureDate = LocalDate.now().plusDays(8);

        ScheduleException customHours = ScheduleException.builder()
                .doctor(doctor)
                .exceptionDate(futureDate)
                .exceptionType(ScheduleExceptionType.CUSTOM_HOURS)
                .startTime(LocalTime.of(14, 0))
                .endTime(LocalTime.of(16, 0))
                .slotDurationMinutes(60)
                .slotCapacity(2)
                .build();

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(scheduleExceptionRepository.findByDoctorIdAndExceptionDate(1L, futureDate))
                .thenReturn(List.of(customHours));
        when(appointmentRepository.countActiveBookings(eq(1L), any(), any())).thenReturn(1);

        List<SlotResponse> slots = availabilityService.generateSlots(1L, futureDate);

        // 14:00-15:00 and 15:00-16:00
        assertEquals(2, slots.size());
        assertEquals(LocalTime.of(14, 0), slots.get(0).getStartDateTime().toLocalTime());
        assertEquals(2, slots.get(0).getTotalCapacity());
        assertEquals(1, slots.get(0).getBookedCount());
        assertEquals(1, slots.get(0).getRemainingCapacity());
        assertTrue(slots.get(0).isAvailable());
    }
}
