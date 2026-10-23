package com.mediqueue.schedule.controller;

import com.mediqueue.common.ApiResponse;
import com.mediqueue.schedule.dto.DoctorScheduleResponse;
import com.mediqueue.schedule.entity.DoctorSchedule;
import com.mediqueue.schedule.repository.DoctorScheduleRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.DayOfWeek;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/schedules")
@RequiredArgsConstructor
@Tag(name = "Schedules (Admin)", description = "Administrative oversight of doctor schedules across departments")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminScheduleController {

    private final DoctorScheduleRepository scheduleRepository;

    @GetMapping
    @Operation(summary = "Search schedules (admin)", description = "Retrieves doctor schedules filtered by doctor, department, and day of week.")
    public ResponseEntity<ApiResponse<List<DoctorScheduleResponse>>> getSchedules(
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) DayOfWeek dayOfWeek
    ) {
        List<DoctorSchedule> schedules;
        if (doctorId != null) {
            schedules = scheduleRepository.findByDoctorId(doctorId);
        } else if (departmentId != null) {
            schedules = scheduleRepository.findByDoctorDepartmentId(departmentId);
        } else {
            schedules = scheduleRepository.findAll();
        }

        if (dayOfWeek != null) {
            schedules = schedules.stream()
                    .filter(s -> s.getDayOfWeek() == dayOfWeek)
                    .toList();
        }

        List<DoctorScheduleResponse> responses = schedules.stream()
                .map(DoctorScheduleResponse::from)
                .toList();

        return ResponseEntity.ok(ApiResponse.ok("Schedules retrieved successfully", responses));
    }
}
