package com.mediqueue.schedule.service;

import com.mediqueue.appointment.enums.AppointmentStatus;
import com.mediqueue.appointment.repository.AppointmentRepository;
import com.mediqueue.doctor.entity.Doctor;
import com.mediqueue.doctor.enums.DoctorVerificationStatus;
import com.mediqueue.doctor.repository.DoctorRepository;
import com.mediqueue.exception.BusinessException;
import com.mediqueue.exception.ResourceNotFoundException;
import com.mediqueue.schedule.dto.SlotResponse;
import com.mediqueue.schedule.entity.DoctorSchedule;
import com.mediqueue.schedule.entity.ScheduleException;
import com.mediqueue.schedule.enums.ScheduleExceptionType;
import com.mediqueue.schedule.repository.DoctorScheduleRepository;
import com.mediqueue.schedule.repository.ScheduleExceptionRepository;
import com.mediqueue.user.enums.AccountStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final DoctorRepository doctorRepository;
    private final DoctorScheduleRepository doctorScheduleRepository;
    private final ScheduleExceptionRepository scheduleExceptionRepository;
    private final AppointmentRepository appointmentRepository;

    @Value("${mediqueue.hospital.timezone:Asia/Kolkata}")
    private String hospitalTimezone;

    public ZoneId getHospitalZoneId() {
        return ZoneId.of(hospitalTimezone);
    }

    public ZonedDateTime nowInHospitalTime() {
        return ZonedDateTime.now(getHospitalZoneId());
    }

    @Transactional(readOnly = true)
    public List<SlotResponse> generateSlots(Long doctorId, LocalDate date) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + doctorId));

        if (doctor.getVerificationStatus() != DoctorVerificationStatus.VERIFIED ||
                doctor.getUser().getAccountStatus() != AccountStatus.ACTIVE) {
            return Collections.emptyList();
        }

        LocalDate todayInHospital = nowInHospitalTime().toLocalDate();
        LocalTime nowTimeInHospital = nowInHospitalTime().toLocalTime();

        if (date.isBefore(todayInHospital)) {
            return Collections.emptyList();
        }

        List<ScheduleException> exceptions = scheduleExceptionRepository.findByDoctorIdAndExceptionDate(doctorId, date);

        // Precedence Rule 1: Full-day leave or holiday -> 0 slots
        for (ScheduleException ex : exceptions) {
            if (ex.getExceptionType() == ScheduleExceptionType.FULL_DAY_LEAVE ||
                    ex.getExceptionType() == ScheduleExceptionType.HOLIDAY) {
                return Collections.emptyList();
            }
        }

        // Check for Custom Hours Exception
        ScheduleException customHoursEx = exceptions.stream()
                .filter(e -> e.getExceptionType() == ScheduleExceptionType.CUSTOM_HOURS)
                .findFirst()
                .orElse(null);

        List<ScheduleInterval> intervalsToGenerate = new ArrayList<>();

        if (customHoursEx != null) {
            intervalsToGenerate.add(new ScheduleInterval(
                    customHoursEx.getStartTime(),
                    customHoursEx.getEndTime(),
                    customHoursEx.getSlotDurationMinutes() != null ? customHoursEx.getSlotDurationMinutes() : 15,
                    customHoursEx.getSlotCapacity() != null ? customHoursEx.getSlotCapacity() : 1,
                    null,
                    null
            ));
        } else {
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            List<DoctorSchedule> weeklySchedules = doctorScheduleRepository.findByDoctorIdAndDayOfWeekAndActiveTrue(doctorId, dayOfWeek);
            for (DoctorSchedule s : weeklySchedules) {
                intervalsToGenerate.add(new ScheduleInterval(
                        s.getStartTime(),
                        s.getEndTime(),
                        s.getSlotDurationMinutes(),
                        s.getSlotCapacity(),
                        s.getBreakStartTime(),
                        s.getBreakEndTime()
                ));
            }
        }

        List<ScheduleException> partialExceptions = exceptions.stream()
                .filter(e -> e.getExceptionType() == ScheduleExceptionType.PARTIAL_UNAVAILABILITY)
                .toList();

        List<SlotResponse> slots = new ArrayList<>();
        Set<AppointmentStatus> activeStatuses = Set.of(
                AppointmentStatus.BOOKED,
                AppointmentStatus.CHECKED_IN,
                AppointmentStatus.IN_PROGRESS
        );

        for (ScheduleInterval interval : intervalsToGenerate) {
            LocalTime current = interval.startTime();
            int duration = interval.durationMinutes();
            int capacity = interval.capacity();

            while (!current.plusMinutes(duration).isAfter(interval.endTime())) {
                LocalTime slotStart = current;
                LocalTime slotEnd = current.plusMinutes(duration);
                current = slotEnd;

                // Check break time
                if (interval.breakStart() != null && interval.breakEnd() != null) {
                    if (slotStart.isBefore(interval.breakEnd()) && slotEnd.isAfter(interval.breakStart())) {
                        continue; // Overlaps break, skip slot
                    }
                }

                // Check partial unavailability exceptions
                boolean overlapsPartialException = false;
                for (ScheduleException pEx : partialExceptions) {
                    if (pEx.getStartTime() != null && pEx.getEndTime() != null) {
                        if (slotStart.isBefore(pEx.getEndTime()) && slotEnd.isAfter(pEx.getStartTime())) {
                            overlapsPartialException = true;
                            break;
                        }
                    }
                }
                if (overlapsPartialException) {
                    continue;
                }

                // If date is today, exclude past slots
                if (date.isEqual(todayInHospital) && !slotStart.isAfter(nowTimeInHospital)) {
                    continue;
                }

                LocalDateTime startDateTime = LocalDateTime.of(date, slotStart);
                LocalDateTime endDateTime = LocalDateTime.of(date, slotEnd);

                int bookedCount = appointmentRepository.countActiveBookings(doctorId, startDateTime, activeStatuses);
                int remainingCapacity = Math.max(0, capacity - bookedCount);

                slots.add(SlotResponse.builder()
                        .startDateTime(startDateTime)
                        .endDateTime(endDateTime)
                        .slotDurationMinutes(duration)
                        .totalCapacity(capacity)
                        .bookedCount(bookedCount)
                        .remainingCapacity(remainingCapacity)
                        .available(remainingCapacity > 0)
                        .build());
            }
        }

        slots.sort(Comparator.comparing(SlotResponse::getStartDateTime));
        return slots;
    }

    @Transactional(readOnly = true)
    public SlotTemplate validateAndResolveSlotTemplate(Doctor doctor, LocalDateTime startDateTime) {
        LocalDate date = startDateTime.toLocalDate();
        LocalTime time = startDateTime.toLocalTime();

        List<SlotResponse> slots = generateSlots(doctor.getId(), date);
        for (SlotResponse s : slots) {
            if (s.getStartDateTime().equals(startDateTime)) {
                return new SlotTemplate(s.getStartDateTime(), s.getEndDateTime(), s.getSlotDurationMinutes(), s.getTotalCapacity());
            }
        }

        throw new BusinessException("The requested appointment slot at " + startDateTime + " is not an available working slot for Doctor " + doctor.getUser().getFullName());
    }

    public record SlotTemplate(LocalDateTime startDateTime, LocalDateTime endDateTime, int durationMinutes, int capacity) {}

    private record ScheduleInterval(
            LocalTime startTime,
            LocalTime endTime,
            int durationMinutes,
            int capacity,
            LocalTime breakStart,
            LocalTime breakEnd
    ) {}
}
