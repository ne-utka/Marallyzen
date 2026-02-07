package neutka.marallys.marallyzen.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import neutka.marallys.marallyzen.Marallyzen;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.joml.Matrix4f;

import neutka.marallys.marallyzen.client.NoDepthTextRenderType;
import neutka.marallys.marallyzen.client.DecoratedPotCarryClient;
import neutka.marallys.marallyzen.client.gui.PromptAnchorUtil;
import neutka.marallys.marallyzen.entity.DecoratedPotCarryEntity;
import neutka.marallys.marallyzen.util.NarrationIcons;

public class DecoratedPotPromptHud {
    private static DecoratedPotPromptHud instance;

    private static final int FADE_IN_DURATION_TICKS = 3;
    private static final int FADE_OUT_DURATION_TICKS = 2;
    private static final float VERTICAL_OFFSET_MAX = 0.1f;
    private static final float SCALE_BASE = 0.0171f;
    private static final float OFFSET_RIGHT = 0.7f;
    private static final int TEXT_DARK_GRAY = 0x555555;
    private static final int NARRATION_BG_COLOR = 0x000000;
    private static final float BACKGROUND_PADDING_X = 3.42f;
    private static final float BACKGROUND_PADDING_Y = 2.855f;
    private static final float BACKGROUND_CORNER_RADIUS = 2.0f;
    private static final float ROUNDED_BG_TEX_SIZE = 64.0f;
    private static final float ROUNDED_BG_CORNER_PX = 10.0f;
    private static final Identifier ROUNDED_BG_TEXTURE =
        Identifier.fromNamespaceAndPath(Marallyzen.MODID, "textures/gui/rounded_prompt_bg.png");

    private boolean targetVisible = false;
    private DecoratedPotCarryEntity targetEntity;

    private float fadeInProgress = 0.0f;
    private float fadeOutProgress = 1.0f;
    private float previousFadeInProgress = 0.0f;
    private float previousFadeOutProgress = 1.0f;

    public static DecoratedPotPromptHud getInstance() {
        if (instance == null) {
            instance = new DecoratedPotPromptHud();
        }
        return instance;
    }

    public void tick() {
        updateTarget();

        previousFadeInProgress = fadeInProgress;
        previousFadeOutProgress = fadeOutProgress;

        if (targetVisible) {
            if (fadeInProgress < 1.0f) {
                fadeInProgress = Mth.clamp(fadeInProgress + (1.0f / FADE_IN_DURATION_TICKS), 0.0f, 1.0f);
            }
            fadeOutProgress = 1.0f;
        } else {
            if (fadeOutProgress > 0.0f) {
                fadeOutProgress = Mth.clamp(fadeOutProgress - (1.0f / FADE_OUT_DURATION_TICKS), 0.0f, 1.0f);
            }
            if (fadeOutProgress <= 0.0f) {
                fadeInProgress = 0.0f;
            }
        }
    }

    public void renderInWorld(PoseStack poseStack, Camera camera, float partialTick) {
        if (targetEntity == null) {
            return;
        }
        if (!targetVisible && fadeOutProgress <= 0.0f) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        float interpolatedFadeIn = Mth.lerp(partialTick, previousFadeInProgress, fadeInProgress);
        float interpolatedFadeOut = Mth.lerp(partialTick, previousFadeOutProgress, fadeOutProgress);
        boolean showing = targetVisible;
        float appearT = showing ? interpolatedFadeIn : interpolatedFadeOut;
        float appear = showing
            ? easeOutCubic(appearT)
            : 1.0f - easeInCubic(1.0f - appearT);
        appear = Mth.clamp(appear, 0.0f, 1.0f);

        double entityX = targetEntity.getX() + 0.5;
        double entityY = PromptAnchorUtil.entityTopY(targetEntity)
            - PromptAnchorUtil.pxToWorld(20.0f, SCALE_BASE);
        double entityZ = targetEntity.getZ() + 0.5;

        Vec3 camPos = camera.position();
        double camX = camPos.x;
        double camY = camPos.y;
        double camZ = camPos.z;

        double dx = entityX - camX;
        double dy = entityY - camY;
        double dz = entityZ - camZ;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance > 32.0) {
            return;
        }

        poseStack.pushPose();

        double dirX = camX - entityX;
        double dirZ = camZ - entityZ;
        double dirDistance = Math.sqrt(dirX * dirX + dirZ * dirZ);

        double offsetX = entityX - camX;
        double offsetY = entityY - camY;
        double offsetZ = entityZ - camZ;

        offsetY -= VERTICAL_OFFSET_MAX * (1.0f - appear);

        if (dirDistance > 0.001) {
            dirX /= dirDistance;
            dirZ /= dirDistance;
            double rightX = dirZ;
            double rightZ = -dirX;
            offsetX += rightX * OFFSET_RIGHT;
            offsetZ += rightZ * OFFSET_RIGHT;
        }

        poseStack.translate(offsetX, offsetY, offsetZ);

        float cameraYaw = camera.yRot();
        float cameraPitch = camera.xRot();
        poseStack.mulPose(Axis.YP.rotationDegrees(-cameraYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(cameraPitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0f));

        float scale = SCALE_BASE;
        if (showing) {
            float back = easeOutBack(appear);
            scale = SCALE_BASE * (0.92f + 0.08f * back);
        } else {
            scale = SCALE_BASE * appear;
        }
        scale = Math.max(0.001f, scale);
        poseStack.scale(scale, scale, scale);

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        MultiBufferSource textSource = bufferSource;
        Font font = mc.font;
        Matrix4f matrix = poseStack.last().pose();

        int alpha = (int) (appear * 255);
        int white = (alpha << 24) | 0xFFFFFF;
        int darkGray = (alpha << 24) | TEXT_DARK_GRAY;

        drawPrompt(font, bufferSource, textSource, matrix, "\u0412\u0437\u0430\u0438\u043c\u043e\u0434\u0435\u0439\u0441\u0442\u0432\u043e\u0432\u0430\u0442\u044c", white, darkGray, alpha);
        bufferSource.endBatch();
        poseStack.popPose();
    }

    private void drawPrompt(Font font, MultiBufferSource.BufferSource bufferSource, MultiBufferSource textSource,
                            Matrix4f matrix,
                            String label, int white, int darkGray, int alpha) {
        Component icon = NarrationIcons.rmb();
        String spacer = " ";
        String action = ">> ";
        float textY = -3.0f;

        float widthIcon = font.width(icon);
        float widthSpacer = font.width(spacer);
        float widthAction = font.width(action);
        float widthLabel = font.width(label);
        float totalWidth = widthIcon + widthSpacer + widthAction + widthLabel;
        float textHeight = font.lineHeight;
        float bgWidth = totalWidth + BACKGROUND_PADDING_X * 2.0f;
        float bgHeight = textHeight + BACKGROUND_PADDING_Y * 2.0f;
        float bgX = -BACKGROUND_PADDING_X;
        float bgY = (textY + textHeight / 2.0f) - bgHeight / 2.0f;
        int bgAlpha = (int) (alpha * 120);
        int bgColor = (bgAlpha << 24) | (NARRATION_BG_COLOR & 0xFFFFFF);
        fillRoundedRect(matrix, bufferSource, bgX, bgY, bgWidth, bgHeight, BACKGROUND_CORNER_RADIUS, bgColor);

        float cursorX = 0.0f;
        drawComponent(matrix, textSource, font, icon, cursorX, textY, white);
        cursorX += widthIcon;
        drawText(matrix, textSource, font, spacer, cursorX, textY, white);
        cursorX += widthSpacer;
        drawText(matrix, textSource, font, action, cursorX, textY, darkGray);
        cursorX += widthAction;
        drawText(matrix, textSource, font, label, cursorX, textY, white);
    }

    private void drawComponent(Matrix4f matrix, MultiBufferSource bufferSource, Font font,
                               Component text, float x, float y, int color) {
        font.drawInBatch(
            text,
            x,
            y,
            color,
            false,
            matrix,
            bufferSource,
            Font.DisplayMode.SEE_THROUGH,
            0,
            15728880
        );
    }

    private void drawText(Matrix4f matrix, MultiBufferSource bufferSource, Font font,
                          String text, float x, float y, int color) {
        font.drawInBatch(
            text,
            x,
            y,
            color,
            false,
            matrix,
            bufferSource,
            Font.DisplayMode.SEE_THROUGH,
            0,
            15728880
        );
    }

    private void fillRect(Matrix4f matrix, MultiBufferSource.BufferSource bufferSource,
                          float x, float y, float width, float height, int color) {
        com.mojang.blaze3d.vertex.VertexConsumer vertexConsumer = bufferSource.getBuffer(
            NoDepthTextRenderType.textNoDepth(
                net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", "textures/misc/white.png")
            )
        );

        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        float left = x;
        float top = y;
        float right = x + width;
        float bottom = y + height;
        float z = 0.01f;

        int light = net.minecraft.client.renderer.LightTexture.FULL_BRIGHT;
        int overlay = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
        int lightU = light & 0xFFFF;
        int lightV = (light >> 16) & 0xFFFF;
        int overlayU = overlay & 0xFFFF;
        int overlayV = (overlay >> 16) & 0xFFFF;

        float normalX = 0.0f;
        float normalY = 0.0f;
        float normalZ = -1.0f;

        vertexConsumer.addVertex(matrix, left, bottom, z)
            .setColor(r, g, b, a)
            .setUv(0.0f, 1.0f)
            .setUv1(overlayU, overlayV)
            .setUv2(lightU, lightV)
            .setNormal(normalX, normalY, normalZ);
        vertexConsumer.addVertex(matrix, right, bottom, z)
            .setColor(r, g, b, a)
            .setUv(1.0f, 1.0f)
            .setUv1(overlayU, overlayV)
            .setUv2(lightU, lightV)
            .setNormal(normalX, normalY, normalZ);
        vertexConsumer.addVertex(matrix, right, top, z)
            .setColor(r, g, b, a)
            .setUv(1.0f, 0.0f)
            .setUv1(overlayU, overlayV)
            .setUv2(lightU, lightV)
            .setNormal(normalX, normalY, normalZ);
        vertexConsumer.addVertex(matrix, left, top, z)
            .setColor(r, g, b, a)
            .setUv(0.0f, 0.0f)
            .setUv1(overlayU, overlayV)
            .setUv2(lightU, lightV)
            .setNormal(normalX, normalY, normalZ);
    }

    private void fillRoundedRect(Matrix4f matrix, MultiBufferSource.BufferSource bufferSource,
                                 float x, float y, float width, float height, float radius, int color) {
        if (radius <= 0.0f) {
            fillRect(matrix, bufferSource, x, y, width, height, color);
            return;
        }
        fillTexturedRect(matrix, bufferSource, x, y, width, height, color);
    }

    private void fillTexturedRect(Matrix4f matrix, MultiBufferSource.BufferSource bufferSource,
                                  float x, float y, float width, float height, int color) {
        float corner = Math.min(Math.max(BACKGROUND_CORNER_RADIUS, 0.0f), Math.min(width, height) / 2.0f);
        if (corner <= 0.0f) {
            fillRect(matrix, bufferSource, x, y, width, height, color);
            return;
        }

        com.mojang.blaze3d.vertex.VertexConsumer vertexConsumer = bufferSource.getBuffer(
            NoDepthTextRenderType.textNoDepth(ROUNDED_BG_TEXTURE)
        );

        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        int light = net.minecraft.client.renderer.LightTexture.FULL_BRIGHT;
        int overlay = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
        int lightU = light & 0xFFFF;
        int lightV = (light >> 16) & 0xFFFF;
        int overlayU = overlay & 0xFFFF;
        int overlayV = (overlay >> 16) & 0xFFFF;

        float z = 0.01f;
        float uCorner = ROUNDED_BG_CORNER_PX / ROUNDED_BG_TEX_SIZE;

        float x0 = x;
        float x1 = x + corner;
        float x2 = x + width - corner;
        float x3 = x + width;
        float y0 = y;
        float y1 = y + corner;
        float y2 = y + height - corner;
        float y3 = y + height;

        drawTexturedQuad(vertexConsumer, matrix, x0, y0, x1, y1, 0.0f, 0.0f, uCorner, uCorner, r, g, b, a, z, overlayU, overlayV, lightU, lightV);
        drawTexturedQuad(vertexConsumer, matrix, x1, y0, x2, y1, uCorner, 0.0f, 1.0f - uCorner, uCorner, r, g, b, a, z, overlayU, overlayV, lightU, lightV);
        drawTexturedQuad(vertexConsumer, matrix, x2, y0, x3, y1, 1.0f - uCorner, 0.0f, 1.0f, uCorner, r, g, b, a, z, overlayU, overlayV, lightU, lightV);

        drawTexturedQuad(vertexConsumer, matrix, x0, y1, x1, y2, 0.0f, uCorner, uCorner, 1.0f - uCorner, r, g, b, a, z, overlayU, overlayV, lightU, lightV);
        drawTexturedQuad(vertexConsumer, matrix, x1, y1, x2, y2, uCorner, uCorner, 1.0f - uCorner, 1.0f - uCorner, r, g, b, a, z, overlayU, overlayV, lightU, lightV);
        drawTexturedQuad(vertexConsumer, matrix, x2, y1, x3, y2, 1.0f - uCorner, uCorner, 1.0f, 1.0f - uCorner, r, g, b, a, z, overlayU, overlayV, lightU, lightV);

        drawTexturedQuad(vertexConsumer, matrix, x0, y2, x1, y3, 0.0f, 1.0f - uCorner, uCorner, 1.0f, r, g, b, a, z, overlayU, overlayV, lightU, lightV);
        drawTexturedQuad(vertexConsumer, matrix, x1, y2, x2, y3, uCorner, 1.0f - uCorner, 1.0f - uCorner, 1.0f, r, g, b, a, z, overlayU, overlayV, lightU, lightV);
        drawTexturedQuad(vertexConsumer, matrix, x2, y2, x3, y3, 1.0f - uCorner, 1.0f - uCorner, 1.0f, 1.0f, r, g, b, a, z, overlayU, overlayV, lightU, lightV);
    }

    private void drawTexturedQuad(com.mojang.blaze3d.vertex.VertexConsumer vertexConsumer, Matrix4f matrix,
                                  float left, float top, float right, float bottom,
                                  float u0, float v0, float u1, float v1,
                                  int r, int g, int b, int a, float z,
                                  int overlayU, int overlayV, int lightU, int lightV) {
        vertexConsumer.addVertex(matrix, left, bottom, z)
            .setColor(r, g, b, a)
            .setUv(u0, v1)
            .setUv1(overlayU, overlayV)
            .setUv2(lightU, lightV)
            .setNormal(0.0f, 0.0f, -1.0f);
        vertexConsumer.addVertex(matrix, right, bottom, z)
            .setColor(r, g, b, a)
            .setUv(u1, v1)
            .setUv1(overlayU, overlayV)
            .setUv2(lightU, lightV)
            .setNormal(0.0f, 0.0f, -1.0f);
        vertexConsumer.addVertex(matrix, right, top, z)
            .setColor(r, g, b, a)
            .setUv(u1, v0)
            .setUv1(overlayU, overlayV)
            .setUv2(lightU, lightV)
            .setNormal(0.0f, 0.0f, -1.0f);
        vertexConsumer.addVertex(matrix, left, top, z)
            .setColor(r, g, b, a)
            .setUv(u0, v0)
            .setUv1(overlayU, overlayV)
            .setUv2(lightU, lightV)
            .setNormal(0.0f, 0.0f, -1.0f);
    }

    private void updateTarget() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            targetVisible = false;
            return;
        }
        if (DecoratedPotCarryClient.isCarrying(mc)) {
            targetVisible = false;
            return;
        }
        HitResult hitResult = mc.hitResult;
        if (hitResult instanceof EntityHitResult entityHit) {
            if (entityHit.getEntity() instanceof DecoratedPotCarryEntity pot
                && pot.getMode() == DecoratedPotCarryEntity.Mode.RESTING) {
                targetVisible = true;
                targetEntity = pot;
                return;
            }
        }
        targetVisible = false;
    }

    private static float easeOutCubic(float t) {
        float inv = 1.0f - t;
        return 1.0f - inv * inv * inv;
    }

    private static float easeInCubic(float t) {
        return t * t * t;
    }

    private static float easeOutBack(float t) {
        float s = 1.70158f;
        t -= 1.0f;
        return t * t * ((s + 1.0f) * t + s) + 1.0f;
    }
}





