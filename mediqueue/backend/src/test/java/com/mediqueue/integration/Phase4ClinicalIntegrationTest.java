package com.mediqueue.integration;

import com.mediqueue.appointment.entity.Appointment;
import com.mediqueue.appointment.enums.AppointmentStatus;
import com.mediqueue.appointment.repository.AppointmentRepository;
import com.mediqueue.audit.entity.ClinicalAuditEvent;
import com.mediqueue.audit.repository.ClinicalAuditEventRepository;
import com.mediqueue.consultation.dto.ConsultationResponse;
import com.mediqueue.consultation.entity.ConsultationRecord;
import com.mediqueue.consultation.enums.ConsultationStatus;
import com.mediqueue.consultation.repository.ConsultationRecordRepository;
import com.mediqueue.consultation.service.ConsultationService;
import com.mediqueue.department.entity.Department;
import com.mediqueue.department.repository.DepartmentRepository;
import com.mediqueue.doctor.entity.Doctor;
import com.mediqueue.doctor.enums.DoctorVerificationStatus;
import com.mediqueue.doctor.repository.DoctorRepository;
import com.mediqueue.followup.dto.FollowUpCreateRequest;
import com.mediqueue.followup.dto.FollowUpResponse;
import com.mediqueue.followup.entity.FollowUp;
import com.mediqueue.followup.enums.FollowUpStatus;
import com.mediqueue.followup.repository.FollowUpRepository;
import com.mediqueue.followup.service.FollowUpService;
import com.mediqueue.medicaldocument.dto.MedicalDocumentResponse;
import com.mediqueue.medicaldocument.enums.DocumentCategory;
import com.mediqueue.medicaldocument.service.MedicalDocumentService;
import com.mediqueue.medicalrecord.dto.VisitSummaryCreateRequest;
import com.mediqueue.medicalrecord.dto.VisitSummaryResponse;
import com.mediqueue.medicalrecord.enums.VisitSummaryStatus;
import com.mediqueue.medicalrecord.repository.VisitSummaryRepository;
import com.mediqueue.medicalrecord.service.VisitSummaryService;
import com.mediqueue.patient.entity.Patient;
import com.mediqueue.patient.repository.PatientRepository;
import com.mediqueue.prescription.dto.PrescriptionCreateRequest;
import com.mediqueue.prescription.dto.PrescriptionItemRequest;
import com.mediqueue.prescription.dto.PrescriptionResponse;
import com.mediqueue.prescription.enums.PrescriptionStatus;
import com.mediqueue.prescription.repository.PrescriptionRepository;
import com.mediqueue.prescription.service.PrescriptionService;
import com.mediqueue.security.AuthenticatedUser;
import com.mediqueue.user.entity.User;
import com.mediqueue.user.enums.AccountStatus;
import com.mediqueue.user.enums.UserRole;
import com.mediqueue.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class Phase4ClinicalIntegrationTest {

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
    private ConsultationRecordRepository consultationRecordRepository;
    @Autowired
    private VisitSummaryRepository visitSummaryRepository;
    @Autowired
    private PrescriptionRepository prescriptionRepository;
    @Autowired
    private FollowUpRepository followUpRepository;
    @Autowired
    private ClinicalAuditEventRepository clinicalAuditEventRepository;

    @Autowired
    private ConsultationService consultationService;
    @Autowired
    private VisitSummaryService visitSummaryService;
    @Autowired
    private PrescriptionService prescriptionService;
    @Autowired
    private FollowUpService followUpService;
    @Autowired
    private MedicalDocumentService medicalDocumentService;

    private User doctorUser;
    private Doctor doctor;
    private User patientUser;
    private Patient patient;
    private Appointment appointment;
    private AuthenticatedUser doctorAuth;
    private AuthenticatedUser patientAuth;

    @BeforeEach
    void setUp() {
        String unique = UUID.randomUUID().toString().substring(0, 8);

        Department dept = departmentRepository.save(Department.builder()
                .name("Cardiology " + unique)
                .description("Cardiology department")
                .active(true)
                .build());

        doctorUser = userRepository.save(User.builder()
                .email("doc_" + unique + "@test.com")
                .passwordHash("$2a$10$hashedpasswordforclinicaltest")
                .fullName("Dr. Test " + unique)
                .role(UserRole.DOCTOR)
                .accountStatus(AccountStatus.ACTIVE)
                .build());

        doctor = doctorRepository.save(Doctor.builder()
                .user(doctorUser)
                .department(dept)
                .specialty("Cardiology")
                .qualification("MD")
                .yearsOfExperience(10)
                .consultationFee(BigDecimal.valueOf(100))
                .verificationStatus(DoctorVerificationStatus.VERIFIED)
                .build());

        patientUser = userRepository.save(User.builder()
                .email("pat_" + unique + "@test.com")
                .passwordHash("$2a$10$hashedpasswordforclinicaltest")
                .fullName("Patient Test " + unique)
                .role(UserRole.PATIENT)
                .accountStatus(AccountStatus.ACTIVE)
                .build());

        patient = patientRepository.save(Patient.builder()
                .user(patientUser)
                .build());

        appointment = appointmentRepository.save(Appointment.builder()
                .appointmentReference("APT-" + unique.toUpperCase())
                .patient(patient)
                .doctor(doctor)
                .department(dept)
                .startDateTime(LocalDateTime.now())
                .endDateTime(LocalDateTime.now().plusMinutes(15))
                .slotDurationMinutes(15)
                .status(AppointmentStatus.CHECKED_IN)
                .reason("Chest pain checkup")
                .build());

        doctorAuth = new AuthenticatedUser(doctorUser);
        patientAuth = new AuthenticatedUser(patientUser);
    }

    @Test
    @DisplayName("Complete Clinical Lifecycle: Start consultation, complete, author summary, prescribe, schedule follow-up, upload doc")
    void fullClinicalWorkflow_success() {
        // 1. Start Consultation
        ConsultationResponse consultationResponse = consultationService.startConsultation(doctorAuth, appointment.getId());
        assertNotNull(consultationResponse);
        assertEquals(ConsultationStatus.IN_PROGRESS, consultationResponse.getStatus());
        assertNotNull(consultationResponse.getActualStartTimestamp());
        assertNull(consultationResponse.getActualEndTimestamp());

        Long consultationId = consultationResponse.getId();

        // 2. Complete Consultation
        ConsultationResponse completedResponse = consultationService.completeConsultation(doctorAuth, consultationId);
        assertEquals(ConsultationStatus.COMPLETED, completedResponse.getStatus());
        assertNotNull(completedResponse.getActualEndTimestamp());
        assertNotNull(completedResponse.getDurationMinutes());
        assertTrue(completedResponse.getDurationMinutes() >= 0);

        // 3. Verify duration feeds prediction data source directly in ConsultationRecordRepository
        List<ConsultationRecord> recentClinical = consultationRecordRepository.findRecentCompletedConsultations(
                doctor.getId(), PageRequest.of(0, 10)
        );
        assertThat(recentClinical).isNotEmpty();
        assertEquals(completedResponse.getId(), recentClinical.get(0).getId());

        // 4. Create and approve Visit Summary
        VisitSummaryCreateRequest summaryReq = new VisitSummaryCreateRequest();
        summaryReq.setConsultationId(consultationId);
        summaryReq.setChiefComplaint("Chest discomfort on exertion");
        summaryReq.setClinicalNotes("Normal heart sounds, BP 120/80");
        summaryReq.setDiagnosis("Mild exertion-induced angina");
        summaryReq.setTreatmentPlan("Rest, nitrate as needed, follow up in 2 weeks");

        VisitSummaryResponse summaryRes = visitSummaryService.createDraft(doctorAuth, summaryReq);
        assertNotNull(summaryRes);
        assertEquals(VisitSummaryStatus.DRAFT, summaryRes.getStatus());

        VisitSummaryResponse approvedRes = visitSummaryService.approveSummary(doctorAuth, summaryRes.getId());
        assertEquals(VisitSummaryStatus.APPROVED, approvedRes.getStatus());
        assertNotNull(approvedRes.getApprovedAt());

        // Verify patient can view approved summary
        VisitSummaryResponse patientSummaryView = visitSummaryService.getSummaryById(patientAuth, approvedRes.getId());
        assertEquals("Mild exertion-induced angina", patientSummaryView.getDiagnosis());

        // 5. Issue Prescription
        PrescriptionItemRequest item1 = new PrescriptionItemRequest();
        item1.setMedicationName("Aspirin");
        item1.setDosage("75mg");
        item1.setFrequency("Once daily");
        item1.setDurationDays(14);
        item1.setRoute("Oral");
        item1.setInstructions("After food in morning");

        PrescriptionCreateRequest rxReq = new PrescriptionCreateRequest();
        rxReq.setConsultationId(consultationId);
        rxReq.setItems(List.of(item1));
        rxReq.setNotes("Take after meals");

        PrescriptionResponse rxRes = prescriptionService.issuePrescription(doctorAuth, rxReq);
        assertNotNull(rxRes);
        assertEquals(PrescriptionStatus.ISSUED, rxRes.getStatus());
        assertEquals(1, rxRes.getItems().size());
        assertEquals("Aspirin", rxRes.getItems().get(0).getMedicationName());

        // Verify patient can view prescription
        PrescriptionResponse patientRx = prescriptionService.getPrescriptionById(patientAuth, rxRes.getId());
        assertEquals(PrescriptionStatus.ISSUED, patientRx.getStatus());

        // 6. Schedule Follow-up
        FollowUpCreateRequest fuReq = new FollowUpCreateRequest();
        fuReq.setConsultationId(consultationId);
        fuReq.setRecommendedDate(LocalDate.now().plusDays(14));
        fuReq.setInstructions("ECG and BP re-evaluation");

        FollowUpResponse fuRes = followUpService.createFollowUp(doctorAuth, fuReq);
        assertNotNull(fuRes);
        assertEquals(FollowUpStatus.SCHEDULED, fuRes.getStatus());
        assertEquals("ECG and BP re-evaluation", fuRes.getInstructions());

        // 7. Upload and download medical document
        MockMultipartFile file = new MockMultipartFile(
                "file", "ecg_trace.pdf", "application/pdf", "%PDF-1.4 dummy ecg scan".getBytes()
        );

        MedicalDocumentResponse docRes = medicalDocumentService.uploadDocument(
                doctorAuth, patient.getId(), appointment.getId(), consultationId, DocumentCategory.LAB_REPORT, file
        );
        assertNotNull(docRes);
        assertEquals("ecg_trace.pdf", docRes.getOriginalFileName());
        assertEquals(DocumentCategory.LAB_REPORT, docRes.getDocumentCategory());

        // Patient downloads document
        var downloadPayload = medicalDocumentService.downloadDocument(patientAuth, docRes.getId());
        assertNotNull(downloadPayload);
        assertEquals("ecg_trace.pdf", downloadPayload.filename());
        assertEquals("application/pdf", downloadPayload.contentType());

        // 8. Verify clinical audit events were created
        List<ClinicalAuditEvent> auditEvents = clinicalAuditEventRepository.findAll();
        assertThat(auditEvents).isNotEmpty();
        boolean hasConsultationStart = auditEvents.stream().anyMatch(e -> "CONSULTATION_STARTED".equals(e.getAction()));
        boolean hasConsultationComplete = auditEvents.stream().anyMatch(e -> "CONSULTATION_COMPLETED".equals(e.getAction()));
        boolean hasSummaryApproved = auditEvents.stream().anyMatch(e -> "VISIT_SUMMARY_APPROVED".equals(e.getAction()));
        boolean hasRxIssued = auditEvents.stream().anyMatch(e -> "PRESCRIPTION_ISSUED".equals(e.getAction()));
        boolean hasDocUploaded = auditEvents.stream().anyMatch(e -> "DOCUMENT_UPLOADED".equals(e.getAction()));

        assertTrue(hasConsultationStart, "Expected CONSULTATION_STARTED audit event");
        assertTrue(hasConsultationComplete, "Expected CONSULTATION_COMPLETED audit event");
        assertTrue(hasSummaryApproved, "Expected VISIT_SUMMARY_APPROVED audit event");
        assertTrue(hasRxIssued, "Expected PRESCRIPTION_ISSUED audit event");
        assertTrue(hasDocUploaded, "Expected DOCUMENT_UPLOADED audit event");
    }
}
