package py.com.ccp.eapn.model;

import java.util.Objects;

public record CreatePortabilityRequest(
    String msisdn,
    String documentNumber,
    Operator donorOperator,
    Operator recipientOperator
) {

    public CreatePortabilityRequest {
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

    public PortabilityRequest toPortabilityRequest() {
        return PortabilityRequest.create(
            msisdn,
            documentNumber,
            donorOperator,
            recipientOperator
        );
    }
}