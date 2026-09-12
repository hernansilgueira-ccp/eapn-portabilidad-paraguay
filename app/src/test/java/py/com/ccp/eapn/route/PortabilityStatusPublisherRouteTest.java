package py.com.ccp.eapn.route;

import org.apache.camel.RoutesBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;
import py.com.ccp.eapn.service.PortabilityJsonSerializer;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortabilityStatusPublisherRouteTest
    extends CamelTestSupport {

    private static final String INPUT =
        "direct:status-publisher-test";

    private static final String AUDIT =
        "mock:status-audit";

    private static final String TOPIC =
        "mock:status-topic";

    @Override
    protected RoutesBuilder createRouteBuilder() {
        return new PortabilityStatusPublisherRoute(
            INPUT,
            AUDIT,
            TOPIC,
            new PortabilityJsonSerializer()
        );
    }

    @Test
    void shouldPublishStatusToAuditAndTopic()
        throws Exception {

        UUID requestId = UUID.randomUUID();

        MockEndpoint audit =
            getMockEndpoint(AUDIT);

        MockEndpoint topic =
            getMockEndpoint(TOPIC);

        audit.expectedMessageCount(1);
        audit.expectedHeaderReceived(
            "requestId",
            requestId.toString()
        );
        audit.expectedHeaderReceived(
            "auditStatus",
            "PIN_GENERATED"
        );

        topic.expectedMessageCount(1);
        topic.expectedHeaderReceived(
            "requestId",
            requestId.toString()
        );
        topic.expectedHeaderReceived(
            "auditStatus",
            "PIN_GENERATED"
        );

        template.sendBodyAndHeaders(
            INPUT,
            null,
            Map.of(
                "requestId",
                requestId.toString(),
                "auditStatus",
                "PIN_GENERATED",
                "auditDetails",
                "PIN generado para la solicitud"
            )
        );

        MockEndpoint.assertIsSatisfied(context);

        String auditJson =
            audit.getExchanges()
                .getFirst()
                .getMessage()
                .getBody(String.class);

        String topicJson =
            topic.getExchanges()
                .getFirst()
                .getMessage()
                .getBody(String.class);

        assertEquals(
            auditJson,
            topicJson
        );

        assertTrue(
            auditJson.contains(
                "\"status\":\"PIN_GENERATED\""
            )
        );

        assertTrue(
            auditJson.contains(
                "\"eventType\":\"STATUS_CHANGED\""
            )
        );
    }
}