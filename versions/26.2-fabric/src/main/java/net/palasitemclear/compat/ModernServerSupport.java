package net.palasitemclear.compat;

import com.mojang.authlib.GameProfile;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.permissions.PermissionSet;

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

        Optional<GameProfile> profile = server.services().profileResolver().fetchById(ownerUuid);
        return profile.map(GameProfile::name).orElse(null);
    }
}
