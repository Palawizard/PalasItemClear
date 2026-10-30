package net.palasitemclear.bin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * In-memory store for recently cleared items with tick-based expiry.
 */
public final class RecoveryBinStore {
    public static final int VIEW_SIZE = RecoveryBinPagination.CONTAINER_SIZE;

    private final List<RecoveryBinEntry> entries = new ArrayList<>();
    private int nextId = 1;

    public void deposit(List<CapturedItem> capturedItems, long currentTick, long retentionTicks) {
        if (capturedItems.isEmpty()) {
            return;
        }

        long expiresAtTick = currentTick + retentionTicks;
        Map<Item, List<RecoveryBinEntry>> openEntries = indexOpenEntries();
        for (CapturedItem captured : capturedItems) {
            depositSingle(captured, expiresAtTick, openEntries);
        }
    }

    /**
     * Groups entries that still have room by item, so each deposit only scans stacks it could
     * actually merge into instead of the whole bin.
     */
    private Map<Item, List<RecoveryBinEntry>> indexOpenEntries() {
        Map<Item, List<RecoveryBinEntry>> openEntries = new HashMap<>();
        for (RecoveryBinEntry entry : entries) {
            if (hasSpace(entry.stack())) {
                openEntries.computeIfAbsent(entry.stack().getItem(), item -> new ArrayList<>()).add(entry);
            }
        }

        return openEntries;
    }

    private void depositSingle(
            CapturedItem captured,
            long expiresAtTick,
            Map<Item, List<RecoveryBinEntry>> openEntries
    ) {
        ItemStack remaining = captured.stack().copy();
        if (remaining.isEmpty()) {
            return;
        }

        List<RecoveryBinEntry> candidates = remaining.isStackable()
                ? openEntries.computeIfAbsent(remaining.getItem(), item -> new ArrayList<>())
                : null;

        if (candidates != null) {
            Iterator<RecoveryBinEntry> iterator = candidates.iterator();
            while (iterator.hasNext()) {
                RecoveryBinEntry entry = iterator.next();
                if (!matchesOwnership(entry, captured.ownerUuid(), captured.ownerName())) {
                    continue;
                }

                if (!ItemStack.isSameItemSameTags(entry.stack(), remaining)) {
                    continue;
                }

                int moved = mergeInto(entry, remaining, expiresAtTick);
                remaining.shrink(moved);
                if (!hasSpace(entry.stack())) {
                    iterator.remove();
                }

                if (remaining.isEmpty()) {
                    return;
                }
            }
        }

        RecoveryBinEntry created = new RecoveryBinEntry(
                nextId++,
                remaining,
                captured.ownerUuid(),
                captured.ownerName(),
                expiresAtTick
        );
        entries.add(created);
        if (candidates != null && hasSpace(created.stack())) {
            candidates.add(created);
        }
    }

    private static boolean hasSpace(ItemStack stack) {
        return stack.getCount() < stack.getMaxStackSize();
    }

    private static int mergeInto(RecoveryBinEntry entry, ItemStack remaining, long expiresAtTick) {
        ItemStack existing = entry.stack();
        int space = existing.getMaxStackSize() - existing.getCount();
        if (space <= 0) {
            return 0;
        }

        int moved = Math.min(space, remaining.getCount());
        ItemStack merged = existing.copy();
        merged.grow(moved);
        entry.setStack(merged);
        entry.absorbExpiry(expiresAtTick);
        return moved;
    }

    static boolean matchesOwnership(RecoveryBinEntry entry, UUID ownerUuid, String ownerName) {
        if (!Objects.equals(entry.ownerUuid().orElse(null), ownerUuid)) {
            return false;
        }

        String entryName = entry.ownerName().orElse(null);
        return entryName == null ? ownerName == null : entryName.equalsIgnoreCase(ownerName);
    }

    public void evictExpired(long currentTick) {
        entries.removeIf(entry -> entry.isExpired(currentTick));
    }

    public List<RecoveryBinEntry> viewEntries(UUID ownerFilter) {
        if (ownerFilter == null) {
            return List.copyOf(entries);
        }

        return entries.stream()
                .filter(entry -> entry.matchesOwner(ownerFilter))
                .toList();
    }

    public List<RecoveryBinEntry> viewForPlayer(String playerName, UUID ownerUuid) {
        if (ownerUuid != null) {
            return viewEntries(ownerUuid);
        }

        if (playerName != null && !playerName.isBlank()) {
            return entries.stream()
                    .filter(entry -> entry.matchesOwnerName(playerName))
                    .toList();
        }

        return List.copyOf(entries);
    }

    public int countForPlayer(String playerName, UUID ownerUuid) {
        return viewForPlayer(playerName, ownerUuid).size();
    }

    public int countEntries(UUID ownerFilter) {
        return viewEntries(ownerFilter).size();
    }

    public Set<String> knownOwnerNames() {
        Set<String> names = new HashSet<>();
        for (RecoveryBinEntry entry : entries) {
            entry.ownerName().ifPresent(names::add);
        }

        return names;
    }

    public UUID resolveOwnerFilter(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return null;
        }

        for (RecoveryBinEntry entry : entries) {
            if (entry.matchesOwnerName(playerName) && entry.ownerUuid().isPresent()) {
                return entry.ownerUuid().get();
            }
        }

        return null;
    }

    public boolean hasEntriesForName(String playerName) {
        return entries.stream().anyMatch(entry -> entry.matchesOwnerName(playerName));
    }

    ItemStack takeById(int id, int amount, long currentTick) {
        if (amount <= 0) {
            return ItemStack.EMPTY;
        }

        evictExpired(currentTick);
        for (RecoveryBinEntry entry : entries) {
            if (entry.id() == id) {
                ItemStack taken = entry.stack().copy();
                taken.setCount(Math.min(amount, entry.stack().getCount()));
                ItemStack remaining = entry.stack().copy();
                remaining.shrink(taken.getCount());
                if (remaining.isEmpty()) {
                    entries.remove(entry);
                } else {
                    entry.setStack(remaining);
                }

                return taken;
            }
        }

        return ItemStack.EMPTY;
    }

    RecoveryBinEntry findById(int id) {
        for (RecoveryBinEntry entry : entries) {
            if (entry.id() == id) {
                return entry;
            }
        }

        return null;
    }

    int size() {
        return entries.size();
    }

    public void clear() {
        entries.clear();
    }

    void seedEntry(UUID ownerUuid, String ownerName, long expiresAtTick) {
        entries.add(RecoveryBinEntry.testEntry(nextId++, ownerUuid, ownerName, expiresAtTick));
    }

    int seedStack(ItemStack stack, long expiresAtTick) {
        int id = nextId++;
        entries.add(new RecoveryBinEntry(id, stack, null, null, expiresAtTick));
        return id;
    }
}
