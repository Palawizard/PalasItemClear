package net.palasitemclear.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class ClearAdminServiceTest {
    @Test
    void helpListsPrimaryCommands() {
        String help = ClearAdminService.help();

        assertTrue(help.contains("/palasitemclear status"));
        assertTrue(help.contains("/palasitemclear bin"));
        assertTrue(help.contains("/palasitemclear set interval"));
    }

    @Test
    void reportsWhenServiceIsUnavailable() {
        assertEquals(ClearAdminService.NOT_RUNNING, ClearAdminService.status(null));
        assertEquals(ClearAdminService.NOT_RUNNING, ClearAdminService.nextClear(null));
    }

    @Test
    void convertsTicksToRoundedUpSeconds() {
        assertEquals(0L, ClearAdminService.ticksToSeconds(0L));
        assertEquals(1L, ClearAdminService.ticksToSeconds(1L));
        assertEquals(1L, ClearAdminService.ticksToSeconds(20L));
        assertEquals(2L, ClearAdminService.ticksToSeconds(21L));
    }
}
