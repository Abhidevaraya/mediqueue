package com.mediqueue.auth.service;

import com.mediqueue.auth.dto.AuthResponse;
import com.mediqueue.auth.dto.LoginRequest;
import com.mediqueue.auth.dto.RegisterRequest;
import com.mediqueue.auth.dto.UserSummaryDto;
import com.mediqueue.exception.ConflictException;
import com.mediqueue.patient.entity.Patient;
import com.mediqueue.patient.repository.PatientRepository;
import com.mediqueue.security.AuthenticatedUser;
import com.mediqueue.security.JwtService;
import com.mediqueue.security.TokenRevocationService;
import com.mediqueue.user.entity.User;
import com.mediqueue.user.enums.AccountStatus;
import com.mediqueue.user.enums.UserRole;
import com.mediqueue.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mediqueue.department.entity.Department;
import com.mediqueue.department.repository.DepartmentRepository;
import com.mediqueue.doctor.dto.DoctorCreateRequest;
import com.mediqueue.doctor.entity.Doctor;
import com.mediqueue.doctor.enums.DoctorVerificationStatus;
import com.mediqueue.doctor.repository.DoctorRepository;
import com.mediqueue.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final DepartmentRepository departmentRepository;
    private final DoctorRepository doctorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenRevocationService tokenRevocationService;

    @Value("${mediqueue.jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = User.normalizeEmail(request.getEmail());

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("An account with email " + normalizedEmail + " already exists");
        }

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber() != null ? request.getPhoneNumber().trim() : null)
                .role(UserRole.PATIENT)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        User savedUser = userRepository.save(user);

        Patient patient = Patient.builder()
                .user(savedUser)
                .build();
        patientRepository.save(patient);

        String token = jwtService.generateToken(savedUser);

        log.info("Patient registered successfully with email: {}", normalizedEmail);

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresInMs(jwtExpirationMs)
                .user(UserSummaryDto.from(savedUser))
                .build();
    }

    @Transactional
    public AuthResponse registerDoctor(DoctorCreateRequest request) {
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
                .accountStatus(AccountStatus.PENDING)
                .build();

        User savedUser = userRepository.save(user);

        Doctor doctor = Doctor.builder()
                .user(savedUser)
                .department(department)
                .specialty(request.getSpecialty().trim())
                .qualification(request.getQualification().trim())
                .yearsOfExperience(request.getYearsOfExperience() != null ? request.getYearsOfExperience() : 0)
                .professionalBio(request.getProfessionalBio() != null ? request.getProfessionalBio().trim() : null)
                .consultationFee(request.getConsultationFee() != null ? request.getConsultationFee() : BigDecimal.ZERO)
                .verificationStatus(DoctorVerificationStatus.PENDING)
                .build();

        doctorRepository.save(doctor);

        log.info("Doctor registered with PENDING verification status: {}", normalizedEmail);

        return AuthResponse.builder()
                .accessToken("")
                .tokenType("Bearer")
                .expiresInMs(0)
                .user(UserSummaryDto.from(savedUser))
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = User.normalizeEmail(request.getEmail());

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        if (user.getAccountStatus() == AccountStatus.BLOCKED) {
            throw new LockedException("Account is blocked. Please contact system support.");
        }

        if (user.getRole() == UserRole.DOCTOR) {
            Doctor doctor = doctorRepository.findByUserId(user.getId()).orElse(null);
            if (doctor != null) {
                if (doctor.getVerificationStatus() == DoctorVerificationStatus.PENDING || user.getAccountStatus() == AccountStatus.PENDING) {
                    throw new LockedException("Your doctor account is pending administrator verification. Please wait for hospital administration approval.");
                } else if (doctor.getVerificationStatus() == DoctorVerificationStatus.REJECTED) {
                    throw new LockedException("Your doctor verification request was rejected. Please contact hospital administration.");
                }
            }
        }

        if (user.getAccountStatus() == AccountStatus.PENDING) {
            throw new LockedException("Your account is pending administrator approval.");
        }

        String token = jwtService.generateToken(user);

        log.info("User logged in successfully: {}", normalizedEmail);

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresInMs(jwtExpirationMs)
                .user(UserSummaryDto.from(user))
                .build();
    }

    @Transactional
    public void logout(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            tokenRevocationService.revokeToken(token);
        }
    }

    @Transactional(readOnly = true)
    public UserSummaryDto getCurrentUser(AuthenticatedUser principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new BadCredentialsException("User session no longer valid"));

        return UserSummaryDto.from(user);
    }
}
