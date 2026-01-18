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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;

import neutka.marallys.marallyzen.entity.PosterEntity;
import neutka.marallys.marallyzen.util.NarrationIcons;
import neutka.marallys.marallyzen.client.gui.PromptAnchorUtil;
import neutka.marallys.marallyzen.client.NoDepthTextRenderType;
import neutka.marallys.marallyzen.client.gui.NoDepthTextBufferSource;

public class PosterEntityPromptHud {
    private static PosterEntityPromptHud instance;

    private static final int FADE_IN_DURATION_TICKS = 3;
    private static final int FADE_OUT_DURATION_TICKS = 2;
    private static final float VERTICAL_OFFSET_MAX = 0.1f;
    private static final float SCALE_BASE = 0.0171f;
    private static final float OFFSET_RIGHT = 0.7f;
    private static final int TEXT_DARK_GRAY = 0x555555;
    private static final int NARRATION_BG_COLOR = 0x000000;
    private static final float BACKGROUND_PADDING_X = 3.42f;
    private static final float BACKGROUND_PADDING_Y = 1.855f;
    private static final int OPTION_HEIGHT_PIXELS = 13;
    private static final int OPTION_SPACING_PIXELS = 4;
    private static final float LINE_TEXT_OFFSET_Y = OPTION_HEIGHT_PIXELS / 2.0f - 3.0f;
    private static final float PROMPT_OFFSET_X = -2.0f;

    private boolean targetVisible = false;
    private PosterEntity targetEntity;

    private float fadeInProgress = 0.0f;
    private float fadeOutProgress = 1.0f;
    private float previousFadeInProgress = 0.0f;
    private float previousFadeOutProgress = 1.0f;

    public static PosterEntityPromptHud getInstance() {
        if (instance == null) {
            instance = new PosterEntityPromptHud();
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

        double entityX = targetEntity.getX();
        double entityY = PromptAnchorUtil.entityTopY(targetEntity)
            - PromptAnchorUtil.pxToWorld(34.0f, SCALE_BASE);
        double entityZ = targetEntity.getZ();

        double camX = camera.getPosition().x;
        double camY = camera.getPosition().y;
        double camZ = camera.getPosition().z;

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
        int white = (alpha << 24) | 0xFFFFFF;
        int darkGray = (alpha << 24) | TEXT_DARK_GRAY;

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.depthFunc(GL11.GL_ALWAYS);
        String label = resolveSideLabel(mc, targetEntity);
        drawSingleLine(font, bufferSource, textSource, matrix, alpha, white, darkGray, label);
        bufferSource.endBatch();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        poseStack.popPose();
    }

    private void drawSingleLine(Font font, MultiBufferSource.BufferSource bufferSource, MultiBufferSource textSource,
                                Matrix4f matrix,
                                int alpha, int white, int darkGray, String label) {
        Component icon = NarrationIcons.rmb();
        String spacer = " ";
        String action = ">> ";
        float currentY = 0.0f;

        float widthIcon = font.width(icon);
        float widthSpacer = font.width(spacer);
        float widthAction = font.width(action);
        float widthLabel = font.width(label);
        float totalWidth = widthIcon + widthSpacer + widthAction + widthLabel;
        float textHeight = font.lineHeight;
        float bgWidth = totalWidth + BACKGROUND_PADDING_X * 2.0f;
        float bgHeight = textHeight + BACKGROUND_PADDING_Y * 2.0f;
        float bgX = -BACKGROUND_PADDING_X + PROMPT_OFFSET_X;
        float textY = currentY + LINE_TEXT_OFFSET_Y;
        float bgY = (currentY + LINE_TEXT_OFFSET_Y + textHeight / 2.0f) - bgHeight / 2.0f;
        int bgAlpha = (int) (alpha * 120);
        int bgColor = (bgAlpha << 24) | (NARRATION_BG_COLOR & 0xFFFFFF);
        fillRect(matrix, bufferSource, bgX, bgY, bgWidth, bgHeight, bgColor);

        float cursorX = PROMPT_OFFSET_X;
        drawComponent(matrix, textSource, font, icon, cursorX, textY, white);
        cursorX += widthIcon;
        drawText(matrix, textSource, font, spacer, cursorX, textY, white);
        cursorX += widthSpacer;
        drawText(matrix, textSource, font, action, cursorX, textY, darkGray);
        cursorX += widthAction;
        drawText(matrix, textSource, font, label, cursorX, textY, white);
    }

    private void updateTarget() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            targetVisible = false;
            return;
        }
        HitResult hitResult = mc.hitResult;
        if (hitResult instanceof EntityHitResult entityHit) {
            if (entityHit.getEntity() instanceof PosterEntity posterEntity
                    && shouldShowForPoster(posterEntity)) {
                targetVisible = true;
                targetEntity = posterEntity;
                return;
            }
        }
        PosterEntity posterEntity = findPosterEntityInFront(mc);
        if (posterEntity != null) {
            targetVisible = true;
            targetEntity = posterEntity;
            return;
        }
        targetVisible = false;
    }

    private boolean shouldShowForPoster(PosterEntity posterEntity) {
        if (posterEntity == null) {
            return false;
        }
        if (posterEntity.getCurrentState() != PosterEntity.State.VIEWING) {
            return false;
        }
        int posterNumber = posterEntity.getPosterNumber();
        return (posterNumber >= 1 && posterNumber <= 10) || posterNumber == 12 || posterNumber == 13;
    }

    private PosterEntity findPosterEntityInFront(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            return null;
        }
        Vec3 eyePos = mc.player.getEyePosition();
        Vec3 lookVec = mc.player.getLookAngle();
        double reachDistance = 5.0;
        Vec3 endPos = eyePos.add(lookVec.scale(reachDistance));
        PosterEntity closestPoster = null;
        double closestDistance = Double.MAX_VALUE;

        for (PosterEntity posterEntity : mc.level.getEntitiesOfClass(
                PosterEntity.class,
                new AABB(
                        eyePos.x - reachDistance,
                        eyePos.y - reachDistance,
                        eyePos.z - reachDistance,
                        eyePos.x + reachDistance,
                        eyePos.y + reachDistance,
                        eyePos.z + reachDistance
                )
        )) {
            if (!shouldShowForPoster(posterEntity)) {
                continue;
            }
            Vec3 posterPos = posterEntity.position();
            Vec3 toPoster = posterPos.subtract(eyePos);
            double distance = toPoster.length();
            if (distance > reachDistance) {
                continue;
            }
            Vec3 normalizedLook = lookVec.normalize();
            Vec3 normalizedToPoster = toPoster.normalize();
            double dot = normalizedLook.dot(normalizedToPoster);
            if (dot < 0.7) {
                continue;
            }
            AABB posterBox = new AABB(
                    posterPos.x - 0.5,
                    posterPos.y - 0.5,
                    posterPos.z - 0.5,
                    posterPos.x + 0.5,
                    posterPos.y + 0.5,
                    posterPos.z + 0.5
            );
            Vec3 hitVec = posterBox.clip(eyePos, endPos).orElse(null);
            if (hitVec == null) {
                continue;
            }
            double hitDistance = hitVec.distanceTo(eyePos);
            if (hitDistance < closestDistance) {
                closestDistance = hitDistance;
                closestPoster = posterEntity;
            }
        }
        return closestPoster;
    }

    private String resolveSideLabel(Minecraft mc, PosterEntity posterEntity) {
        if (mc == null || mc.player == null || posterEntity == null) {
            return "\u041b\u0438\u0446\u0435\u0432\u0430\u044f \u0441\u0442\u043e\u0440\u043e\u043d\u0430";
        }
        boolean viewingFront = isViewingFrontSide(mc.player, posterEntity);
        return "\u041f\u0435\u0440\u0435\u0432\u0435\u0440\u043d\u0443\u0442\u044c";
    }

    private boolean isViewingFrontSide(net.minecraft.client.player.LocalPlayer player, PosterEntity posterEntity) {
        Direction facing = posterEntity.getFacing();
        if (facing == null) {
            return !posterEntity.isFlipped();
        }
        Vec3 posterPos = posterEntity.position();
        Vec3 toPlayer = player.getEyePosition().subtract(posterPos);
        Vec3 frontNormal = Vec3.atLowerCornerOf(facing.getNormal()).normalize();
        boolean playerInFront = frontNormal.dot(toPlayer) >= 0.0;
        return playerInFront ^ posterEntity.isFlipped();
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
}
