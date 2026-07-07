package net.palasitemclear.bin;

import java.util.UUID;
import net.minecraft.world.item.ItemStack;

public record CapturedItem(ItemStack stack, UUID ownerUuid, String ownerName) {
    public CapturedItem {
        stack = stack.copy();
        ownerName = ownerName == null || ownerName.isBlank() ? null : ownerName;
    }
}
