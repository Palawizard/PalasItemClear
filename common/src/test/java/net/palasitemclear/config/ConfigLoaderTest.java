package net.palasitemclear.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ConfigLoaderTest {
    @TempDir
    Path configDirectory;

    @Test
    void createsDefaultConfigWhenMissing() {
        ConfigHolder holder = new ConfigHolder();
        ConfigLoader loader = new ConfigLoader(holder);

        ModConfig loaded = loader.loadOrCreate(configDirectory);

        assertEquals(ModConfig.defaults(), loaded);
        assertTrue(Files.exists(configDirectory.resolve(ConfigConstants.CONFIG_FILE_NAME)));
        assertEquals(loaded, holder.get());
    }

    @Test
    void reloadFallsBackToDefaultsForInvalidConfig() throws Exception {
        Path configPath = configDirectory.resolve(ConfigConstants.CONFIG_FILE_NAME);
        Files.createDirectories(configDirectory);
        Files.writeString(configPath, "{ \"configVersion\": 1, \"schedule\": { \"intervalSeconds\": -1 } }");

        ConfigHolder holder = new ConfigHolder();
        ConfigLoader loader = new ConfigLoader(holder);

        ModConfig loaded = loader.reload(configDirectory);

        assertEquals(ModConfig.defaults(), loaded);
        assertEquals(ModConfig.defaults(), holder.get());
        assertFalse(Files.list(configDirectory).filter(path -> path.getFileName().toString().startsWith(
                ConfigConstants.CONFIG_FILE_NAME + ".invalid-"
        )).toList().isEmpty());
    }

    @Test
    void reloadMigratesOlderConfigVersions() throws Exception {
        Path configPath = configDirectory.resolve(ConfigConstants.CONFIG_FILE_NAME);
        Files.createDirectories(configDirectory);
        Files.writeString(
                configPath,
                """
                {
                  "configVersion": 0,
                  "schedule": {
                    "intervalSeconds": 120,
                    "warningSeconds": [30, 10]
                  },
                  "messages": {
                    "warning": "<yellow>Clear in {seconds}s",
                    "cleared": "<gray>Removed {count}"
                  },
                  "filters": {
                    "excludedItems": [],
                    "excludedDimensions": [],
                    "minAgeTicks": 0,
                    "excludeNamedItems": false,
                    "excludePlayerOwnedItems": false
                  }
                }
                """
        );

        ConfigHolder holder = new ConfigHolder();
        ConfigLoader loader = new ConfigLoader(holder);

        ModConfig loaded = loader.reload(configDirectory);

        assertEquals(ConfigConstants.CURRENT_VERSION, loaded.configVersion());
        assertEquals(120, loaded.schedule().intervalSeconds());
        assertEquals("<yellow>Clear in {seconds}s", loaded.messages().warning());
    }

    @Test
    void persistDoesNotUpdateMemoryWhenTheWriteFails() throws Exception {
        ConfigHolder holder = new ConfigHolder();
        ConfigLoader loader = new ConfigLoader(holder);
        Path invalidDirectory = configDirectory.resolve("not-a-directory");
        Files.writeString(invalidDirectory, "file");
        ModConfig updated = new ModConfig(
                1,
                new ScheduleConfig(120, List.of(60, 30, 5)),
                MessagesConfig.defaults(),
                FiltersConfig.defaults(),
                BinConfig.defaults()
        );

        assertThrows(ConfigPersistenceException.class, () -> loader.persist(invalidDirectory, updated));
        assertEquals(ModConfig.defaults(), holder.get());
    }
}
