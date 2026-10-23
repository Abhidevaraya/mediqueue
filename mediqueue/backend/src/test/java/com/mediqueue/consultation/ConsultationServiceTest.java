package com.mediqueue.consultation;

import com.mediqueue.appointment.entity.Appointment;
import com.mediqueue.appointment.enums.AppointmentStatus;
import com.mediqueue.appointment.repository.AppointmentRepository;
import com.mediqueue.audit.service.ClinicalAuditService;
import com.mediqueue.consultation.dto.ConsultationResponse;
import com.mediqueue.consultation.entity.ConsultationRecord;
import com.mediqueue.consultation.enums.ConsultationStatus;
import com.mediqueue.consultation.repository.ConsultationRecordRepository;
import com.mediqueue.consultation.service.ConsultationService;
import com.mediqueue.doctor.entity.Doctor;
import com.mediqueue.doctor.repository.DoctorRepository;
import com.mediqueue.exception.ConflictException;
import com.mediqueue.patient.entity.Patient;
import com.mediqueue.patient.repository.PatientRepository;
import com.mediqueue.queue.entity.QueueEntry;
import com.mediqueue.queue.enums.QueueStatus;
import com.mediqueue.queue.repository.QueueEntryRepository;
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

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsultationServiceTest {

    @Mock
    private ConsultationRecordRepository consultationRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private QueueEntryRepository queueEntryRepository;

    @Mock
    private ClinicalAuditService auditService;

    @InjectMocks
    private ConsultationService consultationService;

    private User docUser;
    private Doctor doctor;
    private User patientUser;
    private Patient patient;
    private Appointment appointment;
    private AuthenticatedUser docPrincipal;
    private AuthenticatedUser patientPrincipal;

    @BeforeEach
    void setUp() {
        docUser = User.builder().id(10L).fullName("Dr. Smith").role(UserRole.DOCTOR).accountStatus(AccountStatus.ACTIVE).build();
        doctor = Doctor.builder().id(1L).user(docUser).build();
        docPrincipal = new AuthenticatedUser(docUser);

        patientUser = User.builder().id(20L).fullName("Jane Doe").role(UserRole.PATIENT).accountStatus(AccountStatus.ACTIVE).build();
        patient = Patient.builder().id(2L).user(patientUser).build();
        patientPrincipal = new AuthenticatedUser(patientUser);

        appointment = Appointment.builder()
                .id(100L)
                .appointmentReference("APT-20261020-001")
                .doctor(doctor)
                .patient(patient)
                .startDateTime(LocalDateTime.of(2026, 10, 20, 10, 0))
                .endDateTime(LocalDateTime.of(2026, 10, 20, 10, 30))
                .status(AppointmentStatus.CHECKED_IN)
                .build();
    }

    @Test
    @DisplayName("Start consultation successfully updates state to IN_PROGRESS")
    void startConsultation_success() {
        when(appointmentRepository.findById(100L)).thenReturn(Optional.of(appointment));
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(consultationRepository.findByAppointmentId(100L)).thenReturn(Optional.empty());
        when(consultationRepository.findFirstByDoctorIdAndStatus(1L, ConsultationStatus.IN_PROGRESS)).thenReturn(Optional.empty());

        when(consultationRepository.save(any(ConsultationRecord.class))).thenAnswer(inv -> {
            ConsultationRecord cr = inv.getArgument(0);
            cr.setId(50L);
            return cr;
        });

        QueueEntry qe = QueueEntry.builder().id(200L).status(QueueStatus.CALLED).build();
        when(queueEntryRepository.findByAppointmentId(100L)).thenReturn(Optional.of(qe));

        ConsultationResponse resp = consultationService.startConsultation(docPrincipal, 100L);

        assertNotNull(resp);
        assertEquals(ConsultationStatus.IN_PROGRESS, resp.getStatus());
        assertEquals(AppointmentStatus.IN_PROGRESS, appointment.getStatus());
        assertEquals(QueueStatus.IN_PROGRESS, qe.getStatus());
        assertNotNull(qe.getConsultationStartTimestamp());
        verify(auditService).recordEvent(eq(10L), eq("DOCTOR"), eq("CONSULTATION_STARTED"), any(), any(), any(), eq("SUCCESS"), any());
    }

    @Test
    @DisplayName("Start consultation fails if doctor already has an active consultation in progress")
    void startConsultation_failsWhenAnotherActive() {
        when(appointmentRepository.findById(100L)).thenReturn(Optional.of(appointment));
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(consultationRepository.findByAppointmentId(100L)).thenReturn(Optional.empty());

        ConsultationRecord activeCr = ConsultationRecord.builder()
                .id(99L)
                .consultationReference("CON-20261020-ACTIVE")
                .status(ConsultationStatus.IN_PROGRESS)
                .build();
        when(consultationRepository.findFirstByDoctorIdAndStatus(1L, ConsultationStatus.IN_PROGRESS)).thenReturn(Optional.of(activeCr));

        ConflictException ex = assertThrows(ConflictException.class, () ->
                consultationService.startConsultation(docPrincipal, 100L)
        );
        assertTrue(ex.getMessage().contains("already in progress"));
    }

    @Test
    @DisplayName("Complete consultation sets end timestamp and transitions appointment to COMPLETED")
    void completeConsultation_success() {
        ConsultationRecord record = ConsultationRecord.builder()
                .id(50L)
                .consultationReference("CON-20261020-001")
                .appointment(appointment)
                .patient(patient)
                .doctor(doctor)
                .status(ConsultationStatus.IN_PROGRESS)
                .actualStartTimestamp(Instant.now().minusSeconds(1200))
                .build();

        when(consultationRepository.findById(50L)).thenReturn(Optional.of(record));
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(consultationRepository.save(any(ConsultationRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        ConsultationResponse resp = consultationService.completeConsultation(docPrincipal, 50L);

        assertNotNull(resp);
        assertEquals(ConsultationStatus.COMPLETED, resp.getStatus());
        assertEquals(AppointmentStatus.COMPLETED, appointment.getStatus());
        assertNotNull(record.getActualEndTimestamp());
        verify(auditService).recordEvent(eq(10L), eq("DOCTOR"), eq("CONSULTATION_COMPLETED"), any(), any(), any(), eq("SUCCESS"), any());
    }

    @Test
    @DisplayName("Complete consultation fails if status is not IN_PROGRESS")
    void completeConsultation_failsIfNotInProgress() {
        ConsultationRecord record = ConsultationRecord.builder()
                .id(50L)
                .status(ConsultationStatus.COMPLETED)
                .doctor(doctor)
                .build();

        when(consultationRepository.findById(50L)).thenReturn(Optional.of(record));
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));

        assertThrows(ConflictException.class, () ->
                consultationService.completeConsultation(docPrincipal, 50L)
        );
    }

    @Test
    @DisplayName("Doctor cannot manage consultation for another doctor")
    void doctorCannotManageOtherDoctorConsultation() {
        Doctor otherDoc = Doctor.builder().id(99L).user(User.builder().id(999L).build()).build();
        appointment.setDoctor(otherDoc);

        when(appointmentRepository.findById(100L)).thenReturn(Optional.of(appointment));
        when(doctorRepository.findById(99L)).thenReturn(Optional.of(otherDoc));

        assertThrows(AccessDeniedException.class, () ->
                consultationService.startConsultation(docPrincipal, 100L)
        );
    }
}
