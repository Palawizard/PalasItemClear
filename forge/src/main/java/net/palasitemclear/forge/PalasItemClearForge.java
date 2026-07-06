package net.palasitemclear.forge;

import dev.architectury.platform.forge.EventBuses;
import net.palasitemclear.PalasItemClear;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(PalasItemClear.MOD_ID)
public final class PalasItemClearForge {
    public PalasItemClearForge() {
        EventBuses.registerModEventBus(PalasItemClear.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        PalasItemClear.init();
    }
}
