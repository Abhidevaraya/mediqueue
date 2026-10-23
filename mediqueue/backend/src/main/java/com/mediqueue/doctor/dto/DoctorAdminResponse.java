package com.mediqueue.doctor.dto;

import com.mediqueue.doctor.entity.Doctor;
import com.mediqueue.doctor.enums.DoctorVerificationStatus;
import com.mediqueue.user.enums.AccountStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorAdminResponse {

    private Long id;
    private Long userId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private Long departmentId;
    private String departmentName;
    private String specialty;
    private String qualification;
    private Integer yearsOfExperience;
    private String professionalBio;
    private BigDecimal consultationFee;
    private DoctorVerificationStatus verificationStatus;
    private AccountStatus accountStatus;
    private Instant createdAt;
    private Instant updatedAt;

    public static DoctorAdminResponse from(Doctor doctor) {
        return DoctorAdminResponse.builder()
                .id(doctor.getId())
                .userId(doctor.getUser().getId())
                .fullName(doctor.getUser().getFullName())
                .email(doctor.getUser().getEmail())
                .phoneNumber(doctor.getUser().getPhoneNumber())
                .departmentId(doctor.getDepartment().getId())
                .departmentName(doctor.getDepartment().getName())
                .specialty(doctor.getSpecialty())
                .qualification(doctor.getQualification())
                .yearsOfExperience(doctor.getYearsOfExperience())
                .professionalBio(doctor.getProfessionalBio())
                .consultationFee(doctor.getConsultationFee())
                .verificationStatus(doctor.getVerificationStatus())
                .accountStatus(doctor.getUser().getAccountStatus())
                .createdAt(doctor.getCreatedAt())
                .updatedAt(doctor.getUpdatedAt())
                .build();
    }
}
