package com.mediqueue.patient.service;

import com.mediqueue.exception.ResourceNotFoundException;
import com.mediqueue.patient.dto.PatientProfileResponse;
import com.mediqueue.patient.dto.PatientProfileUpdateRequest;
import com.mediqueue.patient.entity.Patient;
import com.mediqueue.patient.repository.PatientRepository;
import com.mediqueue.user.entity.User;
import com.mediqueue.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PatientProfileResponse getMyProfile(Long userId) {
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found for user ID: " + userId));

        return PatientProfileResponse.from(patient);
    }

    @Transactional
    public PatientProfileResponse updateMyProfile(Long userId, PatientProfileUpdateRequest request) {
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found for user ID: " + userId));

        User user = patient.getUser();

        // Update safe user fields if provided
        if (StringUtils.hasText(request.getFullName())) {
            user.setFullName(request.getFullName().trim());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber().trim());
        }
        userRepository.save(user);

        // Update patient profile fields
        if (request.getDateOfBirth() != null) {
            patient.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getGender() != null) {
            patient.setGender(request.getGender().trim());
        }
        if (request.getAddress() != null) {
            patient.setAddress(request.getAddress().trim());
        }

        Patient updatedPatient = patientRepository.save(patient);
        return PatientProfileResponse.from(updatedPatient);
    }
}
