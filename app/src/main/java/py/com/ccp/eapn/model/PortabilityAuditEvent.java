package py.com.ccp.eapn.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PortabilityAuditEvent(
    UUID auditId,
    UUID requestId,
    String eventType,
    PortabilityStatus status,
    String details,
    Instant occurredAt
) {

    public PortabilityAuditEvent {
        Objects.requireNonNull(
            auditId,
            "auditId es obligatorio"
        );
        Objects.requireNonNull(
            requestId,
            "requestId es obligatorio"
        );
        Objects.requireNonNull(
            eventType,
            "eventType es obligatorio"
        );
        Objects.requireNonNull(
            status,
            "status es obligatorio"
        );
        Objects.requireNonNull(
            occurredAt,
            "occurredAt es obligatorio"
        );

        if (eventType.isBlank()) {
            throw new IllegalArgumentException(
                "eventType no puede estar vacío"
            );
        }
    }

    public static PortabilityAuditEvent statusChanged(
        UUID requestId,
        PortabilityStatus status,
        String details
    ) {
        return new PortabilityAuditEvent(
            UUID.randomUUID(),
            requestId,
            "STATUS_CHANGED",
            status,
            details,
            Instant.now()
        );
    }
}