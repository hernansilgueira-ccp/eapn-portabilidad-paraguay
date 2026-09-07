package py.com.ccp.eapn.route;

import org.apache.camel.CamelExecutionException;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;
import py.com.ccp.eapn.model.Operator;
import py.com.ccp.eapn.model.PortabilityRequest;

import static org.junit.jupiter.api.Assertions.assertThrows;

class PortabilityRequestRouteTest extends CamelTestSupport {

    private static final String MOCK_ENDPOINT =
        "mock:portability-requests";

    @Override
    protected RoutesBuilder createRouteBuilder() {
        return new PortabilityRequestRoute(MOCK_ENDPOINT);
    }

    @Test
    void shouldSendValidRequestToOutputChannel()
        throws Exception {

        PortabilityRequest request = PortabilityRequest.create(
            "595981123456",
            "4567890",
            Operator.TIGO,
            Operator.PERSONAL
        );

        MockEndpoint mock = getMockEndpoint(MOCK_ENDPOINT);

        mock.expectedMessageCount(1);
        mock.expectedHeaderReceived(
            "requestId",
            request.requestId().toString()
        );
        mock.expectedHeaderReceived(
            "msisdn",
            request.msisdn()
        );
        mock.expectedHeaderReceived(
            "donorOperator",
            Operator.TIGO.name()
        );
        mock.expectedHeaderReceived(
            "recipientOperator",
            Operator.PERSONAL.name()
        );

        template.sendBody(
            PortabilityRequestRoute.INPUT_ENDPOINT,
            request
        );

        mock.assertIsSatisfied();
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