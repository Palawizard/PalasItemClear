package net.palasitemclear.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.palasitemclear.PalasItemClear;
import net.palasitemclear.command.PalasItemClearCommands;
import net.palasitemclear.server.ServerClearController;

@Mod(PalasItemClear.MOD_ID)
public final class PalasItemClearNeoForge {
    private final ServerClearController controller = new ServerClearController();

    public PalasItemClearNeoForge(IEventBus modBus) {
        PalasItemClear.init();
        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
        NeoForge.EVENT_BUS.addListener(this::onServerTick);
        NeoForge.EVENT_BUS.addListener(this::onServerStopped);
        NeoForge.EVENT_BUS.addListener(PalasItemClearCommands::register);
    }

    private void onServerStarted(ServerStartedEvent event) {
        controller.start(event.getServer());
    }

    private void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            controller.tick();
        }
    }

    private void onServerStopped(ServerStoppedEvent event) {
        controller.stop();
    }
}
