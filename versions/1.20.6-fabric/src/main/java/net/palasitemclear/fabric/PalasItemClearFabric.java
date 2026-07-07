package net.palasitemclear.fabric;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.palasitemclear.PalasItemClear;
import net.palasitemclear.command.PalasItemClearCommands;
import net.palasitemclear.server.ServerClearController;

public final class PalasItemClearFabric implements DedicatedServerModInitializer {
    private final ServerClearController controller = new ServerClearController();

    @Override
    public void onInitializeServer() {
        PalasItemClear.init();
        CommandRegistrationCallback.EVENT.register(PalasItemClearCommands::register);
        ServerLifecycleEvents.SERVER_STARTED.register(controller::start);
        ServerTickEvents.END_SERVER_TICK.register(server -> controller.tick());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> controller.stop());
    }
}
