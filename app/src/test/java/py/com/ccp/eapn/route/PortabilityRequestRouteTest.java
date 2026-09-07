package py.com.ccp.eapn.route;

import org.apache.camel.CamelExecutionException;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;
import py.com.ccp.eapn.model.Operator;
import py.com.ccp.eapn.model.PortabilityRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PortabilityRequestRouteTest extends CamelTestSupport {

    @Override
    protected RoutesBuilder createRouteBuilder() {
        return new PortabilityRequestRoute();
    }

    @Test
    void shouldProcessValidRequest() {
        PortabilityRequest request = PortabilityRequest.create(
            "595981123456",
            "4567890",
            Operator.TIGO,
            Operator.PERSONAL
        );

        PortabilityRequest result = template.requestBody(
            PortabilityRequestRoute.INPUT_ENDPOINT,
            request,
            PortabilityRequest.class
        );

        assertEquals(request, result);
    }

    @Test
    void shouldRejectUnsupportedBody() {
        assertThrows(
            CamelExecutionException.class,
            () -> template.requestBody(
                PortabilityRequestRoute.INPUT_ENDPOINT,
                "mensaje inválido"
            )
        );
    }
}