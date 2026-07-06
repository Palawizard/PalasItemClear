package net.palasitemclear.config;

import java.util.concurrent.atomic.AtomicReference;

public final class ConfigHolder {
    private final AtomicReference<ModConfig> config = new AtomicReference<>(ModConfig.defaults());

    public ModConfig get() {
        return config.get();
    }

    void set(ModConfig value) {
        config.set(value);
    }
}
