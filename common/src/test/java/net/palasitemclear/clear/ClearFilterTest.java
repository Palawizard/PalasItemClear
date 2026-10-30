package net.palasitemclear.clear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.palasitemclear.config.FiltersConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class ClearFilterTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void recognizesEntityAndStackCustomNames() {
        assertTrue(ClearFilter.isNamed(true, false));
        assertTrue(ClearFilter.isNamed(false, true));
        assertFalse(ClearFilter.isNamed(false, false));
    }

    @Test
    void recognizesTagEntries() {
        assertTrue(ClearFilter.isTag("#c:tools"));
        assertFalse(ClearFilter.isTag("minecraft:diamond"));
    }

    @Test
    void appliesPerItemMinimumAgeOverGlobalMinimumAge() {
        ClearFilter filter = new ClearFilter(new FiltersConfig(
                List.of(),
                List.of(),
                600,
                Map.of("minecraft:wheat_seeds", 100, "#minecraft:logs", 2_400),
                false,
                false
        ));

        assertEquals(100, requiredAgeTicks(filter, Items.WHEAT_SEEDS.getDefaultInstance()));
        assertEquals(600, requiredAgeTicks(filter, Items.COBBLESTONE.getDefaultInstance()));
    }

    private static int requiredAgeTicks(ClearFilter filter, ItemStack stack) {
        return filter.requiredAgeTicks(BuiltInRegistries.ITEM.getKey(stack.getItem()), stack);
    }
}
