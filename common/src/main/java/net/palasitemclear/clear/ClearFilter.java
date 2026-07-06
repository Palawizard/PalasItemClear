package net.palasitemclear.clear;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.palasitemclear.config.FiltersConfig;

public final class ClearFilter {
    private final Set<ResourceLocation> excludedItems;
    private final Set<ResourceLocation> excludedDimensions;
    private final int minAgeTicks;
    private final boolean excludeNamedItems;
    private final boolean excludePlayerOwnedItems;

    public ClearFilter(FiltersConfig filters) {
        this.excludedItems = parseResourceLocations(filters.excludedItems());
        this.excludedDimensions = parseResourceLocations(filters.excludedDimensions());
        this.minAgeTicks = filters.minAgeTicks();
        this.excludeNamedItems = filters.excludeNamedItems();
        this.excludePlayerOwnedItems = filters.excludePlayerOwnedItems();
    }

    public boolean isDimensionExcluded(ServerLevel level) {
        return excludedDimensions.contains(level.dimension().location());
    }

    public boolean shouldRemove(ItemEntity item) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item.getItem().getItem());

        if (excludedItems.contains(itemId)) {
            return false;
        }

        if (minAgeTicks > 0 && item.getAge() < minAgeTicks) {
            return false;
        }

        if (excludeNamedItems && item.hasCustomName()) {
            return false;
        }

        if (excludePlayerOwnedItems && item.getOwner() != null) {
            return false;
        }

        return true;
    }

    private static Set<ResourceLocation> parseResourceLocations(Iterable<String> values) {
        Set<ResourceLocation> parsed = new HashSet<>();
        for (String value : values) {
            ResourceLocation location = ResourceLocation.tryParse(value);
            if (location != null) {
                parsed.add(location);
            }
        }

        return parsed;
    }
}
