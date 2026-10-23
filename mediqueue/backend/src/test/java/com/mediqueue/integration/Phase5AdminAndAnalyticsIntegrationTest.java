package com.mediqueue.integration;

import com.mediqueue.analytics.dto.AppointmentAnalyticsResponse;
import com.mediqueue.analytics.dto.HospitalOverviewResponse;
import com.mediqueue.analytics.dto.WaitTimeAnalyticsResponse;
import com.mediqueue.analytics.service.AnalyticsService;
import com.mediqueue.analytics.service.CsvExportService;
import com.mediqueue.appointment.dto.AppointmentCancelRequest;
import com.mediqueue.appointment.dto.AppointmentResponse;
import com.mediqueue.appointment.entity.Appointment;
import com.mediqueue.appointment.enums.AppointmentStatus;
import com.mediqueue.appointment.repository.AppointmentRepository;
import com.mediqueue.appointment.service.AppointmentService;
import com.mediqueue.audit.entity.ClinicalAuditEvent;
import com.mediqueue.audit.repository.ClinicalAuditEventRepository;
import com.mediqueue.audit.service.ClinicalAuditService;
import com.mediqueue.department.entity.Department;
import com.mediqueue.department.repository.DepartmentRepository;
import com.mediqueue.doctor.entity.Doctor;
import com.mediqueue.doctor.enums.DoctorVerificationStatus;
import com.mediqueue.doctor.repository.DoctorRepository;
import com.mediqueue.exception.BusinessException;
import com.mediqueue.patient.entity.Patient;
import com.mediqueue.patient.repository.PatientRepository;
import com.mediqueue.prediction.entity.PredictionLog;
import com.mediqueue.prediction.repository.PredictionLogRepository;
import com.mediqueue.queue.entity.DoctorQueueSession;
import com.mediqueue.queue.entity.QueueEntry;
import com.mediqueue.queue.enums.QueueStatus;
import com.mediqueue.queue.repository.DoctorQueueSessionRepository;
import com.mediqueue.queue.repository.QueueEntryRepository;
import com.mediqueue.security.AuthenticatedUser;
import com.mediqueue.user.dto.UserResponse;
import com.mediqueue.user.entity.User;
import com.mediqueue.user.enums.AccountStatus;
import com.mediqueue.user.enums.UserRole;
import com.mediqueue.user.repository.UserRepository;
import com.mediqueue.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class Phase5AdminAndAnalyticsIntegrationTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private DoctorRepository doctorRepository;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private AppointmentRepository appointmentRepository;
    @Autowired
    private QueueEntryRepository queueEntryRepository;
    @Autowired
    private DoctorQueueSessionRepository sessionRepository;
    @Autowired
    private PredictionLogRepository predictionLogRepository;
    @Autowired
    private ClinicalAuditEventRepository auditEventRepository;

    @Autowired
    private UserService userService;
    @Autowired
    private AppointmentService appointmentService;
    @Autowired
    private AnalyticsService analyticsService;
    @Autowired
    private CsvExportService csvExportService;
    @Autowired
    private ClinicalAuditService auditService;

    private User adminUser;
    private AuthenticatedUser adminAuth;
    private User doctorUser;
    private Doctor doctor;
    private User patientUser;
    private Patient patient;
    private Department department;

    @BeforeEach
    void setUp() {
        String unique = UUID.randomUUID().toString().substring(0, 8);

        adminUser = userRepository.save(User.builder()
                .email("admin_" + unique + "@hospital.org")
                .passwordHash("$2a$10$hashedAdminPassword")
                .fullName("Administrator " + unique)
                .role(UserRole.ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .build());
        adminAuth = new AuthenticatedUser(adminUser);

        department = departmentRepository.save(Department.builder()
                .name("Neurology " + unique)
                .description("Neurology department")
                .active(true)
                .build());

        doctorUser = userRepository.save(User.builder()
                .email("neuro_" + unique + "@hospital.org")
                .passwordHash("$2a$10$hashedDoctorPassword")
                .fullName("Dr. Neuro " + unique)
                .role(UserRole.DOCTOR)
                .accountStatus(AccountStatus.ACTIVE)
                .build());

        doctor = doctorRepository.save(Doctor.builder()
                .user(doctorUser)
                .department(department)
                .specialty("Neurology")
                .qualification("MD Neurology")
                .yearsOfExperience(8)
                .consultationFee(BigDecimal.valueOf(150))
                .verificationStatus(DoctorVerificationStatus.VERIFIED)
                .build());

        patientUser = userRepository.save(User.builder()
                .email("patient_" + unique + "@hospital.org")
                .passwordHash("$2a$10$hashedPatientPassword")
                .fullName("Patient Test " + unique)
                .role(UserRole.PATIENT)
                .accountStatus(AccountStatus.ACTIVE)
                .build());

        patient = patientRepository.save(Patient.builder()
                .user(patientUser)
                .build());
    }

    @Test
    @DisplayName("User Administration: Search users and update user role with self-demotion protection")
    void testUserAdministration() {
        // 1. Search users by keyword
        var searchResult = userService.searchUsers(UserRole.PATIENT, AccountStatus.ACTIVE, patientUser.getFullName(), PageRequest.of(0, 10));
        assertThat(searchResult.getContent()).isNotEmpty();
        assertEquals(patientUser.getEmail(), searchResult.getContent().get(0).getEmail());

        // 2. Update user role
        UserResponse updatedRole = userService.updateUserRole(patientUser.getId(), adminUser.getId(), UserRole.DOCTOR);
        assertEquals(UserRole.DOCTOR, updatedRole.getRole());

        // 3. Admin self-demotion protection
        assertThrows(BusinessException.class, () ->
                userService.updateUserRole(adminUser.getId(), adminUser.getId(), UserRole.PATIENT)
        );
    }

    @Test
    @DisplayName("Appointment Administration: Search and administrative cancellation override")
    void testAppointmentAdministration() {
        Appointment apt = appointmentRepository.save(Appointment.builder()
                .appointmentReference("APT-ADMIN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .patient(patient)
                .doctor(doctor)
                .department(department)
                .startDateTime(LocalDateTime.now())
                .endDateTime(LocalDateTime.now().plusMinutes(15))
                .slotDurationMinutes(15)
                .status(AppointmentStatus.BOOKED)
                .reason("Migraine consultation")
                .build());

        // Create a queue entry linked to appointment
        QueueEntry qe = queueEntryRepository.save(QueueEntry.builder()
                .entryReference("Q-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase())
                .appointment(apt)
                .doctor(doctor)
                .department(department)
                .patient(patient)
                .queueDate(LocalDate.now())
                .tokenNumber(99)
                .status(QueueStatus.WAITING)
                .checkInTimestamp(java.time.Instant.now())
                .build());

        // Administrative cancellation
        AppointmentCancelRequest cancelRequest = new AppointmentCancelRequest();
        cancelRequest.setReason("Doctor emergency leave");

        AppointmentResponse cancelled = appointmentService.cancelAppointment(adminAuth, apt.getId(), cancelRequest);
        assertEquals(AppointmentStatus.CANCELLED, cancelled.getStatus());
        assertEquals("Doctor emergency leave", cancelled.getCancellationReason());

        // Verify audit log
        List<ClinicalAuditEvent> auditEvents = auditEventRepository.findAll();
        assertThat(auditEvents).isNotEmpty();
    }

    @Test
    @DisplayName("Analytics & CSV Export: Verify real metrics calculation and spreadsheet formula defense")
    void testAnalyticsAndCsvExport() {
        LocalDate today = LocalDate.now();

        // Provision appointments for analytics
        appointmentRepository.save(Appointment.builder()
                .appointmentReference("APT-AN-1-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase())
                .patient(patient)
                .doctor(doctor)
                .department(department)
                .startDateTime(LocalDateTime.now())
                .endDateTime(LocalDateTime.now().plusMinutes(15))
                .slotDurationMinutes(15)
                .status(AppointmentStatus.COMPLETED)
                .build());

        appointmentRepository.save(Appointment.builder()
                .appointmentReference("APT-AN-2-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase())
                .patient(patient)
                .doctor(doctor)
                .department(department)
                .startDateTime(LocalDateTime.now())
                .endDateTime(LocalDateTime.now().plusMinutes(15))
                .slotDurationMinutes(15)
                .status(AppointmentStatus.CANCELLED)
                .build());

        // 1. Overview
        HospitalOverviewResponse overview = analyticsService.getOverview();
        assertNotNull(overview);
        assertTrue(overview.getTotalAppointmentsToday() >= 2);

        // 2. Appointment Analytics
        AppointmentAnalyticsResponse aptAnalytics = analyticsService.getAppointmentAnalytics(today, today, null, null);
        assertNotNull(aptAnalytics);
        assertTrue(aptAnalytics.getTotalAppointments() >= 2);
        assertTrue(aptAnalytics.getAttendanceRatePercent() >= 0.0);
        assertTrue(aptAnalytics.getCancellationRatePercent() >= 0.0);

        // 3. Export CSV with formula injection defense
        byte[] csvBytes = csvExportService.exportAppointmentsAnalytics(aptAnalytics);
        String csvContent = new String(csvBytes, StandardCharsets.UTF_8);
        assertTrue(csvContent.contains("Date,Total,Booked,Completed,Cancelled"));

        // Formula injection test
        String sanitized = csvExportService.escapeCsv("=SUM(A1:A10)");
        assertEquals("'=SUM(A1:A10)", sanitized);

        // 4. Department Workload
        var deptWorkload = analyticsService.getDepartmentWorkload(today, today);
        assertThat(deptWorkload).isNotEmpty();

        // 5. Doctor Workload
        var docWorkload = analyticsService.getDoctorWorkload(today, today, department.getId());
        assertThat(docWorkload).isNotEmpty();
    }

    @Test
    @DisplayName("Centralized Audit Log: Record, filter, and export audit trails")
    void testCentralizedAuditLogging() {
        auditService.recordEvent(
                adminUser.getId(),
                "ADMIN",
                "SECURITY_CONFIG_CHECK",
                "SYSTEM",
                1L,
                null,
                "SUCCESS",
                "Audit system operational check"
        );

        var auditPage = auditEventRepository.findFilteredEvents(
                null, null, "SECURITY_CONFIG_CHECK", adminUser.getId(), null, null, "SUCCESS", PageRequest.of(0, 10)
        );

        assertThat(auditPage.getContent()).isNotEmpty();
        assertEquals("SECURITY_CONFIG_CHECK", auditPage.getContent().get(0).getAction());

        byte[] exportBytes = csvExportService.exportAuditLogs(auditPage.getContent());
        String csvText = new String(exportBytes, StandardCharsets.UTF_8);
        assertTrue(csvText.contains("SECURITY_CONFIG_CHECK"));
        assertTrue(csvText.contains("SYSTEM"));
    }
}
