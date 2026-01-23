package neutka.marallys.marallyzen.client.quest;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.quest.QuestCategory;
import neutka.marallys.marallyzen.quest.QuestCategoryColors;
import org.joml.Matrix4f;

@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class QuestZoneVisualRenderer {
    private static final boolean ENABLED = false;
    private static final double MAX_RENDER_DISTANCE = 64.0;
    private static final double BAND_HEIGHT = 0.4;
    private static final double BAND_THICKNESS = 0.04;
    private static final double TOP_FADE_HEIGHT = 0.4;
    private static final double WAVE_FREQUENCY = 0.45;
    private static final double WAVE_SPEED = 2.2;
    private static final int BASE_ALPHA = 18;
    private static final int WAVE_ALPHA = 80;
    private static boolean disabled;

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (!ENABLED) {
            return;
        }
        if (disabled) {
            return;
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
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
            renderZoneBand(event.getPoseStack(), event.getCamera(), mc, zone);
        } catch (Exception e) {
            disabled = true;
            Marallyzen.LOGGER.warn("QuestZoneVisualRenderer: disabled after render error", e);
        }
    }

    private static void renderZoneBand(PoseStack poseStack, Camera camera, Minecraft mc, QuestZoneVisual zone) {
        poseStack.pushPose();
        Vec3 camPos = camera.getPosition();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Matrix4f matrix = poseStack.last().pose();

        int color = colorForCategory(zone.category());
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        AABB bounds = zone.bounds();
        double minX = bounds.minX;
        double maxX = bounds.maxX;
        double minZ = bounds.minZ;
        double maxZ = bounds.maxZ;
        double worldMinY = mc.level.getMinBuildHeight();
        double worldMaxY = mc.level.getMaxBuildHeight();
        float time = (float) (Util.getMillis() / 1000.0);

        if (zone.ignoreHeight()) {
            double segment = 1.0;
            renderEdgeXAdaptive(matrix, mc, minX, maxX, minZ, worldMinY, worldMaxY, r, g, b, time, segment);
            renderEdgeXAdaptive(matrix, mc, minX, maxX, maxZ, worldMinY, worldMaxY, r, g, b, time, segment);
            renderEdgeZAdaptive(matrix, mc, minZ, maxZ, minX, worldMinY, worldMaxY, r, g, b, time, segment);
            renderEdgeZAdaptive(matrix, mc, minZ, maxZ, maxX, worldMinY, worldMaxY, r, g, b, time, segment);
            addCornerAdaptive(matrix, mc, minX, minZ, worldMinY, worldMaxY, r, g, b, time, -1, -1);
            addCornerAdaptive(matrix, mc, minX, maxZ, worldMinY, worldMaxY, r, g, b, time, -1, 1);
            addCornerAdaptive(matrix, mc, maxX, minZ, worldMinY, worldMaxY, r, g, b, time, 1, -1);
            addCornerAdaptive(matrix, mc, maxX, maxZ, worldMinY, worldMaxY, r, g, b, time, 1, 1);
        } else {
            double baseY = clamp(bounds.minY, worldMinY, worldMaxY - 0.1);
            double topY = Math.min(baseY + BAND_HEIGHT, bounds.maxY);
            topY = Math.min(topY, worldMaxY);
            if (topY <= baseY) {
                RenderSystem.disableBlend();
                RenderSystem.depthMask(true);
                RenderSystem.enableCull();
                poseStack.popPose();
                return;
            }
            double maxEdge = Math.max(maxX - minX, maxZ - minZ);
            double segment = Math.max(0.75, maxEdge / 96.0);

            renderEdgeX(matrix, minX, maxX, minZ, baseY, topY, r, g, b, time, segment);
            renderEdgeX(matrix, minX, maxX, maxZ, baseY, topY, r, g, b, time, segment);
            renderEdgeZ(matrix, minZ, maxZ, minX, baseY, topY, r, g, b, time, segment);
            renderEdgeZ(matrix, minZ, maxZ, maxX, baseY, topY, r, g, b, time, segment);
            addCorner(matrix, minX, minZ, baseY, topY, r, g, b, time, -1, -1);
            addCorner(matrix, minX, maxZ, baseY, topY, r, g, b, time, -1, 1);
            addCorner(matrix, maxX, minZ, baseY, topY, r, g, b, time, 1, -1);
            addCorner(matrix, maxX, maxZ, baseY, topY, r, g, b, time, 1, 1);
        }

        poseStack.popPose();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.disableDepthTest();
        RenderSystem.enableCull();
    }

    private static int colorForCategory(QuestCategory category) {
        return QuestCategoryColors.getColor(category);
    }

    private static void renderEdgeX(Matrix4f matrix,
                                    double minX, double maxX, double z,
                                    double baseY, double topY,
                                    int r, int g, int b, float time, double segment) {
        double half = BAND_THICKNESS * 0.5;
        renderStripX(matrix, minX, maxX, z - half, baseY, topY, r, g, b, time, segment);
        renderStripX(matrix, minX, maxX, z + half, baseY, topY, r, g, b, time, segment);
    }

    private static void renderEdgeZ(Matrix4f matrix,
                                    double minZ, double maxZ, double x,
                                    double baseY, double topY,
                                    int r, int g, int b, float time, double segment) {
        double half = BAND_THICKNESS * 0.5;
        renderStripZ(matrix, minZ, maxZ, x - half, baseY, topY, r, g, b, time, segment);
        renderStripZ(matrix, minZ, maxZ, x + half, baseY, topY, r, g, b, time, segment);
    }

    private static void renderEdgeXAdaptive(Matrix4f matrix, Minecraft mc,
                                            double minX, double maxX, double z,
                                            double worldMinY, double worldMaxY,
                                            int r, int g, int b, float time, double segment) {
        double half = BAND_THICKNESS * 0.5;
        renderStripXAdaptive(matrix, mc, minX, maxX, z - half, worldMinY, worldMaxY, r, g, b, time, segment);
        renderStripXAdaptive(matrix, mc, minX, maxX, z + half, worldMinY, worldMaxY, r, g, b, time, segment);
    }

    private static void renderEdgeZAdaptive(Matrix4f matrix, Minecraft mc,
                                            double minZ, double maxZ, double x,
                                            double worldMinY, double worldMaxY,
                                            int r, int g, int b, float time, double segment) {
        double half = BAND_THICKNESS * 0.5;
        renderStripZAdaptive(matrix, mc, minZ, maxZ, x - half, worldMinY, worldMaxY, r, g, b, time, segment);
        renderStripZAdaptive(matrix, mc, minZ, maxZ, x + half, worldMinY, worldMaxY, r, g, b, time, segment);
    }

    private static double surfaceY(Minecraft mc, double x, double z) {
        if (mc.level == null) {
            return 0.0;
        }
        int blockX = Mth.floor(x);
        int blockZ = Mth.floor(z);
        int minY = mc.level.getMinBuildHeight();
        int maxY = mc.level.getMaxBuildHeight() - 1;
        if (!mc.level.hasChunkAt(new net.minecraft.core.BlockPos(blockX, minY, blockZ))) {
            return mc.player != null ? mc.player.getY() : minY;
        }
        LevelChunk chunk = mc.level.getChunk(blockX >> 4, blockZ >> 4);
        int height = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockX & 15, blockZ & 15);
        if (height > minY) {
            BlockPos topPos = new BlockPos(blockX, height - 1, blockZ);
            BlockState state = mc.level.getBlockState(topPos);
            if (!state.isAir() || !state.getFluidState().isEmpty()) {
                return blockSurfaceY(mc, topPos, state) + 0.05;
            }
        }
        return scanSurfaceY(mc, blockX, blockZ, maxY, minY) + 0.05;
    }

    private static double scanSurfaceY(Minecraft mc, int blockX, int blockZ, int maxY, int minY) {
        if (mc.level == null) {
            return minY;
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = maxY; y >= minY; y--) {
            pos.set(blockX, y, blockZ);
            BlockState state = mc.level.getBlockState(pos);
            if (!state.isAir() || !state.getFluidState().isEmpty()) {
                return blockSurfaceY(mc, pos, state);
            }
        }
        return minY;
    }

    private static double blockSurfaceY(Minecraft mc, BlockPos pos, BlockState state) {
        double collisionTop = 0.0;
        VoxelShape shape = state.getCollisionShape(mc.level, pos);
        if (!shape.isEmpty()) {
            collisionTop = shape.max(Direction.Axis.Y);
        }
        double fluidTop = 0.0;
        if (!state.getFluidState().isEmpty()) {
            fluidTop = state.getFluidState().getHeight(mc.level, pos);
        }
        double top = Math.max(collisionTop, fluidTop);
        if (top <= 0.0) {
            return pos.getY();
        }
        return pos.getY() + top;
    }

    private static int waveAlpha(double coord, double other, float time) {
        double phase = (coord * WAVE_FREQUENCY) + (other * 0.12) + (time * WAVE_SPEED);
        double wave = (Math.sin(phase) * 0.5) + 0.5;
        int a = (int) (BASE_ALPHA + wave * WAVE_ALPHA);
        if (a < 0) {
            return 0;
        }
        if (a > 255) {
            return 255;
        }
        return a;
    }

    private static void addCorner(Matrix4f matrix,
                                  double x, double z, double baseY, double topY,
                                  int r, int g, int b, float time,
                                  int xSign, int zSign) {
        double half = BAND_THICKNESS * 0.5;
        double xOffset = x + xSign * half;
        double zOffset = z + zSign * half;
        int a = waveAlpha(x, z, time);
        int aTop = fadeAlpha(a, topY, topY);
        drawQuadGradient(matrix,
                xOffset, baseY, z,
                x, baseY, zOffset,
                x, topY, zOffset,
                xOffset, topY, z,
                r, g, b,
                a, a, aTop, aTop);
    }

    private static void addCornerAdaptive(Matrix4f matrix, Minecraft mc,
                                          double x, double z, double worldMinY, double worldMaxY,
                                          int r, int g, int b, float time,
                                          int xSign, int zSign) {
        double baseY = surfaceY(mc, x, z);
        baseY = clamp(baseY, worldMinY, worldMaxY - 0.1);
        double topY = Math.min(baseY + BAND_HEIGHT, worldMaxY);
        if (topY <= baseY) {
            return;
        }
        addCorner(matrix, x, z, baseY, topY, r, g, b, time, xSign, zSign);
    }

    private static void addQuad(BufferBuilder buffer, Matrix4f matrix,
                                double x1, double y1, double z1,
                                double x2, double y2, double z2,
                                double x3, double y3, double z3,
                                double x4, double y4, double z4,
                                int r, int g, int b, int a) {
        buffer.addVertex(matrix, (float) x1, (float) y1, (float) z1).setColor(r, g, b, a);
        buffer.addVertex(matrix, (float) x2, (float) y2, (float) z2).setColor(r, g, b, a);
        buffer.addVertex(matrix, (float) x3, (float) y3, (float) z3).setColor(r, g, b, a);
        buffer.addVertex(matrix, (float) x4, (float) y4, (float) z4).setColor(r, g, b, a);
    }

    private static void renderStripX(Matrix4f matrix, double minX, double maxX, double z,
                                     double baseY, double topY,
                                     int r, int g, int b, float time, double segment) {
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        double x = minX;
        while (x < maxX) {
            int a = waveAlpha(x, z, time);
            addStripVertex(buffer, matrix, x, baseY, z, r, g, b, fadeAlpha(a, baseY, topY));
            addStripVertex(buffer, matrix, x, topY, z, r, g, b, fadeAlpha(a, topY, topY));
            x += segment;
        }
        int aEnd = waveAlpha(maxX, z, time);
        addStripVertex(buffer, matrix, maxX, baseY, z, r, g, b, fadeAlpha(aEnd, baseY, topY));
        addStripVertex(buffer, matrix, maxX, topY, z, r, g, b, fadeAlpha(aEnd, topY, topY));
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private static void renderStripZ(Matrix4f matrix, double minZ, double maxZ, double x,
                                     double baseY, double topY,
                                     int r, int g, int b, float time, double segment) {
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        double z = minZ;
        while (z < maxZ) {
            int a = waveAlpha(x, z, time);
            addStripVertex(buffer, matrix, x, baseY, z, r, g, b, fadeAlpha(a, baseY, topY));
            addStripVertex(buffer, matrix, x, topY, z, r, g, b, fadeAlpha(a, topY, topY));
            z += segment;
        }
        int aEnd = waveAlpha(x, maxZ, time);
        addStripVertex(buffer, matrix, x, baseY, maxZ, r, g, b, fadeAlpha(aEnd, baseY, topY));
        addStripVertex(buffer, matrix, x, topY, maxZ, r, g, b, fadeAlpha(aEnd, topY, topY));
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private static void renderStripXAdaptive(Matrix4f matrix, Minecraft mc,
                                             double minX, double maxX, double z,
                                             double worldMinY, double worldMaxY,
                                             int r, int g, int b, float time, double segment) {
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        double x = minX;
        while (x < maxX) {
            double baseY = clamp(surfaceY(mc, x, z), worldMinY, worldMaxY - 0.1);
            double topY = Math.min(baseY + BAND_HEIGHT, worldMaxY);
            int a = waveAlpha(x, z, time);
            addStripVertex(buffer, matrix, x, baseY, z, r, g, b, fadeAlpha(a, baseY, topY));
            addStripVertex(buffer, matrix, x, topY, z, r, g, b, fadeAlpha(a, topY, topY));
            x += segment;
        }
        double baseY = clamp(surfaceY(mc, maxX, z), worldMinY, worldMaxY - 0.1);
        double topY = Math.min(baseY + BAND_HEIGHT, worldMaxY);
        int aEnd = waveAlpha(maxX, z, time);
        addStripVertex(buffer, matrix, maxX, baseY, z, r, g, b, fadeAlpha(aEnd, baseY, topY));
        addStripVertex(buffer, matrix, maxX, topY, z, r, g, b, fadeAlpha(aEnd, topY, topY));
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private static void renderStripZAdaptive(Matrix4f matrix, Minecraft mc,
                                             double minZ, double maxZ, double x,
                                             double worldMinY, double worldMaxY,
                                             int r, int g, int b, float time, double segment) {
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        double z = minZ;
        while (z < maxZ) {
            double baseY = clamp(surfaceY(mc, x, z), worldMinY, worldMaxY - 0.1);
            double topY = Math.min(baseY + BAND_HEIGHT, worldMaxY);
            int a = waveAlpha(x, z, time);
            addStripVertex(buffer, matrix, x, baseY, z, r, g, b, fadeAlpha(a, baseY, topY));
            addStripVertex(buffer, matrix, x, topY, z, r, g, b, fadeAlpha(a, topY, topY));
            z += segment;
        }
        double baseY = clamp(surfaceY(mc, x, maxZ), worldMinY, worldMaxY - 0.1);
        double topY = Math.min(baseY + BAND_HEIGHT, worldMaxY);
        int aEnd = waveAlpha(x, maxZ, time);
        addStripVertex(buffer, matrix, x, baseY, maxZ, r, g, b, fadeAlpha(aEnd, baseY, topY));
        addStripVertex(buffer, matrix, x, topY, maxZ, r, g, b, fadeAlpha(aEnd, topY, topY));
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private static void addStripVertex(BufferBuilder buffer, Matrix4f matrix,
                                       double x, double y, double z,
                                       int r, int g, int b, int a) {
        buffer.addVertex(matrix, (float) x, (float) y, (float) z).setColor(r, g, b, a);
    }

    private static void drawQuad(Matrix4f matrix,
                                 double x1, double y1, double z1,
                                 double x2, double y2, double z2,
                                 double x3, double y3, double z3,
                                 double x4, double y4, double z4,
                                 int r, int g, int b, int a) {
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buffer.addVertex(matrix, (float) x1, (float) y1, (float) z1).setColor(r, g, b, a);
        buffer.addVertex(matrix, (float) x2, (float) y2, (float) z2).setColor(r, g, b, a);
        buffer.addVertex(matrix, (float) x3, (float) y3, (float) z3).setColor(r, g, b, a);
        buffer.addVertex(matrix, (float) x4, (float) y4, (float) z4).setColor(r, g, b, a);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private static void drawQuadGradient(Matrix4f matrix,
                                         double x1, double y1, double z1,
                                         double x2, double y2, double z2,
                                         double x3, double y3, double z3,
                                         double x4, double y4, double z4,
                                         int r, int g, int b,
                                         int a1, int a2, int a3, int a4) {
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buffer.addVertex(matrix, (float) x1, (float) y1, (float) z1).setColor(r, g, b, a1);
        buffer.addVertex(matrix, (float) x2, (float) y2, (float) z2).setColor(r, g, b, a2);
        buffer.addVertex(matrix, (float) x3, (float) y3, (float) z3).setColor(r, g, b, a3);
        buffer.addVertex(matrix, (float) x4, (float) y4, (float) z4).setColor(r, g, b, a4);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private static int fadeAlpha(int alpha, double y, double topY) {
        double remaining = topY - y;
        if (remaining <= 0.0) {
            return 0;
        }
        double fade = Math.min(1.0, remaining / TOP_FADE_HEIGHT);
        int scaled = (int) Math.round(alpha * fade);
        if (scaled < 0) {
            return 0;
        }
        if (scaled > 255) {
            return 255;
        }
        return scaled;
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
}
