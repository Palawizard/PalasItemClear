package net.palasitemclear.command;

import net.palasitemclear.clear.ClearResult;
import net.palasitemclear.config.ConfigValidationException;
import net.palasitemclear.config.ModConfig;
import net.palasitemclear.scheduler.ClearScheduler;
import net.palasitemclear.server.ServerClearController;

/**
 * Command-facing operations with stable English feedback for operators.
 */
public final class ClearAdminService {
    public static final String NOT_RUNNING = "The item clear service is not running.";

    private ClearAdminService() {
    }

    public static String help() {
        return String.join(System.lineSeparator(),
                "Palas Item Clear commands:",
                "  /palasitemclear help - Show this help",
                "  /palasitemclear status - Show scheduler state and last clear",
                "  /palasitemclear next - Show time until the next clear",
                "  /palasitemclear clear - Clear dropped items now",
                "  /palasitemclear pause - Pause the automatic schedule",
                "  /palasitemclear resume - Resume the automatic schedule",
                "  /palasitemclear reset - Reset the countdown to the full interval",
                "  /palasitemclear reload - Reload the configuration file",
                "  /palasitemclear set interval <seconds> - Change the clear interval"
        );
    }

    public static String status(ServerClearController controller) {
        if (!isActive(controller)) {
            return NOT_RUNNING;
        }

        ModConfig config = controller.configuration();
        ClearResult lastResult = controller.lastResult();
        long remainingSeconds = ticksToSeconds(controller.remainingTicks());

        return "Scheduler: " + controller.state()
                + ". Interval: " + config.schedule().intervalSeconds() + "s."
                + " Next clear in: " + remainingSeconds + "s."
                + " Last clear removed: " + lastResult.totalRemoved() + " items.";
    }

    public static String nextClear(ServerClearController controller) {
        if (!isActive(controller)) {
            return NOT_RUNNING;
        }

        if (controller.state() != ClearScheduler.State.RUNNING) {
            return "The scheduler is " + controller.state() + "; no clear is scheduled.";
        }

        return "Next clear in " + ticksToSeconds(controller.remainingTicks()) + " seconds.";
    }

    public static String clearNow(ServerClearController controller) {
        if (!isActive(controller)) {
            return NOT_RUNNING;
        }

        ClearResult result = controller.clearNow();
        return "Cleared " + result.totalRemoved() + " dropped items.";
    }

    public static String pause(ServerClearController controller) {
        if (!isActive(controller)) {
            return NOT_RUNNING;
        }

        if (controller.state() == ClearScheduler.State.PAUSED) {
            return "Item clear is already paused.";
        }

        controller.pause();
        return "Item clear paused. Use /palasitemclear resume to continue.";
    }

    public static String resume(ServerClearController controller) {
        if (!isActive(controller)) {
            return NOT_RUNNING;
        }

        if (controller.state() != ClearScheduler.State.PAUSED) {
            return "Item clear is not paused.";
        }

        controller.resume();
        return "Item clear resumed.";
    }

    public static String resetSchedule(ServerClearController controller) {
        if (!isActive(controller)) {
            return NOT_RUNNING;
        }

        controller.resetSchedule();
        return "Clear countdown reset.";
    }

    public static String reload(ServerClearController controller) {
        if (!isActive(controller)) {
            return NOT_RUNNING;
        }

        controller.reloadConfiguration();
        return "Configuration reloaded.";
    }

    public static String setInterval(ServerClearController controller, int intervalSeconds)
            throws ConfigValidationException {
        if (!isActive(controller)) {
            return NOT_RUNNING;
        }

        controller.setIntervalSeconds(intervalSeconds);
        return "Clear interval set to " + intervalSeconds + " seconds.";
    }

    public static boolean isActive(ServerClearController controller) {
        return controller != null && controller.isActive();
    }

    static long ticksToSeconds(long ticks) {
        return Math.max(0L, (ticks + 19L) / 20L);
    }
}
