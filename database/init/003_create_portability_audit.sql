CREATE TABLE IF NOT EXISTS portability_audit (
    audit_id UUID PRIMARY KEY,
    request_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    details VARCHAR(500),
    occurred_at TIMESTAMPTZ NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_audit_portability_request
        FOREIGN KEY (request_id)
        REFERENCES portability_requests (request_id),

    CONSTRAINT chk_audit_status
        CHECK (
            status IN (
                'CREATED',
                'PIN_GENERATED',
                'CONFIRMED',
                'PENDING_DONOR',
                'APPROVED',
                'REJECTED',
                'COMPLETED'
            )
        )
);

CREATE INDEX IF NOT EXISTS idx_portability_audit_request
    ON portability_audit (
        request_id,
        occurred_at
    );

CREATE INDEX IF NOT EXISTS idx_portability_audit_status
    ON portability_audit (status);