package py.com.ccp.eapn.route;

import org.apache.camel.Exchange;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;
import py.com.ccp.eapn.model.PortabilityRequest;
import py.com.ccp.eapn.service.PortabilityJsonSerializer;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortabilityRequestApiRouteTest
    extends CamelTestSupport {

    private static final String INPUT =
        "direct:portability-request-api-test";

    private static final String REQUEST =
        "mock:portability-request";

    private final PortabilityJsonSerializer serializer =
        new PortabilityJsonSerializer();

    @Override
    protected RoutesBuilder createRouteBuilder() {
        return new PortabilityRequestApiRoute(
            INPUT,
            REQUEST,
            serializer
        );
    }

    @Test
    void shouldCreatePortabilityRequest()
        throws Exception {

        UUID requestId = UUID.randomUUID();

        MockEndpoint requestEndpoint =
            getMockEndpoint(REQUEST);

        requestEndpoint.expectedMessageCount(1);

        requestEndpoint.expectedMessagesMatches(
            exchange -> {
                PortabilityRequest request =
                    exchange.getMessage().getBody(
                        PortabilityRequest.class
                    );

                return request != null
                    && "595985555555".equals(
                        request.msisdn()
                    )
                    && "1234568".equals(
                        request.documentNumber()
                    )
                    && "TIGO".equals(
                        request.donorOperator().name()
                    )
                    && "PERSONAL".equals(
                        request.recipientOperator().name()
                    );
            }
        );

        requestEndpoint.whenAnyExchangeReceived(
            exchange ->
                exchange.getMessage().setHeader(
                    "requestId",
                    requestId.toString()
                )
        );

        String json =
            """
            {
              "msisdn": "595985555555",
              "documentNumber": "1234568",
              "donorOperator": "TIGO",
              "recipientOperator": "PERSONAL"
            }
            """;

        Exchange response = template.request(
            INPUT,
            exchange ->
                exchange.getMessage().setBody(json)
        );

        MockEndpoint.assertIsSatisfied(context);

        assertEquals(
            201,
            response.getMessage().getHeader(
                Exchange.HTTP_RESPONSE_CODE
            )
        );

        assertEquals(
            "application/json",
            response.getMessage().getHeader(
                Exchange.CONTENT_TYPE
            )
        );

        String responseBody =
            response.getMessage().getBody(
                String.class
            );

        assertTrue(
            responseBody.contains(
                requestId.toString()
            )
        );

        assertTrue(
            responseBody.contains(
                "\"status\": \"CREATED\""
            )
        );
    }

    @Test
    void shouldRejectSameDonorAndRecipientOperator()
        throws Exception {

        MockEndpoint requestEndpoint =
            getMockEndpoint(REQUEST);

        requestEndpoint.expectedMessageCount(0);

        String json =
            """
            {
              "msisdn": "595985555555",
              "documentNumber": "1234568",
              "donorOperator": "TIGO",
              "recipientOperator": "TIGO"
            }
            """;

        Exchange response = template.request(
            INPUT,
            exchange ->
                exchange.getMessage().setBody(json)
        );

        MockEndpoint.assertIsSatisfied(context);

        assertEquals(
            400,
            response.getMessage().getHeader(
                Exchange.HTTP_RESPONSE_CODE
            )
        );

        String responseBody =
            response.getMessage().getBody(
                String.class
            );

        assertTrue(
            responseBody.contains(
                "\"status\": \"ERROR\""
            )
        );
    }
}