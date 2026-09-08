CREATE TABLE IF NOT EXISTS portability_requests (
    request_id UUID PRIMARY KEY,
    msisdn VARCHAR(12) NOT NULL,
    document_number VARCHAR(20) NOT NULL,
    donor_operator VARCHAR(20) NOT NULL,
    recipient_operator VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    pin_hash VARCHAR(255),
    pin_expires_at TIMESTAMPTZ,
    rejection_reason VARCHAR(255),

    CONSTRAINT chk_portability_msisdn
        CHECK (msisdn ~ '^595[0-9]{9}$'),

    CONSTRAINT chk_donor_operator
        CHECK (
            donor_operator IN (
                'TIGO',
                'PERSONAL',
                'CLARO',
                'VOX'
            )
        ),

    CONSTRAINT chk_recipient_operator
        CHECK (
            recipient_operator IN (
                'TIGO',
                'PERSONAL',
                'CLARO',
                'VOX'
            )
        ),

    CONSTRAINT chk_different_operators
        CHECK (donor_operator <> recipient_operator),

    CONSTRAINT chk_portability_status
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

CREATE INDEX IF NOT EXISTS idx_portability_requests_msisdn
    ON portability_requests (msisdn);

CREATE INDEX IF NOT EXISTS idx_portability_requests_status
    ON portability_requests (status);