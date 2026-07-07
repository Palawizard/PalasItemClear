package net.palasitemclear.clear;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.palasitemclear.bin.CapturedItem;
import net.palasitemclear.compat.ModernServerSupport;

/**
 * Removes dropped item entities from dimensions that are currently loaded.
 */
public final class DroppedItemClearer {
    public ClearResult clear(MinecraftServer server, ClearFilter filter) {
        return clear(server, filter, null);
    }

    public ClearResult clear(MinecraftServer server, ClearFilter filter, List<CapturedItem> capturedOut) {
        Map<Identifier, Integer> removedByDimension = new LinkedHashMap<>();
        int totalRemoved = 0;

        for (ServerLevel level : server.getAllLevels()) {
            if (filter.isDimensionExcluded(level)) {
                continue;
            }

            int removed = clearLevel(server, level, filter, capturedOut);
            if (removed > 0) {
                removedByDimension.put(level.dimension().identifier(), removed);
                totalRemoved += removed;
            }
        }

        return new ClearResult(totalRemoved, removedByDimension);
    }

    private int clearLevel(
            MinecraftServer server,
            ServerLevel level,
            ClearFilter filter,
            List<CapturedItem> capturedOut
    ) {
        List<ItemEntity> droppedItems = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof ItemEntity item && !item.isRemoved() && filter.shouldRemove(item)) {
                droppedItems.add(item);
            }
        }

        for (ItemEntity item : droppedItems) {
            if (capturedOut != null) {
                capturedOut.add(captureItem(item, server));
            }

            item.discard();
        }

        return droppedItems.size();
    }

    private static CapturedItem captureItem(ItemEntity item, MinecraftServer server) {
        UUID ownerUuid = item.getOwner() == null ? null : item.getOwner().getUUID();
        String ownerName = null;

        if (ownerUuid != null) {
            ownerName = ModernServerSupport.resolveOwnerName(server, ownerUuid);
        }

        return new CapturedItem(item.getItem(), ownerUuid, ownerName);
    }
}
