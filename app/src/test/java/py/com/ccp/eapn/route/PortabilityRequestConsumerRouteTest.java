package py.com.ccp.eapn.route;

import org.apache.camel.RoutesBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;
import py.com.ccp.eapn.model.Operator;
import py.com.ccp.eapn.model.PortabilityRequest;
import py.com.ccp.eapn.service.PinService;
import py.com.ccp.eapn.service.PortabilityJsonSerializer;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PortabilityRequestConsumerRouteTest
    extends CamelTestSupport {

    private static final String INPUT =
        "direct:consumer-test";

    private static final String DATABASE =
        "mock:database-update";

    private static final String OUTPUT =
        "mock:pin-notifications";

    private final PortabilityJsonSerializer serializer =
        new PortabilityJsonSerializer();

    @Override
    protected RoutesBuilder createRouteBuilder() {
        return new PortabilityRequestConsumerRoute(
            INPUT,
            DATABASE,
            OUTPUT,
            new PinService(),
            serializer
        );
    }

    @Test
    void shouldGeneratePinAndUpdateRequest()
        throws Exception {

        PortabilityRequest request =
            PortabilityRequest.create(
                "595981123456",
                "4567890",
                Operator.TIGO,
                Operator.PERSONAL
            );

        MockEndpoint database =
            getMockEndpoint(DATABASE);

        MockEndpoint notifications =
            getMockEndpoint(OUTPUT);

        database.expectedMessageCount(1);
        database.expectedHeaderReceived(
            "requestId",
            request.requestId().toString()
        );

        notifications.expectedMessageCount(1);

        template.sendBody(
            INPUT,
            serializer.serialize(request)
        );

        database.assertIsSatisfied();
        notifications.assertIsSatisfied();

        String notificationJson =
            notifications.getExchanges()
                .getFirst()
                .getMessage()
                .getBody(String.class);

        assertTrue(
            notificationJson.contains(
                "\"requestId\":\""
                    + request.requestId()
                    + "\""
            )
        );

        assertTrue(
            notificationJson.matches(
                ".*\"pin\":\"\\d{6}\".*"
            )
        );
    }
}