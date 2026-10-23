package com.mediqueue.doctor.service;

import com.mediqueue.common.PageResponse;
import com.mediqueue.department.entity.Department;
import com.mediqueue.department.repository.DepartmentRepository;
import com.mediqueue.doctor.dto.DoctorAdminResponse;
import com.mediqueue.doctor.dto.DoctorCreateRequest;
import com.mediqueue.doctor.dto.DoctorPublicResponse;
import com.mediqueue.doctor.dto.DoctorUpdateRequest;
import com.mediqueue.doctor.entity.Doctor;
import com.mediqueue.doctor.enums.DoctorVerificationStatus;
import com.mediqueue.doctor.repository.DoctorRepository;
import com.mediqueue.exception.ConflictException;
import com.mediqueue.exception.ResourceNotFoundException;
import com.mediqueue.user.entity.User;
import com.mediqueue.user.enums.AccountStatus;
import com.mediqueue.user.enums.UserRole;
import com.mediqueue.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public PageResponse<DoctorPublicResponse> getPublicDoctors(Long departmentId, String specialty, Pageable pageable) {
        Page<Doctor> page = doctorRepository.findPublicDoctors(departmentId, specialty, pageable);
        return PageResponse.of(page, page.getContent().stream().map(DoctorPublicResponse::from).toList());
    }

    @Transactional(readOnly = true)
    public DoctorPublicResponse getPublicDoctorById(Long id) {
        Doctor doctor = doctorRepository.findPublicDoctorById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found or not currently available with ID: " + id));
        return DoctorPublicResponse.from(doctor);
    }

    @Transactional(readOnly = true)
    public PageResponse<DoctorAdminResponse> getAdminDoctors(DoctorVerificationStatus verificationStatus,
                                                             Long departmentId,
                                                             Pageable pageable) {
        Page<Doctor> page = doctorRepository.findAdminDoctors(verificationStatus, departmentId, pageable);
        return PageResponse.of(page, page.getContent().stream().map(DoctorAdminResponse::from).toList());
    }

    @Transactional(readOnly = true)
    public DoctorAdminResponse getAdminDoctorById(Long id) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + id));
        return DoctorAdminResponse.from(doctor);
    }

    @Transactional
    public DoctorAdminResponse createDoctor(DoctorCreateRequest request) {
        String normalizedEmail = User.normalizeEmail(request.getEmail());
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("An account with email " + normalizedEmail + " already exists");
        }

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + request.getDepartmentId()));

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber() != null ? request.getPhoneNumber().trim() : null)
                .role(UserRole.DOCTOR)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        User savedUser = userRepository.save(user);

        Doctor doctor = Doctor.builder()
                .user(savedUser)
                .department(department)
                .specialty(request.getSpecialty().trim())
                .qualification(request.getQualification().trim())
                .yearsOfExperience(request.getYearsOfExperience())
                .professionalBio(request.getProfessionalBio() != null ? request.getProfessionalBio().trim() : null)
                .consultationFee(request.getConsultationFee())
                .verificationStatus(request.getVerificationStatus() != null ? request.getVerificationStatus() : DoctorVerificationStatus.VERIFIED)
                .build();

        Doctor savedDoctor = doctorRepository.save(doctor);
        return DoctorAdminResponse.from(savedDoctor);
    }

    @Transactional
    public DoctorAdminResponse updateDoctor(Long id, DoctorUpdateRequest request) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + id));

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + request.getDepartmentId()));

        doctor.setDepartment(department);
        doctor.setSpecialty(request.getSpecialty().trim());
        doctor.setQualification(request.getQualification().trim());
        doctor.setYearsOfExperience(request.getYearsOfExperience());
        doctor.setProfessionalBio(request.getProfessionalBio() != null ? request.getProfessionalBio().trim() : null);
        doctor.setConsultationFee(request.getConsultationFee());

        Doctor updated = doctorRepository.save(doctor);
        return DoctorAdminResponse.from(updated);
    }

    @Transactional
    public DoctorAdminResponse updateDoctorVerification(Long id, DoctorVerificationStatus status) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + id));

        doctor.setVerificationStatus(status);
        if (status == DoctorVerificationStatus.VERIFIED) {
            doctor.getUser().setAccountStatus(AccountStatus.ACTIVE);
            userRepository.save(doctor.getUser());
        } else if (status == DoctorVerificationStatus.REJECTED) {
            doctor.getUser().setAccountStatus(AccountStatus.BLOCKED);
            userRepository.save(doctor.getUser());
        }
        Doctor updated = doctorRepository.save(doctor);
        return DoctorAdminResponse.from(updated);
    }

    @Transactional
    public DoctorAdminResponse updateDoctorAccountStatus(Long id, AccountStatus status) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + id));

        doctor.getUser().setAccountStatus(status);
        userRepository.save(doctor.getUser());
        return DoctorAdminResponse.from(doctor);
    }
}
