package com.mediqueue.medicalrecord;

import com.mediqueue.appointment.entity.Appointment;
import com.mediqueue.audit.service.ClinicalAuditService;
import com.mediqueue.consultation.entity.ConsultationRecord;
import com.mediqueue.consultation.repository.ConsultationRecordRepository;
import com.mediqueue.doctor.entity.Doctor;
import com.mediqueue.doctor.repository.DoctorRepository;
import com.mediqueue.exception.ConflictException;
import com.mediqueue.medicalrecord.dto.VisitSummaryCreateRequest;
import com.mediqueue.medicalrecord.dto.VisitSummaryResponse;
import com.mediqueue.medicalrecord.dto.VisitSummaryRevisionRequest;
import com.mediqueue.medicalrecord.dto.VisitSummaryUpdateRequest;
import com.mediqueue.medicalrecord.entity.VisitSummary;
import com.mediqueue.medicalrecord.entity.VisitSummaryRevision;
import com.mediqueue.medicalrecord.enums.VisitSummaryStatus;
import com.mediqueue.medicalrecord.repository.VisitSummaryRepository;
import com.mediqueue.medicalrecord.repository.VisitSummaryRevisionRepository;
import com.mediqueue.medicalrecord.service.VisitSummaryService;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VisitSummaryServiceTest {

    @Mock
    private VisitSummaryRepository summaryRepository;

    @Mock
    private VisitSummaryRevisionRepository revisionRepository;

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
    private VisitSummaryService summaryService;

    private Doctor doctor;
    private AuthenticatedUser docPrincipal;
    private Patient patient;
    private AuthenticatedUser patientPrincipal;
    private ConsultationRecord consultation;
    private VisitSummary draftSummary;

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

        draftSummary = VisitSummary.builder()
                .id(1L)
                .summaryReference("VS-20261020-001")
                .consultation(consultation)
                .appointment(appointment)
                .patient(patient)
                .authoringDoctor(doctor)
                .chiefComplaint("Headache")
                .diagnosis("Migraine")
                .clinicalNotes("Prescribed triptan")
                .treatmentPlan("Rest and hydration")
                .status(VisitSummaryStatus.DRAFT)
                .versionNumber(1)
                .build();
    }

    @Test
    @DisplayName("Doctor creates draft visit summary")
    void createDraft_success() {
        when(consultationRepository.findById(50L)).thenReturn(Optional.of(consultation));
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(summaryRepository.save(any(VisitSummary.class))).thenAnswer(inv -> inv.getArgument(0));

        VisitSummaryCreateRequest req = VisitSummaryCreateRequest.builder()
                .consultationId(50L)
                .chiefComplaint("Headache")
                .diagnosis("Tension headache")
                .clinicalNotes("Routine checkup")
                .treatmentPlan("Hydration")
                .build();

        VisitSummaryResponse resp = summaryService.createDraft(docPrincipal, req);

        assertNotNull(resp);
        assertEquals(VisitSummaryStatus.DRAFT, resp.getStatus());
        assertEquals("Headache", resp.getChiefComplaint());
        verify(auditService).recordEvent(any(), any(), eq("VISIT_SUMMARY_CREATED"), any(), any(), any(), eq("SUCCESS"), any());
    }

    @Test
    @DisplayName("Doctor approves draft summary and triggers patient notification")
    void approveSummary_success() {
        when(summaryRepository.findById(1L)).thenReturn(Optional.of(draftSummary));
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(summaryRepository.save(any(VisitSummary.class))).thenAnswer(inv -> inv.getArgument(0));

        VisitSummaryResponse resp = summaryService.approveSummary(docPrincipal, 1L);

        assertNotNull(resp);
        assertEquals(VisitSummaryStatus.APPROVED, resp.getStatus());
        assertNotNull(resp.getApprovedAt());
        verify(notificationService).createNotification(eq(20L), any(), any(), any(), any(), any());
        verify(auditService).recordEvent(any(), any(), eq("VISIT_SUMMARY_APPROVED"), any(), any(), any(), eq("SUCCESS"), any());
    }

    @Test
    @DisplayName("Updating already approved summary directly is rejected")
    void updateDraft_failsWhenAlreadyApproved() {
        draftSummary.setStatus(VisitSummaryStatus.APPROVED);
        when(summaryRepository.findById(1L)).thenReturn(Optional.of(draftSummary));
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));

        VisitSummaryUpdateRequest req = VisitSummaryUpdateRequest.builder().diagnosis("Updated").build();
        assertThrows(ConflictException.class, () ->
                summaryService.updateDraft(docPrincipal, 1L, req)
        );
    }

    @Test
    @DisplayName("Doctor revises approved summary archives revision and increments version")
    void reviseSummary_success() {
        draftSummary.setStatus(VisitSummaryStatus.APPROVED);
        when(summaryRepository.findById(1L)).thenReturn(Optional.of(draftSummary));
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(summaryRepository.save(any(VisitSummary.class))).thenAnswer(inv -> inv.getArgument(0));

        VisitSummaryRevisionRequest req = VisitSummaryRevisionRequest.builder()
                .revisionReason("Lab results received")
                .diagnosis("Confirmed Migraine with Aura")
                .treatmentPlan("Add daily preventative medication")
                .build();

        VisitSummaryResponse resp = summaryService.reviseSummary(docPrincipal, 1L, req);

        assertNotNull(resp);
        assertEquals(2, resp.getVersionNumber());
        assertEquals("Lab results received", resp.getRevisionReason());
        verify(revisionRepository).save(any(VisitSummaryRevision.class));
        verify(auditService).recordEvent(any(), any(), eq("VISIT_SUMMARY_REVISED"), any(), any(), any(), eq("SUCCESS"), any());
    }

    @Test
    @DisplayName("Patient cannot view unapproved draft summary")
    void patientCannotViewDraftSummary() {
        when(summaryRepository.findById(1L)).thenReturn(Optional.of(draftSummary));

        assertThrows(AccessDeniedException.class, () ->
                summaryService.getSummaryById(patientPrincipal, 1L)
        );
    }

    @Test
    @DisplayName("Patient can view approved summary belonging to them")
    void patientCanViewApprovedSummary() {
        draftSummary.setStatus(VisitSummaryStatus.APPROVED);
        when(summaryRepository.findById(1L)).thenReturn(Optional.of(draftSummary));

        VisitSummaryResponse resp = summaryService.getSummaryById(patientPrincipal, 1L);
        assertNotNull(resp);
        assertEquals(VisitSummaryStatus.APPROVED, resp.getStatus());
    }
}
