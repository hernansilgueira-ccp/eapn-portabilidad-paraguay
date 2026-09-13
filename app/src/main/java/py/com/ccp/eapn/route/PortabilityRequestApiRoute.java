package py.com.ccp.eapn.route;

import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.builder.RouteBuilder;
import py.com.ccp.eapn.model.CreatePortabilityRequest;
import py.com.ccp.eapn.model.PortabilityRequest;
import py.com.ccp.eapn.service.PortabilityJsonSerializer;

public class PortabilityRequestApiRoute
    extends RouteBuilder {

    public static final String API_ENDPOINT =
        "netty-http:http://0.0.0.0:8082"
            + "/api/portability/requests"
            + "?httpMethodRestrict=POST";

    private final String apiEndpoint;
    private final String requestEndpoint;
    private final PortabilityJsonSerializer serializer;

    public PortabilityRequestApiRoute() {
        this(
            API_ENDPOINT,
            PortabilityRequestRoute.INPUT_ENDPOINT,
            new PortabilityJsonSerializer()
        );
    }

    PortabilityRequestApiRoute(
        String apiEndpoint,
        String requestEndpoint,
        PortabilityJsonSerializer serializer
    ) {
        this.apiEndpoint = apiEndpoint;
        this.requestEndpoint = requestEndpoint;
        this.serializer = serializer;
    }

    @Override
    public void configure() {

        onException(Exception.class)
            .handled(true)
            .setHeader(
                Exchange.HTTP_RESPONSE_CODE,
                constant(400)
            )
            .setHeader(
                Exchange.CONTENT_TYPE,
                constant("application/json")
            )
            .setBody(
                constant(
                    """
                    {
                      "status": "ERROR",
                      "message": "No fue posible crear la solicitud de portabilidad"
                    }
                    """
                )
            );

        from(apiEndpoint)
            .routeId("portability-request-api")
            .convertBodyTo(String.class)
            .bean(
                serializer,
                "deserializeCreatePortabilityRequest"
            )
            .process(exchange -> {
                CreatePortabilityRequest command =
                    exchange.getMessage().getBody(
                        CreatePortabilityRequest.class
                    );

                PortabilityRequest request =
                    command.toPortabilityRequest();

                exchange.getMessage().setBody(request);
            })
            .to(requestEndpoint)
            .setExchangePattern(
                ExchangePattern.InOut
            )
            .setHeader(
                Exchange.CONTENT_TYPE,
                constant("application/json")
            )
            .setHeader(
                Exchange.HTTP_RESPONSE_CODE,
                constant(201)
            )
            .setBody(
                simple(
                    """
                    {
                      "requestId": "${header.requestId}",
                      "status": "CREATED"
                    }
                    """
                )
            );
    }
}