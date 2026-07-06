package net.palasitemclear.forge;

import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

public final class PlatformPathsImpl {
    private PlatformPathsImpl() {
    }

    public static Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }
}
