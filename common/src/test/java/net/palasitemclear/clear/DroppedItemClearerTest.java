package net.palasitemclear.clear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;
import org.junit.jupiter.api.Test;

final class DroppedItemClearerTest {
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID THROWER = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void prefersTheCurrentOwnerForAttribution() {
        assertEquals(OWNER, DroppedItemClearer.resolveAttributionUuid(OWNER, THROWER));
    }

    @Test
    void fallsBackToTheThrowerForAttribution() {
        assertEquals(THROWER, DroppedItemClearer.resolveAttributionUuid(null, THROWER));
        assertNull(DroppedItemClearer.resolveAttributionUuid(null, null));
    }
}
