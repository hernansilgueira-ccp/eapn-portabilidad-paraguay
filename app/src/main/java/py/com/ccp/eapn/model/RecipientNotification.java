package py.com.ccp.eapn.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record RecipientNotification(
    UUID requestId,
    String msisdn,
    Operator recipientOperator,
    PortabilityStatus status,
    String rejectionReason,
    Instant notifiedAt
) {

    public RecipientNotification {
        Objects.requireNonNull(
            requestId,
            "requestId es obligatorio"
        );
        Objects.requireNonNull(
            msisdn,
            "msisdn es obligatorio"
        );
        Objects.requireNonNull(
            recipientOperator,
            "recipientOperator es obligatorio"
        );
        Objects.requireNonNull(
            status,
            "status es obligatorio"
        );
        Objects.requireNonNull(
            notifiedAt,
            "notifiedAt es obligatorio"
        );

        if (!msisdn.matches("^595\\d{9}$")) {
            throw new IllegalArgumentException(
                "msisdn debe tener el formato "
                    + "internacional 595XXXXXXXXX"
            );
        }

        if (
            status != PortabilityStatus.COMPLETED
                && status != PortabilityStatus.REJECTED
        ) {
            throw new IllegalArgumentException(
                "La notificación final debe ser "
                    + "COMPLETED o REJECTED"
            );
        }

        if (
            status == PortabilityStatus.REJECTED
                && (
                    rejectionReason == null
                        || rejectionReason.isBlank()
                )
        ) {
            throw new IllegalArgumentException(
                "Una notificación rechazada "
                    + "debe indicar el motivo"
            );
        }

        if (status == PortabilityStatus.COMPLETED) {
            rejectionReason = null;
        }
    }

    public static RecipientNotification completed(
        UUID requestId,
        String msisdn,
        Operator recipientOperator
    ) {
        return new RecipientNotification(
            requestId,
            msisdn,
            recipientOperator,
            PortabilityStatus.COMPLETED,
            null,
            Instant.now()
        );
    }

    public static RecipientNotification rejected(
        UUID requestId,
        String msisdn,
        Operator recipientOperator,
        String rejectionReason
    ) {
        return new RecipientNotification(
            requestId,
            msisdn,
            recipientOperator,
            PortabilityStatus.REJECTED,
            rejectionReason,
            Instant.now()
        );
    }
}