package py.com.ccp.eapn.route;

import org.apache.camel.RoutesBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;
import py.com.ccp.eapn.model.DonorApprovalResult;
import py.com.ccp.eapn.model.Operator;
import py.com.ccp.eapn.service.PortabilityJsonSerializer;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PortabilityCompletionRouteTest
    extends CamelTestSupport {

    private static final String INPUT =
        "direct:completion-test";

    private static final String SELECT =
        "mock:select-portability";

    private static final String INSERT_PORTED =
        "mock:insert-ported";

    private static final String COMPLETE =
        "mock:complete-portability";

    private static final String OUTPUT =
        "mock:recipient-notifications";

    private final PortabilityJsonSerializer serializer =
        new PortabilityJsonSerializer();

    private static final String DLQ =
        "mock:completion-errors-dlq";

    @Override
    protected RoutesBuilder createRouteBuilder() {
        return new PortabilityCompletionRoute(
            INPUT,
            SELECT,
            INSERT_PORTED,
            COMPLETE,
            OUTPUT,
            serializer
        );
    }

    @Test
    void shouldCompleteApprovedPortability()
        throws Exception {

        UUID requestId = UUID.randomUUID();

        configureSelectedRequest();

        MockEndpoint inserted =
            getMockEndpoint(INSERT_PORTED);

        MockEndpoint completed =
            getMockEndpoint(COMPLETE);

        MockEndpoint notifications =
            getMockEndpoint(OUTPUT);

        inserted.expectedMessageCount(1);
        inserted.expectedHeaderReceived(
            "requestId",
            requestId.toString()
        );
        inserted.expectedHeaderReceived(
            "previousOperator",
            "TIGO"
        );
        inserted.expectedHeaderReceived(
            "currentOperator",
            "PERSONAL"
        );

        completed.expectedMessageCount(1);

        notifications.expectedMessageCount(1);
        notifications.expectedHeaderReceived(
            "decisionStatus",
            "APPROVED"
        );

        DonorApprovalResult result =
            DonorApprovalResult.approved(
                requestId,
                Operator.TIGO
            );

        template.sendBody(
            INPUT,
            serializer.serialize(result)
        );

        MockEndpoint.assertIsSatisfied(context);

        String notificationJson =
            notifications.getExchanges()
                .getFirst()
                .getMessage()
                .getBody(String.class);

        assertTrue(
            notificationJson.contains(
                "\"status\":\"COMPLETED\""
            )
        );
        assertTrue(
            notificationJson.contains(
                "\"recipientOperator\":\"PERSONAL\""
            )
        );
    }

    @Test
    void shouldNotifyRejectedPortability()
        throws Exception {

        UUID requestId = UUID.randomUUID();

        configureSelectedRequest();

        MockEndpoint inserted =
            getMockEndpoint(INSERT_PORTED);

        MockEndpoint completed =
            getMockEndpoint(COMPLETE);

        MockEndpoint notifications =
            getMockEndpoint(OUTPUT);

        inserted.expectedMessageCount(0);
        completed.expectedMessageCount(0);

        notifications.expectedMessageCount(1);
        notifications.expectedHeaderReceived(
            "decisionStatus",
            "REJECTED"
        );

        DonorApprovalResult result =
            DonorApprovalResult.rejected(
                requestId,
                Operator.TIGO,
                "DOCUMENT_VALIDATION_FAILED"
            );

        template.sendBody(
            INPUT,
            serializer.serialize(result)
        );

        MockEndpoint.assertIsSatisfied(context);

        String notificationJson =
            notifications.getExchanges()
                .getFirst()
                .getMessage()
                .getBody(String.class);

        assertTrue(
            notificationJson.contains(
                "\"status\":\"REJECTED\""
            )
        );
        assertTrue(
            notificationJson.contains(
                "\"rejectionReason\":"
                    + "\"DOCUMENT_VALIDATION_FAILED\""
            )
        );
    }

    @Test
void shouldSendInvalidResultToDeadLetterQueue()
    throws Exception {

    String invalidJson =
        """
        {
          "requestId": "identificador-invalido",
          "status": "ESTADO_INEXISTENTE"
        }
        """;

    MockEndpoint selected =
        getMockEndpoint(SELECT);

    MockEndpoint inserted =
        getMockEndpoint(INSERT_PORTED);

    MockEndpoint completed =
        getMockEndpoint(COMPLETE);

    MockEndpoint notifications =
        getMockEndpoint(OUTPUT);

    MockEndpoint deadLetterQueue =
        getMockEndpoint(DLQ);

    selected.expectedMessageCount(0);
    inserted.expectedMessageCount(0);
    completed.expectedMessageCount(0);
    notifications.expectedMessageCount(0);

    deadLetterQueue.expectedMessageCount(1);
    deadLetterQueue.expectedBodiesReceived(
        invalidJson
    );

    template.sendBody(
        INPUT,
        invalidJson
    );

    MockEndpoint.assertIsSatisfied(context);
}
    private void configureSelectedRequest() {
        getMockEndpoint(SELECT)
            .whenAnyExchangeReceived(exchange ->
                exchange.getMessage().setBody(
                    Map.of(
                        "msisdn",
                        "595981123456",
                        "donor_operator",
                        "TIGO",
                        "recipient_operator",
                        "PERSONAL"
                    )
                )
            );
    }
}