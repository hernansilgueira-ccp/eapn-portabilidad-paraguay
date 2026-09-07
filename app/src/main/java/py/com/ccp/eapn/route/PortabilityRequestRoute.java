package py.com.ccp.eapn.route;

import org.apache.camel.builder.RouteBuilder;
import py.com.ccp.eapn.model.PortabilityRequest;

public class PortabilityRequestRoute extends RouteBuilder {

    public static final String INPUT_ENDPOINT =
        "direct:portability-request";

    public static final String JMS_ENDPOINT =
        "jms:queue:portability.requests"
            + "?connectionFactory=#jmsConnectionFactory";

    private final String outputEndpoint;

    public PortabilityRequestRoute() {
        this(JMS_ENDPOINT);
    }

    PortabilityRequestRoute(String outputEndpoint) {
        this.outputEndpoint = outputEndpoint;
    }

    @Override
    public void configure() {
        from(INPUT_ENDPOINT)
            .routeId("portability-request")
            .validate(body().isInstanceOf(PortabilityRequest.class))
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
            .convertBodyTo(String.class)
            .to(outputEndpoint);
    }
}