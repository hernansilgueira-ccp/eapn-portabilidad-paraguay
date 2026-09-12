package py.com.ccp.eapn.route;

import org.apache.camel.builder.RouteBuilder;
import py.com.ccp.eapn.model.Operator;
import py.com.ccp.eapn.model.PortabilityRequest;

public class DemoPortabilityRoute
    extends RouteBuilder {

    private static final long MSISDN_SUFFIX_LIMIT =
        10_000_000L;

    @Override
    public void configure() {
        from(
            "timer:portability-demo"
                + "?repeatCount=1&delay=2000"
        )
            .routeId("portability-demo")
            .process(exchange -> {
                long suffix = Math.floorMod(
                    System.currentTimeMillis(),
                    MSISDN_SUFFIX_LIMIT
                );

                String msisdn =
                    "59598"
                        + String.format(
                            "%07d",
                            suffix
                        );

                PortabilityRequest request =
                    PortabilityRequest.create(
                        msisdn,
                        "4567890",
                        Operator.TIGO,
                        Operator.PERSONAL
                    );

                exchange.getMessage().setBody(request);
            })
            .to(PortabilityRequestRoute.INPUT_ENDPOINT)
            .log(
                "Solicitud de demostración enviada: "
                    + "${header.requestId}; MSISDN: "
                    + "${header.msisdn}"
            );
    }
}