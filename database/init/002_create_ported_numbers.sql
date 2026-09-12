CREATE TABLE IF NOT EXISTS ported_numbers (
    request_id UUID PRIMARY KEY,
    msisdn VARCHAR(12) NOT NULL UNIQUE,
    previous_operator VARCHAR(20) NOT NULL,
    current_operator VARCHAR(20) NOT NULL,
    ported_at TIMESTAMPTZ NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ported_number_request
        FOREIGN KEY (request_id)
        REFERENCES portability_requests (request_id),

    CONSTRAINT chk_ported_number_msisdn
        CHECK (msisdn ~ '^595[0-9]{9}$'),

    CONSTRAINT chk_previous_operator
        CHECK (
            previous_operator IN (
                'TIGO',
                'PERSONAL',
                'CLARO',
                'VOX'
            )
        ),

    CONSTRAINT chk_current_operator
        CHECK (
            current_operator IN (
                'TIGO',
                'PERSONAL',
                'CLARO',
                'VOX'
            )
        ),

    CONSTRAINT chk_different_ported_operators
        CHECK (
            previous_operator <> current_operator
        )
);

CREATE INDEX IF NOT EXISTS idx_ported_numbers_msisdn
    ON ported_numbers (msisdn);

CREATE INDEX IF NOT EXISTS idx_ported_numbers_current_operator
    ON ported_numbers (current_operator);