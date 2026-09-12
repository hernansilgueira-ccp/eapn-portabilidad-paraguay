package py.com.ccp.eapn.route;

import org.apache.camel.RoutesBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;
import py.com.ccp.eapn.model.PortabilityAuditEvent;
import py.com.ccp.eapn.model.PortabilityStatus;
import py.com.ccp.eapn.service.PortabilityJsonSerializer;

import java.util.UUID;

class PortabilityAuditRouteTest
    extends CamelTestSupport {

    private static final String INPUT =
        "direct:audit-test";

    private static final String DATABASE =
        "mock:audit-database";

    private static final String DLQ =
        "mock:audit-errors-dlq";

    private final PortabilityJsonSerializer serializer =
        new PortabilityJsonSerializer();

    @Override
    protected RoutesBuilder createRouteBuilder() {
        return new PortabilityAuditRoute(
            INPUT,
            DATABASE,
            DLQ,
            serializer
        );
    }

    @Test
    void shouldPersistAuditEvent()
        throws Exception {

        UUID requestId = UUID.randomUUID();

        PortabilityAuditEvent event =
            PortabilityAuditEvent.statusChanged(
                requestId,
                PortabilityStatus.COMPLETED,
                "Portabilidad completada"
            );

        MockEndpoint database =
            getMockEndpoint(DATABASE);

        MockEndpoint deadLetterQueue =
            getMockEndpoint(DLQ);

        database.expectedMessageCount(1);
        database.expectedHeaderReceived(
            "auditId",
            event.auditId().toString()
        );
        database.expectedHeaderReceived(
            "requestId",
            requestId.toString()
        );
        database.expectedHeaderReceived(
            "eventType",
            "STATUS_CHANGED"
        );
        database.expectedHeaderReceived(
            "status",
            "COMPLETED"
        );
        database.expectedHeaderReceived(
            "details",
            "Portabilidad completada"
        );

        deadLetterQueue.expectedMessageCount(0);

        template.sendBody(
            INPUT,
            serializer.serialize(event)
        );

        MockEndpoint.assertIsSatisfied(context);
    }

    @Test
    void shouldSendInvalidAuditEventToDlq()
        throws Exception {

        String invalidJson =
            """
            {
              "requestId": "identificador-invalido",
              "status": "ESTADO_INEXISTENTE"
            }
            """;

        MockEndpoint database =
            getMockEndpoint(DATABASE);

        MockEndpoint deadLetterQueue =
            getMockEndpoint(DLQ);

        database.expectedMessageCount(0);

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
}