package net.palasitemclear.server;

import net.minecraft.server.MinecraftServer;
import net.palasitemclear.PalasItemClear;
import net.palasitemclear.clear.ClearResult;
import net.palasitemclear.clear.DroppedItemClearer;
import net.palasitemclear.scheduler.ClearScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Owns the scheduler for one dedicated or integrated server instance.
 */
public final class ServerClearController {
    private static final Logger LOGGER = LoggerFactory.getLogger(PalasItemClear.MOD_ID);

    private final ClearScheduler scheduler;
    private final DroppedItemClearer clearer;
    private MinecraftServer server;
    private ClearResult lastResult = ClearResult.empty();

    public ServerClearController() {
        this(new ClearScheduler(PalasItemClear.DEFAULT_CLEAR_INTERVAL_TICKS), new DroppedItemClearer());
    }

    ServerClearController(ClearScheduler scheduler, DroppedItemClearer clearer) {
        this.scheduler = scheduler;
        this.clearer = clearer;
    }

    public void start(MinecraftServer server) {
        if (server == null) {
            LOGGER.warn("Ignoring server start because no server instance was supplied");
            return;
        }

        this.server = server;
        scheduler.start();
        LOGGER.info("Item clearing scheduler started with intervalTicks={}", scheduler.intervalTicks());
    }

    public void tick() {
        MinecraftServer activeServer = server;
        if (activeServer == null || !activeServer.isRunning() || !scheduler.tick()) {
            return;
        }

        try {
            lastResult = clearer.clear(activeServer);
            LOGGER.info("Dropped item clear completed: removed={}, dimensions={}",
                    lastResult.totalRemoved(), lastResult.removedByDimension().size());
        } catch (RuntimeException exception) {
            LOGGER.error("Dropped item clear failed; the scheduler will continue", exception);
        }
    }

    public void stop() {
        if (server == null && scheduler.state() == ClearScheduler.State.STOPPED) {
            return;
        }

        server = null;
        scheduler.stop();
        LOGGER.info("Item clearing scheduler stopped");
    }

    public void pause() {
        scheduler.pause();
    }

    public void resume() {
        scheduler.resume();
    }

    public ClearResult lastResult() {
        return lastResult;
    }

    public ClearScheduler.State state() {
        return scheduler.state();
    }
}
