package net.palasitemclear.clear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;
import org.junit.jupiter.api.Test;

final class DroppedItemClearerTest {
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID THROWER = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void prefersTheCurrentPlayerOwnerForAttribution() {
        assertEquals(OWNER, DroppedItemClearer.resolveAttributionUuid(OWNER, true, THROWER));
    }

    @Test
    void ignoresNonPlayerOwners() {
        assertNull(DroppedItemClearer.resolveAttributionUuid(OWNER, false, THROWER));
        assertNull(DroppedItemClearer.resolveAttributionUuid(OWNER, false, null));
    }

    @Test
    void fallsBackToTheThrowerForAttribution() {
        assertEquals(THROWER, DroppedItemClearer.resolveAttributionUuid(null, false, THROWER));
        assertNull(DroppedItemClearer.resolveAttributionUuid(null, false, null));
    }
}
