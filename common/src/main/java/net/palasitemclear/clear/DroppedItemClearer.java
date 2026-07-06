package net.palasitemclear.clear;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;

/**
 * Removes dropped item entities from dimensions that are currently loaded.
 */
public final class DroppedItemClearer {
    public ClearResult clear(MinecraftServer server, ClearFilter filter) {
        Map<ResourceLocation, Integer> removedByDimension = new LinkedHashMap<>();
        int totalRemoved = 0;

        for (ServerLevel level : server.getAllLevels()) {
            if (filter.isDimensionExcluded(level)) {
                continue;
            }

            int removed = clearLevel(level, filter);
            if (removed > 0) {
                removedByDimension.put(level.dimension().location(), removed);
                totalRemoved += removed;
            }
        }

        return new ClearResult(totalRemoved, removedByDimension);
    }

    private int clearLevel(ServerLevel level, ClearFilter filter) {
        List<ItemEntity> droppedItems = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof ItemEntity item && !item.isRemoved() && filter.shouldRemove(item)) {
                droppedItems.add(item);
            }
        }

        droppedItems.forEach(Entity::discard);
        return droppedItems.size();
    }
}
