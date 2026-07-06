package net.palasitemclear.server;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.palasitemclear.PalasItemClear;
import net.palasitemclear.PlatformPaths;
import net.palasitemclear.clear.ClearFilter;
import net.palasitemclear.clear.ClearResult;
import net.palasitemclear.clear.DroppedItemClearer;
import net.palasitemclear.config.ConfigHolder;
import net.palasitemclear.config.ConfigLoader;
import net.palasitemclear.config.ModConfig;
import net.palasitemclear.message.MiniMessageRenderer;
import net.palasitemclear.scheduler.ClearScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Owns the scheduler for one dedicated or integrated server instance.
 */
public final class ServerClearController {
    private static final Logger LOGGER = LoggerFactory.getLogger(PalasItemClear.MOD_ID);

    private final ConfigHolder configHolder;
    private final ConfigLoader configLoader;
    private final DroppedItemClearer clearer;
    private final Set<Long> firedWarnings = new HashSet<>();
    private ClearScheduler scheduler;
    private MinecraftServer server;
    private ClearResult lastResult = ClearResult.empty();

    public ServerClearController() {
        this(new ConfigHolder(), new DroppedItemClearer());
    }

    ServerClearController(ConfigHolder configHolder, DroppedItemClearer clearer) {
        this.configHolder = configHolder;
        this.configLoader = new ConfigLoader(configHolder);
        this.clearer = clearer;
        this.scheduler = new ClearScheduler(ModConfig.defaults().intervalTicks());
    }

    public void start(MinecraftServer server) {
        if (server == null) {
            LOGGER.warn("Ignoring server start because no server instance was supplied");
            return;
        }

        this.server = server;
        ModConfig config = configLoader.loadOrCreate(PlatformPaths.getConfigDirectory());
        applySchedule(config);
        firedWarnings.clear();
        scheduler.start();
        LOGGER.info("Item clearing scheduler started with intervalTicks={}", scheduler.intervalTicks());
    }

    public void tick() {
        MinecraftServer activeServer = server;
        if (activeServer == null || !activeServer.isRunning()) {
            return;
        }

        if (scheduler.state() == ClearScheduler.State.RUNNING) {
            checkWarnings(activeServer, scheduler.remainingTicks());
        }

        if (!scheduler.tick()) {
            return;
        }

        firedWarnings.clear();

        try {
            ModConfig config = configHolder.get();
            ClearFilter filter = new ClearFilter(config.filters());
            lastResult = clearer.clear(activeServer, filter);
            broadcastClearMessage(activeServer, config, lastResult);
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
        firedWarnings.clear();
        scheduler.stop();
        LOGGER.info("Item clearing scheduler stopped");
    }

    public void pause() {
        scheduler.pause();
    }

    public void resume() {
        scheduler.resume();
    }

    public void reloadConfiguration() {
        if (server == null) {
            return;
        }

        ModConfig config = configLoader.reload(PlatformPaths.getConfigDirectory());
        applySchedule(config);
        firedWarnings.clear();
        LOGGER.info("Configuration reloaded");
    }

    public ModConfig configuration() {
        return configHolder.get();
    }

    public ClearResult lastResult() {
        return lastResult;
    }

    public ClearScheduler.State state() {
        return scheduler.state();
    }

    public long remainingTicks() {
        return scheduler.remainingTicks();
    }

    private void applySchedule(ModConfig config) {
        scheduler = new ClearScheduler(config.intervalTicks());
    }

    private void checkWarnings(MinecraftServer activeServer, long remainingTicks) {
        ModConfig config = configHolder.get();

        for (long warningTicks : config.warningTicksDescending()) {
            if (remainingTicks != warningTicks || !firedWarnings.add(warningTicks)) {
                continue;
            }

            int seconds = (int) (warningTicks / 20L);
            Component message = MiniMessageRenderer.render(
                    config.messages().warning(),
                    Map.of("seconds", Integer.toString(seconds))
            );
            activeServer.getPlayerList().broadcastSystemMessage(message, false);
            LOGGER.info("Broadcast clear warning for {} seconds remaining", seconds);
        }
    }

    private void broadcastClearMessage(MinecraftServer activeServer, ModConfig config, ClearResult result) {
        if (result.totalRemoved() <= 0) {
            return;
        }

        Component message = MiniMessageRenderer.render(
                config.messages().cleared(),
                Map.of("count", Integer.toString(result.totalRemoved()))
        );
        activeServer.getPlayerList().broadcastSystemMessage(message, false);
    }
}
