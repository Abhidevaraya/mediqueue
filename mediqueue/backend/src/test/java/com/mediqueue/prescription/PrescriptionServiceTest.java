package com.mediqueue.prescription;

import com.mediqueue.appointment.entity.Appointment;
import com.mediqueue.audit.service.ClinicalAuditService;
import com.mediqueue.consultation.entity.ConsultationRecord;
import com.mediqueue.consultation.repository.ConsultationRecordRepository;
import com.mediqueue.doctor.entity.Doctor;
import com.mediqueue.doctor.repository.DoctorRepository;
import com.mediqueue.exception.BusinessException;
import com.mediqueue.exception.ConflictException;
import com.mediqueue.notification.service.NotificationService;
import com.mediqueue.patient.entity.Patient;
import com.mediqueue.patient.repository.PatientRepository;
import com.mediqueue.prescription.dto.PrescriptionCreateRequest;
import com.mediqueue.prescription.dto.PrescriptionItemRequest;
import com.mediqueue.prescription.dto.PrescriptionResponse;
import com.mediqueue.prescription.dto.PrescriptionRevisionRequest;
import com.mediqueue.prescription.entity.Prescription;
import com.mediqueue.prescription.enums.PrescriptionStatus;
import com.mediqueue.prescription.repository.PrescriptionRepository;
import com.mediqueue.prescription.service.PrescriptionService;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrescriptionServiceTest {

    @Mock
    private PrescriptionRepository prescriptionRepository;

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
    private PrescriptionService prescriptionService;

    private Doctor doctor;
    private AuthenticatedUser docPrincipal;
    private Patient patient;
    private AuthenticatedUser patientPrincipal;
    private ConsultationRecord consultation;
    private Prescription issuedPrescription;

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

        issuedPrescription = Prescription.builder()
                .id(1L)
                .prescriptionReference("RX-20261020-001")
                .consultation(consultation)
                .appointment(appointment)
                .patient(patient)
                .doctor(doctor)
                .status(PrescriptionStatus.ISSUED)
                .versionNumber(1)
                .build();
    }

    @Test
    @DisplayName("Doctor issues prescription with items persists atomically and notifies patient")
    void issuePrescription_success() {
        when(consultationRepository.findById(50L)).thenReturn(Optional.of(consultation));
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(prescriptionRepository.save(any(Prescription.class))).thenAnswer(inv -> inv.getArgument(0));

        PrescriptionItemRequest item1 = PrescriptionItemRequest.builder()
                .medicationName("Amoxicillin")
                .dosage("500mg")
                .frequency("3 times a day")
                .durationDays(7)
                .instructions("Take with food")
                .build();

        PrescriptionItemRequest item2 = PrescriptionItemRequest.builder()
                .medicationName("Paracetamol")
                .dosage("650mg")
                .frequency("As needed for fever")
                .durationDays(5)
                .build();

        PrescriptionCreateRequest req = PrescriptionCreateRequest.builder()
                .consultationId(50L)
                .notes("Follow course strictly")
                .items(List.of(item1, item2))
                .build();

        PrescriptionResponse resp = prescriptionService.issuePrescription(docPrincipal, req);

        assertNotNull(resp);
        assertEquals(PrescriptionStatus.ISSUED, resp.getStatus());
        assertEquals(2, resp.getItems().size());
        verify(notificationService).createNotification(eq(20L), any(), any(), any(), any(), any());
        verify(auditService).recordEvent(any(), any(), eq("PRESCRIPTION_ISSUED"), any(), any(), any(), eq("SUCCESS"), any());
    }

    @Test
    @DisplayName("Prescription issuance rejects empty items or nonpositive duration")
    void issuePrescription_failsValidation() {
        when(consultationRepository.findById(50L)).thenReturn(Optional.of(consultation));
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));

        PrescriptionCreateRequest reqEmpty = PrescriptionCreateRequest.builder()
                .consultationId(50L)
                .items(List.of())
                .build();

        assertThrows(BusinessException.class, () ->
                prescriptionService.issuePrescription(docPrincipal, reqEmpty)
        );

        PrescriptionItemRequest invalidItem = PrescriptionItemRequest.builder()
                .medicationName("Aspirin")
                .dosage("100mg")
                .frequency("Daily")
                .durationDays(0)
                .build();

        PrescriptionCreateRequest reqInvalid = PrescriptionCreateRequest.builder()
                .consultationId(50L)
                .items(List.of(invalidItem))
                .build();

        assertThrows(BusinessException.class, () ->
                prescriptionService.issuePrescription(docPrincipal, reqInvalid)
        );
    }

    @Test
    @DisplayName("Doctor cancels issued prescription")
    void cancelPrescription_success() {
        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(issuedPrescription));
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(prescriptionRepository.save(any(Prescription.class))).thenAnswer(inv -> inv.getArgument(0));

        PrescriptionResponse resp = prescriptionService.cancelPrescription(docPrincipal, 1L, "Adverse drug reaction reported");

        assertNotNull(resp);
        assertEquals(PrescriptionStatus.CANCELLED, resp.getStatus());
        assertEquals("Adverse drug reaction reported", resp.getCancellationReason());
        verify(auditService).recordEvent(any(), any(), eq("PRESCRIPTION_CANCELLED"), any(), any(), any(), eq("SUCCESS"), any());
    }

    @Test
    @DisplayName("Doctor revises issued prescription creates new linked version")
    void revisePrescription_success() {
        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(issuedPrescription));
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(prescriptionRepository.save(any(Prescription.class))).thenAnswer(inv -> inv.getArgument(0));

        PrescriptionItemRequest newItem = PrescriptionItemRequest.builder()
                .medicationName("Azithromycin")
                .dosage("250mg")
                .frequency("Once daily")
                .durationDays(3)
                .build();

        PrescriptionRevisionRequest req = PrescriptionRevisionRequest.builder()
                .revisionReason("Patient allergic to penicillin")
                .items(List.of(newItem))
                .build();

        PrescriptionResponse resp = prescriptionService.revisePrescription(docPrincipal, 1L, req);

        assertNotNull(resp);
        assertEquals(PrescriptionStatus.ISSUED, resp.getStatus());
        assertEquals(2, resp.getVersionNumber());
        assertEquals(1L, resp.getParentPrescriptionId());
        assertEquals(PrescriptionStatus.REVISED, issuedPrescription.getStatus());
        verify(auditService).recordEvent(any(), any(), eq("PRESCRIPTION_REVISED"), any(), any(), any(), eq("SUCCESS"), any());
    }

    @Test
    @DisplayName("Patient can view their own prescription but not another patient's")
    void patientViewAccessControl() {
        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(issuedPrescription));

        PrescriptionResponse resp = prescriptionService.getPrescriptionById(patientPrincipal, 1L);
        assertNotNull(resp);

        User otherUser = User.builder().id(999L).role(UserRole.PATIENT).accountStatus(AccountStatus.ACTIVE).build();
        AuthenticatedUser otherPrincipal = new AuthenticatedUser(otherUser);

        assertThrows(AccessDeniedException.class, () ->
                prescriptionService.getPrescriptionById(otherPrincipal, 1L)
        );
    }
}
