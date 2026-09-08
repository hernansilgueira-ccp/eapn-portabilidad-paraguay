package py.com.ccp.eapn.route;

import org.apache.camel.builder.RouteBuilder;
import py.com.ccp.eapn.model.PortabilityRequest;
import py.com.ccp.eapn.service.PortabilityJsonSerializer;

public class PortabilityRequestRoute extends RouteBuilder {

    public static final String INPUT_ENDPOINT =
        "direct:portability-request";

    public static final String DATABASE_ENDPOINT =
        "sql:INSERT INTO portability_requests "
            + "(request_id, msisdn, document_number, "
            + "donor_operator, recipient_operator, "
            + "status, requested_at) "
            + "VALUES ("
            + "CAST(:#requestId AS UUID), "
            + ":#msisdn, "
            + ":#documentNumber, "
            + ":#donorOperator, "
            + ":#recipientOperator, "
            + ":#status, "
            + "CAST(:#requestedAt AS TIMESTAMPTZ)"
            + ")"
            + "?dataSource=#dataSource";

    public static final String JMS_ENDPOINT =
        "jms:queue:portability.requests"
            + "?connectionFactory=#jmsConnectionFactory";

    private final String databaseEndpoint;
    private final String outputEndpoint;
    private final PortabilityJsonSerializer serializer;

    public PortabilityRequestRoute() {
        this(
            DATABASE_ENDPOINT,
            JMS_ENDPOINT,
            new PortabilityJsonSerializer()
        );
    }

    PortabilityRequestRoute(
        String databaseEndpoint,
        String outputEndpoint
    ) {
        this(
            databaseEndpoint,
            outputEndpoint,
            new PortabilityJsonSerializer()
        );
    }

    PortabilityRequestRoute(
        String databaseEndpoint,
        String outputEndpoint,
        PortabilityJsonSerializer serializer
    ) {
        this.databaseEndpoint = databaseEndpoint;
        this.outputEndpoint = outputEndpoint;
        this.serializer = serializer;
    }

    @Override
    public void configure() {
        from(INPUT_ENDPOINT)
            .routeId("portability-request")
            .validate(
                body().isInstanceOf(PortabilityRequest.class)
            )
            .setProperty(
                "portabilityRequest",
                body()
            )
            .process(exchange -> {
                PortabilityRequest request =
                    exchange.getMessage().getBody(
                        PortabilityRequest.class
                    );

                exchange.getMessage().setHeader(
                    "requestId",
                    request.requestId().toString()
                );

                exchange.getMessage().setHeader(
                    "msisdn",
                    request.msisdn()
                );

                exchange.getMessage().setHeader(
                    "documentNumber",
                    request.documentNumber()
                );

                exchange.getMessage().setHeader(
                    "donorOperator",
                    request.donorOperator().name()
                );

                exchange.getMessage().setHeader(
                    "recipientOperator",
                    request.recipientOperator().name()
                );

                exchange.getMessage().setHeader(
                    "status",
                    request.status().name()
                );

                exchange.getMessage().setHeader(
                    "requestedAt",
                    request.requestedAt().toString()
                );
            })
            .log(
                "Persistiendo solicitud "
                    + "${header.requestId} en PostgreSQL"
            )
            .to(databaseEndpoint)
            .setBody(
                exchangeProperty("portabilityRequest")
            )
            .log(
                "Enviando solicitud "
                    + "${header.requestId} "
                    + "a portability.requests"
            )
            .bean(serializer, "serialize")
            .to(outputEndpoint);
    }
}