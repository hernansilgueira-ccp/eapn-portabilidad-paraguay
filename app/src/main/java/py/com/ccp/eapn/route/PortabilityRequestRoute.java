package py.com.ccp.eapn.route;

import org.apache.camel.builder.RouteBuilder;
import py.com.ccp.eapn.model.PortabilityRequest;
import py.com.ccp.eapn.service.PortabilityJsonSerializer;

public class PortabilityRequestRoute extends RouteBuilder {

    public static final String INPUT_ENDPOINT =
        "direct:portability-request";

    public static final String JMS_ENDPOINT =
        "jms:queue:portability.requests"
            + "?connectionFactory=#jmsConnectionFactory";

    private final String outputEndpoint;
    private final PortabilityJsonSerializer serializer;

    public PortabilityRequestRoute() {
        this(
            JMS_ENDPOINT,
            new PortabilityJsonSerializer()
        );
    }

    PortabilityRequestRoute(String outputEndpoint) {
        this(
            outputEndpoint,
            new PortabilityJsonSerializer()
        );
    }

    PortabilityRequestRoute(
        String outputEndpoint,
        PortabilityJsonSerializer serializer
    ) {
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
            .setHeader(
                "requestId",
                simple("${body.requestId}")
            )
            .setHeader(
                "msisdn",
                simple("${body.msisdn}")
            )
            .setHeader(
                "donorOperator",
                simple("${body.donorOperator}")
            )
            .setHeader(
                "recipientOperator",
                simple("${body.recipientOperator}")
            )
            .log(
                "Enviando solicitud ${header.requestId} "
                    + "a portability.requests"
            )
            .bean(serializer, "serialize")
            .to(outputEndpoint);
    }
}