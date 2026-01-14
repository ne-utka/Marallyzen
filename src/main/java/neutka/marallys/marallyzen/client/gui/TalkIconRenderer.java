package neutka.marallys.marallyzen.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import neutka.marallys.marallyzen.Marallyzen;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Renders a small floating "talk" icon above NPCs that have dialogs.
 */
@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
@SuppressWarnings("removal") // bus enum marked for removal, kept for current Forge event bus usage
public class TalkIconRenderer {

    private static final Map<UUID, TalkIcon> ICONS = new HashMap<>();
    private static final float TEXT_SCALE = 0.015f; // Convert pixels to world units (blocks) - Reduced by 25%: 0.02 * 0.75
    private static final float BOB_AMPLITUDE = 0.1f; // Blocks
    private static final float BOB_SPEED = 0.08f; // Radians per tick
    private static final ResourceLocation ICON_TEXTURE = ResourceLocation.fromNamespaceAndPath(Marallyzen.MODID, "textures/gui/npcpoint.png");

    public record TalkIcon(int argbColor, boolean visible) { }

    /**
     * Updates icon visibility/color for a specific entity.
     */
    public static void updateIcon(UUID entityId, int argbColor, boolean visible) {
        Minecraft.getInstance().execute(() -> {
            if (!visible) {
                ICONS.remove(entityId);
            } else {
                ICONS.put(entityId, new TalkIcon(argbColor, true));
            }
        });
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }

        Camera camera = event.getCamera();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        float partialTick = mc.getTimer().getGameTimeDeltaPartialTick(false);
        double camX = camera.getPosition().x;
        double camY = camera.getPosition().y;
        double camZ = camera.getPosition().z;
        long gameTime = mc.level.getGameTime();

        // Remove icons whose entities no longer exist
        Iterator<Map.Entry<UUID, TalkIcon>> iterator = ICONS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, TalkIcon> entry = iterator.next();
            Entity entity = findEntityByUuid(entry.getKey());
            if (entity == null || !entry.getValue().visible) {
                iterator.remove();
            }
        }

        for (Map.Entry<UUID, TalkIcon> entry : ICONS.entrySet()) {
            Entity entity = findEntityByUuid(entry.getKey());
            if (entity == null) {
                continue;
            }

            renderIcon(poseStack, bufferSource, camera, entity, entry.getValue(), partialTick, gameTime, camX, camY, camZ);
        }

        bufferSource.endBatch();
    }

    private static void renderIcon(PoseStack poseStack,
                                   MultiBufferSource.BufferSource bufferSource,
                                   Camera camera,
                                   Entity entity,
                                   TalkIcon icon,
                                   float partialTick,
                                   long gameTime,
                                   double camX,
                                   double camY,
                                   double camZ) {
        double x = Mth.lerp(partialTick, entity.xOld, entity.getX()) - camX;
        double y = Mth.lerp(partialTick, entity.yOld, entity.getY()) - camY;
        double z = Mth.lerp(partialTick, entity.zOld, entity.getZ()) - camZ;

        float height = entity.getBbHeight();
        float bob = (float) Math.sin((gameTime + partialTick) * BOB_SPEED) * BOB_AMPLITUDE;
        float yOffset = height + 0.6f + bob; // Slightly above head

        poseStack.pushPose();
        poseStack.translate(x, y + yOffset, z);

        // Billboard to camera
        poseStack.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0f));

        poseStack.scale(TEXT_SCALE, TEXT_SCALE, TEXT_SCALE);

        Matrix4f matrix = poseStack.last().pose();
        int color = icon.argbColor;

        // Draw textured icon (16x16 px) tinted with NPC name color
        float size = 16f;
        float width = size * 0.75f; // 25% narrower
        float halfW = width / 2f;
        float halfH = size / 2f;
        renderTexturedQuad(matrix, bufferSource, -halfW, -halfH, halfW, halfH, color);

        poseStack.popPose();
    }

    private static void renderTexturedQuad(Matrix4f matrix,
                                           MultiBufferSource.BufferSource bufferSource,
                                           float left, float top, float right, float bottom,
                                           int color) {
        var vertexConsumer = bufferSource.getBuffer(RenderType.entityTranslucent(ICON_TEXTURE));

        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        // Packed light (full bright) and overlay (none)
        int light = LightTexture.FULL_BRIGHT;
        int overlay = OverlayTexture.NO_OVERLAY;
        int lightU = light & 0xFFFF;
        int lightV = (light >> 16) & 0xFFFF;
        int overlayU = overlay & 0xFFFF;
        int overlayV = (overlay >> 16) & 0xFFFF;

        // Quad (4 vertices) for RenderType with QUADS mode
        vertexConsumer.addVertex(matrix, left, bottom, 0.0f)
                .setColor(r, g, b, a)
                .setUv(0.0f, 1.0f)
                .setUv1(overlayU, overlayV)
                .setUv2(lightU, lightV)
                .setNormal(0.0f, 0.0f, -1.0f);
        vertexConsumer.addVertex(matrix, right, bottom, 0.0f)
                .setColor(r, g, b, a)
                .setUv(1.0f, 1.0f)
                .setUv1(overlayU, overlayV)
                .setUv2(lightU, lightV)
                .setNormal(0.0f, 0.0f, -1.0f);
        vertexConsumer.addVertex(matrix, right, top, 0.0f)
                .setColor(r, g, b, a)
                .setUv(1.0f, 0.0f)
                .setUv1(overlayU, overlayV)
                .setUv2(lightU, lightV)
                .setNormal(0.0f, 0.0f, -1.0f);
        vertexConsumer.addVertex(matrix, left, top, 0.0f)
                .setColor(r, g, b, a)
                .setUv(0.0f, 0.0f)
                .setUv1(overlayU, overlayV)
                .setUv2(lightU, lightV)
                .setNormal(0.0f, 0.0f, -1.0f);
    }

    private static Entity findEntityByUuid(UUID uuid) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return null;
        }
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e.getUUID().equals(uuid)) {
                return e;
            }
        }
        return null;
    }
}

