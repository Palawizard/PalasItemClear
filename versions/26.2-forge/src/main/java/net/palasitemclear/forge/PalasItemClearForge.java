package net.palasitemclear.forge;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.fml.common.Mod;
import net.palasitemclear.PalasItemClear;
import net.palasitemclear.command.PalasItemClearCommands;
import net.palasitemclear.server.ServerClearController;

@Mod(PalasItemClear.MOD_ID)
public final class PalasItemClearForge {
    private final ServerClearController controller = new ServerClearController();

    public PalasItemClearForge() {
        PalasItemClear.init();
        ServerStartedEvent.BUS.addListener(this::onServerStarted);
        ServerStoppedEvent.BUS.addListener(this::onServerStopped);
        TickEvent.ServerTickEvent.Post.BUS.addListener(this::onServerTickPost);
        net.minecraftforge.event.RegisterCommandsEvent.BUS.addListener(PalasItemClearCommands::register);
    }

    private void onServerStarted(ServerStartedEvent event) {
        controller.start(event.getServer());
    }

    private void onServerTickPost(TickEvent.ServerTickEvent.Post event) {
        controller.tick();
    }

    private void onServerStopped(ServerStoppedEvent event) {
        controller.stop();
    }
}
