package com.mediqueue.integration;

import com.mediqueue.department.entity.Department;
import com.mediqueue.department.repository.DepartmentRepository;
import com.mediqueue.doctor.entity.Doctor;
import com.mediqueue.doctor.enums.DoctorVerificationStatus;
import com.mediqueue.doctor.repository.DoctorRepository;
import com.mediqueue.patient.entity.Patient;
import com.mediqueue.patient.repository.PatientRepository;
import com.mediqueue.security.JwtService;
import com.mediqueue.user.entity.RevokedToken;
import com.mediqueue.user.entity.User;
import com.mediqueue.user.enums.AccountStatus;
import com.mediqueue.user.enums.UserRole;
import com.mediqueue.user.repository.RevokedTokenRepository;
import com.mediqueue.user.repository.UserRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class DatabaseMigrationAndPersistenceTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private RevokedTokenRepository revokedTokenRepository;

    @Test
    @DisplayName("Verify Flyway migrations ran and validated against real MySQL schema")
    void testFlywayMigrationsApplied() {
        assertNotNull(flyway);
        var migrationInfo = flyway.info().applied();
        assertTrue(migrationInfo.length >= 5, "Expected at least 5 applied migrations");
        assertEquals("create users", migrationInfo[0].getDescription().toLowerCase());
        assertEquals("create departments", migrationInfo[1].getDescription().toLowerCase());
        assertEquals("create patients", migrationInfo[2].getDescription().toLowerCase());
        assertEquals("create doctors", migrationInfo[3].getDescription().toLowerCase());
        assertEquals("create revoked tokens", migrationInfo[4].getDescription().toLowerCase());
    }

    @Test
    @Transactional
    @DisplayName("Verify User and Patient persistence with foreign key constraints")
    void testUserAndPatientPersistence() {
        String uniqueEmail = "patient_" + UUID.randomUUID() + "@mediqueue.com";

        User user = User.builder()
                .fullName("Integration Patient")
                .email(uniqueEmail)
                .passwordHash("$2a$10$hashedpassword")
                .phoneNumber("+1234567890")
                .role(UserRole.PATIENT)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        User savedUser = userRepository.save(user);
        assertNotNull(savedUser.getId());

        Patient patient = Patient.builder()
                .user(savedUser)
                .dateOfBirth(LocalDate.of(1995, 5, 20))
                .gender("FEMALE")
                .address("789 Metro Ave")
                .build();
        Patient savedPatient = patientRepository.save(patient);
        assertNotNull(savedPatient.getId());
        assertEquals(savedUser.getId(), savedPatient.getUser().getId());

        var found = patientRepository.findByUserEmail(uniqueEmail);
        assertTrue(found.isPresent());
        assertEquals("Integration Patient", found.get().getUser().getFullName());
    }

    @Test
    @Transactional
    @DisplayName("Verify unique constraint on User email")
    void testUserEmailUniqueConstraint() {
        String duplicateEmail = "duplicate_" + UUID.randomUUID() + "@mediqueue.com";

        User user1 = User.builder()
                .fullName("User One")
                .email(duplicateEmail)
                .passwordHash("hash1")
                .role(UserRole.PATIENT)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        userRepository.saveAndFlush(user1);

        User user2 = User.builder()
                .fullName("User Two")
                .email(duplicateEmail)
                .passwordHash("hash2")
                .role(UserRole.PATIENT)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        assertThrows(DataIntegrityViolationException.class, () -> {
            userRepository.saveAndFlush(user2);
        });
    }

    @Test
    @Transactional
    @DisplayName("Verify Department and Doctor persistence")
    void testDepartmentAndDoctorPersistence() {
        String deptName = "Oncology_" + UUID.randomUUID().toString().substring(0, 8);
        Department dept = Department.builder()
                .name(deptName)
                .description("Cancer care center")
                .active(true)
                .build();
        Department savedDept = departmentRepository.save(dept);

        String docEmail = "doctor_" + UUID.randomUUID() + "@mediqueue.com";
        User docUser = User.builder()
                .fullName("Dr. Integration")
                .email(docEmail)
                .passwordHash("$2a$10$hashedpassword")
                .role(UserRole.DOCTOR)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        User savedDocUser = userRepository.save(docUser);

        Doctor doctor = Doctor.builder()
                .user(savedDocUser)
                .department(savedDept)
                .specialty("Medical Oncology")
                .qualification("MD, DM")
                .yearsOfExperience(10)
                .consultationFee(BigDecimal.valueOf(250.00))
                .verificationStatus(DoctorVerificationStatus.VERIFIED)
                .build();
        Doctor savedDoctor = doctorRepository.save(doctor);
        assertNotNull(savedDoctor.getId());

        var publicDoc = doctorRepository.findPublicDoctorById(savedDoctor.getId());
        assertTrue(publicDoc.isPresent());
        assertEquals("Dr. Integration", publicDoc.get().getUser().getFullName());
        assertEquals(deptName, publicDoc.get().getDepartment().getName());
    }

    @Test
    @Transactional
    @DisplayName("Verify RevokedToken persistence and lookup")
    void testRevokedTokenPersistence() {
        String token = "sample.token." + UUID.randomUUID();
        String tokenHash = JwtService.hashToken(token);

        RevokedToken revokedToken = RevokedToken.builder()
                .tokenHash(tokenHash)
                .userEmail("user@mediqueue.com")
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        revokedTokenRepository.save(revokedToken);

        assertTrue(revokedTokenRepository.existsByTokenHash(tokenHash));
    }
}
