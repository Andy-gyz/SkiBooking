CREATE TABLE email_verification_codes (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    code_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    resend_available_at TIMESTAMPTZ NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_email_verification_codes_email UNIQUE (email),
    CONSTRAINT chk_email_verification_attempt_count CHECK (attempt_count >= 0)
);

CREATE INDEX idx_email_verification_codes_expires_at
    ON email_verification_codes (expires_at);
