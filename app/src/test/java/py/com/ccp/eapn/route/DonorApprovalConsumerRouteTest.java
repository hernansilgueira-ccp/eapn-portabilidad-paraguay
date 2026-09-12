package py.com.ccp.eapn.route;

import org.apache.camel.RoutesBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;
import py.com.ccp.eapn.model.DonorApprovalRequest;
import py.com.ccp.eapn.model.Operator;
import py.com.ccp.eapn.service.DonorDecisionService;
import py.com.ccp.eapn.service.PortabilityJsonSerializer;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DonorApprovalConsumerRouteTest
    extends CamelTestSupport {

    private static final String INPUT =
        "direct:donor-approval-test";

    private static final String APPROVED =
        "mock:approved";

    private static final String REJECTED =
        "mock:rejected";

    private static final String OUTPUT =
        "mock:results";

    private final PortabilityJsonSerializer serializer =
        new PortabilityJsonSerializer();

    @Override
    protected RoutesBuilder createRouteBuilder() {
        return new DonorApprovalConsumerRoute(
            INPUT,
            APPROVED,
            REJECTED,
            OUTPUT,
            new DonorDecisionService(),
            serializer
        );
    }

    @Test
    void shouldApproveValidRequest() throws Exception {
        DonorApprovalRequest request =
            createRequest("4567890");

        MockEndpoint approved =
            getMockEndpoint(APPROVED);

        MockEndpoint rejected =
            getMockEndpoint(REJECTED);

        MockEndpoint results =
            getMockEndpoint(OUTPUT);

        approved.expectedMessageCount(1);
        approved.expectedHeaderReceived(
            "requestId",
            request.requestId().toString()
        );

        rejected.expectedMessageCount(0);

        results.expectedMessageCount(1);
        results.expectedHeaderReceived(
            "decisionStatus",
            "APPROVED"
        );

        template.sendBody(
            INPUT,
            serializer.serialize(request)
        );

        MockEndpoint.assertIsSatisfied(context);

        String resultJson =
            results.getExchanges()
                .getFirst()
                .getMessage()
                .getBody(String.class);

        assertTrue(
            resultJson.contains(
                "\"status\":\"APPROVED\""
            )
        );
    }

    @Test
    void shouldRejectInvalidDocument()
        throws Exception {

        DonorApprovalRequest request =
            createRequest("4567899");

        MockEndpoint approved =
            getMockEndpoint(APPROVED);

        MockEndpoint rejected =
            getMockEndpoint(REJECTED);

        MockEndpoint results =
            getMockEndpoint(OUTPUT);

        approved.expectedMessageCount(0);

        rejected.expectedMessageCount(1);
        rejected.expectedHeaderReceived(
            "rejectionReason",
            "DOCUMENT_VALIDATION_FAILED"
        );

        results.expectedMessageCount(1);
        results.expectedHeaderReceived(
            "decisionStatus",
            "REJECTED"
        );

        template.sendBody(
            INPUT,
            serializer.serialize(request)
        );

        MockEndpoint.assertIsSatisfied(context);

        String resultJson =
            results.getExchanges()
                .getFirst()
                .getMessage()
                .getBody(String.class);

        assertTrue(
            resultJson.contains(
                "\"status\":\"REJECTED\""
            )
        );
        assertTrue(
            resultJson.contains(
                "\"rejectionReason\":"
                    + "\"DOCUMENT_VALIDATION_FAILED\""
            )
        );
    }

    private DonorApprovalRequest createRequest(
        String documentNumber
    ) {
        return new DonorApprovalRequest(
            UUID.randomUUID(),
            "595981123456",
            documentNumber,
            Operator.TIGO,
            Operator.PERSONAL,
            Instant.now()
        );
    }
}