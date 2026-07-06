package net.palasitemclear.clear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

final class ClearResultTest {
    @Test
    void protectsDimensionCountsFromMutation() {
        ResourceLocation overworld = new ResourceLocation("minecraft", "overworld");
        Map<ResourceLocation, Integer> counts = new LinkedHashMap<>();
        counts.put(overworld, 4);

        ClearResult result = new ClearResult(4, counts);
        counts.put(overworld, 12);

        assertEquals(4, result.removedByDimension().get(overworld));
        assertThrows(UnsupportedOperationException.class,
                () -> result.removedByDimension().put(overworld, 1));
    }

    @Test
    void emptyResultContainsNoRemovedItems() {
        ClearResult result = ClearResult.empty();

        assertEquals(0, result.totalRemoved());
        assertEquals(Map.of(), result.removedByDimension());
    }

    @Test
    void rejectsNegativeTotals() {
        assertThrows(IllegalArgumentException.class, () -> new ClearResult(-1, Map.of()));
    }
}
