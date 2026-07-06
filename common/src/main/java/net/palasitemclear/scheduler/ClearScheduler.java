package net.palasitemclear.scheduler;

/**
 * A deterministic tick-based countdown that does not depend on wall-clock time.
 */
public final class ClearScheduler {
    private final long intervalTicks;
    private long remainingTicks;

    public ClearScheduler(long intervalTicks) {
        if (intervalTicks <= 0) {
            throw new IllegalArgumentException("The clear interval must be positive");
        }

        this.intervalTicks = intervalTicks;
        this.remainingTicks = intervalTicks;
    }

    /**
     * Advances the countdown by one server tick.
     *
     * @return {@code true} when a clear is due on this tick
     */
    public boolean tick() {
        remainingTicks--;
        if (remainingTicks > 0) {
            return false;
        }

        remainingTicks = intervalTicks;
        return true;
    }

    public void reset() {
        remainingTicks = intervalTicks;
    }

    public long intervalTicks() {
        return intervalTicks;
    }

    public long remainingTicks() {
        return remainingTicks;
    }
}
