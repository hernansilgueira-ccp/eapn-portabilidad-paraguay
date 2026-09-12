package py.com.ccp.eapn.route;

import org.apache.camel.builder.RouteBuilder;
import py.com.ccp.eapn.model.PinNotification;
import py.com.ccp.eapn.model.PortabilityRequest;
import py.com.ccp.eapn.service.PinService;
import py.com.ccp.eapn.service.PortabilityJsonSerializer;

public class PortabilityRequestConsumerRoute
    extends RouteBuilder {

    public static final String INPUT_ENDPOINT =
        "jms:queue:portability.requests"
            + "?connectionFactory=#jmsConnectionFactory";

    public static final String DATABASE_ENDPOINT =
        "sql:UPDATE portability_requests "
            + "SET status = 'PIN_GENERATED', "
            + "pin_hash = :#pinHash, "
            + "pin_expires_at = "
            + "CAST(:#pinExpiresAt AS TIMESTAMPTZ), "
            + "updated_at = CURRENT_TIMESTAMP "
            + "WHERE request_id = "
            + "CAST(:#requestId AS UUID)"
            + "?dataSource=#dataSource";

    public static final String OUTPUT_ENDPOINT =
        "jms:queue:portability.pin.notifications"
            + "?connectionFactory=#jmsConnectionFactory";

    private final String inputEndpoint;
    private final String databaseEndpoint;
    private final String outputEndpoint;
    private final PinService pinService;
    private final PortabilityJsonSerializer serializer;

    public PortabilityRequestConsumerRoute() {
        this(
            INPUT_ENDPOINT,
            DATABASE_ENDPOINT,
            OUTPUT_ENDPOINT,
            new PinService(),
            new PortabilityJsonSerializer()
        );
    }

    PortabilityRequestConsumerRoute(
        String inputEndpoint,
        String databaseEndpoint,
        String outputEndpoint,
        PinService pinService,
        PortabilityJsonSerializer serializer
    ) {
        this.inputEndpoint = inputEndpoint;
        this.databaseEndpoint = databaseEndpoint;
        this.outputEndpoint = outputEndpoint;
        this.pinService = pinService;
        this.serializer = serializer;
    }

    @Override
    public void configure() {
        from(inputEndpoint)
            .routeId("portability-request-consumer")
            .bean(
                serializer,
                "deserializeRequest"
            )
            .setProperty(
                "portabilityRequest",
                body()
            )
            .bean(
                pinService,
                "generate"
            )
            .process(exchange -> {
                PortabilityRequest request =
                    exchange.getProperty(
                        "portabilityRequest",
                        PortabilityRequest.class
                    );

                PinService.GeneratedPin generatedPin =
                    exchange.getMessage().getBody(
                        PinService.GeneratedPin.class
                    );

                PinNotification notification =
                    new PinNotification(
                        request.requestId(),
                        request.msisdn(),
                        generatedPin.plainPin(),
                        generatedPin.expiresAt()
                    );

                exchange.setProperty(
                    "pinNotification",
                    notification
                );

                exchange.getMessage().setHeader(
                    "requestId",
                    request.requestId().toString()
                );
                exchange.getMessage().setHeader(
                    "pinHash",
                    generatedPin.pinHash()
                );
                exchange.getMessage().setHeader(
                    "pinExpiresAt",
                    generatedPin.expiresAt().toString()
                );
            })
            .log(
                "PIN generado para la solicitud "
                    + "${header.requestId}"
            )
            .to(databaseEndpoint)
            .setBody(
                exchangeProperty("pinNotification")
            )
            .bean(
                serializer,
                "serialize"
            )
            .to(outputEndpoint)
            .log(
                "Notificación de PIN enviada para "
                    + "${header.requestId}"
            );
    }
}