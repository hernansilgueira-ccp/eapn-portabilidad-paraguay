package py.com.ccp.eapn.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record DonorApprovalRequest(
    UUID requestId,
    String msisdn,
    String documentNumber,
    Operator donorOperator,
    Operator recipientOperator,
    Instant submittedAt
) {

    public DonorApprovalRequest {
        Objects.requireNonNull(
            requestId,
            "requestId es obligatorio"
        );
        Objects.requireNonNull(
            msisdn,
            "msisdn es obligatorio"
        );
        Objects.requireNonNull(
            documentNumber,
            "documentNumber es obligatorio"
        );
        Objects.requireNonNull(
            donorOperator,
            "donorOperator es obligatorio"
        );
        Objects.requireNonNull(
            recipientOperator,
            "recipientOperator es obligatorio"
        );
        Objects.requireNonNull(
            submittedAt,
            "submittedAt es obligatorio"
        );

        if (!msisdn.matches("^595\\d{9}$")) {
            throw new IllegalArgumentException(
                "msisdn debe tener el formato "
                    + "internacional 595XXXXXXXXX"
            );
        }

        if (documentNumber.isBlank()) {
            throw new IllegalArgumentException(
                "documentNumber no puede estar vacío"
            );
        }

        if (donorOperator == recipientOperator) {
            throw new IllegalArgumentException(
                "Los operadores donante y receptor "
                    + "deben ser diferentes"
            );
        }
    }
}