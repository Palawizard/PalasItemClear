package net.palasitemclear.config;

import java.util.Comparator;
import java.util.List;

public record ModConfig(
        int configVersion,
        ScheduleConfig schedule,
        MessagesConfig messages,
        FiltersConfig filters,
        BinConfig bin
) {
    public static ModConfig defaults() {
        return new ModConfig(
                ConfigConstants.CURRENT_VERSION,
                ScheduleConfig.defaults(),
                MessagesConfig.defaults(),
                FiltersConfig.defaults(),
                BinConfig.defaults()
        );
    }

    public long intervalTicks() {
        return schedule.intervalSeconds() * 20L;
    }

    public List<Long> warningTicksDescending() {
        return schedule.warningSeconds().stream()
                .map(seconds -> seconds * 20L)
                .sorted(Comparator.reverseOrder())
                .toList();
    }
}
