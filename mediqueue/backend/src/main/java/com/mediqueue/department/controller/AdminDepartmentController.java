package com.mediqueue.department.controller;

import com.mediqueue.common.ApiResponse;
import com.mediqueue.common.PageResponse;
import com.mediqueue.department.dto.DepartmentCreateRequest;
import com.mediqueue.department.dto.DepartmentResponse;
import com.mediqueue.department.dto.DepartmentStatusRequest;
import com.mediqueue.department.dto.DepartmentUpdateRequest;
import com.mediqueue.department.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/departments")
@RequiredArgsConstructor
@Tag(name = "Departments (Admin)", description = "Administrative endpoints for hospital department management")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    @Operation(summary = "List all departments (admin)", description = "Retrieves all departments including inactive ones with pagination.")
    public ResponseEntity<ApiResponse<PageResponse<DepartmentResponse>>> getAllDepartments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int boundedSize = Math.min(Math.max(1, size), 100);
        int validPage = Math.max(0, page);
        Pageable pageable = PageRequest.of(validPage, boundedSize, Sort.by("id").ascending());
        PageResponse<DepartmentResponse> response = departmentService.getAllDepartments(pageable);
        return ResponseEntity.ok(ApiResponse.ok("Departments retrieved successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get department by ID (admin)", description = "Retrieves any department by its ID.")
    public ResponseEntity<ApiResponse<DepartmentResponse>> getDepartmentById(@PathVariable Long id) {
        DepartmentResponse response = departmentService.getDepartmentById(id);
        return ResponseEntity.ok(ApiResponse.ok("Department retrieved successfully", response));
    }

    @PostMapping
    @Operation(summary = "Create department", description = "Creates a new hospital department with unique name.")
    public ResponseEntity<ApiResponse<DepartmentResponse>> createDepartment(
            @Valid @RequestBody DepartmentCreateRequest request
    ) {
        DepartmentResponse response = departmentService.createDepartment(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Department created successfully", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update department", description = "Updates details of an existing hospital department.")
    public ResponseEntity<ApiResponse<DepartmentResponse>> updateDepartment(
            @PathVariable Long id,
            @Valid @RequestBody DepartmentUpdateRequest request
    ) {
        DepartmentResponse response = departmentService.updateDepartment(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Department updated successfully", response));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update department status", description = "Activates or deactivates a hospital department.")
    public ResponseEntity<ApiResponse<DepartmentResponse>> updateDepartmentStatus(
            @PathVariable Long id,
            @Valid @RequestBody DepartmentStatusRequest request
    ) {
        DepartmentResponse response = departmentService.updateDepartmentStatus(id, request.getActive());
        return ResponseEntity.ok(ApiResponse.ok("Department status updated successfully", response));
    }
}
