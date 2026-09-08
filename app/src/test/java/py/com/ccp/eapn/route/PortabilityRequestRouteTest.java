package py.com.ccp.eapn.route;

import org.apache.camel.CamelExecutionException;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;
import py.com.ccp.eapn.model.Operator;
import py.com.ccp.eapn.model.PortabilityRequest;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortabilityRequestRouteTest
    extends CamelTestSupport {

    private static final String MOCK_DATABASE =
        "mock:database";

    private static final String MOCK_ARTEMIS =
        "mock:portability-requests";

    @Override
    protected RoutesBuilder createRouteBuilder() {
        return new PortabilityRequestRoute(
            MOCK_DATABASE,
            MOCK_ARTEMIS
        );
    }

    @Test
    void shouldPersistAndSendValidRequest()
        throws Exception {

        PortabilityRequest request =
            PortabilityRequest.create(
                "595981123456",
                "4567890",
                Operator.TIGO,
                Operator.PERSONAL
            );

        MockEndpoint database =
            getMockEndpoint(MOCK_DATABASE);

        MockEndpoint artemis =
            getMockEndpoint(MOCK_ARTEMIS);

        database.expectedMessageCount(1);
        database.expectedHeaderReceived(
            "requestId",
            request.requestId().toString()
        );
        database.expectedHeaderReceived(
            "status",
            "CREATED"
        );

        artemis.expectedMessageCount(1);
        artemis.expectedHeaderReceived(
            "msisdn",
            "595981123456"
        );

        template.sendBody(
            PortabilityRequestRoute.INPUT_ENDPOINT,
            request
        );

        database.assertIsSatisfied();
        artemis.assertIsSatisfied();

        String json = artemis.getExchanges()
            .getFirst()
            .getMessage()
            .getBody(String.class);

        assertTrue(json.contains("\"requestId\""));
        assertTrue(
            json.contains(
                "\"msisdn\":\"595981123456\""
            )
        );
        assertTrue(
            json.contains("\"status\":\"CREATED\"")
        );
    }

    @Test
    void shouldRejectUnsupportedBody() {
        assertThrows(
            CamelExecutionException.class,
            () -> template.sendBody(
                PortabilityRequestRoute.INPUT_ENDPOINT,
                "mensaje inválido"
            )
        );
    }
}