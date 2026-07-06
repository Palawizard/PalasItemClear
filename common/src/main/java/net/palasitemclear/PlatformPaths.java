package net.palasitemclear;

import dev.architectury.injectables.annotations.ExpectPlatform;

import java.nio.file.Path;

public final class PlatformPaths {
    private PlatformPaths() {
    }

    @ExpectPlatform
    public static Path getConfigDirectory() {
        throw new AssertionError();
    }
}
