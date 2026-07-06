package net.palasitemclear.fabric;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public final class PlatformPathsImpl {
    private PlatformPathsImpl() {
    }

    public static Path getConfigDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }
}
