package net.palasitemclear.bin;

import java.util.Arrays;
import java.util.List;
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
    private final int[] entryIds;

    public RecoveryBinContainer(
            RecoveryBinStore store,
            List<RecoveryBinEntry> pageEntries,
            int page,
            int totalPages
    ) {
        super(RecoveryBinPagination.CONTAINER_SIZE);
        this.store = store;
        this.entryIds = new int[RecoveryBinPagination.CONTAINER_SIZE];
        Arrays.fill(entryIds, -1);

        for (int slot = 0; slot < pageEntries.size() && slot < RecoveryBinPagination.ITEMS_PER_PAGE; slot++) {
            RecoveryBinEntry entry = pageEntries.get(slot);
            setItem(slot, entry.stack().copy());
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
    public void setItem(int slot, ItemStack stack) {
        if (RecoveryBinPagination.isNavigationSlot(slot)) {
            return;
        }

        if (stack.isEmpty()) {
            removeTrackedEntry(slot);
        }

        super.setItem(slot, stack);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (RecoveryBinPagination.isNavigationSlot(slot)) {
            return ItemStack.EMPTY;
        }

        ItemStack removed = super.removeItem(slot, amount);
        syncSlotToStore(slot);
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (RecoveryBinPagination.isNavigationSlot(slot)) {
            return ItemStack.EMPTY;
        }

        ItemStack removed = super.removeItemNoUpdate(slot);
        syncSlotToStore(slot);
        return removed;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    private void syncSlotToStore(int slot) {
        if (entryIds[slot] < 0) {
            return;
        }

        ItemStack remaining = getItem(slot);
        if (remaining.isEmpty()) {
            removeTrackedEntry(slot);
            return;
        }

        store.updateStack(entryIds[slot], remaining);
    }

    private void removeTrackedEntry(int slot) {
        if (entryIds[slot] >= 0) {
            store.removeById(entryIds[slot]);
            entryIds[slot] = -1;
        }
    }
}
