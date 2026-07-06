package net.palasitemclear.fabric;

import net.palasitemclear.PalasItemClear;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.palasitemclear.server.ServerClearController;

public final class PalasItemClearFabric implements ModInitializer {
    private final ServerClearController controller = new ServerClearController();

    @Override
    public void onInitialize() {
        PalasItemClear.init();
        ServerLifecycleEvents.SERVER_STARTED.register(controller::start);
        ServerTickEvents.END_SERVER_TICK.register(server -> controller.tick());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> controller.stop());
    }
}
