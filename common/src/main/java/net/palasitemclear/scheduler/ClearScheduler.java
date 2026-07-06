package net.palasitemclear.scheduler;

/**
 * A deterministic tick-based countdown that does not depend on wall-clock time.
 */
public final class ClearScheduler {
    public enum State {
        RUNNING,
        PAUSED,
        STOPPED
    }

    private final long intervalTicks;
    private long remainingTicks;
    private State state = State.RUNNING;

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
        if (state != State.RUNNING) {
            return false;
        }

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

    public void pause() {
        if (state == State.RUNNING) {
            state = State.PAUSED;
        }
    }

    public void resume() {
        if (state == State.PAUSED) {
            state = State.RUNNING;
        }
    }

    public void stop() {
        state = State.STOPPED;
        reset();
    }

    public void start() {
        state = State.RUNNING;
        reset();
    }

    public long intervalTicks() {
        return intervalTicks;
    }

    public long remainingTicks() {
        return remainingTicks;
    }

    public State state() {
        return state;
    }
}
