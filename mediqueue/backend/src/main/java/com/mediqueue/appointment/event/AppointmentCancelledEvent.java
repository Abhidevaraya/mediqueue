package com.mediqueue.appointment.event;

import com.mediqueue.appointment.entity.Appointment;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class AppointmentCancelledEvent extends ApplicationEvent {

    private final Appointment appointment;
    private final String reason;

    public AppointmentCancelledEvent(Object source, Appointment appointment, String reason) {
        super(source);
        this.appointment = appointment;
        this.reason = reason;
    }
}
