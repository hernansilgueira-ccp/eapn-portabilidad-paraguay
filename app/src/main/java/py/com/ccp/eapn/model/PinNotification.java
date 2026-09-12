package py.com.ccp.eapn.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PinNotification(
    UUID requestId,
    String msisdn,
    String pin,
    Instant expiresAt
) {

    public PinNotification {
        Objects.requireNonNull(
            requestId,
            "requestId es obligatorio"
        );
        Objects.requireNonNull(
            msisdn,
            "msisdn es obligatorio"
        );
        Objects.requireNonNull(
            pin,
            "pin es obligatorio"
        );
        Objects.requireNonNull(
            expiresAt,
            "expiresAt es obligatorio"
        );

        if (!pin.matches("\\d{6}")) {
            throw new IllegalArgumentException(
                "El PIN debe contener seis dígitos"
            );
        }
    }
}