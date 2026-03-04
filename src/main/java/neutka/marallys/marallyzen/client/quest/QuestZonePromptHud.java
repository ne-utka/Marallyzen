package neutka.marallys.marallyzen.client.quest;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import neutka.marallys.marallyzen.MarallyzenClientConfig;
import neutka.marallys.marallyzen.client.NoDepthTextRenderType;
import neutka.marallys.marallyzen.client.gui.PromptAnchorUtil;
import neutka.marallys.marallyzen.client.instance.InstanceClientState;
import neutka.marallys.marallyzen.client.narration.NarrationManager;
import neutka.marallys.marallyzen.network.NetworkHelper;
import neutka.marallys.marallyzen.network.QuestZoneTeleportRequestPacket;
import neutka.marallys.marallyzen.quest.QuestDefinition;
import neutka.marallys.marallyzen.util.NarrationIcons;
import org.joml.Matrix4f;

public class QuestZonePromptHud {
    private static QuestZonePromptHud instance;

    // Lodestone prompt visuals
    private static final int FADE_IN_DURATION_TICKS = 3;
    private static final int FADE_OUT_DURATION_TICKS = 2;
    private static final float VERTICAL_OFFSET_MAX = 0.1f;
    private static final float SCALE_BASE = 0.0171f;
    private static final float OFFSET_RIGHT = 0.7f;
    private static final int TEXT_DARK_GRAY = 0x555555;
    private static final int NARRATION_BG_COLOR = 0x000000;
    private static final float BACKGROUND_PADDING_X = 3.42f;
    private static final float BACKGROUND_PADDING_Y = 1.855f;
    private static final double TARGET_RANGE = 6.0;

    // Countdown behavior
    private static final int COUNTDOWN_SECONDS = 8;
    private static final int TICKS_PER_SECOND = 20;
    private static final int COUNTDOWN_TICKS = COUNTDOWN_SECONDS * TICKS_PER_SECOND;
    private static final int NARRATION_FADE_IN = 5;
    private static final int NARRATION_FADE_OUT = 5;
    private static final int LEAVE_STAY_TICKS = 40;

    private boolean targetVisible = false;
    private BlockPos magnetitePos;
    private String promptLabel;
    private String lodestoneZoneId;
    private float fadeInProgress = 0.0f;
    private float fadeOutProgress = 1.0f;
    private float previousFadeInProgress = 0.0f;
    private float previousFadeOutProgress = 1.0f;

    private boolean countdownActive = false;
    private long countdownStartTick = 0L;
    private int lastFilled = -1;
    private String countdownZoneId;
    private boolean insideZone = false;
    private boolean teleportSent = false;

    public static QuestZonePromptHud getInstance() {
        if (instance == null) {
            instance = new QuestZonePromptHud();
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
        if (magnetitePos == null || promptLabel == null) {
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

        BlockState state = mc.level.getBlockState(magnetitePos);
        double blockX = magnetitePos.getX() + 0.5;
        double blockY = PromptAnchorUtil.blockTopY(mc.level, magnetitePos, state)
                - PromptAnchorUtil.pxToWorld(20.0f, SCALE_BASE);
        double blockZ = magnetitePos.getZ() + 0.5;
        double camX = camera.position().x;
        double camY = camera.position().y;
        double camZ = camera.position().z;
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
        drawPrompt(font, bufferSource, textSource, matrix, promptLabel, white, darkGray, alpha);
        bufferSource.endBatch();
        poseStack.popPose();
    }

    public String activeZoneId() {
        if (targetVisible && lodestoneZoneId != null && promptLabel != null) {
            return lodestoneZoneId;
        }
        return null;
    }

    public BlockPos activeMagnetitePos() {
        if (targetVisible && magnetitePos != null) {
            return magnetitePos;
        }
        return null;
    }

    private void updateTarget() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            resetAll(false);
            return;
        }
        if (InstanceClientState.getInstance().isInInstance()) {
            resetAll(false);
            return;
        }

        QuestZoneVisual zone = QuestClientState.getInstance().activeZone();
        if (zone == null) {
            resetAll(false);
            return;
        }
        if (!mc.level.dimension().equals(zone.dimension())) {
            resetAll(false);
            return;
        }

        boolean usesLodestone = zone.requireLodestone();
        if (usesLodestone) {
            resetCountdown(false);
            updateLodestoneTarget(zone);
            return;
        }

        resetTarget();
        updateCountdownTarget(zone);
    }

    private void updateLodestoneTarget(QuestZoneVisual zone) {
        Minecraft mc = Minecraft.getInstance();
        QuestDefinition definition = QuestClientState.getInstance().getActiveDefinition();
        if (definition == null || definition.instanceSpec() == null) {
            resetTarget();
            return;
        }

        HitResult hitResult = mc.player.pick(TARGET_RANGE, 0.0f, false);
        if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) {
            resetTarget();
            return;
        }
        BlockHitResult blockHit = (BlockHitResult) hitResult;
        BlockPos magnetite = blockHit.getBlockPos();
        if (!zoneContainsBlock(zone, magnetite)) {
            resetTarget();
            return;
        }
        BlockState state = mc.level.getBlockState(magnetite);
        if (!state.is(Blocks.LODESTONE)) {
            resetTarget();
            return;
        }
        targetVisible = true;
        magnetitePos = magnetite;
        promptLabel = "РўРµР»РµРїРѕСЂС‚РёСЂРѕРІР°С‚СЊСЃСЏ";
        lodestoneZoneId = zone.id();
    }

    private void updateCountdownTarget(QuestZoneVisual zone) {
        Minecraft mc = Minecraft.getInstance();
        QuestDefinition definition = QuestClientState.getInstance().getActiveDefinition();
        boolean questHasInstance = definition != null && definition.instanceSpec() != null;
        boolean zoneActive = questHasInstance || zone.alwaysActive()
                || (zone.autoStartQuestId() != null && !zone.autoStartQuestId().isBlank());
        if (!zoneActive) {
            resetCountdown(false);
            return;
        }

        boolean currentlyInside = isPlayerInsideZone(mc, zone);
        if (currentlyInside) {
            if (!insideZone) {
                startCountdown(zone.id());
            }
            tickCountdown(mc);
        } else {
            if (insideZone) {
                resetCountdown(true);
            } else {
                resetCountdown(false);
            }
        }
        insideZone = currentlyInside;
    }

    private boolean isPlayerInsideZone(Minecraft mc, QuestZoneVisual zone) {
        Vec3 pos = mc.player.position();
        if (zone.ignoreHeight()) {
            return pos.x >= zone.bounds().minX && pos.x <= zone.bounds().maxX
                    && pos.z >= zone.bounds().minZ && pos.z <= zone.bounds().maxZ;
        }
        return zone.bounds().contains(pos);
    }

    private void startCountdown(String zoneId) {
        Minecraft mc = Minecraft.getInstance();
        countdownActive = true;
        countdownStartTick = mc.level.getGameTime();
        lastFilled = -1;
        teleportSent = false;
        countdownZoneId = zoneId;

        Component text = buildCountdownText(0);
        NarrationManager.getInstance().startNarration(text, null, NARRATION_FADE_IN, COUNTDOWN_TICKS, NARRATION_FADE_OUT,
                false, false);
    }

    private void tickCountdown(Minecraft mc) {
        if (!countdownActive || mc.level == null) {
            return;
        }
        long elapsed = mc.level.getGameTime() - countdownStartTick;
        if (elapsed < 0) {
            elapsed = 0;
        }
        int filled = (int) Math.min(COUNTDOWN_SECONDS, elapsed / TICKS_PER_SECOND);
        if (filled != lastFilled) {
            lastFilled = filled;
            NarrationManager.getInstance().updateNarrationText(buildCountdownText(filled));
        }

        if (elapsed >= COUNTDOWN_TICKS && !teleportSent) {
            teleportSent = true;
            if (countdownZoneId != null) {
                NetworkHelper.sendToServer(new QuestZoneTeleportRequestPacket(countdownZoneId));
            }
        }
    }

    private void resetTarget() {
        targetVisible = false;
        magnetitePos = null;
        promptLabel = null;
        lodestoneZoneId = null;
    }

    private void resetCountdown(boolean showLeaveMessage) {
        if (showLeaveMessage && (countdownActive || insideZone)) {
            NarrationManager.getInstance().startNarration(
                    Component.literal("Р’С‹ РїРѕРєРёРЅСѓР»Рё Р·РѕРЅСѓ РєРІРµСЃС‚Р°!"),
                    null,
                    3,
                    LEAVE_STAY_TICKS,
                    5,
                    false,
                    false
            );
        }
        countdownActive = false;
        countdownStartTick = 0L;
        lastFilled = -1;
        countdownZoneId = null;
        teleportSent = false;
    }

    private void resetAll(boolean showLeaveMessage) {
        resetTarget();
        resetCountdown(showLeaveMessage);
        insideZone = false;
    }

    private boolean zoneContainsBlock(QuestZoneVisual zone, BlockPos pos) {
        if (zone == null || pos == null) {
            return false;
        }
        Vec3 center = Vec3.atCenterOf(pos);
        if (zone.ignoreHeight()) {
            return center.x >= zone.bounds().minX && center.x <= zone.bounds().maxX
                    && center.z >= zone.bounds().minZ && center.z <= zone.bounds().maxZ;
        }
        return zone.bounds().contains(center);
    }

    private static Component buildCountdownText(int filled) {
        int clamped = Mth.clamp(filled, 0, COUNTDOWN_SECONDS);
        MutableComponent root = Component.literal("");
        root.append(Component.literal("\u2716 ").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xD73A3A))));
        root.append(Component.literal("\u258c ").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x3EB05A))));
        for (int i = 0; i < COUNTDOWN_SECONDS; i++) {
            boolean done = i < clamped;
            String glyph = "\u2588";
            int color = done ? 0xE8EEF8 : 0x7A8190;
            root.append(Component.literal(glyph).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color))));
        }
        root.append(Component.literal(" \u2714").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x56B96C))));
        return root;
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

    private void drawPrompt(Font font, MultiBufferSource.BufferSource bufferSource, MultiBufferSource textSource,
                            Matrix4f matrix,
                            String label, int white, int darkGray, int alpha) {
        Component icon = NarrationIcons.rmb();
        String spacer = " ";
        String action = ">> ";
        float widthIcon = font.width(icon);
        float widthSpacer = font.width(spacer);
        float widthAction = font.width(action);
        float widthLabel = font.width(label);
        float totalWidth = widthIcon + widthSpacer + widthAction + widthLabel;
        float textHeight = font.lineHeight;
        float bgWidth = totalWidth + BACKGROUND_PADDING_X * 2.0f;
        float bgHeight = textHeight + BACKGROUND_PADDING_Y * 2.0f;
        float lineCenterY = 4.0f;
        float textY = lineCenterY - textHeight * 0.5f;
        float bgX = -BACKGROUND_PADDING_X;
        float bgY = lineCenterY - bgHeight * 0.5f - 1.0f;
        int bgAlpha = (int) (alpha * 120);
        int bgColor = (bgAlpha << 24) | (NARRATION_BG_COLOR & 0xFFFFFF);
        if (MarallyzenClientConfig.INTERACTIVE_PROMPT_BACKGROUND.get()) {
            fillRect(matrix, bufferSource, bgX, bgY, bgWidth, bgHeight, bgColor);
        }
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
                true,
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
        int overlayV = overlay & 0xFFFF;
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

    private void drawText(Matrix4f matrix, MultiBufferSource bufferSource, Font font,
                          String text, float x, float y, int color) {
        font.drawInBatch(
                text,
                x,
                y,
                color,
                true,
                matrix,
                bufferSource,
                Font.DisplayMode.SEE_THROUGH,
                0,
                15728880
        );
    }
}
