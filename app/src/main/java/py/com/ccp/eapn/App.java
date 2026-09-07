package py.com.ccp.eapn;

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.camel.main.Main;
import py.com.ccp.eapn.route.PortabilityRequestRoute;
import py.com.ccp.eapn.route.StartupRoute;
import py.com.ccp.eapn.route.DemoPortabilityRoute;

public final class App {

    private static final String DEFAULT_BROKER_URL =
        "tcp://localhost:61616";

    private App() {
    }

    public static void main(String[] args) throws Exception {
        String brokerUrl = environment(
            "ARTEMIS_URL",
            DEFAULT_BROKER_URL
        );

        String brokerUser = environment(
            "ARTEMIS_USER",
            "eapn_user"
        );

        String brokerPassword = environment(
            "ARTEMIS_PASSWORD",
            "eapn_password"
        );

        ActiveMQConnectionFactory connectionFactory =
            new ActiveMQConnectionFactory(brokerUrl);

        connectionFactory.setUser(brokerUser);
        connectionFactory.setPassword(brokerPassword);

        Main main = new Main();
        main.bind("jmsConnectionFactory", connectionFactory);

        main.configure().addRoutesBuilder(new StartupRoute());
        main.configure().addRoutesBuilder(
            new PortabilityRequestRoute()
        );
        if (Boolean.parseBoolean(
            environment("EAPN_DEMO_ENABLED", "false")
        )) {
            main.configure().addRoutesBuilder(
                new DemoPortabilityRoute()
            );
        }
        main.run(args);
    }

    private static String environment(
        String name,
        String defaultValue
    ) {
        String value = System.getenv(name);

        return value == null || value.isBlank()
            ? defaultValue
            : value;
    }
}