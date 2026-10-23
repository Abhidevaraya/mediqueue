package com.mediqueue.appointment;

import com.mediqueue.appointment.dto.AppointmentBookingRequest;
import com.mediqueue.appointment.dto.AppointmentCancelRequest;
import com.mediqueue.appointment.dto.AppointmentRescheduleRequest;
import com.mediqueue.appointment.dto.AppointmentResponse;
import com.mediqueue.appointment.entity.Appointment;
import com.mediqueue.appointment.enums.AppointmentStatus;
import com.mediqueue.appointment.hook.AppointmentBookingHook;
import com.mediqueue.appointment.repository.AppointmentRepository;
import com.mediqueue.appointment.service.AppointmentService;
import com.mediqueue.department.entity.Department;
import com.mediqueue.doctor.entity.Doctor;
import com.mediqueue.doctor.enums.DoctorVerificationStatus;
import com.mediqueue.doctor.repository.DoctorRepository;
import com.mediqueue.exception.BusinessException;
import com.mediqueue.exception.ConflictException;
import com.mediqueue.patient.entity.Patient;
import com.mediqueue.patient.repository.PatientRepository;
import com.mediqueue.schedule.entity.SlotInventory;
import com.mediqueue.schedule.repository.SlotInventoryRepository;
import com.mediqueue.schedule.service.AvailabilityService;
import com.mediqueue.schedule.service.SlotInventoryService;
import com.mediqueue.security.AuthenticatedUser;
import com.mediqueue.user.entity.User;
import com.mediqueue.user.enums.AccountStatus;
import com.mediqueue.user.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private SlotInventoryRepository slotInventoryRepository;

    @Mock
    private SlotInventoryService slotInventoryService;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private AvailabilityService availabilityService;

    @Mock
    private AppointmentBookingHook appointmentBookingHook;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AppointmentService appointmentService;

    private Patient createSamplePatient() {
        User user = User.builder()
                .id(10L)
                .fullName("Alice Patient")
                .role(UserRole.PATIENT)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        return Patient.builder()
                .id(1L)
                .user(user)
                .build();
    }

    private Doctor createSampleDoctor() {
        User user = User.builder()
                .id(20L)
                .fullName("Dr. House")
                .role(UserRole.DOCTOR)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        Department dept = Department.builder().id(5L).name("Diagnostics").active(true).build();
        return Doctor.builder()
                .id(2L)
                .user(user)
                .department(dept)
                .verificationStatus(DoctorVerificationStatus.VERIFIED)
                .build();
    }

    @Test
    @DisplayName("Patient successfully books an appointment and increments inventory")
    void testBookAppointmentSuccess() {
        Patient patient = createSamplePatient();
        Doctor doctor = createSampleDoctor();
        AuthenticatedUser principal = new AuthenticatedUser(patient.getUser());

        LocalDateTime slotTime = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0);
        AppointmentBookingRequest request = AppointmentBookingRequest.builder()
                .doctorId(2L)
                .startDateTime(slotTime)
                .reason("Consultation")
                .build();

        SlotInventory inventory = SlotInventory.builder()
                .id(1L)
                .doctor(doctor)
                .slotStartTime(slotTime)
                .slotEndTime(slotTime.plusMinutes(30))
                .totalCapacity(1)
                .bookedCount(0)
                .build();

        when(patientRepository.findByUserId(10L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(2L)).thenReturn(Optional.of(doctor));
        when(availabilityService.nowInHospitalTime())
                .thenReturn(ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")));
        when(availabilityService.validateAndResolveSlotTemplate(doctor, slotTime))
                .thenReturn(new AvailabilityService.SlotTemplate(slotTime, slotTime.plusMinutes(30), 30, 1));
        when(appointmentRepository.hasConflictingActiveAppointment(eq(1L), any(), any(), any(), isNull()))
                .thenReturn(false);
        when(slotInventoryRepository.findByDoctorIdAndSlotStartTimeWithLock(2L, slotTime))
                .thenReturn(Optional.of(inventory));
        when(appointmentRepository.findByAppointmentReference(any())).thenReturn(Optional.empty());
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> {
            Appointment a = inv.getArgument(0);
            a.setId(100L);
            return a;
        });

        AppointmentResponse response = appointmentService.bookAppointment(principal, request);

        assertNotNull(response);
        assertEquals(AppointmentStatus.BOOKED, response.getStatus());
        assertTrue(response.getAppointmentReference().startsWith("APT-"));
        assertEquals(1, inventory.getBookedCount()); // Capacity counter incremented
        verify(slotInventoryRepository).save(inventory);
        verify(appointmentBookingHook).onAppointmentBooked(any(Appointment.class));
    }

    @Test
    @DisplayName("Reject booking when slot capacity is already reached")
    void testBookAppointmentSlotFull() {
        Patient patient = createSamplePatient();
        Doctor doctor = createSampleDoctor();
        AuthenticatedUser principal = new AuthenticatedUser(patient.getUser());

        LocalDateTime slotTime = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0);
        AppointmentBookingRequest request = AppointmentBookingRequest.builder()
                .doctorId(2L)
                .startDateTime(slotTime)
                .build();

        SlotInventory fullInventory = SlotInventory.builder()
                .id(1L)
                .doctor(doctor)
                .slotStartTime(slotTime)
                .slotEndTime(slotTime.plusMinutes(30))
                .totalCapacity(1)
                .bookedCount(1) // Already full
                .build();

        when(patientRepository.findByUserId(10L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(2L)).thenReturn(Optional.of(doctor));
        when(availabilityService.nowInHospitalTime())
                .thenReturn(ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")));
        when(availabilityService.validateAndResolveSlotTemplate(doctor, slotTime))
                .thenReturn(new AvailabilityService.SlotTemplate(slotTime, slotTime.plusMinutes(30), 30, 1));
        when(appointmentRepository.hasConflictingActiveAppointment(eq(1L), any(), any(), any(), isNull()))
                .thenReturn(false);
        when(slotInventoryRepository.findByDoctorIdAndSlotStartTimeWithLock(2L, slotTime))
                .thenReturn(Optional.of(fullInventory));

        assertThrows(ConflictException.class, () -> appointmentService.bookAppointment(principal, request));
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cancellation releases slot inventory capacity")
    void testCancelAppointmentReleasesInventory() {
        Patient patient = createSamplePatient();
        Doctor doctor = createSampleDoctor();
        AuthenticatedUser principal = new AuthenticatedUser(patient.getUser());

        LocalDateTime slotTime = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0);
        Appointment appointment = Appointment.builder()
                .id(50L)
                .appointmentReference("APT-20261020-ABC123")
                .patient(patient)
                .doctor(doctor)
                .department(doctor.getDepartment())
                .startDateTime(slotTime)
                .endDateTime(slotTime.plusMinutes(30))
                .slotDurationMinutes(30)
                .status(AppointmentStatus.BOOKED)
                .build();

        SlotInventory inventory = SlotInventory.builder()
                .id(1L)
                .doctor(doctor)
                .slotStartTime(slotTime)
                .totalCapacity(1)
                .bookedCount(1)
                .build();

        when(appointmentRepository.findById(50L)).thenReturn(Optional.of(appointment));
        when(slotInventoryRepository.findByDoctorIdAndSlotStartTimeWithLock(2L, slotTime))
                .thenReturn(Optional.of(inventory));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        AppointmentCancelRequest cancelReq = new AppointmentCancelRequest("Family emergency");
        AppointmentResponse response = appointmentService.cancelAppointment(principal, 50L, cancelReq);

        assertEquals(AppointmentStatus.CANCELLED, response.getStatus());
        assertEquals(0, inventory.getBookedCount()); // Decremented
        verify(slotInventoryRepository).save(inventory);
    }

    @Test
    @DisplayName("Rescheduling to same time is rejected")
    void testRescheduleToSameTimeRejected() {
        Patient patient = createSamplePatient();
        Doctor doctor = createSampleDoctor();
        AuthenticatedUser principal = new AuthenticatedUser(patient.getUser());

        LocalDateTime slotTime = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0);
        Appointment appointment = Appointment.builder()
                .id(50L)
                .patient(patient)
                .doctor(doctor)
                .department(doctor.getDepartment())
                .startDateTime(slotTime)
                .status(AppointmentStatus.BOOKED)
                .build();

        when(appointmentRepository.findById(50L)).thenReturn(Optional.of(appointment));

        AppointmentRescheduleRequest req = new AppointmentRescheduleRequest(slotTime);
        assertThrows(BusinessException.class, () -> appointmentService.rescheduleAppointment(principal, 50L, req));
    }
}
