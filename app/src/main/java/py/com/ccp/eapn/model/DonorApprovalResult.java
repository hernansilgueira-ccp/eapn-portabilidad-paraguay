package py.com.ccp.eapn.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record DonorApprovalResult(
    UUID requestId,
    Operator donorOperator,
    PortabilityStatus status,
    String rejectionReason,
    Instant decidedAt
) {

    public DonorApprovalResult {
        Objects.requireNonNull(
            requestId,
            "requestId es obligatorio"
        );
        Objects.requireNonNull(
            donorOperator,
            "donorOperator es obligatorio"
        );
        Objects.requireNonNull(
            status,
            "status es obligatorio"
        );
        Objects.requireNonNull(
            decidedAt,
            "decidedAt es obligatorio"
        );

        if (
            status != PortabilityStatus.APPROVED
                && status != PortabilityStatus.REJECTED
        ) {
            throw new IllegalArgumentException(
                "El resultado debe ser APPROVED o REJECTED"
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
                "Una solicitud rechazada debe indicar el motivo"
            );
        }

        if (status == PortabilityStatus.APPROVED) {
            rejectionReason = null;
        }
    }

    public static DonorApprovalResult approved(
        UUID requestId,
        Operator donorOperator
    ) {
        return new DonorApprovalResult(
            requestId,
            donorOperator,
            PortabilityStatus.APPROVED,
            null,
            Instant.now()
        );
    }

    public static DonorApprovalResult rejected(
        UUID requestId,
        Operator donorOperator,
        String rejectionReason
    ) {
        return new DonorApprovalResult(
            requestId,
            donorOperator,
            PortabilityStatus.REJECTED,
            rejectionReason,
            Instant.now()
        );
    }
}