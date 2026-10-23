package com.mediqueue.schedule.controller;

import com.mediqueue.common.ApiResponse;
import com.mediqueue.schedule.dto.DoctorScheduleCreateRequest;
import com.mediqueue.schedule.dto.DoctorScheduleResponse;
import com.mediqueue.schedule.dto.DoctorScheduleUpdateRequest;
import com.mediqueue.schedule.dto.ScheduleExceptionCreateRequest;
import com.mediqueue.schedule.dto.ScheduleExceptionResponse;
import com.mediqueue.schedule.dto.SlotResponse;
import com.mediqueue.schedule.service.AvailabilityService;
import com.mediqueue.schedule.service.ScheduleService;
import com.mediqueue.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Doctor Schedules & Availability", description = "Endpoints for managing weekly schedules, schedule exceptions, and querying available appointment slots")
public class DoctorScheduleController {

    private final ScheduleService scheduleService;
    private final AvailabilityService availabilityService;

    @GetMapping("/api/v1/doctors/{id}/slots")
    @Operation(summary = "Get available doctor appointment slots", description = "Public endpoint to query available working slots for a verified doctor on a specified date.")
    public ResponseEntity<ApiResponse<List<SlotResponse>>> getAvailableSlots(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<SlotResponse> slots = availabilityService.generateSlots(id, date);
        return ResponseEntity.ok(ApiResponse.ok("Available slots retrieved successfully", slots));
    }

    @GetMapping("/api/v1/doctors/{id}/schedules")
    @Operation(summary = "Get published doctor weekly schedule", description = "Public endpoint returning published active weekly schedules for a doctor.")
    public ResponseEntity<ApiResponse<List<DoctorScheduleResponse>>> getDoctorSchedules(@PathVariable Long id) {
        List<DoctorScheduleResponse> schedules = scheduleService.getDoctorSchedules(id);
        return ResponseEntity.ok(ApiResponse.ok("Doctor weekly schedules retrieved successfully", schedules));
    }

    @PostMapping("/api/v1/doctor/schedules")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    @Operation(summary = "Create weekly schedule", description = "Doctor or Administrator creates a working day schedule interval.")
    public ResponseEntity<ApiResponse<DoctorScheduleResponse>> createSchedule(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody DoctorScheduleCreateRequest request
    ) {
        DoctorScheduleResponse response = scheduleService.createSchedule(principal, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Doctor schedule created successfully", response));
    }

    @PutMapping("/api/v1/doctor/schedules/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    @Operation(summary = "Update weekly schedule", description = "Doctor owner or Administrator updates an existing weekly schedule interval.")
    public ResponseEntity<ApiResponse<DoctorScheduleResponse>> updateSchedule(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id,
            @Valid @RequestBody DoctorScheduleUpdateRequest request
    ) {
        DoctorScheduleResponse response = scheduleService.updateSchedule(principal, id, request);
        return ResponseEntity.ok(ApiResponse.ok("Doctor schedule updated successfully", response));
    }

    @PostMapping("/api/v1/doctor/schedules/exceptions")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    @Operation(summary = "Create schedule exception", description = "Records doctor leave, holiday, partial unavailability, or custom hours for a date.")
    public ResponseEntity<ApiResponse<ScheduleExceptionResponse>> createException(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody ScheduleExceptionCreateRequest request
    ) {
        ScheduleExceptionResponse response = scheduleService.createException(principal, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Schedule exception created successfully", response));
    }

    @GetMapping("/api/v1/doctor/schedules/exceptions")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    @Operation(summary = "List schedule exceptions", description = "Lists exceptions recorded for a doctor.")
    public ResponseEntity<ApiResponse<List<ScheduleExceptionResponse>>> getExceptions(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) Long doctorId
    ) {
        List<ScheduleExceptionResponse> response = scheduleService.getExceptions(principal, doctorId);
        return ResponseEntity.ok(ApiResponse.ok("Schedule exceptions retrieved successfully", response));
    }
}
