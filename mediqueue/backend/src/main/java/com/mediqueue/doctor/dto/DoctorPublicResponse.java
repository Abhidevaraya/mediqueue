package com.mediqueue.doctor.dto;

import com.mediqueue.doctor.entity.Doctor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorPublicResponse {

    private Long id;
    private String fullName;
    private String specialty;
    private Long departmentId;
    private String departmentName;
    private String qualification;
    private Integer yearsOfExperience;
    private String professionalBio;
    private BigDecimal consultationFee;

    public static DoctorPublicResponse from(Doctor doctor) {
        return DoctorPublicResponse.builder()
                .id(doctor.getId())
                .fullName(doctor.getUser().getFullName())
                .specialty(doctor.getSpecialty())
                .departmentId(doctor.getDepartment().getId())
                .departmentName(doctor.getDepartment().getName())
                .qualification(doctor.getQualification())
                .yearsOfExperience(doctor.getYearsOfExperience())
                .professionalBio(doctor.getProfessionalBio())
                .consultationFee(doctor.getConsultationFee())
                .build();
    }
}
