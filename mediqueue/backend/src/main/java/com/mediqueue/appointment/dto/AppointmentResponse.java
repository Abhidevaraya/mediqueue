package com.mediqueue.appointment.dto;

import com.mediqueue.appointment.entity.Appointment;
import com.mediqueue.appointment.enums.AppointmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentResponse {

    private Long id;
    private String appointmentReference;

    private Long patientId;
    private String patientName;
    private String patientEmail;
    private String patientPhone;

    private Long doctorId;
    private String doctorName;
    private String specialty;
    private BigDecimal consultationFee;

    private Long departmentId;
    private String departmentName;

    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private Integer slotDurationMinutes;

    private AppointmentStatus status;
    private String reason;
    private Instant bookingTimestamp;
    private Instant cancellationTimestamp;
    private String cancellationReason;
    private String rescheduledFromReference;
    private Instant rescheduledTimestamp;

    private Instant createdAt;
    private Instant updatedAt;

    public static AppointmentResponse from(Appointment appointment) {
        return AppointmentResponse.builder()
                .id(appointment.getId())
                .appointmentReference(appointment.getAppointmentReference())
                .patientId(appointment.getPatient().getId())
                .patientName(appointment.getPatient().getUser().getFullName())
                .patientEmail(appointment.getPatient().getUser().getEmail())
                .patientPhone(appointment.getPatient().getUser().getPhoneNumber())
                .doctorId(appointment.getDoctor().getId())
                .doctorName(appointment.getDoctor().getUser().getFullName())
                .specialty(appointment.getDoctor().getSpecialty())
                .consultationFee(appointment.getDoctor().getConsultationFee())
                .departmentId(appointment.getDepartment().getId())
                .departmentName(appointment.getDepartment().getName())
                .startDateTime(appointment.getStartDateTime())
                .endDateTime(appointment.getEndDateTime())
                .slotDurationMinutes(appointment.getSlotDurationMinutes())
                .status(appointment.getStatus())
                .reason(appointment.getReason())
                .bookingTimestamp(appointment.getBookingTimestamp())
                .cancellationTimestamp(appointment.getCancellationTimestamp())
                .cancellationReason(appointment.getCancellationReason())
                .rescheduledFromReference(appointment.getRescheduledFromReference())
                .rescheduledTimestamp(appointment.getRescheduledTimestamp())
                .createdAt(appointment.getCreatedAt())
                .updatedAt(appointment.getUpdatedAt())
                .build();
    }
}
