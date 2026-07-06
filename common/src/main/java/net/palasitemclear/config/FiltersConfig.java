package net.palasitemclear.config;

import java.util.List;

public record FiltersConfig(
        List<String> excludedItems,
        List<String> excludedDimensions,
        int minAgeTicks,
        boolean excludeNamedItems,
        boolean excludePlayerOwnedItems
) {
    public static FiltersConfig defaults() {
        return new FiltersConfig(List.of(), List.of(), 0, false, false);
    }
}
