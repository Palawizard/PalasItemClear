package net.palasitemclear.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class ConfigValidatorTest {
    @Test
    void defaultsAreValid() throws ConfigValidationException {
        ModConfig config = ConfigValidator.validate(ModConfig.defaults());

        assertEquals(300, config.schedule().intervalSeconds());
        assertEquals(List.of(60, 30, 5), config.schedule().warningSeconds());
        assertEquals(6000L, config.intervalTicks());
        assertEquals(List.of(1200L, 600L, 100L), config.warningTicksDescending());
        assertEquals(120, config.bin().retentionSeconds());
    }

    @Test
    void normalizesMissingSections() throws ConfigValidationException {
        ModConfig config = ConfigValidator.validate(new ModConfig(0, null, null, null, null));

        assertEquals(ConfigConstants.CURRENT_VERSION, config.configVersion());
        assertEquals(MessagesConfig.defaults().warning(), config.messages().warning());
        assertEquals(FiltersConfig.defaults().excludedItems(), config.filters().excludedItems());
        assertEquals(BinConfig.defaults().retentionSeconds(), config.bin().retentionSeconds());
    }

    @Test
    void rejectsWarningsGreaterThanOrEqualToInterval() {
        ModConfig invalid = new ModConfig(
                1,
                new ScheduleConfig(300, List.of(300)),
                MessagesConfig.defaults(),
                FiltersConfig.defaults(),
                BinConfig.defaults()
        );

        ConfigValidationException exception = assertThrows(
                ConfigValidationException.class,
                () -> ConfigValidator.validate(invalid)
        );
        assertTrue(exception.getMessage().contains("less than the clear interval"));
    }

    @Test
    void rejectsDuplicateWarnings() {
        ModConfig invalid = new ModConfig(
                1,
                new ScheduleConfig(300, List.of(60, 60)),
                MessagesConfig.defaults(),
                FiltersConfig.defaults(),
                BinConfig.defaults()
        );

        assertThrows(ConfigValidationException.class, () -> ConfigValidator.validate(invalid));
    }

    @Test
    void rejectsNegativeMinAgeTicks() {
        ModConfig invalid = new ModConfig(
                1,
                ScheduleConfig.defaults(),
                MessagesConfig.defaults(),
                new FiltersConfig(List.of(), List.of(), -1, false, false),
                BinConfig.defaults()
        );

        assertThrows(ConfigValidationException.class, () -> ConfigValidator.validate(invalid));
    }

    @Test
    void acceptsConfiguredFilterLists() throws ConfigValidationException {
        ModConfig config = ConfigValidator.validate(new ModConfig(
                1,
                ScheduleConfig.defaults(),
                MessagesConfig.defaults(),
                new FiltersConfig(
                        List.of("minecraft:diamond"),
                        List.of("minecraft:the_end"),
                        100,
                        true,
                        true
                ),
                BinConfig.defaults()
        ));

        assertEquals(List.of("minecraft:diamond"), config.filters().excludedItems());
        assertEquals(List.of("minecraft:the_end"), config.filters().excludedDimensions());
        assertEquals(100, config.filters().minAgeTicks());
        assertTrue(config.filters().excludeNamedItems());
        assertTrue(config.filters().excludePlayerOwnedItems());
    }

    @Test
    void rejectsMalformedResourceIds() {
        ModConfig invalid = new ModConfig(
                1,
                ScheduleConfig.defaults(),
                MessagesConfig.defaults(),
                new FiltersConfig(List.of("Minecraft:Diamond"), List.of(), 0, false, false),
                BinConfig.defaults()
        );

        assertThrows(ConfigValidationException.class, () -> ConfigValidator.validate(invalid));
    }

    @Test
    void normalizesInvalidBinRetention() throws ConfigValidationException {
        ModConfig config = ConfigValidator.validate(new ModConfig(
                1,
                ScheduleConfig.defaults(),
                MessagesConfig.defaults(),
                FiltersConfig.defaults(),
                new BinConfig(0)
        ));

        assertEquals(BinConfig.defaults().retentionSeconds(), config.bin().retentionSeconds());
    }
}
