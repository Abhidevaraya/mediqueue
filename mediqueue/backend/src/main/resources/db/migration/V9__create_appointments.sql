CREATE TABLE appointments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    appointment_reference VARCHAR(50) NOT NULL,
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    start_date_time DATETIME(6) NOT NULL,
    end_date_time DATETIME(6) NOT NULL,
    slot_duration_minutes INT NOT NULL,
    status VARCHAR(25) NOT NULL DEFAULT 'BOOKED',
    reason VARCHAR(500) NULL,
    booking_timestamp TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    cancellation_timestamp TIMESTAMP(6) NULL,
    cancellation_reason VARCHAR(500) NULL,
    rescheduled_from_reference VARCHAR(50) NULL,
    rescheduled_timestamp TIMESTAMP(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_appointments_reference UNIQUE (appointment_reference),
    CONSTRAINT fk_appointments_patient FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT fk_appointments_doctor FOREIGN KEY (doctor_id) REFERENCES doctors (id),
    CONSTRAINT fk_appointments_department FOREIGN KEY (department_id) REFERENCES departments (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_appointments_patient_date ON appointments (patient_id, start_date_time);
CREATE INDEX idx_appointments_doctor_date ON appointments (doctor_id, start_date_time);
CREATE INDEX idx_appointments_status ON appointments (status);
