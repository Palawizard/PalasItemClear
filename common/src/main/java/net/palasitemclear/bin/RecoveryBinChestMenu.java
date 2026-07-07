package net.palasitemclear.bin;

import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;

final class RecoveryBinChestMenu extends ChestMenu {
    private final ServerPlayer player;
    private final RecoveryBinStore store;
    private final String filterName;
    private final UUID ownerUuid;
    private final int page;

    RecoveryBinChestMenu(
            int containerId,
            net.minecraft.world.entity.player.Inventory playerInventory,
            RecoveryBinContainer container,
            ServerPlayer player,
            RecoveryBinStore store,
            String filterName,
            UUID ownerUuid,
            int page
    ) {
        super(MenuType.GENERIC_9x6, containerId, playerInventory, container, 6);
        this.player = player;
        this.store = store;
        this.filterName = filterName;
        this.ownerUuid = ownerUuid;
        this.page = page;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player ignored) {
        if (slotId >= 0 && slotId < slots.size()) {
            Slot slot = slots.get(slotId);
            if (slot.container instanceof RecoveryBinContainer) {
                int containerSlot = slot.getContainerSlot();
                if (RecoveryBinPagination.isNavigationSlot(containerSlot)) {
                    handleNavigation(containerSlot);
                    return;
                }
            }
        }

        super.clicked(slotId, button, clickType, player);
    }

    private void handleNavigation(int containerSlot) {
        int newPage = page;
        if (RecoveryBinPagination.isPreviousSlot(containerSlot)) {
            newPage--;
        } else if (RecoveryBinPagination.isNextSlot(containerSlot)) {
            newPage++;
        } else {
            return;
        }

        if (newPage < 0) {
            return;
        }

        RecoveryBinMenu.open(player, store, filterName, ownerUuid, newPage);
    }
}
