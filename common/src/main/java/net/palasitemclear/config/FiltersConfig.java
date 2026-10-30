package net.palasitemclear.config;

import java.util.List;
import java.util.Map;

/**
 * Item IDs in {@code excludedItems} and {@code minAgeTicksOverrides} may also be item tags written
 * as {@code #namespace:path}.
 */
public record FiltersConfig(
        List<String> excludedItems,
        List<String> excludedDimensions,
        int minAgeTicks,
        Map<String, Integer> minAgeTicksOverrides,
        boolean excludeNamedItems,
        boolean excludePlayerOwnedItems
) {
    public static final String TAG_PREFIX = "#";

    public static FiltersConfig defaults() {
        return new FiltersConfig(List.of(), List.of(), 0, Map.of(), false, false);
    }
}
