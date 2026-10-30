package net.palasitemclear.compat;

import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.players.NameAndId;

public final class ModernServerSupport {
    private ModernServerSupport() {
    }

    public static boolean hasAdminPermission(CommandSourceStack source) {
        PermissionSet permissions = source.permissions();
        if (permissions instanceof LevelBasedPermissionSet levelBased) {
            return levelBased.level().isEqualOrHigherThan(PermissionLevel.GAMEMASTERS);
        }
        return false;
    }

    public static String resolveOwnerName(MinecraftServer server, UUID ownerUuid) {
        ServerPlayer online = server.getPlayerList().getPlayer(ownerUuid);
        if (online != null) {
            return online.getGameProfile().name();
        }

        // Local user cache only: profileResolver() would block the server thread on a Mojang request.
        return server.services().nameToIdCache().get(ownerUuid).map(NameAndId::name).orElse(null);
    }
}
