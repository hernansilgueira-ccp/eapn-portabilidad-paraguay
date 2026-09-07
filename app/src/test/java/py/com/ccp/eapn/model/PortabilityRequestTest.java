package py.com.ccp.eapn.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PortabilityRequestTest {

    @Test
    void shouldCreateValidPortabilityRequest() {
        PortabilityRequest request = PortabilityRequest.create(
            "595981123456",
            "4567890",
            Operator.TIGO,
            Operator.PERSONAL
        );

        assertNotNull(request.requestId());
        assertNotNull(request.requestedAt());
        assertEquals(PortabilityStatus.CREATED, request.status());
        assertEquals("595981123456", request.msisdn());
    }

    @Test
    void shouldRejectInvalidMsisdn() {
        assertThrows(
            IllegalArgumentException.class,
            () -> PortabilityRequest.create(
                "0981123456",
                "4567890",
                Operator.TIGO,
                Operator.PERSONAL
            )
        );
    }

    @Test
    void shouldRejectEqualOperators() {
        assertThrows(
            IllegalArgumentException.class,
            () -> PortabilityRequest.create(
                "595981123456",
                "4567890",
                Operator.TIGO,
                Operator.TIGO
            )
        );
    }
}