package net.palasitemclear.config;

public record MessagesConfig(String warning, String cleared) {
    public static MessagesConfig defaults() {
        return new MessagesConfig(
                "<yellow>Items on the ground will be cleared in <gold>{seconds}</gold> seconds.",
                "<gray>Cleared <white>{count}</white> dropped items."
        );
    }
}
