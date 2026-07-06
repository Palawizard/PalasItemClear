package net.palasitemclear.fabric;

import net.palasitemclear.PalasItemClear;
import net.fabricmc.api.ModInitializer;

public final class PalasItemClearFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        PalasItemClear.init();
    }
}
