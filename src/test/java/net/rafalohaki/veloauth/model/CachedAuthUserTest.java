package net.rafalohaki.veloauth.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CachedAuthUserTest {

    @Test
    void constructor_nonPremiumWithPremiumUuidShouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new CachedAuthUser(
                UUID.randomUUID(),
                "Alice",
                "127.0.0.1",
                System.currentTimeMillis(),
                false,
                UUID.randomUUID()
        ));
    }

    @Test
    void toString_shouldRedactSensitiveFields() {
        CachedAuthUser user = new CachedAuthUser(
                UUID.randomUUID(),
                "Alice",
                "192.168.0.10",
                System.currentTimeMillis(),
                true,
                UUID.randomUUID()
        );

        String description = user.toString();

        assertTrue(description.contains("[REDACTED]"));
        assertFalse(description.contains("192.168.0.10"));
        assertFalse(description.contains(user.getUuid().toString()));
        assertFalse(description.contains(user.getPremiumUuid().toString()));
    }
}
