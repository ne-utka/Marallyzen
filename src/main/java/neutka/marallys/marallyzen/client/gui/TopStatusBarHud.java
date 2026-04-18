package neutka.marallys.marallyzen.client.gui;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import neutka.marallys.marallyzen.Marallyzen;

@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT)
public final class TopStatusBarHud {
    private static final int BASELINE_HEIGHT = 1080;
    private static final int BASE_BAR_VERTICAL_PADDING = 8;
    private static final int BASE_TEXT_GAP = 10;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int BAR_ALPHA = (int) (0.3f * 255.0f);
    private static final int BAR_COLOR = (BAR_ALPHA << 24) | 0x000000;

    private static final Identifier DEFAULT_ICON =
            Identifier.fromNamespaceAndPath("minecraft", "textures/item/amethyst_shard.png");

    private static volatile Identifier icon = DEFAULT_ICON;
    private static volatile float value = 3240.0f;
    private static volatile String text = "3.24K";
    private static volatile Identifier resolvedIconForSize = DEFAULT_ICON;
    private static volatile int iconTextureWidth = 16;
    private static volatile int iconTextureHeight = 16;

    private TopStatusBarHud() {
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.screen != null) {
            return;
        }
        if (mc.options == null || mc.options.hideGui) {
            return;
        }
        if (mc.font == null) {
            return;
        }

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        float scale = screenHeight / (float) BASELINE_HEIGHT;
        String renderText = (text == null || text.isBlank()) ? "3.24K" : text;
        int textHeight = mc.font.lineHeight;
        int textWidth = mc.font.width(renderText);
        int textGap = Math.round(BASE_TEXT_GAP * scale);

        ensureIconSizeResolved(mc, icon);
        int textureWidth = Math.max(1, iconTextureWidth);
        int textureHeight = Math.max(1, iconTextureHeight);
        int iconHeight = Math.max(1, textHeight);
        int iconWidth = Math.max(1, Math.round(iconHeight * (textureWidth / (float) textureHeight)));

        int verticalPadding = Math.max(1, Math.round(BASE_BAR_VERTICAL_PADDING * scale));
        int barHeight = iconHeight + verticalPadding * 2;
        int blockWidth = iconWidth + textGap + textWidth;
        int iconX = (screenWidth - blockWidth) / 2;
        int iconY = (barHeight - iconHeight) / 2;

        event.getGuiGraphics().fill(0, 0, screenWidth, barHeight, BAR_COLOR);
        event.getGuiGraphics().blit(RenderPipelines.GUI_TEXTURED, icon, iconX, iconY,
                0, 0, iconWidth, iconHeight, textureWidth, textureHeight, textureWidth, textureHeight, 0xFFFFFFFF);

        int textX = iconX + iconWidth + textGap;
        int textY = (barHeight - textHeight) / 2;
        event.getGuiGraphics().drawString(mc.font, renderText, textX, textY, TEXT_COLOR, true);
    }

    public static void setValue(float newValue) {
        value = newValue;
        text = formatCompact(newValue);
    }

    public static void setIcon(Identifier texture) {
        icon = texture != null ? texture : DEFAULT_ICON;
        resolvedIconForSize = null;
        iconTextureWidth = 16;
        iconTextureHeight = 16;
    }

    public static void setText(String newText) {
        if (newText == null || newText.isBlank()) {
            text = formatCompact(value);
            return;
        }
        text = newText;
    }

    private static String formatCompact(float number) {
        float abs = Math.abs(number);
        if (abs >= 1000.0f) {
            float compact = number / 1000.0f;
            float rounded = Mth.floor(compact * 100.0f + 0.5f) / 100.0f;
            return rounded + "K";
        }

        if (number == (int) number) {
            return Integer.toString((int) number);
        }
        float rounded = Mth.floor(number * 100.0f + 0.5f) / 100.0f;
        return Float.toString(rounded);
    }

    private static void ensureIconSizeResolved(Minecraft mc, Identifier texture) {
        if (mc == null || texture == null || mc.getResourceManager() == null) {
            return;
        }
        if (texture.equals(resolvedIconForSize)) {
            return;
        }
        synchronized (TopStatusBarHud.class) {
            if (texture.equals(resolvedIconForSize)) {
                return;
            }

            int width = 16;
            int height = 16;
            Resource resource = mc.getResourceManager().getResource(texture).orElse(null);
            if (resource != null) {
                try (var stream = resource.open(); NativeImage image = NativeImage.read(stream)) {
                    if (image.getWidth() > 0 && image.getHeight() > 0) {
                        width = image.getWidth();
                        height = image.getHeight();
                    }
                } catch (Exception ignored) {
                }
            }

            iconTextureWidth = width;
            iconTextureHeight = height;
            resolvedIconForSize = texture;
        }
    }
}
