package net.palasitemclear.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class ClearSchedulerTest {
    @Test
    void rejectsNonPositiveIntervals() {
        assertThrows(IllegalArgumentException.class, () -> new ClearScheduler(0));
        assertThrows(IllegalArgumentException.class, () -> new ClearScheduler(-1));
    }

    @Test
    void clearsExactlyWhenCountdownReachesZero() {
        ClearScheduler scheduler = new ClearScheduler(3);

        assertFalse(scheduler.tick());
        assertEquals(2, scheduler.remainingTicks());
        assertFalse(scheduler.tick());
        assertEquals(1, scheduler.remainingTicks());
        assertTrue(scheduler.tick());
    }

    @Test
    void restartsCountdownAfterEveryClear() {
        ClearScheduler scheduler = new ClearScheduler(2);

        assertFalse(scheduler.tick());
        assertTrue(scheduler.tick());
        assertEquals(2, scheduler.remainingTicks());
        assertFalse(scheduler.tick());
        assertTrue(scheduler.tick());
    }

    @Test
    void resetRestoresTheFullInterval() {
        ClearScheduler scheduler = new ClearScheduler(20);

        scheduler.tick();
        scheduler.tick();
        scheduler.reset();

        assertEquals(20, scheduler.remainingTicks());
    }

    @Test
    void pausedCountdownDoesNotAdvance() {
        ClearScheduler scheduler = new ClearScheduler(2);

        scheduler.pause();

        assertFalse(scheduler.tick());
        assertEquals(2, scheduler.remainingTicks());
        assertEquals(ClearScheduler.State.PAUSED, scheduler.state());

        scheduler.resume();
        assertFalse(scheduler.tick());
        assertEquals(1, scheduler.remainingTicks());
    }

    @Test
    void stoppedCountdownRequiresAStart() {
        ClearScheduler scheduler = new ClearScheduler(1);

        scheduler.stop();

        assertFalse(scheduler.tick());
        assertEquals(ClearScheduler.State.STOPPED, scheduler.state());

        scheduler.start();
        assertTrue(scheduler.tick());
        assertEquals(ClearScheduler.State.RUNNING, scheduler.state());
    }
}
