package py.com.ccp.eapn.route;

import org.apache.camel.RoutesBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;
import py.com.ccp.eapn.model.PinConfirmation;
import py.com.ccp.eapn.service.PinService;
import py.com.ccp.eapn.service.PortabilityJsonSerializer;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

class PinConfirmationRouteTest
    extends CamelTestSupport {

    private static final String INPUT =
        "direct:pin-confirmation-test";

    private static final String SELECT =
        "mock:select-request";

    private static final String CONFIRM =
        "mock:confirm-request";

    private static final String REJECT =
        "mock:reject-request";

    private static final String PENDING =
        "mock:pending-donor";

    private static final String APPROVAL =
        "mock:donor-approval";

    private final PinService pinService =
        new PinService();

    private final PortabilityJsonSerializer serializer =
        new PortabilityJsonSerializer();

    @Override
    protected RoutesBuilder createRouteBuilder() {
        return new PinConfirmationRoute(
            INPUT,
            SELECT,
            CONFIRM,
            PENDING,
            REJECT,
            APPROVAL,
            pinService,
            serializer
        );
    }

    @Test
    void shouldConfirmValidPin() throws Exception {
        UUID requestId = UUID.randomUUID();

        PinService.GeneratedPin generatedPin =
            pinService.generate();

        configureSelectedRequest(
            generatedPin.pinHash(),
            generatedPin.expiresAt()
        );

        MockEndpoint confirmed =
            getMockEndpoint(CONFIRM);

        MockEndpoint rejected =
            getMockEndpoint(REJECT);

        MockEndpoint pending =
            getMockEndpoint(PENDING);

        MockEndpoint approval =
            getMockEndpoint(APPROVAL);

        confirmed.expectedMessageCount(1);
        confirmed.expectedHeaderReceived(
            "requestId",
            requestId.toString()
        );
        confirmed.expectedHeaderReceived(
            "confirmationStatus",
            "CONFIRMED"
        );

        rejected.expectedMessageCount(0);

        pending.expectedMessageCount(1);
        pending.expectedHeaderReceived(
            "requestId",
            requestId.toString()
        );

        approval.expectedMessageCount(1);
        approval.expectedHeaderReceived(
            "requestId",
            requestId.toString()
        );
        approval.expectedHeaderReceived(
            "donorOperator",
            "TIGO"
        );
        approval.expectedMessagesMatches(
            exchange -> {
                String json =
                    exchange.getMessage()
                        .getBody(String.class);

                return json.contains(
                    "\"donorOperator\":\"TIGO\""
                );
            }
        );

        sendConfirmation(
            requestId,
            generatedPin.plainPin()
        );

        MockEndpoint.assertIsSatisfied(
            context
        );
    }

    @Test
    void shouldRejectIncorrectPin() throws Exception {
        UUID requestId = UUID.randomUUID();

        PinService.GeneratedPin generatedPin =
            pinService.generate();

        configureSelectedRequest(
            generatedPin.pinHash(),
            generatedPin.expiresAt()
        );

        MockEndpoint confirmed =
            getMockEndpoint(CONFIRM);

        MockEndpoint rejected =
            getMockEndpoint(REJECT);

        confirmed.expectedMessageCount(0);

        rejected.expectedMessageCount(1);
        rejected.expectedHeaderReceived(
            "confirmationStatus",
            "REJECTED"
        );
        rejected.expectedHeaderReceived(
            "rejectionReason",
            "INVALID_PIN"
        );

        String originalPin =
            generatedPin.plainPin();

        String incorrectPin =
            (originalPin.charAt(0) == '0' ? "1" : "0")
                + originalPin.substring(1);

        sendConfirmation(
            requestId,
            incorrectPin
        );

        MockEndpoint.assertIsSatisfied(
            context
        );
    }

    @Test
    void shouldRejectExpiredPin() throws Exception {
        UUID requestId = UUID.randomUUID();

        PinService.GeneratedPin generatedPin =
            pinService.generate();

        configureSelectedRequest(
            generatedPin.pinHash(),
            Instant.now().minusSeconds(1)
        );

        MockEndpoint confirmed =
            getMockEndpoint(CONFIRM);

        MockEndpoint rejected =
            getMockEndpoint(REJECT);

        confirmed.expectedMessageCount(0);

        rejected.expectedMessageCount(1);
        rejected.expectedHeaderReceived(
            "confirmationStatus",
            "REJECTED"
        );
        rejected.expectedHeaderReceived(
            "rejectionReason",
            "PIN_EXPIRED"
        );

        sendConfirmation(
            requestId,
            generatedPin.plainPin()
        );

        MockEndpoint.assertIsSatisfied(
            context
        );
    }

    private void configureSelectedRequest(
    String pinHash,
    Instant expiresAt
) {
    getMockEndpoint(SELECT)
        .whenAnyExchangeReceived(exchange ->
            exchange.getMessage().setBody(
                Map.of(
                    "pin_hash",
                    pinHash,
                    "pin_expires_at",
                    expiresAt,
                    "status",
                    "PIN_GENERATED",
                    "msisdn",
                    "595981123456",
                    "document_number",
                    "4567890",
                    "donor_operator",
                    "TIGO",
                    "recipient_operator",
                    "PERSONAL"
                )
            )
        );
}

    private void sendConfirmation(
        UUID requestId,
        String pin
    ) throws Exception {

        PinConfirmation confirmation =
            new PinConfirmation(
                requestId,
                pin
            );

        template.sendBody(
            INPUT,
            serializer.serialize(confirmation)
        );
    }
}