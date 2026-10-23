package com.mediqueue.integration;

import com.mediqueue.appointment.dto.AppointmentBookingRequest;
import com.mediqueue.appointment.dto.AppointmentCancelRequest;
import com.mediqueue.appointment.dto.AppointmentResponse;
import com.mediqueue.appointment.enums.AppointmentStatus;
import com.mediqueue.appointment.repository.AppointmentRepository;
import com.mediqueue.appointment.service.AppointmentService;
import com.mediqueue.department.entity.Department;
import com.mediqueue.department.repository.DepartmentRepository;
import com.mediqueue.doctor.entity.Doctor;
import com.mediqueue.doctor.enums.DoctorVerificationStatus;
import com.mediqueue.doctor.repository.DoctorRepository;
import com.mediqueue.exception.BusinessException;
import com.mediqueue.exception.ConflictException;
import com.mediqueue.patient.entity.Patient;
import com.mediqueue.patient.repository.PatientRepository;
import com.mediqueue.schedule.entity.DoctorSchedule;
import com.mediqueue.schedule.entity.SlotInventory;
import com.mediqueue.schedule.repository.DoctorScheduleRepository;
import com.mediqueue.schedule.repository.SlotInventoryRepository;
import com.mediqueue.security.AuthenticatedUser;
import com.mediqueue.user.entity.User;
import com.mediqueue.user.enums.AccountStatus;
import com.mediqueue.user.enums.UserRole;
import com.mediqueue.user.repository.UserRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class Phase2DatabaseAndConcurrencyTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private DoctorScheduleRepository doctorScheduleRepository;

    @Autowired
    private SlotInventoryRepository slotInventoryRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private AppointmentService appointmentService;

    @Test
    @DisplayName("Verify Flyway migrations V1 through V9 are successfully applied to MySQL")
    void testFlywayPhase2MigrationsApplied() {
        assertNotNull(flyway);
        var applied = flyway.info().applied();
        assertTrue(applied.length >= 9, "Expected at least 9 applied migrations");
        assertEquals("create doctor schedules", applied[5].getDescription().toLowerCase());
        assertEquals("create schedule exceptions", applied[6].getDescription().toLowerCase());
        assertEquals("create slot inventories", applied[7].getDescription().toLowerCase());
        assertEquals("create appointments", applied[8].getDescription().toLowerCase());
    }

    @Test
    @DisplayName("Verify concurrent booking competition strictly prevents overbooking slot capacity in MySQL")
    void testConcurrentBookingCapacityEnforcement() throws Exception {
        // 1. Setup Department & Doctor
        String deptName = "ConcurrencyDept_" + UUID.randomUUID().toString().substring(0, 8);
        Department dept = departmentRepository.save(Department.builder().name(deptName).active(true).build());

        String docEmail = "doc_" + UUID.randomUUID() + "@mediqueue.com";
        User docUser = userRepository.save(User.builder()
                .fullName("Dr. Concurrency Specialist")
                .email(docEmail)
                .passwordHash("$2a$10$hashedpassword")
                .role(UserRole.DOCTOR)
                .accountStatus(AccountStatus.ACTIVE)
                .build());

        Doctor doctor = doctorRepository.save(Doctor.builder()
                .user(docUser)
                .department(dept)
                .specialty("Concurrency Studies")
                .qualification("MD, PhD")
                .yearsOfExperience(15)
                .consultationFee(BigDecimal.valueOf(100.00))
                .verificationStatus(DoctorVerificationStatus.VERIFIED)
                .build());

        // Target Date: next Wednesday in the future
        LocalDate targetDate = LocalDate.now().plusWeeks(1).with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY));
        LocalTime startTime = LocalTime.of(10, 0);
        LocalTime endTime = LocalTime.of(12, 0);

        // Schedule with capacity = 2 for slot 10:00 - 10:30
        int slotCapacity = 2;
        doctorScheduleRepository.save(DoctorSchedule.builder()
                .doctor(doctor)
                .dayOfWeek(DayOfWeek.WEDNESDAY)
                .startTime(startTime)
                .endTime(endTime)
                .slotDurationMinutes(30)
                .slotCapacity(slotCapacity)
                .active(true)
                .build());

        LocalDateTime bookingSlot = LocalDateTime.of(targetDate, startTime);

        // 2. Pre-create 8 distinct patients to simulate 8 competing simultaneous requests
        int concurrentPatients = 8;
        List<AuthenticatedUser> patientPrincipals = new ArrayList<>();
        for (int i = 0; i < concurrentPatients; i++) {
            String pEmail = "patient_conc_" + i + "_" + UUID.randomUUID() + "@mediqueue.com";
            User pUser = userRepository.save(User.builder()
                    .fullName("Competing Patient " + i)
                    .email(pEmail)
                    .passwordHash("hash")
                    .role(UserRole.PATIENT)
                    .accountStatus(AccountStatus.ACTIVE)
                    .build());
            patientRepository.save(Patient.builder().user(pUser).build());
            patientPrincipals.add(new AuthenticatedUser(pUser));
        }

        // 3. Launch 8 concurrent booking threads attempting to book the exact same slot
        ExecutorService executor = Executors.newFixedThreadPool(concurrentPatients);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(concurrentPatients);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger capacityConflictCount = new AtomicInteger(0);
        List<AppointmentResponse> bookedAppointments = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < concurrentPatients; i++) {
            final AuthenticatedUser principal = patientPrincipals.get(i);
            executor.submit(() -> {
                try {
                    startGate.await(); // Synchronize all threads to start simultaneously
                    AppointmentBookingRequest request = AppointmentBookingRequest.builder()
                            .doctorId(doctor.getId())
                            .startDateTime(bookingSlot)
                            .reason("Competing booking test")
                            .build();

                    AppointmentResponse resp = appointmentService.bookAppointment(principal, request);
                    successCount.incrementAndGet();
                    bookedAppointments.add(resp);
                } catch (ConflictException | BusinessException e) {
                    capacityConflictCount.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("Unexpected concurrency thread exception: " + e.getClass().getName() + ": " + e.getMessage());
                } finally {
                    endGate.countDown();
                }
            });
        }

        // Unleash all threads at once
        startGate.countDown();
        endGate.await();
        executor.shutdown();

        // 4. Assert Concurrency Invariants
        assertEquals(slotCapacity, successCount.get(), "Exactly configured capacity (" + slotCapacity + ") must succeed");
        assertEquals(concurrentPatients - slotCapacity, capacityConflictCount.get(), "All excess requests must be rejected with ConflictException");

        // 5. Verify MySQL Database Consistency
        SlotInventory inventory = slotInventoryRepository.findByDoctorIdAndSlotStartTime(doctor.getId(), bookingSlot)
                .orElseThrow();
        assertEquals(slotCapacity, inventory.getBookedCount(), "Slot inventory booked count must match slot capacity");

        int activeCount = appointmentRepository.countActiveBookings(
                doctor.getId(),
                bookingSlot,
                List.of(AppointmentStatus.BOOKED)
        );
        assertEquals(slotCapacity, activeCount, "Total active database appointments must strictly equal slot capacity");

        // 6. Test Atomic Cancellation Capacity Release
        AppointmentResponse firstBooked = bookedAppointments.get(0);
        AuthenticatedUser ownerPrincipal = patientPrincipals.stream()
                .filter(p -> p.getEmail().equals(firstBooked.getPatientEmail()))
                .findFirst()
                .orElseThrow();

        appointmentService.cancelAppointment(ownerPrincipal, firstBooked.getId(), new AppointmentCancelRequest("Cancelling test"));

        SlotInventory updatedInventory = slotInventoryRepository.findByDoctorIdAndSlotStartTime(doctor.getId(), bookingSlot)
                .orElseThrow();
        assertEquals(slotCapacity - 1, updatedInventory.getBookedCount(), "Cancellation must decrement bookedCount in MySQL");
    }
}
