package py.com.ccp.eapn.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PortabilityRequest(
    UUID requestId,
    String msisdn,
    String documentNumber,
    Operator donorOperator,
    Operator recipientOperator,
    PortabilityStatus status,
    Instant requestedAt
) {

    public PortabilityRequest {
        Objects.requireNonNull(requestId, "requestId es obligatorio");
        Objects.requireNonNull(msisdn, "msisdn es obligatorio");
        Objects.requireNonNull(documentNumber, "documentNumber es obligatorio");
        Objects.requireNonNull(donorOperator, "donorOperator es obligatorio");
        Objects.requireNonNull(recipientOperator, "recipientOperator es obligatorio");
        Objects.requireNonNull(status, "status es obligatorio");
        Objects.requireNonNull(requestedAt, "requestedAt es obligatorio");

        if (!msisdn.matches("^595\\d{9}$")) {
            throw new IllegalArgumentException(
                "msisdn debe tener el formato internacional 595XXXXXXXXX"
            );
        }

        if (documentNumber.isBlank()) {
            throw new IllegalArgumentException(
                "documentNumber no puede estar vacío"
            );
        }

        if (donorOperator == recipientOperator) {
            throw new IllegalArgumentException(
                "Los operadores donante y receptor deben ser diferentes"
            );
        }
    }

    public static PortabilityRequest create(
        String msisdn,
        String documentNumber,
        Operator donorOperator,
        Operator recipientOperator
    ) {
        return new PortabilityRequest(
            UUID.randomUUID(),
            msisdn,
            documentNumber,
            donorOperator,
            recipientOperator,
            PortabilityStatus.CREATED,
            Instant.now()
        );
    }
}