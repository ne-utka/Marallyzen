package neutka.marallys.marallyzen.denizen.text;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

public final class DenizenTextFormatter {
    private DenizenTextFormatter() {
    }

    public static Component format(String raw) {
        if (raw == null || raw.isEmpty()) {
            return Component.empty();
        }
        raw = normalizeColorCodes(raw);
        MutableComponent root = Component.empty();
        Style style = Style.EMPTY;
        int index = 0;
        while (index < raw.length()) {
            int start = raw.indexOf("<&", index);
            if (start == -1) {
                appendStyled(root, raw.substring(index), style);
                break;
            }
            if (start > index) {
                appendStyled(root, raw.substring(index, start), style);
            }
            int end = raw.indexOf('>', start);
            if (end == -1) {
                appendStyled(root, raw.substring(start), style);
                break;
            }
            String token = raw.substring(start + 2, end);
            style = applyToken(style, token);
            index = end + 1;
        }
        return root;
    }

    private static void appendStyled(MutableComponent root, String text, Style style) {
        if (text.isEmpty()) {
            return;
        }
        root.append(Component.literal(text).setStyle(style));
    }

    private static Style applyToken(Style style, String token) {
        if (token == null || token.isEmpty()) {
            return style;
        }
        if (token.startsWith("color[#") && token.endsWith("]")) {
            String hex = token.substring("color[#".length(), token.length() - 1);
            try {
                int rgb = Integer.parseInt(hex, 16);
                return style.withColor(TextColor.fromRgb(rgb));
            }
            catch (NumberFormatException ignored) {
                return style;
            }
        }
        if (token.length() == 1) {
            ChatFormatting formatting = colorFromCode(token.charAt(0));
            if (formatting != null) {
                return style.applyFormat(formatting);
            }
        }
        return style;
    }

    private static String normalizeColorCodes(String raw) {
        StringBuilder out = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char current = raw.charAt(i);
            if (current == '&' && i + 1 < raw.length()) {
                if (i > 0 && raw.charAt(i - 1) == '<') {
                    out.append(current);
                    continue;
                }
                char code = raw.charAt(i + 1);
                if (isFormatCode(code)) {
                    out.append("<&").append(code).append('>');
                    i++;
                    continue;
                }
            }
            out.append(current);
        }
        return out.toString();
    }

    private static boolean isFormatCode(char code) {
        return switch (Character.toLowerCase(code)) {
            case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'a', 'b', 'c', 'd', 'e', 'f', 'k', 'l', 'm', 'n', 'o', 'r' -> true;
            default -> false;
        };
    }

    private static ChatFormatting colorFromCode(char code) {
        return switch (Character.toLowerCase(code)) {
            case '0' -> ChatFormatting.BLACK;
            case '1' -> ChatFormatting.DARK_BLUE;
            case '2' -> ChatFormatting.DARK_GREEN;
            case '3' -> ChatFormatting.DARK_AQUA;
            case '4' -> ChatFormatting.DARK_RED;
            case '5' -> ChatFormatting.DARK_PURPLE;
            case '6' -> ChatFormatting.GOLD;
            case '7' -> ChatFormatting.GRAY;
            case '8' -> ChatFormatting.DARK_GRAY;
            case '9' -> ChatFormatting.BLUE;
            case 'a' -> ChatFormatting.GREEN;
            case 'b' -> ChatFormatting.AQUA;
            case 'c' -> ChatFormatting.RED;
            case 'd' -> ChatFormatting.LIGHT_PURPLE;
            case 'e' -> ChatFormatting.YELLOW;
            case 'f' -> ChatFormatting.WHITE;
            case 'r' -> ChatFormatting.RESET;
            default -> null;
        };
    }
}
