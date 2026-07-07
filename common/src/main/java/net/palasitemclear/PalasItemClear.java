package net.palasitemclear;

import net.palasitemclear.command.PalasItemClearCommands;

public final class PalasItemClear {
    public static final String MOD_ID = "palasitemclear";
    public static final long DEFAULT_CLEAR_INTERVAL_TICKS = 5 * 60 * 20;

    private PalasItemClear() {
    }

    public static void init() {
        PalasItemClearCommands.register();
    }
}
