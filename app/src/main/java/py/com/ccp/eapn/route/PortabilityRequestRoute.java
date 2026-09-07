package py.com.ccp.eapn.route;

import org.apache.camel.builder.RouteBuilder;
import py.com.ccp.eapn.model.PortabilityRequest;

public class PortabilityRequestRoute extends RouteBuilder {

    public static final String INPUT_ENDPOINT =
        "direct:portability-request";

    @Override
    public void configure() {
        from(INPUT_ENDPOINT)
            .routeId("portability-request")
            .validate(body().isInstanceOf(PortabilityRequest.class))
            .log(
                "Solicitud ${body.requestId} recibida para el número "
                    + "${body.msisdn}"
            )
            .to("direct:portability-validated");

        from("direct:portability-validated")
            .routeId("portability-validated")
            .log(
                "Solicitud ${body.requestId} validada correctamente"
            );
    }
}