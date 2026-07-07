package net.palasitemclear.bin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

final class RecoveryBinPerformanceTest {
    private static final int DROPPED_ENTITY_COUNT = 10_000;

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    @Timeout(10)
    void depositsTenThousandDroppedItemsWithinTheServerTickBudgetGuardrail() {
        RecoveryBinStore store = new RecoveryBinStore();
        List<CapturedItem> captured = new ArrayList<>(DROPPED_ENTITY_COUNT);
        for (int index = 0; index < DROPPED_ENTITY_COUNT; index++) {
            captured.add(new CapturedItem(new ItemStack(Items.COBBLESTONE), null, null));
        }

        org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(Duration.ofSeconds(5), () ->
                store.deposit(captured, 100L, 2_400L)
        );

        int storedItems = store.viewEntries(null).stream().mapToInt(entry -> entry.stack().getCount()).sum();
        assertEquals(DROPPED_ENTITY_COUNT, storedItems);
    }
}
