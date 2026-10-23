CREATE TABLE doctors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    specialty VARCHAR(100) NOT NULL,
    qualification VARCHAR(150) NOT NULL,
    years_of_experience INT NOT NULL DEFAULT 0,
    professional_bio VARCHAR(1000) NULL,
    consultation_fee DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    verification_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_doctors_user_id UNIQUE (user_id),
    CONSTRAINT fk_doctors_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_doctors_department_id FOREIGN KEY (department_id) REFERENCES departments (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_doctors_dept_verification ON doctors (department_id, verification_status);
CREATE INDEX idx_doctors_specialty ON doctors (specialty);
