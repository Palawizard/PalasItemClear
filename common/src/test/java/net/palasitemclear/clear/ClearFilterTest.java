package net.palasitemclear.clear;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class ClearFilterTest {
    @Test
    void recognizesEntityAndStackCustomNames() {
        assertTrue(ClearFilter.isNamed(true, false));
        assertTrue(ClearFilter.isNamed(false, true));
        assertFalse(ClearFilter.isNamed(false, false));
    }
}
