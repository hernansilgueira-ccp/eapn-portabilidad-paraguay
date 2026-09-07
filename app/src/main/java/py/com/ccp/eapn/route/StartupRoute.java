package py.com.ccp.eapn.route;

import org.apache.camel.builder.RouteBuilder;

public class StartupRoute extends RouteBuilder {

    public static final String STARTUP_MESSAGE =
        "EAPN de Portabilidad Paraguay iniciada correctamente";

    @Override
    public void configure() {
        from("timer:inicio?repeatCount=1")
            .routeId("inicio-eapn")
            .setBody(constant(STARTUP_MESSAGE))
            .log("${body}");
    }
}