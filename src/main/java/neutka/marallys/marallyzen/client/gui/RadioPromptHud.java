package neutka.marallys.marallyzen.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import org.lwjgl.opengl.GL11;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import org.joml.Matrix4f;

import neutka.marallys.marallyzen.blocks.MarallyzenBlocks;
import neutka.marallys.marallyzen.client.ClientRadioManager;
import neutka.marallys.marallyzen.util.NarrationIcons;
import neutka.marallys.marallyzen.client.gui.PromptAnchorUtil;
import neutka.marallys.marallyzen.client.NoDepthTextRenderType;
import neutka.marallys.marallyzen.client.gui.NoDepthTextBufferSource;

public class RadioPromptHud {
    private static RadioPromptHud instance;

    private static final int FADE_IN_DURATION_TICKS = 3;
    private static final int FADE_OUT_DURATION_TICKS = 2;
    private static final float VERTICAL_OFFSET_MAX = 0.1f;
    private static final float SCALE_BASE = 0.0171f;
    private static final float OFFSET_RIGHT = 0.7f;
    private static final int PRIME_COLOR = 0xD48E03;
    private static final int TEXT_GRAY = 0xA0A0A0;
    private static final int NARRATION_TEXT_COLOR = 0x404040;
    private static final int NARRATION_BG_COLOR = 0x000000;
    private static final float BACKGROUND_PADDING_X = 3.42f;
    private static final float BACKGROUND_PADDING_Y = 1.855f;
    private static final int OPTION_HEIGHT_PIXELS = 13;
    private static final int OPTION_SPACING_PIXELS = 4;
    private static final float LINE_TEXT_OFFSET_Y = OPTION_HEIGHT_PIXELS / 2.0f - 3.0f;

    private boolean targetVisible = false;
    private BlockPos targetPos;

    private float fadeInProgress = 0.0f;
    private float fadeOutProgress = 1.0f;
    private float previousFadeInProgress = 0.0f;
    private float previousFadeOutProgress = 1.0f;

    public static RadioPromptHud getInstance() {
        if (instance == null) {
            instance = new RadioPromptHud();
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
        if (targetPos == null) {
            return;
        }
        if (!targetVisible && fadeOutProgress <= 0.0f) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        if (neutka.marallys.marallyzen.client.cutscene.ScreenFadeManager.getInstance().isActive()) {
            return;
        }
        if (neutka.marallys.marallyzen.client.cutscene.EyesCloseManager.getInstance().isActive()) {
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

        double blockX = targetPos.getX() + 0.5;
        BlockState state = mc.level.getBlockState(targetPos);
        double blockY = PromptAnchorUtil.blockTopY(mc.level, targetPos, state)
            - PromptAnchorUtil.pxToWorld(20.0f, SCALE_BASE);
        double blockZ = targetPos.getZ() + 0.5;

        double camX = camera.getPosition().x;
        double camY = camera.getPosition().y;
        double camZ = camera.getPosition().z;

        double dx = blockX - camX;
        double dy = blockY - camY;
        double dz = blockZ - camZ;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance > 32.0) {
            return;
        }

        poseStack.pushPose();

        double dirX = camX - blockX;
        double dirZ = camZ - blockZ;
        double dirDistance = Math.sqrt(dirX * dirX + dirZ * dirZ);

        double offsetX = blockX - camX;
        double offsetY = blockY - camY;
        double offsetZ = blockZ - camZ;

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

        float cameraYaw = camera.getYRot();
        float cameraPitch = camera.getXRot();
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
        MultiBufferSource textSource = new NoDepthTextBufferSource(bufferSource);
        Font font = mc.font;
        Matrix4f matrix = poseStack.last().pose();

        int alpha = (int) (appear * 255);
        int color = (alpha << 24) | 0xFFFFFF;
        int primeColor = (alpha << 24) | PRIME_COLOR;
        int narrationColor = (alpha << 24) | NARRATION_TEXT_COLOR;
        int grayColor = (alpha << 24) | TEXT_GRAY;

        boolean isPlaying = ClientRadioManager.isPlaying(targetPos);
        String toggleLabel = isPlaying ? "Выкл." : "Вкл.";

        float currentY = 0.0f;
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.depthFunc(GL11.GL_ALWAYS);
        if (isPlaying) {
            currentY -= (OPTION_HEIGHT_PIXELS + OPTION_SPACING_PIXELS);
            currentY = renderTitleLine(font, bufferSource, textSource, matrix, currentY, alpha, ClientRadioManager.getStationName(targetPos), primeColor);
            currentY = renderSwitchLine(font, bufferSource, textSource, matrix, currentY, alpha, grayColor, narrationColor, color, false);
        }
        renderToggleLine(font, bufferSource, textSource, matrix, currentY, alpha, color, narrationColor, toggleLabel, !isPlaying);
        bufferSource.endBatch();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        poseStack.popPose();
    }

    private void updateTarget() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            targetVisible = false;
            return;
        }
        HitResult hitResult = mc.hitResult;
        if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) {
            targetVisible = false;
            return;
        }
        BlockHitResult blockHit = (BlockHitResult) hitResult;
        BlockPos pos = blockHit.getBlockPos();
        BlockState state = mc.level.getBlockState(pos);
        Block block = state.getBlock();
        if (block == MarallyzenBlocks.RADIO.get()) {
            targetVisible = true;
            targetPos = pos;
        } else {
            targetVisible = false;
        }
    }

    private float easeOutCubic(float t) {
        return 1.0f - (float) Math.pow(1.0f - t, 3);
    }

    private float easeInCubic(float t) {
        return t * t * t;
    }

    private float easeOutBack(float t) {
        float c1 = 1.70158f;
        float c3 = c1 + 1.0f;
        return 1.0f + c3 * (float) Math.pow(t - 1, 3) + c1 * (float) Math.pow(t - 1, 2);
    }

    private float renderTitleLine(Font font, MultiBufferSource.BufferSource bufferSource, MultiBufferSource textSource,
                                  Matrix4f matrix,
                                  float currentY, int alpha, String title, int color) {
        float textWidth = font.width(title);
        float textHeight = font.lineHeight;
        float bgWidth = textWidth + BACKGROUND_PADDING_X * 2.0f;
        float bgHeight = textHeight + BACKGROUND_PADDING_Y * 2.0f;
        float bgX = -BACKGROUND_PADDING_X;
        float textY = currentY + LINE_TEXT_OFFSET_Y;
        float bgY = (currentY + LINE_TEXT_OFFSET_Y + textHeight / 2.0f) - bgHeight / 2.0f;
        int bgAlpha = (int) (alpha * 120);
        int bgColor = (bgAlpha << 24) | (NARRATION_BG_COLOR & 0xFFFFFF);
        fillRect(matrix, bufferSource, bgX, bgY, bgWidth, bgHeight, bgColor);
        drawText(matrix, textSource, font, title, 0.0f, textY, color);
        return currentY + OPTION_HEIGHT_PIXELS + OPTION_SPACING_PIXELS;
    }

    private float renderSwitchLine(Font font, MultiBufferSource.BufferSource bufferSource, MultiBufferSource textSource,
                                   Matrix4f matrix,
                                   float currentY, int alpha, int grayColor, int narrationColor, int whiteColor,
                                   boolean drawBackground) {
        Component icon = NarrationIcons.rmb();
        String spacer = " ";
        String action = ">> ";
        String label = "Переключить";
        float widthIcon = font.width(icon);
        float widthSpacer = font.width(spacer);
        float widthAction = font.width(action);
        float widthLabel = font.width(label);
        float totalWidth = widthIcon + widthSpacer + widthAction + widthLabel;
        float textHeight = font.lineHeight;
        float bgWidth = totalWidth + BACKGROUND_PADDING_X * 2.0f;
        float bgHeight = textHeight + BACKGROUND_PADDING_Y * 2.0f;
        float bgX = -BACKGROUND_PADDING_X;
        float textY = currentY + LINE_TEXT_OFFSET_Y;
        float bgY = (currentY + LINE_TEXT_OFFSET_Y + textHeight / 2.0f) - bgHeight / 2.0f;
        if (drawBackground) {
            int bgAlpha = (int) (alpha * 120);
            int bgColor = (bgAlpha << 24) | (NARRATION_BG_COLOR & 0xFFFFFF);
            fillRect(matrix, bufferSource, bgX, bgY, bgWidth, bgHeight, bgColor);
        }

        float cursorX = 0.0f;
        drawComponent(matrix, textSource, font, icon, cursorX, textY, grayColor);
        cursorX += widthIcon;
        drawText(matrix, textSource, font, spacer, cursorX, textY, grayColor);
        cursorX += widthSpacer;
        drawText(matrix, textSource, font, action, cursorX, textY, narrationColor);
        cursorX += widthAction;
        drawText(matrix, textSource, font, label, cursorX, textY, whiteColor);
        return currentY + OPTION_HEIGHT_PIXELS + OPTION_SPACING_PIXELS;
    }

    private void renderToggleLine(Font font, MultiBufferSource.BufferSource bufferSource, MultiBufferSource textSource,
                                  Matrix4f matrix,
                                  float currentY, int alpha, int color, int narrationColor, String label,
                                  boolean drawBackground) {
        Component base = Component.literal("Shift + ");
        Component icon = NarrationIcons.rmb();
        String spacer = " ";
        String action = ">> ";
        float widthBase = font.width(base);
        float widthIcon = font.width(icon);
        float widthSpacer = font.width(spacer);
        float widthAction = font.width(action);
        float widthLabel = font.width(label);
        float totalWidth = widthBase + widthIcon + widthSpacer + widthAction + widthLabel;
        float textHeight = font.lineHeight;
        float bgWidth = totalWidth + BACKGROUND_PADDING_X * 2.0f;
        float bgHeight = textHeight + BACKGROUND_PADDING_Y * 2.0f;
        float bgX = -BACKGROUND_PADDING_X;
        float textY = currentY + LINE_TEXT_OFFSET_Y;
        float bgY = (currentY + LINE_TEXT_OFFSET_Y + textHeight / 2.0f) - bgHeight / 2.0f;
        if (drawBackground) {
            int bgAlpha = (int) (alpha * 120);
            int bgColor = (bgAlpha << 24) | (NARRATION_BG_COLOR & 0xFFFFFF);
            fillRect(matrix, bufferSource, bgX, bgY, bgWidth, bgHeight, bgColor);
        }

        float cursorX = 0.0f;
        drawComponent(matrix, textSource, font, base, cursorX, textY, color);
        cursorX += widthBase;
        drawComponent(matrix, textSource, font, icon, cursorX, textY, color);
        cursorX += widthIcon;
        drawText(matrix, textSource, font, spacer, cursorX, textY, color);
        cursorX += widthSpacer;
        drawText(matrix, textSource, font, action, cursorX, textY, narrationColor);
        cursorX += widthAction;
        drawText(matrix, textSource, font, label, cursorX, textY, color);
    }

    private void fillRect(Matrix4f matrix, MultiBufferSource.BufferSource bufferSource,
                          float x, float y, float width, float height, int color) {
        com.mojang.blaze3d.vertex.VertexConsumer vertexConsumer = bufferSource.getBuffer(
            NoDepthTextRenderType.textNoDepth(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft", "textures/misc/white.png")
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
}

