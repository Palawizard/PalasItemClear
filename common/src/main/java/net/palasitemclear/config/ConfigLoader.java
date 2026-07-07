package net.palasitemclear.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import net.palasitemclear.PalasItemClear;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ConfigLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(PalasItemClear.MOD_ID);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final ConfigHolder holder;

    public ConfigLoader(ConfigHolder holder) {
        this.holder = holder;
    }

    public ModConfig loadOrCreate(Path configDirectory) {
        Path configPath = configDirectory.resolve(ConfigConstants.CONFIG_FILE_NAME);

        if (!Files.exists(configPath)) {
            ModConfig defaults = ModConfig.defaults();
            try {
                writeConfig(configPath, defaults);
                LOGGER.info("Created default configuration at {}", configPath);
            } catch (IOException exception) {
                LOGGER.warn("Could not create default configuration at {}; using in-memory defaults", configPath, exception);
            }
            holder.set(defaults);
            return defaults;
        }

        return reload(configDirectory);
    }

    public ModConfig reload(Path configDirectory) {
        Path configPath = configDirectory.resolve(ConfigConstants.CONFIG_FILE_NAME);

        try {
            ModConfig loaded = readConfig(configPath);
            ModConfig validated = ConfigValidator.validate(loaded);
            ModConfig migrated = migrateIfNeeded(validated);

            if (migrated.configVersion() != loaded.configVersion()
                    || !migrated.equals(loaded)) {
                try {
                    writeConfig(configPath, migrated);
                } catch (IOException exception) {
                    LOGGER.warn("Could not persist normalized configuration at {}", configPath, exception);
                }
            }

            holder.set(migrated);
            LOGGER.info(
                    "Loaded configuration version {} with intervalSeconds={} and warnings={}",
                    migrated.configVersion(),
                    migrated.schedule().intervalSeconds(),
                    migrated.schedule().warningSeconds()
            );
            return migrated;
        } catch (ConfigValidationException | IOException | JsonSyntaxException exception) {
            backupBrokenConfig(configPath, exception);
            ModConfig defaults = ModConfig.defaults();
            holder.set(defaults);
            LOGGER.warn("Using default configuration after a load failure", exception);
            return defaults;
        }
    }

    private ModConfig readConfig(Path configPath) throws IOException, ConfigValidationException {
        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            ModConfig parsed = GSON.fromJson(reader, ModConfig.class);
            if (parsed == null) {
                throw new ConfigValidationException("Configuration file is empty");
            }

            return parsed;
        }
    }

    private ModConfig migrateIfNeeded(ModConfig config) {
        if (config.configVersion() < ConfigConstants.CURRENT_VERSION) {
            return new ModConfig(
                    ConfigConstants.CURRENT_VERSION,
                    config.schedule(),
                    config.messages(),
                    config.filters(),
                    config.bin() == null ? BinConfig.defaults() : config.bin()
            );
        }

        return config;
    }

    public void persist(Path configDirectory, ModConfig config) throws ConfigPersistenceException {
        Path configPath = configDirectory.resolve(ConfigConstants.CONFIG_FILE_NAME);
        try {
            writeConfig(configPath, config);
        } catch (IOException exception) {
            throw new ConfigPersistenceException("Failed to save configuration to " + configPath, exception);
        }
        holder.set(config);
    }

    private void writeConfig(Path configPath, ModConfig config) throws IOException {
        Path tempPath = configPath.resolveSibling(configPath.getFileName() + ".tmp");

        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(tempPath, StandardCharsets.UTF_8)) {
                GSON.toJson(config, writer);
            }

            try {
                Files.move(tempPath, configPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(tempPath, configPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            try {
                Files.deleteIfExists(tempPath);
            } catch (IOException cleanupException) {
                exception.addSuppressed(cleanupException);
            }

            throw exception;
        }
    }

    private void backupBrokenConfig(Path configPath, Exception exception) {
        if (!Files.exists(configPath)) {
            return;
        }

        Path backupPath = configPath.resolveSibling(
                configPath.getFileName() + ".invalid-" + Instant.now().getEpochSecond() + ".json"
        );

        try {
            Files.copy(configPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.warn("Backed up invalid configuration to {} ({})", backupPath, exception.getMessage());
        } catch (IOException backupException) {
            LOGGER.warn("Failed to back up invalid configuration at {}", configPath, backupException);
        }
    }
}
