package net.palasitemclear.clear;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.palasitemclear.config.FiltersConfig;

public final class ClearFilter {
    private final Set<ResourceLocation> excludedItems = new HashSet<>();
    private final List<TagKey<Item>> excludedItemTags = new ArrayList<>();
    private final Set<ResourceLocation> excludedDimensions = new HashSet<>();
    private final int minAgeTicks;
    private final Map<ResourceLocation, Integer> itemMinAgeTicks = new HashMap<>();
    private final Map<TagKey<Item>, Integer> tagMinAgeTicks = new HashMap<>();
    private final boolean excludeNamedItems;
    private final boolean excludePlayerOwnedItems;

    public ClearFilter(FiltersConfig filters) {
        for (String value : filters.excludedItems()) {
            if (isTag(value)) {
                TagKey<Item> tag = parseItemTag(value);
                if (tag != null) {
                    excludedItemTags.add(tag);
                }
            } else {
                ResourceLocation location = ResourceLocation.tryParse(value);
                if (location != null) {
                    excludedItems.add(location);
                }
            }
        }

        for (String value : filters.excludedDimensions()) {
            ResourceLocation location = ResourceLocation.tryParse(value);
            if (location != null) {
                excludedDimensions.add(location);
            }
        }

        for (Map.Entry<String, Integer> override : filters.minAgeTicksOverrides().entrySet()) {
            if (isTag(override.getKey())) {
                TagKey<Item> tag = parseItemTag(override.getKey());
                if (tag != null) {
                    tagMinAgeTicks.put(tag, override.getValue());
                }
            } else {
                ResourceLocation location = ResourceLocation.tryParse(override.getKey());
                if (location != null) {
                    itemMinAgeTicks.put(location, override.getValue());
                }
            }
        }

        this.minAgeTicks = filters.minAgeTicks();
        this.excludeNamedItems = filters.excludeNamedItems();
        this.excludePlayerOwnedItems = filters.excludePlayerOwnedItems();
    }

    public boolean isDimensionExcluded(ServerLevel level) {
        return excludedDimensions.contains(level.dimension().location());
    }

    public boolean shouldRemove(ItemEntity item) {
        ItemStack stack = item.getItem();
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());

        if (excludedItems.contains(itemId)) {
            return false;
        }

        for (TagKey<Item> tag : excludedItemTags) {
            if (stack.is(tag)) {
                return false;
            }
        }

        int requiredAgeTicks = requiredAgeTicks(itemId, stack);
        if (requiredAgeTicks > 0 && item.getAge() < requiredAgeTicks) {
            return false;
        }

        if (excludeNamedItems && isNamed(item.hasCustomName(), item.getItem().hasCustomHoverName())) {
            return false;
        }

        if (excludePlayerOwnedItems && item.getOwner() instanceof Player) {
            return false;
        }

        return true;
    }

    /**
     * An item ID override wins over tag overrides; when several tags match, the longest age applies.
     * Items without an override use the global minimum age.
     */
    int requiredAgeTicks(ResourceLocation itemId, ItemStack stack) {
        Integer itemOverride = itemMinAgeTicks.get(itemId);
        if (itemOverride != null) {
            return itemOverride;
        }

        Integer tagOverride = null;
        for (Map.Entry<TagKey<Item>, Integer> override : tagMinAgeTicks.entrySet()) {
            if (stack.is(override.getKey()) && (tagOverride == null || override.getValue() > tagOverride)) {
                tagOverride = override.getValue();
            }
        }

        return tagOverride != null ? tagOverride : minAgeTicks;
    }

    static boolean isNamed(boolean entityHasCustomName, boolean stackHasCustomName) {
        return entityHasCustomName || stackHasCustomName;
    }

    static boolean isTag(String value) {
        return value.startsWith(FiltersConfig.TAG_PREFIX);
    }

    private static TagKey<Item> parseItemTag(String value) {
        ResourceLocation location = ResourceLocation.tryParse(value.substring(FiltersConfig.TAG_PREFIX.length()));
        return location == null ? null : TagKey.create(Registries.ITEM, location);
    }
}
