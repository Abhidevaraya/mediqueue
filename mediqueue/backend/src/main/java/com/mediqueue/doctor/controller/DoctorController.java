package com.mediqueue.doctor.controller;

import com.mediqueue.common.ApiResponse;
import com.mediqueue.common.PageResponse;
import com.mediqueue.doctor.dto.DoctorPublicResponse;
import com.mediqueue.doctor.service.DoctorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/doctors")
@RequiredArgsConstructor
@Tag(name = "Doctors (Public)", description = "Public read-only directory of verified doctors")
public class DoctorController {

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "id", "specialty", "qualification", "yearsOfExperience", "consultationFee"
    );

    private final DoctorService doctorService;

    @GetMapping
    @Operation(summary = "Search verified doctors", description = "Retrieves a paginated list of active, verified doctors with optional department and specialty filters.")
    public ResponseEntity<ApiResponse<PageResponse<DoctorPublicResponse>>> getPublicDoctors(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String specialty,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        int boundedSize = Math.min(Math.max(1, size), 100);
        int validPage = Math.max(0, page);

        String validatedSortBy = ALLOWED_SORT_PROPERTIES.contains(sortBy) ? sortBy : "id";
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(validPage, boundedSize, Sort.by(direction, validatedSortBy));
        PageResponse<DoctorPublicResponse> response = doctorService.getPublicDoctors(departmentId, specialty, pageable);

        return ResponseEntity.ok(ApiResponse.ok("Doctors retrieved successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get doctor details by ID", description = "Retrieves public details of a verified, active doctor by ID.")
    public ResponseEntity<ApiResponse<DoctorPublicResponse>> getPublicDoctorById(@PathVariable Long id) {
        DoctorPublicResponse response = doctorService.getPublicDoctorById(id);
        return ResponseEntity.ok(ApiResponse.ok("Doctor details retrieved successfully", response));
    }
}
