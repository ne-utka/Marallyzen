package neutka.marallys.marallyzen.util;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import neutka.marallys.marallyzen.Marallyzen;

public final class NarrationIcons {
    private static final ResourceLocation ICON_FONT =
            ResourceLocation.fromNamespaceAndPath(Marallyzen.MODID, "narration_icons");
    private static final String RMB_GLYPH_1 = "\ue900";
    private static final String RMB_GLYPH_2 = "\ue901";
    private static final long RMB_ANIMATION_PERIOD_MS = 500L;

    private NarrationIcons() {
    }

    public static Component rmb() {
        return Component.literal(currentRmbGlyph()).withStyle(style -> style.withFont(ICON_FONT));
    }

    private static String currentRmbGlyph() {
        long frame = (System.currentTimeMillis() / RMB_ANIMATION_PERIOD_MS) & 1L;
        return frame == 0L ? RMB_GLYPH_1 : RMB_GLYPH_2;
    }
}
