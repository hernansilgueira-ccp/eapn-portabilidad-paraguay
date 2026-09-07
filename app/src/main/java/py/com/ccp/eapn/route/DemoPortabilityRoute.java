package py.com.ccp.eapn.route;

import org.apache.camel.builder.RouteBuilder;
import py.com.ccp.eapn.model.Operator;
import py.com.ccp.eapn.model.PortabilityRequest;

public class DemoPortabilityRoute extends RouteBuilder {

    @Override
    public void configure() {
        from("timer:portability-demo?repeatCount=1&delay=2000")
            .routeId("portability-demo")
            .process(exchange -> {
                PortabilityRequest request =
                    PortabilityRequest.create(
                        "595981123456",
                        "4567890",
                        Operator.TIGO,
                        Operator.PERSONAL
                    );

                exchange.getMessage().setBody(request);
            })
            .to(PortabilityRequestRoute.INPUT_ENDPOINT)
            .log(
                "Solicitud de demostración enviada: "
                    + "${header.requestId}"
            );
    }
}