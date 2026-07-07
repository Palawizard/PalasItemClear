package net.palasitemclear.bin;

import java.util.Arrays;
import java.util.List;
import java.util.function.LongSupplier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Chest-sized paginated view over {@link RecoveryBinStore} entries.
 */
public final class RecoveryBinContainer extends SimpleContainer {
    private final RecoveryBinStore store;
    private final LongSupplier currentTick;
    private final int[] entryIds;

    public RecoveryBinContainer(
            RecoveryBinStore store,
            List<RecoveryBinEntry> pageEntries,
            int page,
            int totalPages,
            LongSupplier currentTick
    ) {
        super(RecoveryBinPagination.CONTAINER_SIZE);
        this.store = store;
        this.currentTick = currentTick;
        this.entryIds = new int[RecoveryBinPagination.CONTAINER_SIZE];
        Arrays.fill(entryIds, -1);

        for (int slot = 0; slot < pageEntries.size() && slot < RecoveryBinPagination.ITEMS_PER_PAGE; slot++) {
            RecoveryBinEntry entry = pageEntries.get(slot);
            super.setItem(slot, entry.stack().copy());
            entryIds[slot] = entry.id();
        }

        fillNavigation(page, totalPages);
    }

    private void fillNavigation(int page, int totalPages) {
        for (int slot = RecoveryBinPagination.NAV_ROW_START; slot < RecoveryBinPagination.CONTAINER_SIZE; slot++) {
            ItemStack navigationStack;
            if (slot == RecoveryBinPagination.SLOT_PREVIOUS && page > 0) {
                navigationStack = navigationItem(Items.ARROW, "Previous Page");
            } else if (slot == RecoveryBinPagination.SLOT_NEXT && page < totalPages - 1) {
                navigationStack = navigationItem(Items.ARROW, "Next Page");
            } else if (slot == RecoveryBinPagination.SLOT_PAGE_INFO) {
                navigationStack = navigationItem(Items.PAPER, "Page " + (page + 1) + " / " + totalPages);
            } else {
                navigationStack = navigationItem(Items.GRAY_STAINED_GLASS_PANE, " ");
            }

            super.setItem(slot, navigationStack);
        }
    }

    private static ItemStack navigationItem(net.minecraft.world.item.Item item, String label) {
        ItemStack stack = new ItemStack(item);
        stack.setHoverName(Component.literal(label));
        return stack;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (RecoveryBinPagination.isNavigationSlot(slot)) {
            return ItemStack.EMPTY;
        }

        return takeTrackedItem(slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (RecoveryBinPagination.isNavigationSlot(slot)) {
            return ItemStack.EMPTY;
        }

        return takeTrackedItem(slot, getItem(slot).getCount());
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    private ItemStack takeTrackedItem(int slot, int amount) {
        if (entryIds[slot] < 0) {
            return ItemStack.EMPTY;
        }

        ItemStack taken = store.takeById(entryIds[slot], amount, currentTick.getAsLong());
        RecoveryBinEntry remaining = store.findById(entryIds[slot]);
        if (remaining == null) {
            super.setItem(slot, ItemStack.EMPTY);
            entryIds[slot] = -1;
        } else {
            super.setItem(slot, remaining.stack().copy());
        }

        return taken;
    }
}
