-- ============================================================================
-- V13: Phase 5 Schema Enhancements, Notification Reliability & Analytics Indexes
-- ============================================================================

-- 1. Notification reliability & delivery tracking columns
ALTER TABLE notifications
    ADD COLUMN delivery_status VARCHAR(30) NOT NULL DEFAULT 'SENT',
    ADD COLUMN retry_count INT NOT NULL DEFAULT 0,
    ADD COLUMN last_attempt_at TIMESTAMP(6) NULL,
    ADD COLUMN failure_reason VARCHAR(255) NULL;

CREATE INDEX idx_notif_delivery ON notifications (delivery_status, created_at);

-- 2. Performance indexes for Analytics & Administrative searches
CREATE INDEX idx_apt_start_status ON appointments (start_date_time, status);
CREATE INDEX idx_apt_doc_status ON appointments (doctor_id, status);
CREATE INDEX idx_apt_dept_status ON appointments (department_id, status);

CREATE INDEX idx_cr_actual_start ON consultation_records (doctor_id, actual_start_timestamp);

CREATE INDEX idx_pred_doc_date ON prediction_logs (doctor_id, queue_date);

CREATE INDEX idx_cae_action_time ON clinical_audit_events (action, event_timestamp);
CREATE INDEX idx_cae_outcome_time ON clinical_audit_events (outcome, event_timestamp);
