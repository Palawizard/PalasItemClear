package net.palasitemclear;

import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public final class PlatformPaths {
    private PlatformPaths() {
    }

    public static Path getConfigDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }
}
