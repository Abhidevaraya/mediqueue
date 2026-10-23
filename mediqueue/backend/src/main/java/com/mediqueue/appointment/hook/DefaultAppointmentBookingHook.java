package com.mediqueue.appointment.hook;

import com.mediqueue.appointment.entity.Appointment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@org.springframework.context.annotation.Primary
@lombok.RequiredArgsConstructor
public class DefaultAppointmentBookingHook implements AppointmentBookingHook {

    private static final Logger log = LoggerFactory.getLogger(DefaultAppointmentBookingHook.class);
    private final com.mediqueue.notification.service.NotificationService notificationService;

    @Override
    public void onAppointmentBooked(Appointment appointment) {
        log.info("Appointment booking hook executed for reference: {}. Ready for queue check-in on {}",
                appointment.getAppointmentReference(), appointment.getStartDateTime().toLocalDate());
        try {
            notificationService.createNotification(
                    appointment.getPatient().getUser().getId(),
                    com.mediqueue.notification.enums.NotificationType.APPOINTMENT_CHANGED,
                    "Appointment Confirmed",
                    String.format("Your appointment (%s) with Dr. %s is confirmed for %s.",
                            appointment.getAppointmentReference(),
                            appointment.getDoctor().getUser().getFullName(),
                            appointment.getStartDateTime()),
                    appointment.getId(),
                    null
            );
        } catch (Exception e) {
            log.warn("Failed to dispatch appointment confirmation notification: {}", e.getMessage());
        }
    }
}
