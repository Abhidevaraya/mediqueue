package com.mediqueue.queue;

import com.mediqueue.appointment.entity.Appointment;
import com.mediqueue.appointment.enums.AppointmentStatus;
import com.mediqueue.appointment.repository.AppointmentRepository;
import com.mediqueue.department.entity.Department;
import com.mediqueue.doctor.entity.Doctor;
import com.mediqueue.doctor.repository.DoctorRepository;
import com.mediqueue.exception.BusinessException;
import com.mediqueue.exception.ConflictException;
import com.mediqueue.exception.ResourceNotFoundException;
import com.mediqueue.notification.service.NotificationService;
import com.mediqueue.patient.entity.Patient;
import com.mediqueue.queue.dto.DoctorQueueSummaryResponse;
import com.mediqueue.queue.dto.QueueCheckInRequest;
import com.mediqueue.queue.dto.QueueEntryResponse;
import com.mediqueue.queue.dto.QueueSessionResponse;
import com.mediqueue.queue.entity.DoctorQueueSession;
import com.mediqueue.queue.entity.QueueEntry;
import com.mediqueue.queue.enums.QueuePriority;
import com.mediqueue.queue.enums.QueueStatus;
import com.mediqueue.queue.repository.DoctorQueueSessionRepository;
import com.mediqueue.queue.repository.QueueEntryRepository;
import com.mediqueue.queue.repository.QueueEventRepository;
import com.mediqueue.queue.service.DoctorQueueSessionService;
import com.mediqueue.queue.service.QueueService;
import com.mediqueue.queue.validation.QueueTransitionValidator;
import com.mediqueue.schedule.service.AvailabilityService;
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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueueServiceTest {

    @Mock
    private QueueEntryRepository queueEntryRepository;

    @Mock
    private DoctorQueueSessionRepository sessionRepository;

    @Mock
    private QueueEventRepository eventRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private AvailabilityService availabilityService;

    @Mock
    private DoctorQueueSessionService doctorQueueSessionService;

    @Spy
    private QueueTransitionValidator transitionValidator = new QueueTransitionValidator();

    @Mock
    private NotificationService notificationService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private QueueService queueService;

    private User patientUser;
    private Patient patient;
    private User doctorUser;
    private Doctor doctor;
    private Department department;
    private Appointment appointment;
    private AuthenticatedUser patientPrincipal;
    private AuthenticatedUser doctorPrincipal;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        today = LocalDate.of(2026, 10, 20);

        patientUser = User.builder()
                .id(1L)
                .fullName("Alice Patient")
                .email("alice@example.com")
                .role(UserRole.PATIENT)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        patient = Patient.builder().id(10L).user(patientUser).build();
        patientPrincipal = new AuthenticatedUser(patientUser);

        doctorUser = User.builder()
                .id(2L)
                .fullName("Dr. House")
                .email("house@example.com")
                .role(UserRole.DOCTOR)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        department = Department.builder().id(5L).name("Diagnostic").active(true).build();
        doctor = Doctor.builder().id(20L).user(doctorUser).department(department).build();
        doctorPrincipal = new AuthenticatedUser(doctorUser);

        appointment = Appointment.builder()
                .id(100L)
                .appointmentReference("APT-20261020-ABC123")
                .patient(patient)
                .doctor(doctor)
                .department(department)
                .startDateTime(LocalDateTime.of(today, java.time.LocalTime.of(10, 0)))
                .endDateTime(LocalDateTime.of(today, java.time.LocalTime.of(10, 30)))
                .status(AppointmentStatus.BOOKED)
                .build();
    }

    @Test
    @DisplayName("Check-in succeeds and allocates sequential token")
    void checkIn_success() {
        when(availabilityService.nowInHospitalTime()).thenReturn(ZonedDateTime.of(today, java.time.LocalTime.of(9, 30), ZoneId.of("Asia/Kolkata")));
        when(appointmentRepository.findById(100L)).thenReturn(Optional.of(appointment));
        when(queueEntryRepository.findByAppointmentId(100L)).thenReturn(Optional.empty());

        DoctorQueueSession session = DoctorQueueSession.builder()
                .id(1L)
                .doctor(doctor)
                .queueDate(today)
                .lastTokenNumber(4)
                .isPaused(false)
                .build();
        when(sessionRepository.findByDoctorIdAndQueueDateWithLock(doctor.getId(), today)).thenReturn(Optional.of(session));
        when(queueEntryRepository.save(any(QueueEntry.class))).thenAnswer(invocation -> {
            QueueEntry qe = invocation.getArgument(0);
            qe.setId(500L);
            return qe;
        });

        QueueEntryResponse response = queueService.checkIn(patientPrincipal, new QueueCheckInRequest(100L));

        assertNotNull(response);
        assertEquals(5, response.getTokenNumber());
        assertEquals(QueueStatus.WAITING, response.getStatus());
        assertEquals(AppointmentStatus.CHECKED_IN, appointment.getStatus());
        verify(sessionRepository).save(session);
        verify(eventPublisher).publishEvent(any());
        verify(notificationService).createNotification(eq(patientUser.getId()), any(), any(), any(), eq(100L), any());
    }

    @Test
    @DisplayName("Check-in fails if appointment is on a different date")
    void checkIn_failsDifferentDate() {
        when(availabilityService.nowInHospitalTime()).thenReturn(ZonedDateTime.of(today.plusDays(1), java.time.LocalTime.of(9, 30), ZoneId.of("Asia/Kolkata")));
        when(appointmentRepository.findById(100L)).thenReturn(Optional.of(appointment));
        when(queueEntryRepository.findByAppointmentId(100L)).thenReturn(Optional.empty());

        assertThrows(ConflictException.class, () ->
                queueService.checkIn(patientPrincipal, new QueueCheckInRequest(100L))
        );
    }

    @Test
    @DisplayName("Call-next fails when doctor queue is paused")
    void callNext_failsWhenPaused() {
        when(availabilityService.nowInHospitalTime()).thenReturn(ZonedDateTime.of(today, java.time.LocalTime.of(10, 0), ZoneId.of("Asia/Kolkata")));
        when(doctorRepository.findById(doctor.getId())).thenReturn(Optional.of(doctor));

        DoctorQueueSession session = DoctorQueueSession.builder()
                .id(1L)
                .doctor(doctor)
                .queueDate(today)
                .isPaused(true)
                .pauseReason("Doctor on emergency round")
                .build();
        when(sessionRepository.findByDoctorIdAndQueueDateWithLock(doctor.getId(), today)).thenReturn(Optional.of(session));

        ConflictException ex = assertThrows(ConflictException.class, () ->
                queueService.callNext(doctorPrincipal, doctor.getId())
        );
        assertTrue(ex.getMessage().contains("paused"));
    }

    @Test
    @DisplayName("Call-next succeeds and transitions WAITING entry to CALLED")
    void callNext_success() {
        when(availabilityService.nowInHospitalTime()).thenReturn(ZonedDateTime.of(today, java.time.LocalTime.of(10, 0), ZoneId.of("Asia/Kolkata")));
        when(doctorRepository.findById(doctor.getId())).thenReturn(Optional.of(doctor));

        DoctorQueueSession session = DoctorQueueSession.builder()
                .id(1L)
                .doctor(doctor)
                .queueDate(today)
                .isPaused(false)
                .build();
        when(sessionRepository.findByDoctorIdAndQueueDateWithLock(doctor.getId(), today)).thenReturn(Optional.of(session));
        when(queueEntryRepository.findFirstByDoctorIdAndQueueDateAndStatus(doctor.getId(), today, QueueStatus.CALLED)).thenReturn(Optional.empty());
        when(queueEntryRepository.findFirstByDoctorIdAndQueueDateAndStatus(doctor.getId(), today, QueueStatus.IN_PROGRESS)).thenReturn(Optional.empty());

        QueueEntry waitingEntry = QueueEntry.builder()
                .id(501L)
                .entryReference("QE-20261020-001")
                .appointment(appointment)
                .patient(patient)
                .doctor(doctor)
                .department(department)
                .queueDate(today)
                .tokenNumber(1)
                .status(QueueStatus.WAITING)
                .priorityCategory(QueuePriority.STANDARD)
                .checkInTimestamp(Instant.now())
                .build();
        when(queueEntryRepository.findNextWaitingWithLock(doctor.getId(), today)).thenReturn(Optional.of(waitingEntry));
        when(queueEntryRepository.save(any(QueueEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        QueueEntryResponse response = queueService.callNext(doctorPrincipal, doctor.getId());

        assertEquals(QueueStatus.CALLED, response.getStatus());
        assertEquals(1, response.getTokenNumber());
        assertNotNull(waitingEntry.getCalledTimestamp());
        verify(notificationService).createNotification(eq(patientUser.getId()), any(), any(), any(), any(), eq(501L));
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    @DisplayName("Start and complete consultation lifecycle")
    void startAndCompleteConsultation_success() {
        QueueEntry entry = QueueEntry.builder()
                .id(502L)
                .entryReference("QE-20261020-002")
                .appointment(appointment)
                .patient(patient)
                .doctor(doctor)
                .department(department)
                .queueDate(today)
                .tokenNumber(2)
                .status(QueueStatus.CALLED)
                .priorityCategory(QueuePriority.STANDARD)
                .checkInTimestamp(Instant.now())
                .calledTimestamp(Instant.now())
                .build();

        when(queueEntryRepository.findByIdWithLock(502L)).thenReturn(Optional.of(entry));
        when(doctorRepository.findById(doctor.getId())).thenReturn(Optional.of(doctor));

        DoctorQueueSession session = DoctorQueueSession.builder()
                .id(1L)
                .doctor(doctor)
                .queueDate(today)
                .isPaused(false)
                .currentCalledToken(2)
                .build();
        when(sessionRepository.findByDoctorIdAndQueueDateWithLock(doctor.getId(), today)).thenReturn(Optional.of(session));

        when(queueEntryRepository.findFirstByDoctorIdAndQueueDateAndStatus(doctor.getId(), today, QueueStatus.IN_PROGRESS)).thenReturn(Optional.empty());
        when(queueEntryRepository.save(any(QueueEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        // Start consultation
        QueueEntryResponse startResp = queueService.startConsultation(doctorPrincipal, 502L);
        assertEquals(QueueStatus.IN_PROGRESS, startResp.getStatus());
        assertEquals(AppointmentStatus.IN_PROGRESS, appointment.getStatus());

        // Complete consultation
        QueueEntryResponse completeResp = queueService.completeConsultation(doctorPrincipal, 502L);
        assertEquals(QueueStatus.COMPLETED, completeResp.getStatus());
        assertEquals(AppointmentStatus.COMPLETED, appointment.getStatus());
        assertNotNull(entry.getConsultationEndTimestamp());
    }

    @Test
    @DisplayName("Pause and resume doctor queue")
    void pauseAndResumeQueue_success() {
        when(availabilityService.nowInHospitalTime()).thenReturn(ZonedDateTime.of(today, java.time.LocalTime.of(11, 0), ZoneId.of("Asia/Kolkata")));
        when(doctorRepository.findById(doctor.getId())).thenReturn(Optional.of(doctor));

        DoctorQueueSession session = DoctorQueueSession.builder()
                .id(1L)
                .doctor(doctor)
                .queueDate(today)
                .isPaused(false)
                .build();
        when(sessionRepository.findByDoctorIdAndQueueDateWithLock(doctor.getId(), today)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(DoctorQueueSession.class))).thenAnswer(inv -> inv.getArgument(0));

        QueueSessionResponse pauseResp = queueService.pauseQueue(doctorPrincipal, doctor.getId(), "Lunch break");
        assertTrue(pauseResp.getIsPaused());
        assertEquals("Lunch break", pauseResp.getPauseReason());

        QueueSessionResponse resumeResp = queueService.resumeQueue(doctorPrincipal, doctor.getId());
        assertFalse(resumeResp.getIsPaused());
        assertNull(resumeResp.getPauseReason());
    }

    @Test
    @DisplayName("Invalid queue transitions are rejected by validator")
    void transitionValidator_enforcesLegalTransitions() {
        // WAITING cannot jump to COMPLETED
        assertThrows(BusinessException.class, () ->
                transitionValidator.validateTransition(QueueStatus.WAITING, QueueStatus.COMPLETED)
        );

        // COMPLETED cannot be transitioned to WAITING
        assertThrows(BusinessException.class, () ->
                transitionValidator.validateTransition(QueueStatus.COMPLETED, QueueStatus.WAITING)
        );
    }
}
