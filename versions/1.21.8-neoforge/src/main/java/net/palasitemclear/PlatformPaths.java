package net.palasitemclear;

import java.nio.file.Path;
import net.neoforged.fml.loading.FMLPaths;

public final class PlatformPaths {
    private PlatformPaths() {
    }

    public static Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }
}
