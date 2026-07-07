package net.palasitemclear.bin;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;

public final class RecoveryBinMenu {
    private RecoveryBinMenu() {
    }

    public static Component titleFor(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return Component.literal("Recovery Bin");
        }

        return Component.literal("Recovery Bin: " + playerName);
    }

    public static void open(
            ServerPlayer player,
            RecoveryBinStore store,
            String playerName,
            UUID ownerUuid
    ) {
        open(player, store, playerName, ownerUuid, 0);
    }

    public static void open(
            ServerPlayer player,
            RecoveryBinStore store,
            String playerName,
            UUID ownerUuid,
            int page
    ) {
        MinecraftServer server = player.serverLevel().getServer();
        store.evictExpired(server.getTickCount());

        List<RecoveryBinEntry> allEntries = store.viewForPlayer(playerName, ownerUuid);
        int totalPages = RecoveryBinPagination.pageCount(allEntries.size());
        int safePage = Math.min(Math.max(page, 0), totalPages - 1);
        List<RecoveryBinEntry> pageEntries = RecoveryBinPagination.slice(allEntries, safePage);
        RecoveryBinContainer container = new RecoveryBinContainer(store, pageEntries, safePage, totalPages);

        MenuProvider provider = new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return titleFor(playerName);
            }

            @Override
            public AbstractContainerMenu createMenu(
                    int containerId,
                    net.minecraft.world.entity.player.Inventory inventory,
                    net.minecraft.world.entity.player.Player ignored
            ) {
                return new RecoveryBinChestMenu(
                        containerId,
                        inventory,
                        container,
                        player,
                        store,
                        playerName,
                        ownerUuid,
                        safePage
                );
            }
        };

        player.openMenu(provider);
    }

    public static Optional<UUID> resolveOwnerFilter(
            MinecraftServer server,
            RecoveryBinStore store,
            String playerName
    ) {
        if (playerName == null || playerName.isBlank()) {
            return Optional.empty();
        }

        ServerPlayer online = server.getPlayerList().getPlayerByName(playerName);
        if (online != null) {
            return Optional.of(online.getUUID());
        }

        UUID fromStore = store.resolveOwnerFilter(playerName);
        if (fromStore != null) {
            return Optional.of(fromStore);
        }

        return Optional.empty();
    }

    public static String normalizePlayerName(String playerName) {
        return playerName.toLowerCase(Locale.ROOT);
    }
}
