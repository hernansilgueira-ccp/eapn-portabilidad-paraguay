package py.com.ccp.eapn.model;

import java.util.Objects;
import java.util.UUID;

public record PinConfirmation(
    UUID requestId,
    String pin
) {

    public PinConfirmation {
        Objects.requireNonNull(
            requestId,
            "requestId es obligatorio"
        );
        Objects.requireNonNull(
            pin,
            "pin es obligatorio"
        );

        if (!pin.matches("\\d{6}")) {
            throw new IllegalArgumentException(
                "El PIN debe contener seis dígitos"
            );
        }
    }
}