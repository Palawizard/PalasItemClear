package net.palasitemclear.message;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

final class MiniMessageRendererTest {
    @Test
    void rendersNamedColorsAndPlaceholders() {
        Component component = MiniMessageRenderer.render(
                "<yellow>Items will clear in <gold>{seconds}</gold> seconds.",
                Map.of("seconds", "30")
        );

        assertTrue(component.getString().contains("Items will clear in 30 seconds."));
    }

    @Test
    void rendersNestedTags() {
        Component component = MiniMessageRenderer.render(
                "<gray>Cleared <white>{count}</white> dropped items.",
                Map.of("count", "12")
        );

        assertTrue(component.getString().contains("Cleared 12 dropped items."));
    }
}
