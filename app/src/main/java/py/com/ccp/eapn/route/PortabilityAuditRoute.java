package py.com.ccp.eapn.route;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import py.com.ccp.eapn.model.PortabilityAuditEvent;
import py.com.ccp.eapn.service.PortabilityJsonSerializer;

public class PortabilityAuditRoute
    extends RouteBuilder {

    public static final String INPUT_ENDPOINT =
        "jms:queue:portability.audit"
            + "?connectionFactory=#jmsConnectionFactory";

    public static final String DATABASE_ENDPOINT =
        "sql:INSERT INTO portability_audit ("
            + "audit_id, request_id, event_type, "
            + "status, details, occurred_at"
            + ") VALUES ("
            + "CAST(:#auditId AS UUID), "
            + "CAST(:#requestId AS UUID), "
            + ":#eventType, :#status, :#details, "
            + "CAST(:#occurredAt AS TIMESTAMPTZ)"
            + ")"
            + "?dataSource=#dataSource";

    public static final String DEAD_LETTER_ENDPOINT =
        "jms:queue:portability.errors.dlq"
            + "?connectionFactory=#jmsConnectionFactory";

    private final String inputEndpoint;
    private final String databaseEndpoint;
    private final String deadLetterEndpoint;
    private final PortabilityJsonSerializer serializer;

    public PortabilityAuditRoute() {
        this(
            INPUT_ENDPOINT,
            DATABASE_ENDPOINT,
            DEAD_LETTER_ENDPOINT,
            new PortabilityJsonSerializer()
        );
    }

    PortabilityAuditRoute(
        String inputEndpoint,
        String databaseEndpoint,
        String deadLetterEndpoint,
        PortabilityJsonSerializer serializer
    ) {
        this.inputEndpoint = inputEndpoint;
        this.databaseEndpoint = databaseEndpoint;
        this.deadLetterEndpoint = deadLetterEndpoint;
        this.serializer = serializer;
    }

    @Override
    public void configure() {
        errorHandler(
            deadLetterChannel(deadLetterEndpoint)
                .useOriginalMessage()
                .maximumRedeliveries(3)
                .redeliveryDelay(1000)
                .retryAttemptedLogLevel(
                    LoggingLevel.WARN
                )
        );

        from(inputEndpoint)
            .routeId("portability-audit")
            .bean(
                serializer,
                "deserializeAuditEvent"
            )
            .process(exchange -> {
                PortabilityAuditEvent event =
                    exchange.getMessage().getBody(
                        PortabilityAuditEvent.class
                    );

                exchange.getMessage().setHeader(
                    "auditId",
                    event.auditId().toString()
                );
                exchange.getMessage().setHeader(
                    "requestId",
                    event.requestId().toString()
                );
                exchange.getMessage().setHeader(
                    "eventType",
                    event.eventType()
                );
                exchange.getMessage().setHeader(
                    "status",
                    event.status().name()
                );
                exchange.getMessage().setHeader(
                    "details",
                    event.details()
                );
                exchange.getMessage().setHeader(
                    "occurredAt",
                    event.occurredAt().toString()
                );
            })
            .to(databaseEndpoint)
            .log(
                "Evento de auditoría registrado para "
                    + "${header.requestId}: ${header.status}"
            );
    }
}