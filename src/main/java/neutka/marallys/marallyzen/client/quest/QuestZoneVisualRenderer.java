package neutka.marallys.marallyzen.client.quest;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.quest.QuestCategory;
import neutka.marallys.marallyzen.quest.QuestCategoryColors;

@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT)
public class QuestZoneVisualRenderer {
    private static boolean enabled = false;
    private static final double MAX_RENDER_DISTANCE = 64.0;
    private static final double BAND_HEIGHT = 0.4;
    private static final int BASE_ALPHA = 18;
    private static final int WAVE_ALPHA = 80;
    private static final double WAVE_SPEED = 2.2;
    private static boolean disabled;

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent.AfterEntities event) {
        if (!enabled || disabled) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        QuestZoneVisual zone = QuestClientState.getInstance().activeZone();
        if (zone == null) {
            return;
        }
        if (!zone.dimension().equals(mc.level.dimension())) {
            return;
        }
        double distance = zone.distanceTo(mc.player.position());
        if (distance > MAX_RENDER_DISTANCE) {
            return;
        }
        try {
            renderZoneBand(event.getPoseStack(), mc, zone);
        } catch (Exception e) {
            disabled = true;
            Marallyzen.LOGGER.warn("QuestZoneVisualRenderer: disabled after render error", e);
        }
    }

    private static void renderZoneBand(com.mojang.blaze3d.vertex.PoseStack poseStack, Minecraft mc, QuestZoneVisual zone) {
        poseStack.pushPose();
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.position();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        AABB bounds = zone.bounds();
        double minX = bounds.minX;
        double maxX = bounds.maxX;
        double minZ = bounds.minZ;
        double maxZ = bounds.maxZ;
        double worldMinY = mc.level.getMinY();
        double worldMaxY = mc.level.getMaxY();

        double baseY;
        double topY;
        if (zone.ignoreHeight()) {
            baseY = worldMinY;
            topY = worldMaxY;
        } else {
            baseY = clamp(bounds.minY, worldMinY, worldMaxY - 0.1);
            topY = Math.min(baseY + BAND_HEIGHT, bounds.maxY);
            topY = Math.min(topY, worldMaxY);
            if (topY <= baseY) {
                poseStack.popPose();
                return;
            }
        }

        int color = colorForCategory(zone.category());
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;

        float time = (float) (Util.getMillis() / 1000.0);
        float wave = (float) ((Math.sin(time * WAVE_SPEED) * 0.5) + 0.5);
        float alpha = Mth.clamp((BASE_ALPHA + wave * WAVE_ALPHA) / 255.0f, 0.0f, 1.0f);

        AABB box = new AABB(minX, baseY, minZ, maxX, topY, maxZ);
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        renderLineBox(poseStack, buffer.getBuffer(RenderTypes.lines()), box, r, g, b, alpha);
        buffer.endBatch(RenderTypes.lines());

        poseStack.popPose();
    }

    private static int colorForCategory(QuestCategory category) {
        return QuestCategoryColors.getColor(category);
    }

    private static double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private static void renderLineBox(com.mojang.blaze3d.vertex.PoseStack poseStack, VertexConsumer consumer, AABB box,
                                      float r, float g, float b, float a) {
        Matrix4f matrix = poseStack.last().pose();
        float minX = (float) box.minX;
        float minY = (float) box.minY;
        float minZ = (float) box.minZ;
        float maxX = (float) box.maxX;
        float maxY = (float) box.maxY;
        float maxZ = (float) box.maxZ;

        // X edges
        addLine(consumer, matrix, minX, minY, minZ, maxX, minY, minZ, r, g, b, a, 1, 0, 0);
        addLine(consumer, matrix, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a, 1, 0, 0);
        addLine(consumer, matrix, minX, minY, maxZ, maxX, minY, maxZ, r, g, b, a, 1, 0, 0);
        addLine(consumer, matrix, minX, maxY, maxZ, maxX, maxY, maxZ, r, g, b, a, 1, 0, 0);
        // Y edges
        addLine(consumer, matrix, minX, minY, minZ, minX, maxY, minZ, r, g, b, a, 0, 1, 0);
        addLine(consumer, matrix, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a, 0, 1, 0);
        addLine(consumer, matrix, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a, 0, 1, 0);
        addLine(consumer, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a, 0, 1, 0);
        // Z edges
        addLine(consumer, matrix, minX, minY, minZ, minX, minY, maxZ, r, g, b, a, 0, 0, 1);
        addLine(consumer, matrix, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a, 0, 0, 1);
        addLine(consumer, matrix, minX, maxY, minZ, minX, maxY, maxZ, r, g, b, a, 0, 0, 1);
        addLine(consumer, matrix, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a, 0, 0, 1);
    }

    private static void addLine(VertexConsumer consumer, Matrix4f matrix,
                                float x1, float y1, float z1, float x2, float y2, float z2,
                                float r, float g, float b, float a,
                                float nx, float ny, float nz) {
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setNormal(nx, ny, nz);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setNormal(nx, ny, nz);
    }
}






