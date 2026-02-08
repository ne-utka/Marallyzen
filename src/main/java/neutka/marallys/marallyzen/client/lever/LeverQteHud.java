package neutka.marallys.marallyzen.client.lever;

import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import neutka.marallys.marallyzen.Marallyzen;

public final class LeverQteHud {
    private static final Identifier LMB_ICON_0 =
        Identifier.fromNamespaceAndPath(Marallyzen.MODID, "textures/icons/lmb_interact_0.png");
    private static final Identifier LMB_ICON_1 =
        Identifier.fromNamespaceAndPath(Marallyzen.MODID, "textures/icons/lmb_interact_1.png");
    private static final Identifier RMB_ICON_0 =
        Identifier.fromNamespaceAndPath(Marallyzen.MODID, "textures/icons/rmb_interact_0.png");
    private static final Identifier RMB_ICON_1 =
        Identifier.fromNamespaceAndPath(Marallyzen.MODID, "textures/icons/rmb_interact_1.png");

    private static final int ICON_DRAW_SIZE = 48;
    private static final int EDGE_MARGIN = 24;
    private static final int ICON_ANIM_MS = 140;

    private LeverQteHud() {
    }

    public static void render(GuiGraphics guiGraphics, float partialTick) {
        if (!LeverQteClient.isHudVisible()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();

        int baseY = (height / 2) - (ICON_DRAW_SIZE / 2);
        float centerX = (width / 2.0f) - (ICON_DRAW_SIZE / 2.0f);
        float leftX = EDGE_MARGIN;
        float rightX = width - EDGE_MARGIN - ICON_DRAW_SIZE;
        float moveFactor = 0.20f;
        leftX = leftX + (centerX - leftX) * moveFactor;
        rightX = rightX - (rightX - centerX) * moveFactor;

        boolean expectRight = LeverQteClient.isExpectRight();
        float pulse = 0.06f * (1.0f + Mth.sin((Util.getMillis() / 140.0f))) * 0.5f;
        float leftScale = expectRight ? 1.0f : (1.0f + pulse);
        float rightScale = expectRight ? (1.0f + pulse) : 1.0f;

        int failTicks = LeverQteClient.getFailFlashTicks();
        int failedButton = LeverQteClient.getFailedButton();
        int successTicks = LeverQteClient.getSuccessFlashTicks();
        float failAlpha = failTicks > 0 ? Mth.clamp(failTicks / 10.0f, 0.0f, 1.0f) : 0.0f;
        float successAlpha = successTicks > 0 ? Mth.clamp(successTicks / 8.0f, 0.0f, 1.0f) : 0.0f;

        float leftShake = 0.0f;
        float rightShake = 0.0f;
        if (failTicks > 0 && failedButton >= 0) {
            float shake = Mth.sin((Util.getMillis() / 60.0f)) * 1.2f;
            if (failedButton == 0) {
                leftShake = shake;
            } else if (failedButton == 1) {
                rightShake = shake;
            }
        }

        float leftFail = failedButton == 0 ? failAlpha : 0.0f;
        float rightFail = failedButton == 1 ? failAlpha : 0.0f;
        boolean leftAnim = !expectRight && leftFail <= 0.0f;
        boolean rightAnim = expectRight && rightFail <= 0.0f;
        Identifier leftIcon = pickIcon(false, leftAnim);
        Identifier rightIcon = pickIcon(true, rightAnim);
        renderIcon(guiGraphics, leftIcon, leftX + leftShake, baseY, leftScale, leftFail, expectRight ? 0.0f : 1.0f);
        renderIcon(guiGraphics, rightIcon, rightX + rightShake, baseY, rightScale, rightFail, expectRight ? 1.0f : 0.0f);
    }

    private static void renderIcon(GuiGraphics guiGraphics, Identifier texture, float x, float y,
                                   float scale, float failAlpha, float active) {
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(x + ICON_DRAW_SIZE * 0.5f, y + ICON_DRAW_SIZE * 0.5f);
        guiGraphics.pose().scale(scale, scale);
        guiGraphics.pose().translate(-ICON_DRAW_SIZE * 0.5f, -ICON_DRAW_SIZE * 0.5f);

        float r = 1.0f;
        float g = 1.0f;
        float b = 1.0f;
        boolean tinted = false;
        if (failAlpha > 0.0f) {
            // Red on fail.
            r = 1.0f;
            g = 0.15f;
            b = 0.15f;
            tinted = true;
        } else if (active > 0.0f) {
            // Boosted orange for active prompt.
            r = 1.0f;
            g = 0.45f;
            b = 0.05f;
            tinted = true;
        }

        int color = ARGB.color(255, (int) (r * 255.0f), (int) (g * 255.0f), (int) (b * 255.0f));
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, 0, 0, 0.0f, 0.0f, ICON_DRAW_SIZE, ICON_DRAW_SIZE, ICON_DRAW_SIZE, ICON_DRAW_SIZE, color);
        guiGraphics.pose().popMatrix();
    }

    private static Identifier pickIcon(boolean right, boolean animate) {
        int frame = animate ? (int) ((Util.getMillis() / (long) ICON_ANIM_MS) % 2L) : 0;
        if (right) {
            return frame == 0 ? RMB_ICON_0 : RMB_ICON_1;
        }
        return frame == 0 ? LMB_ICON_0 : LMB_ICON_1;
    }
}


