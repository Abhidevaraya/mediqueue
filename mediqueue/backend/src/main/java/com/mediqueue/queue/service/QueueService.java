package com.mediqueue.queue.service;

import com.mediqueue.appointment.entity.Appointment;
import com.mediqueue.appointment.enums.AppointmentStatus;
import com.mediqueue.appointment.repository.AppointmentRepository;
import com.mediqueue.doctor.entity.Doctor;
import com.mediqueue.doctor.repository.DoctorRepository;
import com.mediqueue.exception.BusinessException;
import com.mediqueue.exception.ConflictException;
import com.mediqueue.exception.ResourceNotFoundException;
import com.mediqueue.notification.enums.NotificationType;
import com.mediqueue.notification.service.NotificationService;
import com.mediqueue.queue.dto.DoctorQueueSummaryResponse;
import com.mediqueue.queue.dto.QueueCheckInRequest;
import com.mediqueue.queue.dto.QueueEntryResponse;
import com.mediqueue.queue.dto.QueueEventResponse;
import com.mediqueue.queue.dto.QueuePublicEntryResponse;
import com.mediqueue.queue.dto.QueueSessionResponse;
import com.mediqueue.queue.entity.DoctorQueueSession;
import com.mediqueue.queue.entity.QueueEntry;
import com.mediqueue.queue.entity.QueueEvent;
import com.mediqueue.queue.enums.QueueEventType;
import com.mediqueue.queue.enums.QueuePriority;
import com.mediqueue.queue.enums.QueueStatus;
import com.mediqueue.queue.event.QueueDomainEvent;
import com.mediqueue.queue.repository.DoctorQueueSessionRepository;
import com.mediqueue.queue.repository.QueueEntryRepository;
import com.mediqueue.queue.repository.QueueEventRepository;
import com.mediqueue.queue.validation.QueueTransitionValidator;
import com.mediqueue.schedule.service.AvailabilityService;
import com.mediqueue.security.AuthenticatedUser;
import com.mediqueue.user.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueueService {

    private static final String CHAR_POOL = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final QueueEntryRepository queueEntryRepository;
    private final DoctorQueueSessionRepository sessionRepository;
    private final DoctorQueueSessionService doctorQueueSessionService;
    private final QueueEventRepository eventRepository;
    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final AvailabilityService availabilityService;
    private final QueueTransitionValidator transitionValidator;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.mediqueue.consultation.repository.ConsultationRecordRepository consultationRecordRepository;

    @Transactional
    public QueueEntryResponse checkIn(AuthenticatedUser principal, QueueCheckInRequest request) {
        Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + request.getAppointmentId()));

        // Ownership check: Patient owner, authorized Doctor or Admin
        validateAppointmentCheckInAccess(principal, appointment);

        // Idempotency: If already checked in and has queue entry, return existing entry
        Optional<QueueEntry> existingEntry = queueEntryRepository.findByAppointmentId(appointment.getId());
        if (existingEntry.isPresent()) {
            log.info("Check-in idempotent return for appointment reference: {}", appointment.getAppointmentReference());
            return QueueEntryResponse.from(existingEntry.get());
        }

        if (appointment.getStatus() != AppointmentStatus.BOOKED) {
            throw new ConflictException("Only appointments in BOOKED status can be checked in. Current status: " + appointment.getStatus());
        }

        LocalDate todayHospital = availabilityService.nowInHospitalTime().toLocalDate();
        LocalDate aptDate = appointment.getStartDateTime().toLocalDate();
        if (!aptDate.equals(todayHospital)) {
            throw new ConflictException(String.format(
                    "Check-in is only permitted on the day of the appointment. Appointment date is %s, today is %s",
                    aptDate, todayHospital
            ));
        }

        Doctor doctor = appointment.getDoctor();

        // Concurrency-safe token allocation via locked DoctorQueueSession
        doctorQueueSessionService.ensureSessionExists(doctor.getId(), todayHospital);
        DoctorQueueSession session = sessionRepository.findByDoctorIdAndQueueDateWithLock(doctor.getId(), todayHospital)
                .orElseThrow(() -> new IllegalStateException("Failed to acquire doctor queue session with lock"));

        int tokenNumber = session.getLastTokenNumber() + 1;
        session.setLastTokenNumber(tokenNumber);
        sessionRepository.save(session);

        String entryRef = generateUniqueEntryReference(todayHospital);

        QueueEntry queueEntry = QueueEntry.builder()
                .entryReference(entryRef)
                .appointment(appointment)
                .patient(appointment.getPatient())
                .doctor(doctor)
                .department(appointment.getDepartment())
                .queueDate(todayHospital)
                .tokenNumber(tokenNumber)
                .status(QueueStatus.WAITING)
                .priorityCategory(QueuePriority.STANDARD)
                .checkInTimestamp(Instant.now())
                .build();

        QueueEntry savedEntry = queueEntryRepository.save(queueEntry);

        // Update appointment status to CHECKED_IN
        appointment.setStatus(AppointmentStatus.CHECKED_IN);
        appointmentRepository.save(appointment);

        // Record audit event
        recordEvent(savedEntry, null, QueueStatus.WAITING, QueueEventType.PATIENT_CHECKED_IN, principal.getId(), "Patient checked in", null);

        // Notify patient
        notificationService.createNotification(
                appointment.getPatient().getUser().getId(),
                NotificationType.QUEUE_STATUS_CHANGED,
                "Check-In Successful",
                String.format("You are checked in! Your queue token is %d for Dr. %s.", tokenNumber, doctor.getUser().getFullName()),
                appointment.getId(),
                savedEntry.getId()
        );

        // Emit domain event for real-time STOMP publication
        eventPublisher.publishEvent(new QueueDomainEvent(
                this,
                QueueEventType.PATIENT_CHECKED_IN,
                doctor.getId(),
                todayHospital,
                savedEntry,
                null,
                QueueStatus.WAITING,
                session.getIsPaused(),
                "Patient checked in with token " + tokenNumber
        ));

        log.info("Patient checked in: Token {} for Doctor {}", tokenNumber, doctor.getId());
        return QueueEntryResponse.from(savedEntry);
    }

    @Transactional
    public QueueEntryResponse callNext(AuthenticatedUser principal, Long doctorId) {
        Doctor doctor = validateAndResolveDoctor(principal, doctorId);
        LocalDate todayHospital = availabilityService.nowInHospitalTime().toLocalDate();

        doctorQueueSessionService.ensureSessionExists(doctor.getId(), todayHospital);
        DoctorQueueSession session = sessionRepository.findByDoctorIdAndQueueDateWithLock(doctor.getId(), todayHospital)
                .orElseThrow(() -> new IllegalStateException("Failed to acquire doctor queue session with lock"));

        if (session.getIsPaused()) {
            throw new ConflictException("Doctor queue is paused: " + session.getPauseReason());
        }

        // Single active patient rule: cannot call if someone is currently CALLED or IN_PROGRESS
        if (session.getCurrentCalledToken() != null) {
            throw new ConflictException("There is already a called patient awaiting consultation: Token " + session.getCurrentCalledToken());
        }
        if (session.getCurrentInProgressToken() != null) {
            throw new ConflictException("A consultation is currently in progress: Token " + session.getCurrentInProgressToken());
        }

        Optional<QueueEntry> activeCalled = queueEntryRepository.findFirstByDoctorIdAndQueueDateAndStatus(
                doctor.getId(), todayHospital, QueueStatus.CALLED
        );
        if (activeCalled.isPresent()) {
            throw new ConflictException("There is already a called patient awaiting consultation: Token " + activeCalled.get().getTokenNumber());
        }

        Optional<QueueEntry> activeInProgress = queueEntryRepository.findFirstByDoctorIdAndQueueDateAndStatus(
                doctor.getId(), todayHospital, QueueStatus.IN_PROGRESS
        );
        if (activeInProgress.isPresent()) {
            throw new ConflictException("A consultation is currently in progress: Token " + activeInProgress.get().getTokenNumber());
        }

        // Lock the next waiting entry
        QueueEntry nextWaiting = queueEntryRepository.findNextWaitingWithLock(doctor.getId(), todayHospital)
                .orElseThrow(() -> new ResourceNotFoundException("No patients currently waiting in queue for doctor ID: " + doctor.getId()));

        transitionValidator.validateTransition(nextWaiting.getStatus(), QueueStatus.CALLED);

        QueueStatus prevStatus = nextWaiting.getStatus();
        nextWaiting.setStatus(QueueStatus.CALLED);
        nextWaiting.setCalledTimestamp(Instant.now());
        QueueEntry saved = queueEntryRepository.save(nextWaiting);

        session.setCurrentCalledToken(saved.getTokenNumber());
        sessionRepository.save(session);

        recordEvent(saved, prevStatus, QueueStatus.CALLED, QueueEventType.TOKEN_CALLED, principal.getId(), "Doctor called next patient", null);

        // In-app notification
        notificationService.createNotification(
                saved.getPatient().getUser().getId(),
                NotificationType.TOKEN_CALLED,
                "Your Token Has Been Called!",
                String.format("Token %d called by Dr. %s. Please enter the consultation room.", saved.getTokenNumber(), doctor.getUser().getFullName()),
                saved.getAppointment().getId(),
                saved.getId()
        );

        eventPublisher.publishEvent(new QueueDomainEvent(
                this,
                QueueEventType.TOKEN_CALLED,
                doctor.getId(),
                todayHospital,
                saved,
                prevStatus,
                QueueStatus.CALLED,
                session.getIsPaused(),
                "Token " + saved.getTokenNumber() + " called"
        ));

        log.info("Called next patient: Token {} for doctor {}", saved.getTokenNumber(), doctor.getId());
        return QueueEntryResponse.from(saved);
    }

    @Transactional
    public QueueEntryResponse callEntry(AuthenticatedUser principal, Long queueEntryId) {
        QueueEntry entry = queueEntryRepository.findByIdWithLock(queueEntryId)
                .orElseThrow(() -> new ResourceNotFoundException("Queue entry not found with ID: " + queueEntryId));

        Doctor doctor = validateAndResolveDoctor(principal, entry.getDoctor().getId());
        LocalDate todayHospital = entry.getQueueDate();

        DoctorQueueSession session = sessionRepository.findByDoctorIdAndQueueDateWithLock(doctor.getId(), todayHospital)
                .orElseThrow(() -> new IllegalStateException("Failed to acquire doctor queue session"));

        if (session.getIsPaused()) {
            throw new ConflictException("Doctor queue is paused: " + session.getPauseReason());
        }

        if (session.getCurrentCalledToken() != null && !session.getCurrentCalledToken().equals(entry.getTokenNumber())) {
            throw new ConflictException("There is already another called patient: Token " + session.getCurrentCalledToken());
        }
        if (session.getCurrentInProgressToken() != null) {
            throw new ConflictException("A consultation is currently in progress: Token " + session.getCurrentInProgressToken());
        }

        Optional<QueueEntry> activeCalled = queueEntryRepository.findFirstByDoctorIdAndQueueDateAndStatus(
                doctor.getId(), todayHospital, QueueStatus.CALLED
        );
        if (activeCalled.isPresent() && !activeCalled.get().getId().equals(entry.getId())) {
            throw new ConflictException("There is already another called patient: Token " + activeCalled.get().getTokenNumber());
        }

        transitionValidator.validateTransition(entry.getStatus(), QueueStatus.CALLED);

        QueueStatus prevStatus = entry.getStatus();
        entry.setStatus(QueueStatus.CALLED);
        entry.setCalledTimestamp(Instant.now());
        QueueEntry saved = queueEntryRepository.save(entry);

        session.setCurrentCalledToken(saved.getTokenNumber());
        sessionRepository.save(session);

        recordEvent(saved, prevStatus, QueueStatus.CALLED, QueueEventType.TOKEN_CALLED, principal.getId(), "Doctor called token directly", null);

        notificationService.createNotification(
                saved.getPatient().getUser().getId(),
                NotificationType.TOKEN_CALLED,
                "Your Token Has Been Called!",
                String.format("Token %d called by Dr. %s.", saved.getTokenNumber(), doctor.getUser().getFullName()),
                saved.getAppointment().getId(),
                saved.getId()
        );

        eventPublisher.publishEvent(new QueueDomainEvent(
                this,
                QueueEventType.TOKEN_CALLED,
                doctor.getId(),
                todayHospital,
                saved,
                prevStatus,
                QueueStatus.CALLED,
                session.getIsPaused(),
                "Token " + saved.getTokenNumber() + " called"
        ));

        return QueueEntryResponse.from(saved);
    }

    @Transactional
    public QueueEntryResponse recallEntry(AuthenticatedUser principal, Long queueEntryId) {
        QueueEntry entry = queueEntryRepository.findByIdWithLock(queueEntryId)
                .orElseThrow(() -> new ResourceNotFoundException("Queue entry not found with ID: " + queueEntryId));

        Doctor doctor = validateAndResolveDoctor(principal, entry.getDoctor().getId());

        if (entry.getStatus() != QueueStatus.CALLED) {
            throw new ConflictException("Only CALLED tokens can be recalled. Current status: " + entry.getStatus());
        }

        entry.setCalledTimestamp(Instant.now());
        QueueEntry saved = queueEntryRepository.save(entry);

        recordEvent(saved, QueueStatus.CALLED, QueueStatus.CALLED, QueueEventType.TOKEN_RECALLED, principal.getId(), "Doctor recalled patient", null);

        notificationService.createNotification(
                saved.getPatient().getUser().getId(),
                NotificationType.TOKEN_CALLED,
                "Token Recalled!",
                String.format("Reminder: Token %d is still being called by Dr. %s.", saved.getTokenNumber(), doctor.getUser().getFullName()),
                saved.getAppointment().getId(),
                saved.getId()
        );

        eventPublisher.publishEvent(new QueueDomainEvent(
                this,
                QueueEventType.TOKEN_RECALLED,
                doctor.getId(),
                entry.getQueueDate(),
                saved,
                QueueStatus.CALLED,
                QueueStatus.CALLED,
                false,
                "Token " + saved.getTokenNumber() + " recalled"
        ));

        return QueueEntryResponse.from(saved);
    }

    @Transactional
    public QueueEntryResponse skipEntry(AuthenticatedUser principal, Long queueEntryId, String reason) {
        QueueEntry entry = queueEntryRepository.findByIdWithLock(queueEntryId)
                .orElseThrow(() -> new ResourceNotFoundException("Queue entry not found with ID: " + queueEntryId));

        Doctor doctor = validateAndResolveDoctor(principal, entry.getDoctor().getId());

        transitionValidator.validateTransition(entry.getStatus(), QueueStatus.SKIPPED);

        QueueStatus prevStatus = entry.getStatus();
        entry.setStatus(QueueStatus.SKIPPED);
        entry.setSkipTimestamp(Instant.now());
        entry.setSkipReason(reason != null ? reason.trim() : "Patient absent when called");
        QueueEntry saved = queueEntryRepository.save(entry);

        sessionRepository.findByDoctorIdAndQueueDateWithLock(doctor.getId(), entry.getQueueDate())
                .ifPresent(sess -> {
                    if (sess.getCurrentCalledToken() != null && sess.getCurrentCalledToken().equals(entry.getTokenNumber())) {
                        sess.setCurrentCalledToken(null);
                    }
                    if (sess.getCurrentInProgressToken() != null && sess.getCurrentInProgressToken().equals(entry.getTokenNumber())) {
                        sess.setCurrentInProgressToken(null);
                    }
                    sessionRepository.save(sess);
                });

        recordEvent(saved, prevStatus, QueueStatus.SKIPPED, QueueEventType.PATIENT_SKIPPED, principal.getId(), entry.getSkipReason(), null);

        notificationService.createNotification(
                saved.getPatient().getUser().getId(),
                NotificationType.QUEUE_STATUS_CHANGED,
                "Token Skipped",
                String.format("Token %d was skipped because you were not present. Contact the reception desk to requeue.", saved.getTokenNumber()),
                saved.getAppointment().getId(),
                saved.getId()
        );

        eventPublisher.publishEvent(new QueueDomainEvent(
                this,
                QueueEventType.PATIENT_SKIPPED,
                doctor.getId(),
                entry.getQueueDate(),
                saved,
                prevStatus,
                QueueStatus.SKIPPED,
                false,
                "Token " + saved.getTokenNumber() + " skipped"
        ));

        return QueueEntryResponse.from(saved);
    }

    @Transactional
    public QueueEntryResponse requeueEntry(AuthenticatedUser principal, Long queueEntryId) {
        QueueEntry entry = queueEntryRepository.findByIdWithLock(queueEntryId)
                .orElseThrow(() -> new ResourceNotFoundException("Queue entry not found with ID: " + queueEntryId));

        Doctor doctor = validateAndResolveDoctor(principal, entry.getDoctor().getId());

        transitionValidator.validateTransition(entry.getStatus(), QueueStatus.WAITING);

        QueueStatus prevStatus = entry.getStatus();
        entry.setStatus(QueueStatus.WAITING);
        entry.setCheckInTimestamp(Instant.now()); // placed at current tail of active waiting queue
        entry.setSkipTimestamp(null);
        entry.setSkipReason(null);
        QueueEntry saved = queueEntryRepository.save(entry);

        recordEvent(saved, prevStatus, QueueStatus.WAITING, QueueEventType.PATIENT_REQUEUED, principal.getId(), "Patient returned and requeued", null);

        notificationService.createNotification(
                saved.getPatient().getUser().getId(),
                NotificationType.QUEUE_STATUS_CHANGED,
                "Token Requeued",
                String.format("Token %d has been placed back in the waiting queue.", saved.getTokenNumber()),
                saved.getAppointment().getId(),
                saved.getId()
        );

        eventPublisher.publishEvent(new QueueDomainEvent(
                this,
                QueueEventType.PATIENT_REQUEUED,
                doctor.getId(),
                entry.getQueueDate(),
                saved,
                prevStatus,
                QueueStatus.WAITING,
                false,
                "Token " + saved.getTokenNumber() + " requeued"
        ));

        return QueueEntryResponse.from(saved);
    }

    @Transactional
    public QueueEntryResponse startConsultation(AuthenticatedUser principal, Long queueEntryId) {
        QueueEntry entry = queueEntryRepository.findByIdWithLock(queueEntryId)
                .orElseThrow(() -> new ResourceNotFoundException("Queue entry not found with ID: " + queueEntryId));

        Doctor doctor = validateAndResolveDoctor(principal, entry.getDoctor().getId());

        DoctorQueueSession session = sessionRepository.findByDoctorIdAndQueueDateWithLock(doctor.getId(), entry.getQueueDate())
                .orElse(null);

        if (session != null && session.getCurrentInProgressToken() != null && !session.getCurrentInProgressToken().equals(entry.getTokenNumber())) {
            throw new ConflictException("Another consultation is already in progress: Token " + session.getCurrentInProgressToken());
        }

        // Check if another consultation is already running
        Optional<QueueEntry> alreadyRunning = queueEntryRepository.findFirstByDoctorIdAndQueueDateAndStatus(
                doctor.getId(), entry.getQueueDate(), QueueStatus.IN_PROGRESS
        );
        if (alreadyRunning.isPresent() && !alreadyRunning.get().getId().equals(entry.getId())) {
            throw new ConflictException("Another consultation is already in progress: Token " + alreadyRunning.get().getTokenNumber());
        }

        transitionValidator.validateTransition(entry.getStatus(), QueueStatus.IN_PROGRESS);

        QueueStatus prevStatus = entry.getStatus();
        entry.setStatus(QueueStatus.IN_PROGRESS);
        entry.setConsultationStartTimestamp(Instant.now());
        QueueEntry saved = queueEntryRepository.save(entry);

        if (session != null) {
            if (session.getCurrentCalledToken() != null && session.getCurrentCalledToken().equals(entry.getTokenNumber())) {
                session.setCurrentCalledToken(null);
            }
            session.setCurrentInProgressToken(entry.getTokenNumber());
            sessionRepository.save(session);
        }

        // Update appointment status to IN_PROGRESS
        Appointment apt = entry.getAppointment();
        apt.setStatus(AppointmentStatus.IN_PROGRESS);
        appointmentRepository.save(apt);

        if (consultationRecordRepository != null) {
            com.mediqueue.consultation.entity.ConsultationRecord cr = consultationRecordRepository.findByAppointmentId(apt.getId())
                    .orElseGet(() -> com.mediqueue.consultation.entity.ConsultationRecord.builder()
                            .consultationReference("CON-" + entry.getQueueDate().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + entry.getTokenNumber() + "-" + java.util.UUID.randomUUID().toString().substring(0, 6).toUpperCase())
                            .appointment(apt)
                            .patient(entry.getPatient())
                            .doctor(doctor)
                            .build());
            cr.setStatus(com.mediqueue.consultation.enums.ConsultationStatus.IN_PROGRESS);
            cr.setActualStartTimestamp(saved.getConsultationStartTimestamp());
            consultationRecordRepository.save(cr);
        }

        recordEvent(saved, prevStatus, QueueStatus.IN_PROGRESS, QueueEventType.CONSULTATION_STARTED, principal.getId(), "Consultation started", null);

        eventPublisher.publishEvent(new QueueDomainEvent(
                this,
                QueueEventType.CONSULTATION_STARTED,
                doctor.getId(),
                entry.getQueueDate(),
                saved,
                prevStatus,
                QueueStatus.IN_PROGRESS,
                false,
                "Consultation started for token " + saved.getTokenNumber()
        ));

        return QueueEntryResponse.from(saved);
    }

    @Transactional
    public QueueEntryResponse completeConsultation(AuthenticatedUser principal, Long queueEntryId) {
        QueueEntry entry = queueEntryRepository.findByIdWithLock(queueEntryId)
                .orElseThrow(() -> new ResourceNotFoundException("Queue entry not found with ID: " + queueEntryId));

        Doctor doctor = validateAndResolveDoctor(principal, entry.getDoctor().getId());

        transitionValidator.validateTransition(entry.getStatus(), QueueStatus.COMPLETED);

        QueueStatus prevStatus = entry.getStatus();
        entry.setStatus(QueueStatus.COMPLETED);
        entry.setConsultationEndTimestamp(Instant.now());
        QueueEntry saved = queueEntryRepository.save(entry);

        sessionRepository.findByDoctorIdAndQueueDateWithLock(doctor.getId(), entry.getQueueDate())
                .ifPresent(sess -> {
                    if (sess.getCurrentInProgressToken() != null && sess.getCurrentInProgressToken().equals(entry.getTokenNumber())) {
                        sess.setCurrentInProgressToken(null);
                    }
                    if (sess.getCurrentCalledToken() != null && sess.getCurrentCalledToken().equals(entry.getTokenNumber())) {
                        sess.setCurrentCalledToken(null);
                    }
                    sessionRepository.save(sess);
                });

        // Update appointment status to COMPLETED
        Appointment apt = entry.getAppointment();
        apt.setStatus(AppointmentStatus.COMPLETED);
        appointmentRepository.save(apt);

        if (consultationRecordRepository != null) {
            consultationRecordRepository.findByAppointmentId(apt.getId()).ifPresent(cr -> {
                cr.setStatus(com.mediqueue.consultation.enums.ConsultationStatus.COMPLETED);
                cr.setActualEndTimestamp(saved.getConsultationEndTimestamp());
                consultationRecordRepository.save(cr);
            });
        }

        recordEvent(saved, prevStatus, QueueStatus.COMPLETED, QueueEventType.CONSULTATION_COMPLETED, principal.getId(), "Consultation completed", null);

        notificationService.createNotification(
                saved.getPatient().getUser().getId(),
                NotificationType.QUEUE_STATUS_CHANGED,
                "Consultation Completed",
                String.format("Your consultation with Dr. %s is completed. Thank you!", doctor.getUser().getFullName()),
                saved.getAppointment().getId(),
                saved.getId()
        );

        eventPublisher.publishEvent(new QueueDomainEvent(
                this,
                QueueEventType.CONSULTATION_COMPLETED,
                doctor.getId(),
                entry.getQueueDate(),
                saved,
                prevStatus,
                QueueStatus.COMPLETED,
                false,
                "Consultation completed for token " + saved.getTokenNumber()
        ));

        return QueueEntryResponse.from(saved);
    }

    @Transactional
    public QueueSessionResponse pauseQueue(AuthenticatedUser principal, Long doctorId, String reason) {
        Doctor doctor = validateAndResolveDoctor(principal, doctorId);
        LocalDate todayHospital = availabilityService.nowInHospitalTime().toLocalDate();

        doctorQueueSessionService.ensureSessionExists(doctor.getId(), todayHospital);
        DoctorQueueSession session = sessionRepository.findByDoctorIdAndQueueDateWithLock(doctor.getId(), todayHospital)
                .orElseThrow(() -> new IllegalStateException("Failed to acquire doctor queue session"));

        session.setIsPaused(true);
        session.setPausedAt(Instant.now());
        session.setPauseReason(reason != null ? reason.trim() : "Doctor temporary break");
        session.setPausedByUserId(principal.getId());
        DoctorQueueSession saved = sessionRepository.save(session);

        eventPublisher.publishEvent(new QueueDomainEvent(
                this,
                QueueEventType.QUEUE_PAUSED,
                doctor.getId(),
                todayHospital,
                null,
                null,
                null,
                true,
                "Queue paused: " + saved.getPauseReason()
        ));

        return QueueSessionResponse.from(saved);
    }

    @Transactional
    public QueueSessionResponse resumeQueue(AuthenticatedUser principal, Long doctorId) {
        Doctor doctor = validateAndResolveDoctor(principal, doctorId);
        LocalDate todayHospital = availabilityService.nowInHospitalTime().toLocalDate();

        doctorQueueSessionService.ensureSessionExists(doctor.getId(), todayHospital);
        DoctorQueueSession session = sessionRepository.findByDoctorIdAndQueueDateWithLock(doctor.getId(), todayHospital)
                .orElseThrow(() -> new IllegalStateException("Failed to acquire doctor queue session"));

        session.setIsPaused(false);
        session.setPausedAt(null);
        session.setPauseReason(null);
        DoctorQueueSession saved = sessionRepository.save(session);

        eventPublisher.publishEvent(new QueueDomainEvent(
                this,
                QueueEventType.QUEUE_RESUMED,
                doctor.getId(),
                todayHospital,
                null,
                null,
                null,
                false,
                "Queue resumed"
        ));

        return QueueSessionResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public DoctorQueueSummaryResponse getDoctorQueueToday(AuthenticatedUser principal, Long doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + doctorId));

        LocalDate todayHospital = availabilityService.nowInHospitalTime().toLocalDate();
        List<QueueEntry> entries = queueEntryRepository.findByDoctorIdAndQueueDateOrderByCheckInTimestampAsc(doctor.getId(), todayHospital);

        Optional<DoctorQueueSession> sessionOpt = sessionRepository.findByDoctorIdAndQueueDate(doctor.getId(), todayHospital);
        boolean isPaused = sessionOpt.map(DoctorQueueSession::getIsPaused).orElse(false);
        String pauseReason = sessionOpt.map(DoctorQueueSession::getPauseReason).orElse(null);

        int waitingCount = (int) entries.stream().filter(e -> e.getStatus() == QueueStatus.WAITING).count();
        int calledCount = (int) entries.stream().filter(e -> e.getStatus() == QueueStatus.CALLED).count();
        int inProgressCount = (int) entries.stream().filter(e -> e.getStatus() == QueueStatus.IN_PROGRESS).count();
        int completedCount = (int) entries.stream().filter(e -> e.getStatus() == QueueStatus.COMPLETED).count();
        int skippedCount = (int) entries.stream().filter(e -> e.getStatus() == QueueStatus.SKIPPED).count();

        Integer servingToken = entries.stream()
                .filter(e -> e.getStatus() == QueueStatus.IN_PROGRESS)
                .findFirst()
                .map(QueueEntry::getTokenNumber)
                .orElse(null);

        Integer calledToken = entries.stream()
                .filter(e -> e.getStatus() == QueueStatus.CALLED)
                .findFirst()
                .map(QueueEntry::getTokenNumber)
                .orElse(null);

        boolean isAuthorizedStaff = principal != null && (
                principal.getRole() == UserRole.ADMIN ||
                        (principal.getRole() == UserRole.DOCTOR && doctor.getUser().getId().equals(principal.getId()))
        );

        List<QueueEntryResponse> fullEntries = isAuthorizedStaff
                ? entries.stream().map(QueueEntryResponse::from).toList()
                : null;

        List<QueuePublicEntryResponse> publicEntries = entries.stream().map(QueuePublicEntryResponse::from).toList();

        return DoctorQueueSummaryResponse.builder()
                .doctorId(doctor.getId())
                .doctorName(doctor.getUser().getFullName())
                .queueDate(todayHospital)
                .isPaused(isPaused)
                .pauseReason(pauseReason)
                .totalCheckedIn(entries.size())
                .waitingCount(waitingCount)
                .calledCount(calledCount)
                .inProgressCount(inProgressCount)
                .completedCount(completedCount)
                .skippedCount(skippedCount)
                .currentServingToken(servingToken)
                .currentCalledToken(calledToken)
                .entries(fullEntries)
                .publicEntries(publicEntries)
                .build();
    }

    @Transactional(readOnly = true)
    public QueueEntryResponse getMyActiveQueueStatus(AuthenticatedUser principal) {
        LocalDate todayHospital = availabilityService.nowInHospitalTime().toLocalDate();
        List<QueueEntry> activeEntries = queueEntryRepository.findActiveByPatientUserId(
                principal.getId(),
                todayHospital,
                List.of(QueueStatus.WAITING, QueueStatus.CALLED, QueueStatus.IN_PROGRESS)
        );

        if (activeEntries.isEmpty()) {
            throw new ResourceNotFoundException("No active queue entry found for you today");
        }

        return QueueEntryResponse.from(activeEntries.get(0));
    }

    @Transactional(readOnly = true)
    public List<QueueEventResponse> getQueueEvents(AuthenticatedUser principal, Long queueEntryId) {
        QueueEntry entry = queueEntryRepository.findById(queueEntryId)
                .orElseThrow(() -> new ResourceNotFoundException("Queue entry not found with ID: " + queueEntryId));

        validateQueueEntryViewAccess(principal, entry);

        List<QueueEvent> events = eventRepository.findByQueueEntryIdOrderByEventTimestampDesc(entry.getId());
        return events.stream().map(QueueEventResponse::from).toList();
    }

    @org.springframework.context.event.EventListener
    public void handleAppointmentCancelledEvent(com.mediqueue.appointment.event.AppointmentCancelledEvent event) {
        onAppointmentCancelled(event.getAppointment(), event.getReason());
    }

    @Transactional
    public void onAppointmentCancelled(Appointment appointment, String reason) {
        queueEntryRepository.findByAppointmentId(appointment.getId()).ifPresent(entry -> {
            if (entry.getStatus() == QueueStatus.WAITING || entry.getStatus() == QueueStatus.CALLED) {
                entry.setStatus(QueueStatus.CANCELLED);
                entry.setCancellationTimestamp(Instant.now());
                entry.setCancellationReason(reason);
                queueEntryRepository.save(entry);

                sessionRepository.findByDoctorIdAndQueueDateWithLock(entry.getDoctor().getId(), entry.getQueueDate())
                        .ifPresent(sess -> {
                            if (sess.getCurrentCalledToken() != null && sess.getCurrentCalledToken().equals(entry.getTokenNumber())) {
                                sess.setCurrentCalledToken(null);
                            }
                            if (sess.getCurrentInProgressToken() != null && sess.getCurrentInProgressToken().equals(entry.getTokenNumber())) {
                                sess.setCurrentInProgressToken(null);
                            }
                            sessionRepository.save(sess);
                        });

                recordEvent(entry, entry.getStatus(), QueueStatus.CANCELLED, QueueEventType.ENTRY_CANCELLED, null, reason, null);

                eventPublisher.publishEvent(new QueueDomainEvent(
                        this,
                        QueueEventType.ENTRY_CANCELLED,
                        entry.getDoctor().getId(),
                        entry.getQueueDate(),
                        entry,
                        entry.getStatus(),
                        QueueStatus.CANCELLED,
                        false,
                        "Token " + entry.getTokenNumber() + " cancelled"
                ));
            }
        });
    }

    private void recordEvent(
            QueueEntry entry,
            QueueStatus prev,
            QueueStatus next,
            QueueEventType type,
            Long actorUserId,
            String reason,
            String meta
    ) {
        QueueEvent event = QueueEvent.builder()
                .queueEntry(entry)
                .doctorId(entry.getDoctor().getId())
                .queueDate(entry.getQueueDate())
                .previousStatus(prev)
                .newStatus(next)
                .eventType(type)
                .actorUserId(actorUserId)
                .eventTimestamp(Instant.now())
                .reason(reason)
                .metadata(meta)
                .build();
        eventRepository.save(event);
    }

    private Doctor validateAndResolveDoctor(AuthenticatedUser principal, Long doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + doctorId));

        if (principal.getRole() == UserRole.ADMIN) {
            return doctor;
        }

        if (principal.getRole() == UserRole.DOCTOR) {
            if (doctor.getUser().getId().equals(principal.getId())) {
                return doctor;
            }
        }

        throw new AccessDeniedException("Access denied: You are not authorized to manage this doctor's queue");
    }

    private void validateAppointmentCheckInAccess(AuthenticatedUser principal, Appointment appointment) {
        if (principal.getRole() == UserRole.ADMIN) {
            return;
        }
        if (principal.getRole() == UserRole.PATIENT) {
            if (appointment.getPatient().getUser().getId().equals(principal.getId())) {
                return;
            }
        }
        if (principal.getRole() == UserRole.DOCTOR) {
            if (appointment.getDoctor().getUser().getId().equals(principal.getId())) {
                return;
            }
        }
        throw new AccessDeniedException("Access denied: You do not have permission to check in for this appointment");
    }

    private void validateQueueEntryViewAccess(AuthenticatedUser principal, QueueEntry entry) {
        if (principal.getRole() == UserRole.ADMIN) {
            return;
        }
        if (principal.getRole() == UserRole.PATIENT) {
            if (entry.getPatient().getUser().getId().equals(principal.getId())) {
                return;
            }
        }
        if (principal.getRole() == UserRole.DOCTOR) {
            if (entry.getDoctor().getUser().getId().equals(principal.getId())) {
                return;
            }
        }
        throw new AccessDeniedException("Access denied: You do not have permission to inspect this queue entry");
    }

    private String generateUniqueEntryReference(LocalDate date) {
        String datePrefix = date.format(DateTimeFormatter.BASIC_ISO_DATE);
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(CHAR_POOL.charAt(RANDOM.nextInt(CHAR_POOL.length())));
        }
        return "QE-" + datePrefix + "-" + sb;
    }
}
