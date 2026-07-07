package net.palasitemclear.forge;

import net.palasitemclear.PalasItemClear;
import net.palasitemclear.server.ServerClearController;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(PalasItemClear.MOD_ID)
public final class PalasItemClearForge {
    private final ServerClearController controller = new ServerClearController();

    public PalasItemClearForge() {
        PalasItemClear.init();
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        controller.start(event.getServer());
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            controller.tick();
        }
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        controller.stop();
    }
}
