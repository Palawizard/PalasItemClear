package net.palasitemclear.bin;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

final class RecoveryBinStackingTest {
    private static final UUID PLAYER_ONE = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PLAYER_TWO = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void matchesOwnershipForSamePlayer() {
        RecoveryBinEntry entry = RecoveryBinEntry.testEntry(1, PLAYER_ONE, "Palawi", 100L);

        assertTrue(RecoveryBinStore.matchesOwnership(entry, PLAYER_ONE, "Palawi"));
        assertTrue(RecoveryBinStore.matchesOwnership(entry, PLAYER_ONE, "palawi"));
    }

    @Test
    void rejectsDifferentOwners() {
        RecoveryBinEntry entry = RecoveryBinEntry.testEntry(1, PLAYER_ONE, "Palawi", 100L);

        assertFalse(RecoveryBinStore.matchesOwnership(entry, PLAYER_TWO, "Palawi"));
        assertFalse(RecoveryBinStore.matchesOwnership(entry, PLAYER_ONE, "Bob"));
    }

    @Test
    void matchesUnknownOwnership() {
        RecoveryBinEntry entry = RecoveryBinEntry.testEntry(1, null, null, 100L);

        assertTrue(RecoveryBinStore.matchesOwnership(entry, null, null));
        assertFalse(RecoveryBinStore.matchesOwnership(entry, PLAYER_ONE, "Palawi"));
    }
}
