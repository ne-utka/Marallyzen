package neutka.marallys.marallyzen.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import neutka.marallys.marallyzen.Marallyzen;

public final class NarrationIcons {
    private static final FontDescription ICON_FONT =
            new FontDescription.Resource(Identifier.fromNamespaceAndPath(Marallyzen.MODID, "narration_icons"));
    private static final String RMB_GLYPH_1 = "\ue900";
    private static final String RMB_GLYPH_2 = "\ue901";
    private static final String LMB_GLYPH_1 = "\ue902";
    private static final String LMB_GLYPH_2 = "\ue903";
    private static final long ANIMATION_PERIOD_MS = 500L;

    private NarrationIcons() {
    }

    public static Component rmb() {
        return Component.literal(currentRmbGlyph()).withStyle(style -> style.withFont(ICON_FONT));
    }

    public static Component lmb() {
        return Component.literal(currentLmbGlyph()).withStyle(style -> style.withFont(ICON_FONT));
    }

    private static String currentRmbGlyph() {
        long frame = (System.currentTimeMillis() / ANIMATION_PERIOD_MS) & 1L;
        return frame == 0L ? RMB_GLYPH_1 : RMB_GLYPH_2;
    }

    private static String currentLmbGlyph() {
        long frame = (System.currentTimeMillis() / ANIMATION_PERIOD_MS) & 1L;
        return frame == 0L ? LMB_GLYPH_1 : LMB_GLYPH_2;
    }
}


