package com.mediqueue.appointment.hook;

import com.mediqueue.appointment.entity.Appointment;

/**
 * Extension boundary for Phase 3: Real-Time Queue Management & Token Generation.
 * Phase 3 will subscribe to or implement this hook to assign queue tokens upon appointment confirmation.
 */
public interface AppointmentBookingHook {

    void onAppointmentBooked(Appointment appointment);
}
