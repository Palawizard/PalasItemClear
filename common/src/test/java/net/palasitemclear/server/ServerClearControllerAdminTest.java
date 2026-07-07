package net.palasitemclear.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.palasitemclear.clear.DroppedItemClearer;
import net.palasitemclear.command.ClearAdminService;
import net.palasitemclear.config.ConfigHolder;
import net.palasitemclear.config.ConfigValidationException;
import net.palasitemclear.scheduler.ClearScheduler;
import org.junit.jupiter.api.Test;

final class ServerClearControllerAdminTest {
    @Test
    void pauseAndResumeTransitionSchedulerState() {
        ServerClearController controller = new ServerClearController(new ConfigHolder(), new DroppedItemClearer());

        controller.pause();
        assertEquals(ClearScheduler.State.PAUSED, controller.state());

        controller.resume();
        assertEquals(ClearScheduler.State.RUNNING, controller.state());
    }

    @Test
    void adminServiceRequiresActiveServer() {
        ServerClearController controller = new ServerClearController(new ConfigHolder(), new DroppedItemClearer());

        assertEquals(ClearAdminService.NOT_RUNNING, ClearAdminService.pause(controller));
        assertEquals(ClearAdminService.NOT_RUNNING, ClearAdminService.resume(controller));
    }

    @Test
    void setIntervalUpdatesSchedulerAndConfiguration() throws ConfigValidationException {
        ServerClearController controller = new ServerClearController(new ConfigHolder(), new DroppedItemClearer());

        controller.setIntervalSeconds(120);

        assertEquals(120, controller.configuration().schedule().intervalSeconds());
        assertEquals(120L * 20L, controller.remainingTicks());
    }

    @Test
    void setIntervalPreservesPausedState() throws ConfigValidationException {
        ServerClearController controller = new ServerClearController(new ConfigHolder(), new DroppedItemClearer());
        controller.pause();

        controller.setIntervalSeconds(120);

        assertEquals(ClearScheduler.State.PAUSED, controller.state());
        assertEquals(120L * 20L, controller.remainingTicks());
    }

    @Test
    void resetScheduleRestoresFullInterval() {
        ServerClearController controller = new ServerClearController(new ConfigHolder(), new DroppedItemClearer());
        long intervalTicks = controller.remainingTicks();

        controller.resetSchedule();

        assertEquals(intervalTicks, controller.remainingTicks());
        assertEquals(ClearScheduler.State.RUNNING, controller.state());
    }

    @Test
    void rejectsIntervalThatInvalidatesWarnings() {
        ServerClearController controller = new ServerClearController(new ConfigHolder(), new DroppedItemClearer());

        assertThrows(ConfigValidationException.class, () -> controller.setIntervalSeconds(30));
    }
}
