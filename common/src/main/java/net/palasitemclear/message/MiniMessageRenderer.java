package net.palasitemclear.message;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

public final class MiniMessageRenderer {
    private static final Map<String, ChatFormatting> NAMED_COLORS = Map.ofEntries(
            Map.entry("black", ChatFormatting.BLACK),
            Map.entry("dark_blue", ChatFormatting.DARK_BLUE),
            Map.entry("dark_green", ChatFormatting.DARK_GREEN),
            Map.entry("dark_aqua", ChatFormatting.DARK_AQUA),
            Map.entry("dark_red", ChatFormatting.DARK_RED),
            Map.entry("dark_purple", ChatFormatting.DARK_PURPLE),
            Map.entry("gold", ChatFormatting.GOLD),
            Map.entry("gray", ChatFormatting.GRAY),
            Map.entry("grey", ChatFormatting.GRAY),
            Map.entry("dark_gray", ChatFormatting.DARK_GRAY),
            Map.entry("dark_grey", ChatFormatting.DARK_GRAY),
            Map.entry("blue", ChatFormatting.BLUE),
            Map.entry("green", ChatFormatting.GREEN),
            Map.entry("aqua", ChatFormatting.AQUA),
            Map.entry("red", ChatFormatting.RED),
            Map.entry("light_purple", ChatFormatting.LIGHT_PURPLE),
            Map.entry("yellow", ChatFormatting.YELLOW),
            Map.entry("white", ChatFormatting.WHITE)
    );

    private MiniMessageRenderer() {
    }

    public static Component render(String template, Map<String, String> placeholders) {
        String resolved = applyPlaceholders(template, placeholders);
        MutableComponent root = Component.empty();
        parseNodes(resolved, Style.EMPTY, root);
        return root;
    }

    private static String applyPlaceholders(String template, Map<String, String> placeholders) {
        String resolved = template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            resolved = resolved.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        return resolved;
    }

    private static void parseNodes(String input, Style activeStyle, MutableComponent target) {
        int index = 0;

        while (index < input.length()) {
            int tagStart = input.indexOf('<', index);
            if (tagStart < 0) {
                appendText(target, input.substring(index), activeStyle);
                return;
            }

            if (tagStart > index) {
                appendText(target, input.substring(index, tagStart), activeStyle);
            }

            int tagEnd = input.indexOf('>', tagStart);
            if (tagEnd < 0) {
                appendText(target, input.substring(tagStart), activeStyle);
                return;
            }

            String tagContent = input.substring(tagStart + 1, tagEnd).trim();
            index = tagEnd + 1;

            if (tagContent.startsWith("/")) {
                continue;
            }

            Style nextStyle = styleForTag(tagContent, activeStyle);
            int closingStart = input.indexOf("</" + tagContent + ">", index);
            if (closingStart < 0) {
                parseNodes(input.substring(index), nextStyle, target);
                return;
            }

            MutableComponent nested = Component.empty();
            parseNodes(input.substring(index, closingStart), nextStyle, nested);
            target.append(nested);
            index = closingStart + tagContent.length() + 3;
        }
    }

    private static Style styleForTag(String tagContent, Style activeStyle) {
        String normalized = tagContent.toLowerCase(Locale.ROOT);

        if (normalized.equals("bold")) {
            return activeStyle.withBold(true);
        }

        if (normalized.equals("italic")) {
            return activeStyle.withItalic(true);
        }

        if (normalized.equals("underlined")) {
            return activeStyle.withUnderlined(true);
        }

        if (normalized.equals("strikethrough")) {
            return activeStyle.withStrikethrough(true);
        }

        if (normalized.equals("obfuscated")) {
            return activeStyle.withObfuscated(true);
        }

        if (normalized.equals("reset")) {
            return Style.EMPTY;
        }

        ChatFormatting formatting = NAMED_COLORS.get(normalized);
        if (formatting != null) {
            TextColor color = TextColor.fromLegacyFormat(formatting);
            return activeStyle.withColor(color);
        }

        if (normalized.startsWith("#") && (normalized.length() == 7 || normalized.length() == 4)) {
            try {
                return activeStyle.withColor(TextColor.parseColor(normalized));
            } catch (NumberFormatException ignored) {
                return activeStyle;
            }
        }

        return activeStyle;
    }

    private static void appendText(MutableComponent target, String text, Style style) {
        if (text.isEmpty()) {
            return;
        }

        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (character == '\n') {
                parts.add(current.toString());
                current.setLength(0);
                continue;
            }

            current.append(character);
        }

        parts.add(current.toString());

        for (int index = 0; index < parts.size(); index++) {
            String part = parts.get(index);
            if (!part.isEmpty()) {
                target.append(Component.literal(part).setStyle(style));
            }

            if (index < parts.size() - 1) {
                target.append(Component.literal("\n").setStyle(style));
            }
        }
    }
}
