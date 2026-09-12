package py.com.ccp.eapn;

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.camel.main.Main;
import org.postgresql.ds.PGSimpleDataSource;
import py.com.ccp.eapn.route.DemoPortabilityRoute;
import py.com.ccp.eapn.route.PortabilityRequestRoute;
import py.com.ccp.eapn.route.StartupRoute;
import py.com.ccp.eapn.route.PortabilityRequestConsumerRoute;
import py.com.ccp.eapn.route.PinConfirmationRoute;
import py.com.ccp.eapn.route.PinConfirmationApiRoute;
import py.com.ccp.eapn.route.DonorApprovalConsumerRoute;
import py.com.ccp.eapn.route.PortabilityCompletionRoute;

public final class App {

    private static final String DEFAULT_BROKER_URL =
        "tcp://localhost:61616";

    private static final String DEFAULT_POSTGRES_URL =
        "jdbc:postgresql://localhost:5432/eapn";

    private App() {
    }

    public static void main(String[] args) throws Exception {
        ActiveMQConnectionFactory connectionFactory =
            createArtemisConnectionFactory();

        PGSimpleDataSource dataSource =
            createPostgresDataSource();

        Main main = new Main();

        main.bind(
            "jmsConnectionFactory",
            connectionFactory
        );
        main.bind(
            "dataSource",
            dataSource
        );

        main.configure().addRoutesBuilder(
            new StartupRoute()
        );
        main.configure().addRoutesBuilder(
            new PortabilityRequestRoute()
        );

        main.configure().addRoutesBuilder(
            new PortabilityRequestConsumerRoute()
        );
        main.configure().addRoutesBuilder(
            new PinConfirmationRoute()
        );
        main.configure().addRoutesBuilder(
            new PinConfirmationApiRoute()
        );
        main.configure().addRoutesBuilder(
            new DonorApprovalConsumerRoute()
        );
        main.configure().addRoutesBuilder(
            new PortabilityCompletionRoute()
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

    private static ActiveMQConnectionFactory
        createArtemisConnectionFactory() {

        ActiveMQConnectionFactory connectionFactory =
            new ActiveMQConnectionFactory(
                environment(
                    "ARTEMIS_URL",
                    DEFAULT_BROKER_URL
                )
            );

        connectionFactory.setUser(
            environment(
                "ARTEMIS_USER",
                "eapn_user"
            )
        );
        connectionFactory.setPassword(
            environment(
                "ARTEMIS_PASSWORD",
                "eapn_password"
            )
        );

        return connectionFactory;
    }

    private static PGSimpleDataSource
        createPostgresDataSource() {

        PGSimpleDataSource dataSource =
            new PGSimpleDataSource();

        dataSource.setURL(
            environment(
                "POSTGRES_URL",
                DEFAULT_POSTGRES_URL
            )
        );
        dataSource.setUser(
            environment(
                "POSTGRES_USER",
                "eapn_user"
            )
        );
        dataSource.setPassword(
            environment(
                "POSTGRES_PASSWORD",
                "eapn_password"
            )
        );

        return dataSource;
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