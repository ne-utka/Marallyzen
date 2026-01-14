package neutka.marallys.marallyzen.util;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import neutka.marallys.marallyzen.Marallyzen;

public final class NarrationIcons {
    private static final ResourceLocation ICON_FONT =
            ResourceLocation.fromNamespaceAndPath(Marallyzen.MODID, "narration_icons");
    private static final String RMB_GLYPH = "\ue900";

    private NarrationIcons() {
    }

    public static Component rmb() {
        return Component.literal(RMB_GLYPH).withStyle(style -> style.withFont(ICON_FONT));
    }
}
