CREATE TABLE doctor_queue_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    doctor_id BIGINT NOT NULL,
    queue_date DATE NOT NULL,
    last_token_number INT NOT NULL DEFAULT 0,
    is_paused BOOLEAN NOT NULL DEFAULT FALSE,
    paused_at TIMESTAMP(6) NULL,
    pause_reason VARCHAR(255) NULL,
    paused_by_user_id BIGINT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_dqs_doctor FOREIGN KEY (doctor_id) REFERENCES doctors (id) ON DELETE CASCADE,
    CONSTRAINT uk_dqs_doctor_date UNIQUE (doctor_id, queue_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE queue_entries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    entry_reference VARCHAR(64) NOT NULL UNIQUE,
    appointment_id BIGINT NOT NULL UNIQUE,
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    queue_date DATE NOT NULL,
    token_number INT NOT NULL,
    status VARCHAR(30) NOT NULL,
    priority_category VARCHAR(30) NOT NULL DEFAULT 'STANDARD',
    check_in_timestamp TIMESTAMP(6) NOT NULL,
    called_timestamp TIMESTAMP(6) NULL,
    consultation_start_timestamp TIMESTAMP(6) NULL,
    consultation_end_timestamp TIMESTAMP(6) NULL,
    skip_timestamp TIMESTAMP(6) NULL,
    skip_reason VARCHAR(255) NULL,
    cancellation_timestamp TIMESTAMP(6) NULL,
    cancellation_reason VARCHAR(255) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_qe_appointment FOREIGN KEY (appointment_id) REFERENCES appointments (id) ON DELETE CASCADE,
    CONSTRAINT fk_qe_patient FOREIGN KEY (patient_id) REFERENCES patients (id) ON DELETE CASCADE,
    CONSTRAINT fk_qe_doctor FOREIGN KEY (doctor_id) REFERENCES doctors (id) ON DELETE CASCADE,
    CONSTRAINT fk_qe_department FOREIGN KEY (department_id) REFERENCES departments (id) ON DELETE CASCADE,
    CONSTRAINT uk_qe_doc_date_token UNIQUE (doctor_id, queue_date, token_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_qe_doc_date_status ON queue_entries (doctor_id, queue_date, status);
CREATE INDEX idx_qe_patient_date ON queue_entries (patient_id, queue_date);
CREATE INDEX idx_qe_order ON queue_entries (doctor_id, queue_date, status, check_in_timestamp);

CREATE TABLE queue_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    queue_entry_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    queue_date DATE NOT NULL,
    previous_status VARCHAR(30) NULL,
    new_status VARCHAR(30) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    actor_user_id BIGINT NULL,
    event_timestamp TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    reason VARCHAR(255) NULL,
    metadata TEXT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_qevt_entry FOREIGN KEY (queue_entry_id) REFERENCES queue_entries (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_qevt_entry ON queue_events (queue_entry_id, event_timestamp);
CREATE INDEX idx_qevt_doc_date ON queue_events (doctor_id, queue_date);

CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_user_id BIGINT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    title VARCHAR(150) NOT NULL,
    message VARCHAR(500) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP(6) NULL,
    appointment_id BIGINT NULL,
    queue_entry_id BIGINT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_notif_user FOREIGN KEY (recipient_user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_notif_user_read ON notifications (recipient_user_id, is_read, created_at);

CREATE TABLE prediction_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    queue_entry_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    queue_date DATE NOT NULL,
    eligible_patients_ahead INT NOT NULL,
    predicted_wait_minutes INT NOT NULL,
    prediction_method VARCHAR(50) NOT NULL,
    uses_fallback BOOLEAN NOT NULL,
    actual_wait_minutes INT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_pred_qe FOREIGN KEY (queue_entry_id) REFERENCES queue_entries (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_pred_qe ON prediction_logs (queue_entry_id);
