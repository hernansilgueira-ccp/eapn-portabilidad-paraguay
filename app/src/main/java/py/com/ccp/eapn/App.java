package py.com.ccp.eapn;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.main.Main;

public class App {

    public String getGreeting() {
        return "EAPN de Portabilidad Paraguay iniciada correctamente";
    }

    public static void main(String[] args) throws Exception {
        App app = new App();
        Main main = new Main();

        main.configure().addRoutesBuilder(new RouteBuilder() {
            @Override
            public void configure() {
                from("timer:inicio?repeatCount=1")
                    .routeId("inicio-eapn")
                    .setBody(constant(app.getGreeting()))
                    .log("${body}");
            }
        });

        main.run(args);
    }
}