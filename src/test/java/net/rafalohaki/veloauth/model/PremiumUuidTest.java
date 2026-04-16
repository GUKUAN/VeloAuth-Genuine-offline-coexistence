package net.rafalohaki.veloauth.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PremiumUuidTest {

    @Test
    void constructor_invalidUuidShouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new PremiumUuid("invalid-uuid", "Alice"));
    }

    @Test
    void setNickname_blankValueShouldThrowIllegalArgumentException() {
        PremiumUuid premiumUuid = new PremiumUuid(UUID.randomUUID(), "Alice");

        assertThrows(IllegalArgumentException.class, () -> premiumUuid.setNickname(" "));
    }

    @Test
    void toString_shouldRedactUuid() {
        PremiumUuid premiumUuid = new PremiumUuid(UUID.randomUUID(), "Alice");

        String description = premiumUuid.toString();

        assertTrue(description.contains("[REDACTED]"));
        assertFalse(description.contains(premiumUuid.getUuidString()));
    }
}
