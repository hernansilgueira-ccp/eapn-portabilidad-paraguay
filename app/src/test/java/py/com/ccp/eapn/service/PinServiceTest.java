package py.com.ccp.eapn.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PinServiceTest {

    @Test
    void shouldGenerateSixDigitPin() {
        PinService.GeneratedPin generatedPin =
            new PinService().generate();

        assertTrue(
            generatedPin.plainPin().matches("\\d{6}")
        );
    }

    @Test
    void shouldGenerateSha256Hash() {
        PinService.GeneratedPin generatedPin =
            new PinService().generate();

        assertEquals(
            64,
            generatedPin.pinHash().length()
        );
        assertTrue(
            generatedPin.pinHash()
                .matches("[0-9a-f]{64}")
        );
    }

    @Test
    void shouldGenerateFutureExpiration() {
        PinService.GeneratedPin generatedPin =
            new PinService().generate();

        assertTrue(
            generatedPin.expiresAt()
                .isAfter(Instant.now())
        );
    }
    @Test
void shouldAcceptMatchingPin() {
    PinService service = new PinService();

    PinService.GeneratedPin generatedPin =
        service.generate();

    assertTrue(
        service.matches(
            generatedPin.plainPin(),
            generatedPin.pinHash()
        )
    );
}

@Test
void shouldRejectIncorrectPin() {
    PinService service = new PinService();

    PinService.GeneratedPin generatedPin =
        service.generate();

    String originalPin =
        generatedPin.plainPin();

    String incorrectPin =
        (originalPin.charAt(0) == '0' ? "1" : "0")
            + originalPin.substring(1);

    assertTrue(
        !service.matches(
            incorrectPin,
            generatedPin.pinHash()
        )
    );
}
}