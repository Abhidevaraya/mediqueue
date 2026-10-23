package com.mediqueue.integration;

import com.mediqueue.appointment.entity.Appointment;
import com.mediqueue.appointment.enums.AppointmentStatus;
import com.mediqueue.appointment.repository.AppointmentRepository;
import com.mediqueue.department.entity.Department;
import com.mediqueue.department.repository.DepartmentRepository;
import com.mediqueue.doctor.entity.Doctor;
import com.mediqueue.doctor.enums.DoctorVerificationStatus;
import com.mediqueue.doctor.repository.DoctorRepository;
import com.mediqueue.exception.ConflictException;
import com.mediqueue.patient.entity.Patient;
import com.mediqueue.patient.repository.PatientRepository;
import com.mediqueue.queue.dto.QueueCheckInRequest;
import com.mediqueue.queue.dto.QueueEntryResponse;
import com.mediqueue.queue.entity.DoctorQueueSession;
import com.mediqueue.queue.entity.QueueEntry;
import com.mediqueue.queue.enums.QueueStatus;
import com.mediqueue.queue.repository.DoctorQueueSessionRepository;
import com.mediqueue.queue.repository.QueueEntryRepository;
import com.mediqueue.queue.service.QueueService;
import com.mediqueue.schedule.service.AvailabilityService;
import com.mediqueue.security.AuthenticatedUser;
import com.mediqueue.user.entity.User;
import com.mediqueue.user.enums.AccountStatus;
import com.mediqueue.user.enums.UserRole;
import com.mediqueue.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class Phase3DatabaseAndConcurrencyTest {

    @Autowired
    private QueueService queueService;

    @Autowired
    private QueueEntryRepository queueEntryRepository;

    @Autowired
    private DoctorQueueSessionRepository sessionRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private AvailabilityService availabilityService;

    @Test
    @DisplayName("Concurrent Check-In Allocates Strictly Unique Sequential Tokens in MySQL")
    void testConcurrentCheckInAndTokenAllocation() throws Exception {
        LocalDate todayHospital = availabilityService.nowInHospitalTime().toLocalDate();

        // 1. Provision Department and Doctor
        Department department = departmentRepository.save(Department.builder()
                .name("General Medicine " + System.currentTimeMillis())
                .description("Primary Care")
                .active(true)
                .build());

        User docUser = userRepository.save(User.builder()
                .email("dr.queue_" + System.currentTimeMillis() + "@example.com")
                .passwordHash("$2a$10$dummyhashforqueuetestonly")
                .fullName("Dr. Queue Specialist")
                .role(UserRole.DOCTOR)
                .accountStatus(AccountStatus.ACTIVE)
                .build());

        Doctor doctor = doctorRepository.save(Doctor.builder()
                .user(docUser)
                .department(department)
                .specialty("Primary Care")
                .qualification("MD General Medicine")
                .yearsOfExperience(10)
                .consultationFee(new BigDecimal("100.00"))
                .verificationStatus(DoctorVerificationStatus.VERIFIED)
                .build());

        AuthenticatedUser docPrincipal = new AuthenticatedUser(docUser);

        // 2. Provision 8 Patients and 8 Appointments for today
        int numPatients = 8;
        List<Appointment> appointments = new ArrayList<>();
        List<AuthenticatedUser> patientPrincipals = new ArrayList<>();

        for (int i = 0; i < numPatients; i++) {
            User pUser = userRepository.save(User.builder()
                    .email("pat.queue_" + i + "_" + System.currentTimeMillis() + "@example.com")
                    .passwordHash("$2a$10$dummyhashforqueuetestonly")
                    .fullName("Patient Queue " + i)
                    .role(UserRole.PATIENT)
                    .accountStatus(AccountStatus.ACTIVE)
                    .build());

            Patient patient = patientRepository.save(Patient.builder()
                    .user(pUser)
                    .build());

            patientPrincipals.add(new AuthenticatedUser(pUser));

            Appointment apt = appointmentRepository.save(Appointment.builder()
                    .appointmentReference("APT-Q-" + i + "-" + System.currentTimeMillis())
                    .patient(patient)
                    .doctor(doctor)
                    .department(department)
                    .startDateTime(LocalDateTime.of(todayHospital, LocalTime.of(9, 0).plusMinutes(i * 15L)))
                    .endDateTime(LocalDateTime.of(todayHospital, LocalTime.of(9, 15).plusMinutes(i * 15L)))
                    .slotDurationMinutes(15)
                    .status(AppointmentStatus.BOOKED)
                    .bookingTimestamp(Instant.now())
                    .build());

            appointments.add(apt);
        }

        // 3. Launch 8 concurrent threads attempting check-in simultaneously
        ExecutorService executor = Executors.newFixedThreadPool(numPatients);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(numPatients);

        List<QueueEntryResponse> checkInResults = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger checkInErrors = new AtomicInteger(0);

        for (int i = 0; i < numPatients; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startGate.await();
                    QueueEntryResponse resp = queueService.checkIn(
                            patientPrincipals.get(index),
                            new QueueCheckInRequest(appointments.get(index).getId())
                    );
                    checkInResults.add(resp);
                } catch (Exception e) {
                    checkInErrors.incrementAndGet();
                    System.err.println("Concurrent check-in thread failed: " + e.getMessage());
                } finally {
                    endGate.countDown();
                }
            });
        }

        startGate.countDown();
        endGate.await();

        // 4. Verify Concurrency Invariants
        assertEquals(0, checkInErrors.get(), "Zero check-in requests should fail");
        assertEquals(numPatients, checkInResults.size(), "All 8 check-ins must succeed");

        Set<Integer> allocatedTokens = new HashSet<>();
        for (QueueEntryResponse resp : checkInResults) {
            allocatedTokens.add(resp.getTokenNumber());
        }

        assertEquals(numPatients, allocatedTokens.size(), "Every allocated token must be strictly unique!");
        for (int t = 1; t <= numPatients; t++) {
            assertTrue(allocatedTokens.contains(t), "Token sequence must contain token " + t);
        }

        // 5. Verify MySQL Database Consistency
        DoctorQueueSession session = sessionRepository.findByDoctorIdAndQueueDate(doctor.getId(), todayHospital)
                .orElseThrow();
        assertEquals(numPatients, session.getLastTokenNumber(), "Session last_token_number in MySQL must equal 8");

        List<QueueEntry> entries = queueEntryRepository.findByDoctorIdAndQueueDateOrderByCheckInTimestampAsc(
                doctor.getId(), todayHospital
        );
        assertEquals(numPatients, entries.size(), "MySQL queue_entries table must contain exactly 8 rows");

        // 6. Test Concurrent Call-Next Protection (Competing Doctor Requests)
        int callThreads = 4;
        ExecutorService callExecutor = Executors.newFixedThreadPool(callThreads);
        CountDownLatch callStart = new CountDownLatch(1);
        CountDownLatch callEnd = new CountDownLatch(callThreads);

        AtomicInteger callSuccessCount = new AtomicInteger(0);
        AtomicInteger callConflictCount = new AtomicInteger(0);

        for (int i = 0; i < callThreads; i++) {
            callExecutor.submit(() -> {
                try {
                    callStart.await();
                    queueService.callNext(docPrincipal, doctor.getId());
                    callSuccessCount.incrementAndGet();
                } catch (ConflictException e) {
                    callConflictCount.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("Call next unexpected error: " + e.getMessage());
                } finally {
                    callEnd.countDown();
                }
            });
        }

        callStart.countDown();
        callEnd.await();
        callExecutor.shutdown();

        // Invariant: Exactly 1 call-next succeeds, other competing requests receive ConflictException
        assertEquals(1, callSuccessCount.get(), "Exactly 1 call-next request must succeed");
        assertEquals(callThreads - 1, callConflictCount.get(), "Other competing call-next threads must be rejected");

        // 7. Verify Consultation Progression
        QueueEntry calledEntry = queueEntryRepository.findFirstByDoctorIdAndQueueDateAndStatus(
                doctor.getId(), todayHospital, QueueStatus.CALLED
        ).orElseThrow();
        assertEquals(1, calledEntry.getTokenNumber(), "First waiting token (Token 1) must be called");

        // Start consultation
        QueueEntryResponse inProgressResp = queueService.startConsultation(docPrincipal, calledEntry.getId());
        assertEquals(QueueStatus.IN_PROGRESS, inProgressResp.getStatus());

        // Complete consultation
        QueueEntryResponse completedResp = queueService.completeConsultation(docPrincipal, calledEntry.getId());
        assertEquals(QueueStatus.COMPLETED, completedResp.getStatus());

        executor.shutdown();
    }
}
