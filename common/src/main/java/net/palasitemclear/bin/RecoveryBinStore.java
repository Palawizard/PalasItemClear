package net.palasitemclear.bin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
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
        for (CapturedItem captured : capturedItems) {
            depositSingle(captured, expiresAtTick);
        }
    }

    private void depositSingle(CapturedItem captured, long expiresAtTick) {
        ItemStack remaining = captured.stack().copy();
        if (remaining.isEmpty()) {
            return;
        }

        for (RecoveryBinEntry entry : entries) {
            if (!matchesOwnership(entry, captured.ownerUuid(), captured.ownerName())) {
                continue;
            }

            if (!ItemStack.isSameItemSameTags(entry.stack(), remaining)) {
                continue;
            }

            int moved = mergeInto(entry, remaining, expiresAtTick);
            remaining.shrink(moved);
            if (remaining.isEmpty()) {
                return;
            }
        }

        entries.add(new RecoveryBinEntry(
                nextId++,
                remaining,
                captured.ownerUuid(),
                captured.ownerName(),
                expiresAtTick
        ));
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
        return Objects.equals(entry.ownerUuid().orElse(null), ownerUuid)
                && Objects.equals(normalizeOwnerName(entry.ownerName().orElse(null)), normalizeOwnerName(ownerName));
    }

    private static String normalizeOwnerName(String ownerName) {
        return ownerName == null ? null : ownerName.toLowerCase(Locale.ROOT);
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
