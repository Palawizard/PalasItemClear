package net.palasitemclear.config;

public record BinConfig(int retentionSeconds) {
    public static BinConfig defaults() {
        return new BinConfig(120);
    }

    public long retentionTicks() {
        return retentionSeconds * 20L;
    }
}
