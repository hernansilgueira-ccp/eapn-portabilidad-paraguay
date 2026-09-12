package py.com.ccp.eapn.route;

import org.apache.camel.ExchangePattern;
import org.apache.camel.builder.RouteBuilder;
import py.com.ccp.eapn.model.PortabilityAuditEvent;
import py.com.ccp.eapn.model.PortabilityStatus;
import py.com.ccp.eapn.service.PortabilityJsonSerializer;

import java.util.UUID;

public class PortabilityStatusPublisherRoute
    extends RouteBuilder {

    public static final String INPUT_ENDPOINT =
        "seda:portability-status-publisher"
            + "?waitForTaskToComplete=Never";

    public static final String AUDIT_ENDPOINT =
        "jms:queue:portability.audit"
            + "?connectionFactory=#jmsConnectionFactory";

    public static final String STATUS_TOPIC_ENDPOINT =
        "jms:topic:portability.status.events"
            + "?connectionFactory=#jmsConnectionFactory";

    private final String inputEndpoint;
    private final String auditEndpoint;
    private final String statusTopicEndpoint;
    private final PortabilityJsonSerializer serializer;

    public PortabilityStatusPublisherRoute() {
        this(
            INPUT_ENDPOINT,
            AUDIT_ENDPOINT,
            STATUS_TOPIC_ENDPOINT,
            new PortabilityJsonSerializer()
        );
    }

    PortabilityStatusPublisherRoute(
        String inputEndpoint,
        String auditEndpoint,
        String statusTopicEndpoint,
        PortabilityJsonSerializer serializer
    ) {
        this.inputEndpoint = inputEndpoint;
        this.auditEndpoint = auditEndpoint;
        this.statusTopicEndpoint = statusTopicEndpoint;
        this.serializer = serializer;
    }

    @Override
    public void configure() {
        from(inputEndpoint)
            .routeId("portability-status-publisher")
            .process(exchange -> {
                String requestId =
                    exchange.getMessage().getHeader(
                        "requestId",
                        String.class
                    );

                String status =
                    exchange.getMessage().getHeader(
                        "auditStatus",
                        String.class
                    );

                String details =
                    exchange.getMessage().getHeader(
                        "auditDetails",
                        String.class
                    );

                PortabilityAuditEvent event =
                    PortabilityAuditEvent.statusChanged(
                        UUID.fromString(requestId),
                        PortabilityStatus.valueOf(status),
                        details
                    );

                exchange.getMessage().setBody(event);
            })
            .bean(
                serializer,
                "serialize"
            )
            .setExchangePattern(
                ExchangePattern.InOnly
            )
            .multicast()
                .parallelProcessing()
                .to(
                    auditEndpoint,
                    statusTopicEndpoint
                )
            .end()
            .log(
                "Evento ${header.auditStatus} publicado "
                    + "para ${header.requestId}"
            );
    }
}