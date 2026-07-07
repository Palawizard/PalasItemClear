package net.palasitemclear.bin;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.world.item.ItemStack;

final class RecoveryBinEntry {
    private final int id;
    private ItemStack stack;
    private final UUID ownerUuid;
    private final String ownerName;
    private long expiresAtTick;

    RecoveryBinEntry(int id, ItemStack stack, UUID ownerUuid, String ownerName, long expiresAtTick) {
        this.id = id;
        this.stack = stack == null ? null : stack.copy();
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.expiresAtTick = expiresAtTick;
    }

    static RecoveryBinEntry testEntry(int id, UUID ownerUuid, String ownerName, long expiresAtTick) {
        return new RecoveryBinEntry(id, null, ownerUuid, ownerName, expiresAtTick);
    }

    int id() {
        return id;
    }

    ItemStack stack() {
        if (stack == null) {
            throw new IllegalStateException("Recovery bin entry has no item stack");
        }

        return stack;
    }

    void setStack(ItemStack value) {
        this.stack = value.copy();
    }

    void absorbExpiry(long otherExpiryTick) {
        expiresAtTick = Math.min(expiresAtTick, otherExpiryTick);
    }

    Optional<UUID> ownerUuid() {
        return Optional.ofNullable(ownerUuid);
    }

    Optional<String> ownerName() {
        return Optional.ofNullable(ownerName);
    }

    long expiresAtTick() {
        return expiresAtTick;
    }

    boolean isExpired(long currentTick) {
        return currentTick >= expiresAtTick;
    }

    boolean matchesOwner(UUID filter) {
        return ownerUuid != null && ownerUuid.equals(filter);
    }

    boolean matchesOwnerName(String filterName) {
        return ownerName != null && ownerName.equalsIgnoreCase(filterName);
    }
}
