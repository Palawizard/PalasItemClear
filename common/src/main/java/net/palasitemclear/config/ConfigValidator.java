package net.palasitemclear.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

public final class ConfigValidator {
    private ConfigValidator() {
    }

    public static ModConfig normalize(ModConfig raw) {
        ScheduleConfig schedule = raw.schedule() == null ? ScheduleConfig.defaults() : raw.schedule();
        MessagesConfig messages = raw.messages() == null ? MessagesConfig.defaults() : raw.messages();
        FiltersConfig filters = raw.filters() == null ? FiltersConfig.defaults() : raw.filters();
        BinConfig bin = raw.bin() == null ? BinConfig.defaults() : raw.bin();

        int configVersion = raw.configVersion() <= 0 ? ConfigConstants.CURRENT_VERSION : raw.configVersion();

        List<Integer> warningSeconds = schedule.warningSeconds() == null
                ? ScheduleConfig.defaults().warningSeconds()
                : List.copyOf(schedule.warningSeconds());

        List<String> excludedItems = filters.excludedItems() == null
                ? List.of()
                : List.copyOf(filters.excludedItems());

        List<String> excludedDimensions = filters.excludedDimensions() == null
                ? List.of()
                : List.copyOf(filters.excludedDimensions());

        String warningMessage = messages.warning() == null || messages.warning().isBlank()
                ? MessagesConfig.defaults().warning()
                : messages.warning();

        String clearedMessage = messages.cleared() == null || messages.cleared().isBlank()
                ? MessagesConfig.defaults().cleared()
                : messages.cleared();

        return new ModConfig(
                configVersion,
                new ScheduleConfig(schedule.intervalSeconds(), warningSeconds),
                new MessagesConfig(warningMessage, clearedMessage),
                new FiltersConfig(
                        excludedItems,
                        excludedDimensions,
                        filters.minAgeTicks(),
                        filters.excludeNamedItems(),
                        filters.excludePlayerOwnedItems()
                ),
                new BinConfig(bin.retentionSeconds() <= 0 ? BinConfig.defaults().retentionSeconds() : bin.retentionSeconds())
        );
    }

    public static ModConfig validate(ModConfig raw) throws ConfigValidationException {
        ModConfig config = normalize(raw);

        if (config.configVersion() > ConfigConstants.CURRENT_VERSION) {
            throw new ConfigValidationException(
                    "Unsupported config version " + config.configVersion()
                            + "; this mod supports up to version " + ConfigConstants.CURRENT_VERSION
            );
        }

        if (config.schedule().intervalSeconds() <= 0) {
            throw new ConfigValidationException("schedule.intervalSeconds must be positive");
        }

        int intervalSeconds = config.schedule().intervalSeconds();
        Set<Integer> seenWarnings = new HashSet<>();
        List<Integer> sortedWarnings = new ArrayList<>(config.schedule().warningSeconds());

        for (int warningSeconds : sortedWarnings) {
            if (warningSeconds <= 0) {
                throw new ConfigValidationException("Each schedule.warningSeconds entry must be positive");
            }

            if (warningSeconds >= intervalSeconds) {
                throw new ConfigValidationException(
                        "Warning time " + warningSeconds + "s must be less than the clear interval "
                                + intervalSeconds + "s"
                );
            }

            if (!seenWarnings.add(warningSeconds)) {
                throw new ConfigValidationException("Duplicate warning time: " + warningSeconds + "s");
            }
        }

        if (config.filters().minAgeTicks() < 0) {
            throw new ConfigValidationException("filters.minAgeTicks cannot be negative");
        }

        validateResourceIds(config.filters().excludedItems(), "filters.excludedItems");
        validateResourceIds(config.filters().excludedDimensions(), "filters.excludedDimensions");

        if (config.bin().retentionSeconds() <= 0) {
            throw new ConfigValidationException("bin.retentionSeconds must be positive");
        }

        return config;
    }

    private static void validateResourceIds(List<String> values, String fieldName) throws ConfigValidationException {
        for (String value : values) {
            if (value == null || value.isBlank()) {
                throw new ConfigValidationException(fieldName + " entries cannot be blank");
            }

            if (!value.contains(":")) {
                throw new ConfigValidationException(fieldName + " entry must use namespace:id format: " + value);
            }

            String[] parts = value.split(":", 2);
            if (parts[0].isBlank() || parts[1].isBlank() || ResourceLocation.tryParse(value) == null) {
                throw new ConfigValidationException(fieldName + " contains an invalid resource ID: " + value);
            }
        }
    }
}
