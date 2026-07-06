package net.palasitemclear.server;

import net.minecraft.server.MinecraftServer;
import net.palasitemclear.PalasItemClear;
import net.palasitemclear.clear.ClearResult;
import net.palasitemclear.clear.DroppedItemClearer;
import net.palasitemclear.scheduler.ClearScheduler;

/**
 * Owns the scheduler for one dedicated or integrated server instance.
 */
public final class ServerClearController {
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
        this.server = server;
        scheduler.reset();
    }

    public void tick() {
        if (server != null && scheduler.tick()) {
            lastResult = clearer.clear(server);
        }
    }

    public void stop() {
        server = null;
        scheduler.reset();
    }

    public ClearResult lastResult() {
        return lastResult;
    }
}
