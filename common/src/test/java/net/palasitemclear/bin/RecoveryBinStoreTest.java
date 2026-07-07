package net.palasitemclear.bin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class RecoveryBinStoreTest {
    private static final UUID PLAYER_ONE = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PLAYER_TWO = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void evictsExpiredEntries() {
        RecoveryBinStore store = new RecoveryBinStore();
        store.seedEntry(PLAYER_ONE, "Alice", 140L);

        store.evictExpired(139L);
        assertEquals(1, store.size());

        store.evictExpired(140L);
        assertEquals(0, store.size());
    }

    @Test
    void filtersEntriesByOwner() {
        RecoveryBinStore store = new RecoveryBinStore();
        store.seedEntry(PLAYER_ONE, "Alice", 200L);
        store.seedEntry(PLAYER_TWO, "Bob", 200L);
        store.seedEntry(null, null, 200L);

        assertEquals(1, store.viewEntries(PLAYER_ONE).size());
        assertEquals(1, store.viewForPlayer("Bob", null).size());
        assertEquals(3, store.viewForPlayer(null, null).size());
    }

    @Test
    void resolvesOwnerNamesFromStoredEntries() {
        RecoveryBinStore store = new RecoveryBinStore();
        store.seedEntry(PLAYER_ONE, "Alice", 200L);

        assertTrue(store.hasEntriesForName("alice"));
        assertEquals(PLAYER_ONE, store.resolveOwnerFilter("Alice"));
        assertTrue(store.knownOwnerNames().contains("Alice"));
    }

    @Test
    void onlyAllowsAStackToBeRecoveredOnceAcrossViews() {
        RecoveryBinStore store = new RecoveryBinStore();
        int entryId = store.seedStack(new ItemStack(Items.DIAMOND, 8), 200L);

        assertEquals(8, store.takeById(entryId, 64, 100L).getCount());
        assertTrue(store.takeById(entryId, 64, 100L).isEmpty());
    }

    @Test
    void refusesRecoveryAfterExpiry() {
        RecoveryBinStore store = new RecoveryBinStore();
        int entryId = store.seedStack(new ItemStack(Items.DIAMOND, 8), 100L);

        assertTrue(store.takeById(entryId, 64, 100L).isEmpty());
        assertEquals(0, store.size());
    }
}
