package py.com.ccp.eapn;

import org.junit.jupiter.api.Test;
import py.com.ccp.eapn.route.StartupRoute;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppTest {

    @Test
    void shouldExposeApplicationStartupMessage() {
        assertEquals(
            "EAPN de Portabilidad Paraguay iniciada correctamente",
            StartupRoute.STARTUP_MESSAGE
        );
    }
}