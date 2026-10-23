package com.mediqueue.department.service;

import com.mediqueue.common.PageResponse;
import com.mediqueue.department.dto.DepartmentCreateRequest;
import com.mediqueue.department.dto.DepartmentResponse;
import com.mediqueue.department.dto.DepartmentUpdateRequest;
import com.mediqueue.department.entity.Department;
import com.mediqueue.department.repository.DepartmentRepository;
import com.mediqueue.exception.ConflictException;
import com.mediqueue.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    @Transactional(readOnly = true)
    public PageResponse<DepartmentResponse> getPublicDepartments(Pageable pageable) {
        Page<Department> page = departmentRepository.findByActiveTrue(pageable);
        return PageResponse.of(page, page.getContent().stream().map(DepartmentResponse::from).toList());
    }

    @Transactional(readOnly = true)
    public DepartmentResponse getPublicDepartmentById(Long id) {
        Department department = departmentRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found or inactive with ID: " + id));
        return DepartmentResponse.from(department);
    }

    @Transactional(readOnly = true)
    public PageResponse<DepartmentResponse> getAllDepartments(Pageable pageable) {
        Page<Department> page = departmentRepository.findAll(pageable);
        return PageResponse.of(page, page.getContent().stream().map(DepartmentResponse::from).toList());
    }

    @Transactional(readOnly = true)
    public DepartmentResponse getDepartmentById(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + id));
        return DepartmentResponse.from(department);
    }

    @Transactional
    public DepartmentResponse createDepartment(DepartmentCreateRequest request) {
        String trimmedName = request.getName().trim();
        if (departmentRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new ConflictException("Department with name '" + trimmedName + "' already exists");
        }

        Department department = Department.builder()
                .name(trimmedName)
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .active(true)
                .build();

        Department saved = departmentRepository.save(department);
        return DepartmentResponse.from(saved);
    }

    @Transactional
    public DepartmentResponse updateDepartment(Long id, DepartmentUpdateRequest request) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + id));

        String trimmedName = request.getName().trim();
        if (departmentRepository.existsByNameIgnoreCaseAndIdNot(trimmedName, id)) {
            throw new ConflictException("Another department with name '" + trimmedName + "' already exists");
        }

        department.setName(trimmedName);
        department.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        if (request.getActive() != null) {
            department.setActive(request.getActive());
        }

        Department updated = departmentRepository.save(department);
        return DepartmentResponse.from(updated);
    }

    @Transactional
    public DepartmentResponse updateDepartmentStatus(Long id, boolean active) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + id));

        department.setActive(active);
        Department updated = departmentRepository.save(department);
        return DepartmentResponse.from(updated);
    }
}
