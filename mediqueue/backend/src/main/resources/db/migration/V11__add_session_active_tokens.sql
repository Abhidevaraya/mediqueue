-- V11: Add current active token tracking to doctor_queue_sessions for serializable concurrency control
ALTER TABLE doctor_queue_sessions
    ADD COLUMN current_called_token INT NULL AFTER last_token_number,
    ADD COLUMN current_in_progress_token INT NULL AFTER current_called_token;
