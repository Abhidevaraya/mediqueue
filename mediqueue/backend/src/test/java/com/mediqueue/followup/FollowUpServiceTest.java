package com.mediqueue.followup;

import com.mediqueue.appointment.entity.Appointment;
import com.mediqueue.audit.service.ClinicalAuditService;
import com.mediqueue.consultation.entity.ConsultationRecord;
import com.mediqueue.consultation.repository.ConsultationRecordRepository;
import com.mediqueue.doctor.entity.Doctor;
import com.mediqueue.doctor.repository.DoctorRepository;
import com.mediqueue.exception.BusinessException;
import com.mediqueue.exception.ConflictException;
import com.mediqueue.followup.dto.FollowUpCreateRequest;
import com.mediqueue.followup.dto.FollowUpResponse;
import com.mediqueue.followup.entity.FollowUp;
import com.mediqueue.followup.enums.FollowUpStatus;
import com.mediqueue.followup.repository.FollowUpRepository;
import com.mediqueue.followup.service.FollowUpService;
import com.mediqueue.notification.service.NotificationService;
import com.mediqueue.patient.entity.Patient;
import com.mediqueue.patient.repository.PatientRepository;
import com.mediqueue.security.AuthenticatedUser;
import com.mediqueue.user.entity.User;
import com.mediqueue.user.enums.AccountStatus;
import com.mediqueue.user.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FollowUpServiceTest {

    @Mock
    private FollowUpRepository followUpRepository;

    @Mock
    private ConsultationRecordRepository consultationRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ClinicalAuditService auditService;

    @InjectMocks
    private FollowUpService followUpService;

    private Doctor doctor;
    private AuthenticatedUser docPrincipal;
    private Patient patient;
    private AuthenticatedUser patientPrincipal;
    private ConsultationRecord consultation;
    private FollowUp followUp;

    @BeforeEach
    void setUp() {
        User docUser = User.builder().id(10L).fullName("Dr. Adams").role(UserRole.DOCTOR).accountStatus(AccountStatus.ACTIVE).build();
        doctor = Doctor.builder().id(1L).user(docUser).build();
        docPrincipal = new AuthenticatedUser(docUser);

        User patientUser = User.builder().id(20L).fullName("John Doe").role(UserRole.PATIENT).accountStatus(AccountStatus.ACTIVE).build();
        patient = Patient.builder().id(2L).user(patientUser).build();
        patientPrincipal = new AuthenticatedUser(patientUser);

        Appointment appointment = Appointment.builder().id(100L).appointmentReference("APT-100").patient(patient).doctor(doctor).build();
        consultation = ConsultationRecord.builder().id(50L).consultationReference("CON-50").appointment(appointment).patient(patient).doctor(doctor).build();

        followUp = FollowUp.builder()
                .id(1L)
                .followUpReference("FLW-20261020-001")
                .consultation(consultation)
                .appointment(appointment)
                .patient(patient)
                .doctor(doctor)
                .recommendedDate(LocalDate.now().plusDays(14))
                .instructions("Post-medication blood pressure check")
                .status(FollowUpStatus.SCHEDULED)
                .build();
    }

    @Test
    @DisplayName("Doctor schedules follow-up recommendation and notifies patient")
    void createFollowUp_success() {
        when(consultationRepository.findById(50L)).thenReturn(Optional.of(consultation));
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(followUpRepository.save(any(FollowUp.class))).thenAnswer(inv -> inv.getArgument(0));

        FollowUpCreateRequest req = FollowUpCreateRequest.builder()
                .consultationId(50L)
                .recommendedDate(LocalDate.now().plusDays(7))
                .instructions("Review test results")
                .build();

        FollowUpResponse resp = followUpService.createFollowUp(docPrincipal, req);

        assertNotNull(resp);
        assertEquals(FollowUpStatus.SCHEDULED, resp.getStatus());
        assertEquals("Review test results", resp.getInstructions());
        verify(notificationService).createNotification(eq(20L), any(), any(), any(), any(), any());
        verify(auditService).recordEvent(any(), any(), eq("FOLLOW_UP_CREATED"), any(), any(), any(), eq("SUCCESS"), any());
    }

    @Test
    @DisplayName("Creating follow-up with past date fails validation")
    void createFollowUp_failsPastDate() {
        when(consultationRepository.findById(50L)).thenReturn(Optional.of(consultation));
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));

        FollowUpCreateRequest req = FollowUpCreateRequest.builder()
                .consultationId(50L)
                .recommendedDate(LocalDate.now().minusDays(1))
                .build();

        assertThrows(BusinessException.class, () ->
                followUpService.createFollowUp(docPrincipal, req)
        );
    }

    @Test
    @DisplayName("Doctor marks follow-up completed")
    void completeFollowUp_success() {
        when(followUpRepository.findById(1L)).thenReturn(Optional.of(followUp));
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(followUpRepository.save(any(FollowUp.class))).thenAnswer(inv -> inv.getArgument(0));

        FollowUpResponse resp = followUpService.completeFollowUp(docPrincipal, 1L);

        assertNotNull(resp);
        assertEquals(FollowUpStatus.COMPLETED, resp.getStatus());
        assertNotNull(resp.getCompletedAt());
        verify(auditService).recordEvent(any(), any(), eq("FOLLOW_UP_COMPLETED"), any(), any(), any(), eq("SUCCESS"), any());
    }

    @Test
    @DisplayName("Doctor cancels follow-up")
    void cancelFollowUp_success() {
        when(followUpRepository.findById(1L)).thenReturn(Optional.of(followUp));
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(followUpRepository.save(any(FollowUp.class))).thenAnswer(inv -> inv.getArgument(0));

        FollowUpResponse resp = followUpService.cancelFollowUp(docPrincipal, 1L, "Patient relocated");

        assertNotNull(resp);
        assertEquals(FollowUpStatus.CANCELLED, resp.getStatus());
        assertEquals("Patient relocated", resp.getCancellationReason());
        verify(auditService).recordEvent(any(), any(), eq("FOLLOW_UP_CANCELLED"), any(), any(), any(), eq("SUCCESS"), any());
    }

    @Test
    @DisplayName("Patient can view own follow-up but not another patient's")
    void patientViewAccessControl() {
        when(followUpRepository.findById(1L)).thenReturn(Optional.of(followUp));

        FollowUpResponse resp = followUpService.getFollowUpById(patientPrincipal, 1L);
        assertNotNull(resp);

        User otherUser = User.builder().id(999L).role(UserRole.PATIENT).accountStatus(AccountStatus.ACTIVE).build();
        AuthenticatedUser otherPrincipal = new AuthenticatedUser(otherUser);

        assertThrows(AccessDeniedException.class, () ->
                followUpService.getFollowUpById(otherPrincipal, 1L)
        );
    }
}
