package py.com.ccp.eapn.route;

import org.apache.camel.builder.RouteBuilder;
import py.com.ccp.eapn.model.PinConfirmation;
import py.com.ccp.eapn.service.PinService;
import py.com.ccp.eapn.service.PortabilityJsonSerializer;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;

public class PinConfirmationRoute extends RouteBuilder {

    public static final String INPUT_ENDPOINT =
        "direct:pin-confirmation";

    public static final String SELECT_ENDPOINT =
        "sql:SELECT pin_hash, pin_expires_at, status "
            + "FROM portability_requests "
            + "WHERE request_id = CAST(:#requestId AS UUID)"
            + "?dataSource=#dataSource"
            + "&outputType=SelectOne";

    public static final String CONFIRM_ENDPOINT =
        "sql:UPDATE portability_requests "
            + "SET status = 'CONFIRMED', "
            + "rejection_reason = NULL, "
            + "updated_at = CURRENT_TIMESTAMP "
            + "WHERE request_id = CAST(:#requestId AS UUID)"
            + "?dataSource=#dataSource";

    public static final String REJECT_ENDPOINT =
        "sql:UPDATE portability_requests "
            + "SET status = 'REJECTED', "
            + "rejection_reason = :#rejectionReason, "
            + "updated_at = CURRENT_TIMESTAMP "
            + "WHERE request_id = CAST(:#requestId AS UUID)"
            + "?dataSource=#dataSource";

    private final String inputEndpoint;
    private final String selectEndpoint;
    private final String confirmEndpoint;
    private final String rejectEndpoint;
    private final PinService pinService;
    private final PortabilityJsonSerializer serializer;

    public PinConfirmationRoute() {
        this(
            INPUT_ENDPOINT,
            SELECT_ENDPOINT,
            CONFIRM_ENDPOINT,
            REJECT_ENDPOINT,
            new PinService(),
            new PortabilityJsonSerializer()
        );
    }

    PinConfirmationRoute(
        String inputEndpoint,
        String selectEndpoint,
        String confirmEndpoint,
        String rejectEndpoint,
        PinService pinService,
        PortabilityJsonSerializer serializer
    ) {
        this.inputEndpoint = inputEndpoint;
        this.selectEndpoint = selectEndpoint;
        this.confirmEndpoint = confirmEndpoint;
        this.rejectEndpoint = rejectEndpoint;
        this.pinService = pinService;
        this.serializer = serializer;
    }

    @Override
    public void configure() {
        from(inputEndpoint)
            .routeId("pin-confirmation")
            .bean(
                serializer,
                "deserializePinConfirmation"
            )
            .process(exchange -> {
                PinConfirmation confirmation =
                    exchange.getMessage().getBody(
                        PinConfirmation.class
                    );

                exchange.setProperty(
                    "pinConfirmation",
                    confirmation
                );

                exchange.getMessage().setHeader(
                    "requestId",
                    confirmation.requestId().toString()
                );
            })
            .to(selectEndpoint)
            .process(exchange -> {
                Map<?, ?> requestData =
                    exchange.getMessage().getBody(
                        Map.class
                    );

                if (requestData == null) {
                    throw new IllegalArgumentException(
                        "No existe la solicitud indicada"
                    );
                }

                PinConfirmation confirmation =
                    exchange.getProperty(
                        "pinConfirmation",
                        PinConfirmation.class
                    );

                String currentStatus = String.valueOf(
                    value(requestData, "status")
                );

                if (!"PIN_GENERATED".equals(currentStatus)) {
                    throw new IllegalStateException(
                        "La solicitud no está esperando "
                            + "la confirmación del PIN"
                    );
                }

                String pinHash = String.valueOf(
                    value(requestData, "pin_hash")
                );

                Instant expiresAt = toInstant(
                    value(requestData, "pin_expires_at")
                );

                boolean expired =
                    expiresAt == null
                        || !expiresAt.isAfter(Instant.now());

                boolean valid =
                    !expired
                        && pinService.matches(
                            confirmation.pin(),
                            pinHash
                        );

                exchange.getMessage().setHeader(
                    "confirmationStatus",
                    valid ? "CONFIRMED" : "REJECTED"
                );

                if (!valid) {
                    exchange.getMessage().setHeader(
                        "rejectionReason",
                        expired
                            ? "PIN_EXPIRED"
                            : "INVALID_PIN"
                    );
                }
            })
            .choice()
                .when(
                    header("confirmationStatus")
                        .isEqualTo("CONFIRMED")
                )
                    .to(confirmEndpoint)
                    .log(
                        "PIN confirmado para la solicitud "
                            + "${header.requestId}"
                    )
                .otherwise()
                    .to(rejectEndpoint)
                    .log(
                        "Solicitud ${header.requestId} rechazada: "
                            + "${header.rejectionReason}"
                    )
            .end();
    }

    private static Object value(
        Map<?, ?> values,
        String expectedKey
    ) {
        return values.entrySet()
            .stream()
            .filter(entry ->
                expectedKey.equalsIgnoreCase(
                    String.valueOf(entry.getKey())
                )
            )
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(null);
    }

    private static Instant toInstant(Object value) {
        if (value instanceof Instant instant) {
            return instant;
        }

        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toInstant();
        }

        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant();
        }

        return value == null
            ? null
            : Instant.parse(value.toString());
    }
}