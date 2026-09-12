package py.com.ccp.eapn.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

public class PinService {

    private static final int PIN_BOUND = 1_000_000;
    private static final Duration PIN_VALIDITY =
        Duration.ofMinutes(5);

    private final SecureRandom secureRandom;

    public PinService() {
        this.secureRandom = new SecureRandom();
    }

    public GeneratedPin generate() {
        String plainPin = String.format(
            "%06d",
            secureRandom.nextInt(PIN_BOUND)
        );

        String pinHash = hash(plainPin);
        Instant expiresAt =
            Instant.now().plus(PIN_VALIDITY);

        return new GeneratedPin(
            plainPin,
            pinHash,
            expiresAt
        );
    }

    private String hash(String value) {
        try {
            MessageDigest digest =
                MessageDigest.getInstance("SHA-256");

            byte[] encoded = digest.digest(
                value.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(encoded);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                "SHA-256 no está disponible",
                exception
            );
        }
    }

    public record GeneratedPin(
        String plainPin,
        String pinHash,
        Instant expiresAt
    ) {
    }
}