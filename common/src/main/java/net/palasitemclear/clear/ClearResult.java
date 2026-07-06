package net.palasitemclear.clear;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

public record ClearResult(int totalRemoved, Map<ResourceLocation, Integer> removedByDimension) {
    public ClearResult {
        if (totalRemoved < 0) {
            throw new IllegalArgumentException("The removed item count cannot be negative");
        }

        removedByDimension = Collections.unmodifiableMap(new LinkedHashMap<>(removedByDimension));
    }

    public static ClearResult empty() {
        return new ClearResult(0, Map.of());
    }
}
