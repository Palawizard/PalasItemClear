package net.palasitemclear.config;

import java.util.List;

public record ScheduleConfig(int intervalSeconds, List<Integer> warningSeconds) {
    public static ScheduleConfig defaults() {
        return new ScheduleConfig(300, List.of(60, 30, 5));
    }
}
