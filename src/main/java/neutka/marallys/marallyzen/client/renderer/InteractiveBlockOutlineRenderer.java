package neutka.marallys.marallyzen.client.renderer;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.blocks.DictaphoneSimpleBlock;
import neutka.marallys.marallyzen.blocks.InteractiveBlockTargeting;
import neutka.marallys.marallyzen.blocks.MarallyzenBlocks;
import neutka.marallys.marallyzen.blocks.PosterBlock;
import neutka.marallys.marallyzen.client.ClientDictaphoneManager;
import neutka.marallys.marallyzen.client.ClientPosterManager;
import neutka.marallys.marallyzen.client.renderer.PosterTextures;
import neutka.marallys.marallyzen.client.quest.QuestClientState;
import neutka.marallys.marallyzen.quest.QuestDefinition;
import neutka.marallys.marallyzen.quest.QuestCategoryColors;
import neutka.marallys.marallyzen.quest.QuestInstance;
import neutka.marallys.marallyzen.quest.QuestJsonUtils;
import neutka.marallys.marallyzen.quest.QuestStep;
import neutka.marallys.marallyzen.quest.QuestTriggerDef;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unified texture outline renderer for interactive blocks.
 */
@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class InteractiveBlockOutlineRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger(InteractiveBlockOutlineRenderer.class);

    private static final int OUTLINE_COLOR = 0xD48E03;
    private static final int OUTLINE_R = (OUTLINE_COLOR >> 16) & 0xFF;
    private static final int OUTLINE_G = (OUTLINE_COLOR >> 8) & 0xFF;
    private static final int OUTLINE_B = OUTLINE_COLOR & 0xFF;
    private static final int OUTLINE_A = 128;

    private static final float OUTLINE_THICKNESS = 0.03125f;
    private static final int ALPHA_THRESHOLD = 128;

    private static final int QUEST_SCAN_INTERVAL = 6;
    private static final double QUEST_SCAN_RADIUS = 15.0;
    private static final int QUEST_MAX_TARGETS = 5;
    private static final double QUEST_BAND_HEIGHT = 0.4;
    private static final double QUEST_BAND_THICKNESS = 0.04;
    private static final double QUEST_TOP_FADE_HEIGHT = 0.4;
    private static final double QUEST_WAVE_FREQUENCY = 0.45;
    private static final double QUEST_WAVE_SPEED = 2.2;
    private static final int QUEST_BASE_ALPHA = 18;
    private static final int QUEST_WAVE_ALPHA = 80;

    private static final ResourceLocation OLD_LAPTOP_TEX =
        ResourceLocation.fromNamespaceAndPath(Marallyzen.MODID, "textures/block/old_laptop.png");
    private static final ResourceLocation RADIO_TEX =
        ResourceLocation.fromNamespaceAndPath(Marallyzen.MODID, "textures/block/radio.png");
    private static final ResourceLocation OLD_TV_TEX =
        ResourceLocation.fromNamespaceAndPath(Marallyzen.MODID, "textures/block/old_tv.png");
    private static final ResourceLocation MIRROR_TEX =
        ResourceLocation.fromNamespaceAndPath(Marallyzen.MODID, "textures/block/mirror.png");
    private static final ResourceLocation DICTAPHONE_TEX =
        ResourceLocation.fromNamespaceAndPath(Marallyzen.MODID, "textures/block/dictaphone.png");
    private static final ResourceLocation DICTAPHONE_SIMPLE_TEX =
        ResourceLocation.fromNamespaceAndPath(Marallyzen.MODID, "textures/block/dictaphone_simple.png");

    private static final Map<ResourceLocation, List<EdgeSegment>> OUTLINE_CACHE = new HashMap<>();
    private static Target lastTarget = null;
    private static long lastQuestScanTick = -1;
    private static final List<AABB> cachedQuestAreas = new ArrayList<>();
    private static int cachedQuestColor = OUTLINE_COLOR;
    private static String cachedQuestId;
    private static String cachedStepId;

    private static final class EdgeSegment {
        final float x1;
        final float y1;
        final float x2;
        final float y2;

        EdgeSegment(float x1, float y1, float x2, float y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }
    }

    private enum OutlineKind {
        TEXTURE_ALPHA,
        BOUNDS,
        MODEL,
        MODEL_AND_TEXTURE
    }

    private record Bounds(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {}

    private record OutlineSpec(ResourceLocation texture, float width, float height, float offsetX, float offsetY,
                               OutlineKind kind, Bounds bounds, ResourceLocation model) {}

    private enum OutlineMode {
        POSTER,
        MIRROR,
        OLD_LAPTOP,
        RADIO,
        OLD_TV,
        VIDEO_CAMERA,
        CHAIN,
        DICTAPHONE,
        DICTAPHONE_SIMPLE
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

        // Quest area highlights disabled for now.
        // List<AABB> questAreas = getQuestAreas(mc);
        Target target = getTarget(mc);
        lastTarget = target;

        if (target == null) {
            return;
        }

        Camera camera = event.getCamera();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        // Quest area highlights disabled for now.
        // if (questAreas != null && !questAreas.isEmpty()) {
        //     renderQuestAreas(poseStack, bufferSource, camera, questAreas);
        // }
        if (target != null) {
            if (target.mode == OutlineMode.CHAIN) {
                renderChainOutline(poseStack, bufferSource, camera, target.pos, target.spec);
            } else {
                renderBlockOutline(poseStack, bufferSource, camera, target.pos, target.spec, target.mode, target.state);
            }
        }

        bufferSource.endBatch();
    }
    private static Target getTarget(Minecraft mc) {
        HitResult hitResult = mc.hitResult;
        if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) {
            return null;
        }

        BlockHitResult blockHit = (BlockHitResult) hitResult;
        return resolveTargetFromPos(mc, blockHit.getBlockPos());
    }

    private static Target resolveTargetFromPos(Minecraft mc, BlockPos pos) {
        BlockState state = mc.level.getBlockState(pos);
        if (state.getBlock() instanceof DictaphoneSimpleBlock
            && ClientDictaphoneManager.hasClientDictaphone(pos)) {
            return null;
        }
        if (state.getBlock() instanceof PosterBlock
            && ClientPosterManager.isPosterHidden(pos)) {
            return null;
        }
        InteractiveBlockTargeting.Type type = InteractiveBlockTargeting.getType(state);
        if (type == InteractiveBlockTargeting.Type.NONE) {
            return null;
        }

        OutlineMode mode = toOutlineMode(type);
        if (mode == null) {
            return null;
        }
        OutlineSpec spec = getOutlineSpec(mode, state);
        if (spec == null) {
            return null;
        }
        return new Target(pos, state, mode, spec);
    }

    private enum QuestHintType {
        POSTER_OLD,
        POSTER_PAPER,
        POSTER_REGULAR,
        DICTAPHONE,
        OLD_TV
    }

    private static List<AABB> getQuestAreas(Minecraft mc) {
        if (mc.level == null || mc.player == null) {
            return List.of();
        }
        QuestClientState questState = QuestClientState.getInstance();
        QuestDefinition definition = questState.getActiveDefinition();
        QuestInstance instance = questState.getActiveInstance();
        if (definition == null || instance == null || instance.state() != QuestInstance.State.ACTIVE) {
            return List.of();
        }
        QuestStep step = definition.getStep(instance.currentStepIndex());
        if (step == null || step.triggers() == null || step.triggers().isEmpty()) {
            return List.of();
        }
        List<QuestHintType> hintTypes = collectQuestHintTypes(step);
        if (hintTypes.isEmpty()) {
            return List.of();
        }

        String questId = definition.id();
        String stepId = step.id();
        if (cachedQuestId == null || cachedStepId == null
            || !cachedQuestId.equals(questId) || !cachedStepId.equals(stepId)) {
            cachedQuestId = questId;
            cachedStepId = stepId;
            cachedQuestAreas.clear();
        }

        long gameTime = mc.level.getGameTime();
        cachedQuestColor = QuestCategoryColors.getColor(definition.resolvedCategory());
        if (lastQuestScanTick >= 0 && gameTime - lastQuestScanTick < QUEST_SCAN_INTERVAL) {
            return cachedQuestAreas;
        }
        lastQuestScanTick = gameTime;
        cachedQuestAreas.clear();

        Vec3 focus = mc.player.position();
        AABB scanBox = new AABB(
            focus.x - QUEST_SCAN_RADIUS, focus.y - QUEST_SCAN_RADIUS, focus.z - QUEST_SCAN_RADIUS,
            focus.x + QUEST_SCAN_RADIUS, focus.y + QUEST_SCAN_RADIUS, focus.z + QUEST_SCAN_RADIUS
        );

        int minX = Mth.floor(scanBox.minX);
        int minY = Mth.floor(scanBox.minY);
        int minZ = Mth.floor(scanBox.minZ);
        int maxX = Mth.floor(scanBox.maxX);
        int maxY = Mth.floor(scanBox.maxY);
        int maxZ = Mth.floor(scanBox.maxZ);

        int found = 0;
        for (int y = minY; y <= maxY && found < QUEST_MAX_TARGETS; y++) {
            for (int x = minX; x <= maxX && found < QUEST_MAX_TARGETS; x++) {
                for (int z = minZ; z <= maxZ && found < QUEST_MAX_TARGETS; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = mc.level.getBlockState(pos);
                    if (!matchesQuestHint(state, hintTypes)) {
                        continue;
                    }
                    addQuestArea(mc, pos, state);
                    found++;
                }
            }
        }
        return cachedQuestAreas;
    }

    private static List<QuestHintType> collectQuestHintTypes(QuestStep step) {
        List<QuestHintType> types = new ArrayList<>();
        for (QuestTriggerDef trigger : step.triggers()) {
            if (trigger == null || trigger.type() == null) {
                continue;
            }
            if (!"custom".equals(trigger.type())) {
                continue;
            }
            String eventId = QuestJsonUtils.getString(trigger.data(), "eventId", "");
            if ("poster_old".equals(eventId)) {
                types.add(QuestHintType.POSTER_OLD);
            } else if ("poster_paper".equals(eventId)) {
                types.add(QuestHintType.POSTER_PAPER);
            } else if ("poster".equals(eventId)) {
                types.add(QuestHintType.POSTER_REGULAR);
            } else if ("dictaphone_finished".equals(eventId)) {
                types.add(QuestHintType.DICTAPHONE);
            } else if ("tv_on".equals(eventId) || "tv_off".equals(eventId)) {
                types.add(QuestHintType.OLD_TV);
            }
        }
        return types;
    }

    private static boolean matchesQuestHint(BlockState state, List<QuestHintType> hintTypes) {
        if (state == null || hintTypes == null || hintTypes.isEmpty()) {
            return false;
        }
        Block block = state.getBlock();
        for (QuestHintType type : hintTypes) {
            if (type == QuestHintType.OLD_TV && block == MarallyzenBlocks.OLD_TV.get()) {
                return true;
            }
            if (type == QuestHintType.DICTAPHONE && (block == MarallyzenBlocks.DICTAPHONE.get()
                || block == MarallyzenBlocks.DICTAPHONE_SIMPLE.get())) {
                return true;
            }
            if (type == QuestHintType.POSTER_OLD && block instanceof PosterBlock posterBlock) {
                if (posterBlock.getPosterNumber() == 11) {
                    return true;
                }
            }
            if (type == QuestHintType.POSTER_PAPER && block instanceof PosterBlock posterBlock) {
                int number = posterBlock.getPosterNumber();
                if (number == 12 || number == 13) {
                    return true;
                }
            }
            if (type == QuestHintType.POSTER_REGULAR && block instanceof PosterBlock posterBlock) {
                int number = posterBlock.getPosterNumber();
                if (number >= 1 && number <= 10) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void addQuestArea(Minecraft mc, BlockPos pos, BlockState state) {
        double baseY = pos.getY();
        if (state != null && state.getBlock() instanceof PosterBlock) {
            baseY = questSurfaceY(mc, pos);
        }
        double minX = pos.getX() - 1.0;
        double minZ = pos.getZ() - 1.0;
        double maxX = pos.getX() + 2.0;
        double maxZ = pos.getZ() + 2.0;
        double minY = baseY;
        double maxY = baseY + 1.0;
        AABB area = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        mergeQuestArea(area);
    }

    private static void mergeQuestArea(AABB area) {
        final double eps = 1.0e-6;
        for (int i = 0; i < cachedQuestAreas.size(); i++) {
            AABB existing = cachedQuestAreas.get(i);
            if (touchesOrIntersects(existing, area, eps)) {
                cachedQuestAreas.set(i, existing.minmax(area));
                return;
            }
        }
        cachedQuestAreas.add(area);
    }

    private static boolean touchesOrIntersects(AABB a, AABB b, double eps) {
        boolean overlapX = a.maxX >= b.minX - eps && b.maxX >= a.minX - eps;
        boolean overlapY = a.maxY >= b.minY - eps && b.maxY >= a.minY - eps;
        boolean overlapZ = a.maxZ >= b.minZ - eps && b.maxZ >= a.minZ - eps;
        return overlapX && overlapY && overlapZ;
    }

    private static void renderQuestAreas(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
                                         Camera camera, List<AABB> areas) {
        if (areas == null || areas.isEmpty()) {
            return;
        }
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
        int r = (cachedQuestColor >> 16) & 0xFF;
        int g = (cachedQuestColor >> 8) & 0xFF;
        int b = cachedQuestColor & 0xFF;
        float time = (float) (net.minecraft.Util.getMillis() / 1000.0);

        for (AABB area : areas) {
            double minX = area.minX;
            double maxX = area.maxX;
            double minZ = area.minZ;
            double maxZ = area.maxZ;
            double baseY = area.minY;
            double topY = Math.min(baseY + QUEST_BAND_HEIGHT, area.maxY);
            if (topY <= baseY) {
                continue;
            }
            double maxEdge = Math.max(maxX - minX, maxZ - minZ);
            double segment = Math.max(0.75, maxEdge / 96.0);
            renderQuestEdgeX(matrix, minX, maxX, minZ, baseY, topY, r, g, b, time, segment);
            renderQuestEdgeX(matrix, minX, maxX, maxZ, baseY, topY, r, g, b, time, segment);
            renderQuestEdgeZ(matrix, minZ, maxZ, minX, baseY, topY, r, g, b, time, segment);
            renderQuestEdgeZ(matrix, minZ, maxZ, maxX, baseY, topY, r, g, b, time, segment);
            addQuestCorner(matrix, minX, minZ, baseY, topY, r, g, b, time, -1, -1);
            addQuestCorner(matrix, minX, maxZ, baseY, topY, r, g, b, time, -1, 1);
            addQuestCorner(matrix, maxX, minZ, baseY, topY, r, g, b, time, 1, -1);
            addQuestCorner(matrix, maxX, maxZ, baseY, topY, r, g, b, time, 1, 1);
        }

        poseStack.popPose();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.disableDepthTest();
        RenderSystem.enableCull();
    }

    private static double questSurfaceY(Minecraft mc, BlockPos pos) {
        if (mc == null || mc.level == null) {
            return pos.getY();
        }
        if (!mc.level.hasChunkAt(pos)) {
            return pos.getY();
        }
        int minY = mc.level.getMinBuildHeight();
        int startY = pos.getY() - 1;
        int endY = Math.max(minY, pos.getY() - 12);
        BlockPos.MutableBlockPos scan = new BlockPos.MutableBlockPos();
        for (int y = startY; y >= endY; y--) {
            scan.set(pos.getX(), y, pos.getZ());
            BlockState state = mc.level.getBlockState(scan);
            if (!state.isAir() || !state.getFluidState().isEmpty()) {
                return questBlockSurfaceY(mc, scan, state);
            }
        }
        return pos.getY();
    }

    private static double questBlockSurfaceY(Minecraft mc, BlockPos pos, BlockState state) {
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

    private static void renderQuestEdgeX(Matrix4f matrix,
                                         double minX, double maxX, double z,
                                         double baseY, double topY,
                                         int r, int g, int b, float time, double segment) {
        double half = QUEST_BAND_THICKNESS * 0.5;
        renderQuestStripX(matrix, minX, maxX, z - half, baseY, topY, r, g, b, time, segment);
        renderQuestStripX(matrix, minX, maxX, z + half, baseY, topY, r, g, b, time, segment);
    }

    private static void renderQuestEdgeZ(Matrix4f matrix,
                                         double minZ, double maxZ, double x,
                                         double baseY, double topY,
                                         int r, int g, int b, float time, double segment) {
        double half = QUEST_BAND_THICKNESS * 0.5;
        renderQuestStripZ(matrix, minZ, maxZ, x - half, baseY, topY, r, g, b, time, segment);
        renderQuestStripZ(matrix, minZ, maxZ, x + half, baseY, topY, r, g, b, time, segment);
    }

    private static void renderQuestStripX(Matrix4f matrix, double minX, double maxX, double z,
                                          double baseY, double topY,
                                          int r, int g, int b, float time, double segment) {
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        double x = minX;
        while (x < maxX) {
            int a = questWaveAlpha(x, z, time);
            addQuestStripVertex(buffer, matrix, x, baseY, z, r, g, b, questFadeAlpha(a, baseY, topY));
            addQuestStripVertex(buffer, matrix, x, topY, z, r, g, b, questFadeAlpha(a, topY, topY));
            x += segment;
        }
        int aEnd = questWaveAlpha(maxX, z, time);
        addQuestStripVertex(buffer, matrix, maxX, baseY, z, r, g, b, questFadeAlpha(aEnd, baseY, topY));
        addQuestStripVertex(buffer, matrix, maxX, topY, z, r, g, b, questFadeAlpha(aEnd, topY, topY));
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private static void renderQuestStripZ(Matrix4f matrix, double minZ, double maxZ, double x,
                                          double baseY, double topY,
                                          int r, int g, int b, float time, double segment) {
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        double z = minZ;
        while (z < maxZ) {
            int a = questWaveAlpha(x, z, time);
            addQuestStripVertex(buffer, matrix, x, baseY, z, r, g, b, questFadeAlpha(a, baseY, topY));
            addQuestStripVertex(buffer, matrix, x, topY, z, r, g, b, questFadeAlpha(a, topY, topY));
            z += segment;
        }
        int aEnd = questWaveAlpha(x, maxZ, time);
        addQuestStripVertex(buffer, matrix, x, baseY, maxZ, r, g, b, questFadeAlpha(aEnd, baseY, topY));
        addQuestStripVertex(buffer, matrix, x, topY, maxZ, r, g, b, questFadeAlpha(aEnd, topY, topY));
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private static void addQuestCorner(Matrix4f matrix,
                                       double x, double z, double baseY, double topY,
                                       int r, int g, int b, float time,
                                       int xSign, int zSign) {
        double half = QUEST_BAND_THICKNESS * 0.5;
        double xOffset = x + xSign * half;
        double zOffset = z + zSign * half;
        int a = questWaveAlpha(x, z, time);
        int aTop = questFadeAlpha(a, topY, topY);
        drawQuestQuadGradient(matrix,
            xOffset, baseY, z,
            x, baseY, zOffset,
            x, topY, zOffset,
            xOffset, topY, z,
            r, g, b,
            a, a, aTop, aTop);
    }

    private static void addQuestStripVertex(BufferBuilder buffer, Matrix4f matrix,
                                            double x, double y, double z,
                                            int r, int g, int b, int a) {
        buffer.addVertex(matrix, (float) x, (float) y, (float) z).setColor(r, g, b, a);
    }

    private static void drawQuestQuadGradient(Matrix4f matrix,
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

    private static int questWaveAlpha(double coord, double other, float time) {
        double phase = (coord * QUEST_WAVE_FREQUENCY) + (other * 0.12) + (time * QUEST_WAVE_SPEED);
        double wave = (Math.sin(phase) * 0.5) + 0.5;
        int a = (int) (QUEST_BASE_ALPHA + wave * QUEST_WAVE_ALPHA);
        if (a < 0) {
            return 0;
        }
        if (a > 255) {
            return 255;
        }
        return a;
    }

    private static int questFadeAlpha(int alpha, double y, double topY) {
        double remaining = topY - y;
        if (remaining <= 0.0) {
            return 0;
        }
        double fade = Math.min(1.0, remaining / QUEST_TOP_FADE_HEIGHT);
        int scaled = (int) Math.round(alpha * fade);
        if (scaled < 0) {
            return 0;
        }
        if (scaled > 255) {
            return 255;
        }
        return scaled;
    }

    private static OutlineMode toOutlineMode(InteractiveBlockTargeting.Type type) {
        return switch (type) {
            case POSTER -> OutlineMode.POSTER;
            case MIRROR -> OutlineMode.MIRROR;
            case OLD_LAPTOP -> OutlineMode.OLD_LAPTOP;
            case RADIO -> OutlineMode.RADIO;
            case OLD_TV -> OutlineMode.OLD_TV;
            case VIDEO_CAMERA -> null;
            case CHAIN -> OutlineMode.CHAIN;
            case DICTAPHONE -> OutlineMode.DICTAPHONE;
            case DICTAPHONE_SIMPLE -> OutlineMode.DICTAPHONE_SIMPLE;
            case NONE -> null;
        };
    }

    private record Target(BlockPos pos, BlockState state, OutlineMode mode, OutlineSpec spec) {}

    private static OutlineSpec getOutlineSpec(OutlineMode mode, BlockState state) {
        Block block = state.getBlock();
        if (mode == OutlineMode.POSTER && block instanceof PosterBlock posterBlock) {
            int posterNumber = posterBlock.getPosterNumber();
            ResourceLocation tex = PosterTextures.getSmallTexture(posterNumber);
            return new OutlineSpec(tex, 10.0f / 16.0f, 15.0f / 16.0f, 3.0f / 16.0f, 0.0f,
                OutlineKind.TEXTURE_ALPHA, null, null);
        }
        if (mode == OutlineMode.MIRROR && block == MarallyzenBlocks.MIRROR.get()) {
            return new OutlineSpec(MIRROR_TEX, 0.0f, 0.0f, 0.0f, 0.0f,
                OutlineKind.MODEL, null, ResourceLocation.fromNamespaceAndPath(Marallyzen.MODID, "models/block/mirror.json"));
        }
        if (mode == OutlineMode.OLD_LAPTOP && block == MarallyzenBlocks.OLD_LAPTOP.get()) {
            return new OutlineSpec(OLD_LAPTOP_TEX, 0.0f, 0.0f, 0.0f, 0.0f,
                OutlineKind.MODEL, null, ResourceLocation.fromNamespaceAndPath(Marallyzen.MODID, "models/block/old_laptop.json"));
        }
        if (mode == OutlineMode.RADIO && block == MarallyzenBlocks.RADIO.get()) {
            return new OutlineSpec(RADIO_TEX, 0.0f, 0.0f, 0.0f, 0.0f,
                OutlineKind.MODEL, null, ResourceLocation.fromNamespaceAndPath(Marallyzen.MODID, "models/block/radio.json"));
        }
        if (mode == OutlineMode.OLD_TV && block == MarallyzenBlocks.OLD_TV.get()) {
            return new OutlineSpec(OLD_TV_TEX, 0.0f, 0.0f, 0.0f, 0.0f,
                OutlineKind.MODEL, null, ResourceLocation.fromNamespaceAndPath(Marallyzen.MODID, "models/block/old_tv.json"));
        }
        if (mode == OutlineMode.CHAIN && block == MarallyzenBlocks.INTERACTIVE_CHAIN.get()) {
            return new OutlineSpec(
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/chain.png"),
                1.0f, 1.0f, 0.0f, 0.0f,
                OutlineKind.MODEL_AND_TEXTURE,
                null,
                ResourceLocation.fromNamespaceAndPath("minecraft", "models/block/chain.json"));
        }
        if (mode == OutlineMode.DICTAPHONE && block == MarallyzenBlocks.DICTAPHONE.get()) {
            return new OutlineSpec(DICTAPHONE_TEX, 0.0f, 0.0f, 0.0f, 0.0f,
                OutlineKind.MODEL, null, ResourceLocation.fromNamespaceAndPath(Marallyzen.MODID, "models/block/dictaphone.json"));
        }
        if (mode == OutlineMode.DICTAPHONE_SIMPLE && block == MarallyzenBlocks.DICTAPHONE_SIMPLE.get()) {
            boolean showFull = state.hasProperty(DictaphoneSimpleBlock.SHOW)
                && state.getValue(DictaphoneSimpleBlock.SHOW);
            ResourceLocation model = showFull
                ? ResourceLocation.fromNamespaceAndPath(Marallyzen.MODID, "models/block/dictaphone.json")
                : ResourceLocation.fromNamespaceAndPath(Marallyzen.MODID, "models/block/dictaphone_simple.json");
            ResourceLocation texture = showFull ? DICTAPHONE_TEX : DICTAPHONE_SIMPLE_TEX;
            return new OutlineSpec(texture, 0.0f, 0.0f, 0.0f, 0.0f, OutlineKind.MODEL, null, model);
        }
        return null;
    }

    private static void renderBlockOutline(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
                                           Camera camera, BlockPos pos, OutlineSpec spec, OutlineMode mode, BlockState state) {
        poseStack.pushPose();

        double camX = camera.getPosition().x;
        double camY = camera.getPosition().y;
        double camZ = camera.getPosition().z;
        poseStack.translate(pos.getX() - camX, pos.getY() - camY, pos.getZ() - camZ);

        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.lines());

        boolean chainTopCap = false;
        boolean chainBottomCap = false;
        if (mode == OutlineMode.CHAIN) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                chainTopCap = mc.level.getBlockState(pos.above()).getBlock() != MarallyzenBlocks.INTERACTIVE_CHAIN.get();
                chainBottomCap = mc.level.getBlockState(pos.below()).getBlock() != MarallyzenBlocks.INTERACTIVE_CHAIN.get();
            }
        }

        if (spec.kind == OutlineKind.BOUNDS && spec.bounds != null) {
            renderBoundsOutline(vertexConsumer, matrix, spec.bounds);
        } else if ((spec.kind == OutlineKind.MODEL || spec.kind == OutlineKind.MODEL_AND_TEXTURE) && spec.model != null) {
            if (mode == OutlineMode.DICTAPHONE_SIMPLE) {
                renderModelOutlineFromQuads(state, vertexConsumer, matrix);
            } else {
                List<EdgeSegment3D> modelEdges = getOrBuildModelOutline(spec.model);
                if (!modelEdges.isEmpty()) {
                    Direction[] facings = getRenderFacings(mode, state);
                    for (Direction facing : facings) {
                        for (EdgeSegment3D edge : modelEdges) {
                            if (mode == OutlineMode.CHAIN) {
                                continue;
                            }
                            if (shouldSkipChainBoundary(edge, chainTopCap, chainBottomCap)) {
                                continue;
                            }
                            EdgeSegment3D rotated = rotateEdgeForFacing(edge, facing);
                            renderLine(vertexConsumer, matrix,
                                rotated.x1, rotated.y1, rotated.z1,
                                rotated.x2, rotated.y2, rotated.z2);
                        }
                    }
                }
            }
        }
        if (spec.kind == OutlineKind.TEXTURE_ALPHA || spec.kind == OutlineKind.MODEL_AND_TEXTURE) {
            List<EdgeSegment> outline = getOrBuildOutline(spec.texture);
            if (outline.isEmpty()) {
                poseStack.popPose();
                return;
            }

            if (mode == OutlineMode.CHAIN) {
                renderTextureOutlineOnModelQuads(state, outline, vertexConsumer, matrix, chainTopCap, chainBottomCap);
                poseStack.popPose();
                return;
            }

            float scaleX = spec.width / 16.0f;
            float scaleY = spec.height / 16.0f;
            float offsetX = spec.offsetX;
            float offsetY = spec.offsetY;

            float rightEdge = offsetX + spec.width;
            float topEdge = offsetY + spec.height;

            Direction[] facings = getRenderFacings(mode, state);
            for (Direction facing : facings) {
                float zPos = switch (facing) {
                    case NORTH -> 1.0f - OUTLINE_THICKNESS;
                    case SOUTH -> OUTLINE_THICKNESS;
                    case EAST -> OUTLINE_THICKNESS;
                    case WEST -> 1.0f - OUTLINE_THICKNESS;
                    default -> 1.0f - OUTLINE_THICKNESS;
                };

                boolean mirrorHorizontal = (facing == Direction.NORTH || facing == Direction.EAST);

                for (EdgeSegment edge : outline) {
                    float texX1 = edge.x1 * scaleX;
                    float texY1 = edge.y1 * scaleY;
                    float texX2 = edge.x2 * scaleX;
                    float texY2 = edge.y2 * scaleY;

                    float x1 = mirrorHorizontal ? rightEdge - texX1 : offsetX + texX1;
                    float x2 = mirrorHorizontal ? rightEdge - texX2 : offsetX + texX2;
                    float y1 = topEdge - texY1;
                    float y2 = topEdge - texY2;

                    if (facing == Direction.NORTH || facing == Direction.SOUTH) {
                        renderLine(vertexConsumer, matrix, x1, y1, zPos, x2, y2, zPos);
                    } else {
                        renderLine(vertexConsumer, matrix, zPos, y1, x1, zPos, y2, x2);
                    }
                }
            }
        }

        poseStack.popPose();
    }

    private static void renderChainOutline(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
                                           Camera camera, BlockPos pos, OutlineSpec spec) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        BlockPos root = pos;
        while (mc.level.getBlockState(root.above()).getBlock() == MarallyzenBlocks.INTERACTIVE_CHAIN.get()) {
            root = root.above();
        }

        List<EdgeSegment> outline = getOrBuildOutline(spec.texture);
        List<EdgeSegment3D> modelEdges = getOrBuildModelOutline(spec.model);
        if (outline.isEmpty() && modelEdges.isEmpty()) {
            return;
        }

        BlockPos current = root;
        while (mc.level.getBlockState(current).getBlock() == MarallyzenBlocks.INTERACTIVE_CHAIN.get()) {
            BlockState state = mc.level.getBlockState(current);
            renderBlockOutline(poseStack, bufferSource, camera, current, spec, OutlineMode.CHAIN, state);
            current = current.below();
        }
    }

    private static Direction[] getRenderFacings(OutlineMode mode, BlockState state) {
        if (mode == OutlineMode.POSTER && state.getBlock() instanceof PosterBlock) {
            return new Direction[] { state.getValue(PosterBlock.FACING) };
        }
        if (state.hasProperty(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING)) {
            return new Direction[] { state.getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING) };
        }
        return new Direction[] { Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST };
    }

    private static void renderBoundsOutline(VertexConsumer consumer, Matrix4f matrix, Bounds bounds) {
        float minX = bounds.minX / 16.0f;
        float minY = bounds.minY / 16.0f;
        float minZ = bounds.minZ / 16.0f;
        float maxX = bounds.maxX / 16.0f;
        float maxY = bounds.maxY / 16.0f;
        float maxZ = bounds.maxZ / 16.0f;

        // Bottom rectangle
        renderLine(consumer, matrix, minX, minY, minZ, maxX, minY, minZ);
        renderLine(consumer, matrix, maxX, minY, minZ, maxX, minY, maxZ);
        renderLine(consumer, matrix, maxX, minY, maxZ, minX, minY, maxZ);
        renderLine(consumer, matrix, minX, minY, maxZ, minX, minY, minZ);

        // Top rectangle
        renderLine(consumer, matrix, minX, maxY, minZ, maxX, maxY, minZ);
        renderLine(consumer, matrix, maxX, maxY, minZ, maxX, maxY, maxZ);
        renderLine(consumer, matrix, maxX, maxY, maxZ, minX, maxY, maxZ);
        renderLine(consumer, matrix, minX, maxY, maxZ, minX, maxY, minZ);

        // Vertical edges
        renderLine(consumer, matrix, minX, minY, minZ, minX, maxY, minZ);
        renderLine(consumer, matrix, maxX, minY, minZ, maxX, maxY, minZ);
        renderLine(consumer, matrix, maxX, minY, maxZ, maxX, maxY, maxZ);
        renderLine(consumer, matrix, minX, minY, maxZ, minX, maxY, maxZ);
    }

    private static void renderTextureOutlineOnModelQuads(BlockState state, List<EdgeSegment> outline,
                                                         VertexConsumer consumer, Matrix4f matrix,
                                                         boolean chainTopCap, boolean chainBottomCap) {
        Minecraft mc = Minecraft.getInstance();
        BakedModel model = mc.getBlockRenderer().getBlockModel(state);
        boolean isChain = state.getBlock() == MarallyzenBlocks.INTERACTIVE_CHAIN.get();

        for (RenderType renderType : model.getRenderTypes(state, RandomSource.create(0), ModelData.EMPTY)) {
            renderModelQuads(model.getQuads(state, null, RandomSource.create(0), ModelData.EMPTY, renderType),
                outline, consumer, matrix);
            for (Direction dir : Direction.values()) {
                if (isChain && ((dir == Direction.UP && !chainTopCap) || (dir == Direction.DOWN && !chainBottomCap))) {
                    continue;
                }
                renderModelQuads(model.getQuads(state, dir, RandomSource.create(0), ModelData.EMPTY, renderType),
                    outline, consumer, matrix);
            }
        }
    }

    private static void renderModelQuads(List<BakedQuad> quads, List<EdgeSegment> outline,
                                         VertexConsumer consumer, Matrix4f matrix) {
        if (quads == null || quads.isEmpty()) {
            return;
        }
        for (BakedQuad quad : quads) {
            TextureAtlasSprite sprite = quad.getSprite();
            if (sprite == null) {
                continue;
            }

            QuadMapping mapping = QuadMapping.fromQuad(quad);
            if (mapping == null) {
                for (EdgeSegment edge : outline) {
                    float u1 = sprite.getU(edge.x1);
                    float v1 = sprite.getV(edge.y1);
                    float u2 = sprite.getU(edge.x2);
                    float v2 = sprite.getV(edge.y2);

                    float[] p1 = mapUvToPosFallback(quad, u1, v1);
                    float[] p2 = mapUvToPosFallback(quad, u2, v2);
                    if (p1 == null || p2 == null) {
                        continue;
                    }
                    renderLine(consumer, matrix, p1[0], p1[1], p1[2], p2[0], p2[1], p2[2]);
                }
                continue;
            }

            for (EdgeSegment edge : outline) {
                float u1 = sprite.getU(edge.x1);
                float v1 = sprite.getV(edge.y1);
                float u2 = sprite.getU(edge.x2);
                float v2 = sprite.getV(edge.y2);

                float[] p1 = mapping.mapUvToPos(u1, v1);
                float[] p2 = mapping.mapUvToPos(u2, v2);
                if (p1 == null || p2 == null) {
                    continue;
                }

                renderLine(consumer, matrix, p1[0], p1[1], p1[2], p2[0], p2[1], p2[2]);
            }
        }
    }

    private static final float UV_EPSILON = 1.0e-4f;

    private static final class QuadMapping {
        private final float px0;
        private final float py0;
        private final float pz0;
        private final float px1;
        private final float py1;
        private final float pz1;
        private final float px3;
        private final float py3;
        private final float pz3;
        private final float u0;
        private final float v0;
        private final float du1x;
        private final float du1y;
        private final float du2x;
        private final float du2y;
        private final float det;

        private QuadMapping(float px0, float py0, float pz0,
                             float px1, float py1, float pz1,
                             float px3, float py3, float pz3,
                             float u0, float v0,
                             float du1x, float du1y,
                             float du2x, float du2y) {
            this.px0 = px0;
            this.py0 = py0;
            this.pz0 = pz0;
            this.px1 = px1;
            this.py1 = py1;
            this.pz1 = pz1;
            this.px3 = px3;
            this.py3 = py3;
            this.pz3 = pz3;
            this.u0 = u0;
            this.v0 = v0;
            this.du1x = du1x;
            this.du1y = du1y;
            this.du2x = du2x;
            this.du2y = du2y;
            this.det = du1x * du2y - du1y * du2x;
        }

        static QuadMapping fromQuad(BakedQuad quad) {
            int[] data = quad.getVertices();
            if (data.length < 32) {
                return null;
            }

            Vertex v0 = Vertex.from(data, 0);
            Vertex v1 = Vertex.from(data, 1);
            Vertex v3 = Vertex.from(data, 3);

            float du1x = v1.u - v0.u;
            float du1y = v1.v - v0.v;
            float du2x = v3.u - v0.u;
            float du2y = v3.v - v0.v;

            float det = du1x * du2y - du1y * du2x;
            if (Math.abs(det) < UV_EPSILON) {
                return null;
            }

            return new QuadMapping(
                v0.x, v0.y, v0.z,
                v1.x, v1.y, v1.z,
                v3.x, v3.y, v3.z,
                v0.u, v0.v,
                du1x, du1y,
                du2x, du2y
            );
        }

        float[] mapUvToPos(float u, float v) {
            float dux = u - u0;
            float duy = v - v0;
            float detInv = 1.0f / det;
            float s = (dux * du2y - duy * du2x) * detInv;
            float t = (du1x * duy - du1y * dux) * detInv;
            if (s < -UV_EPSILON || s > 1.0f + UV_EPSILON || t < -UV_EPSILON || t > 1.0f + UV_EPSILON) {
                return null;
            }

            float x = px0 + (px1 - px0) * s + (px3 - px0) * t;
            float y = py0 + (py1 - py0) * s + (py3 - py0) * t;
            float z = pz0 + (pz1 - pz0) * s + (pz3 - pz0) * t;
            return new float[] { x, y, z };
        }
    }

    private static float[] mapUvToPosFallback(BakedQuad quad, float u, float v) {
        int[] data = quad.getVertices();
        if (data.length < 32) {
            return null;
        }
        Vertex v0 = Vertex.from(data, 0);
        Vertex v1 = Vertex.from(data, 1);
        Vertex v2 = Vertex.from(data, 2);
        Vertex v3 = Vertex.from(data, 3);

        Vertex uAxis = pickAxisVertex(v0, v1, v2, v3, true);
        Vertex vAxis = pickAxisVertex(v0, v1, v2, v3, false);
        if (uAxis == null || vAxis == null) {
            return null;
        }

        float denomU = uAxis.u - v0.u;
        float denomV = vAxis.v - v0.v;
        if (Math.abs(denomU) < UV_EPSILON || Math.abs(denomV) < UV_EPSILON) {
            return null;
        }

        float s = (u - v0.u) / denomU;
        float t = (v - v0.v) / denomV;
        if (s < -UV_EPSILON || s > 1.0f + UV_EPSILON || t < -UV_EPSILON || t > 1.0f + UV_EPSILON) {
            return null;
        }

        float x = v0.x + (uAxis.x - v0.x) * s + (vAxis.x - v0.x) * t;
        float y = v0.y + (uAxis.y - v0.y) * s + (vAxis.y - v0.y) * t;
        float z = v0.z + (uAxis.z - v0.z) * s + (vAxis.z - v0.z) * t;
        return new float[] { x, y, z };
    }

    private static Vertex pickAxisVertex(Vertex v0, Vertex v1, Vertex v2, Vertex v3, boolean pickU) {
        Vertex best = null;
        float bestDelta = 0.0f;
        for (Vertex v : new Vertex[] { v1, v2, v3 }) {
            float delta = pickU ? Math.abs(v.u - v0.u) : Math.abs(v.v - v0.v);
            if (delta > bestDelta) {
                bestDelta = delta;
                best = v;
            }
        }
        return best;
    }

    private static final class Vertex {
        private static final int STRIDE = 8;

        final float x;
        final float y;
        final float z;
        final float u;
        final float v;

        private Vertex(float x, float y, float z, float u, float v) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.u = u;
            this.v = v;
        }

        static Vertex from(int[] data, int index) {
            int base = index * STRIDE;
            float x = Float.intBitsToFloat(data[base]);
            float y = Float.intBitsToFloat(data[base + 1]);
            float z = Float.intBitsToFloat(data[base + 2]);
            float u = Float.intBitsToFloat(data[base + 4]);
            float v = Float.intBitsToFloat(data[base + 5]);
            return new Vertex(x, y, z, u, v);
        }
    }

    private record EdgeSegment3D(float x1, float y1, float z1, float x2, float y2, float z2) {}

    private static final Map<ResourceLocation, List<EdgeSegment3D>> MODEL_OUTLINE_CACHE = new HashMap<>();

    private static List<EdgeSegment3D> getOrBuildModelOutline(ResourceLocation modelLoc) {
        return MODEL_OUTLINE_CACHE.computeIfAbsent(modelLoc, InteractiveBlockOutlineRenderer::buildModelOutline);
    }

    private static List<EdgeSegment3D> buildModelOutline(ResourceLocation modelLoc) {
        List<EdgeSegment3D> edges = new ArrayList<>();
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getResourceManager() == null) {
                LOGGER.warn("ResourceManager not available, cannot load model {}", modelLoc);
                return edges;
            }

            Resource resource = mc.getResourceManager().getResource(modelLoc).orElse(null);
            if (resource == null) {
                LOGGER.warn("Model not found: {}", modelLoc);
                return edges;
            }

            try (InputStream stream = resource.open()) {
                JsonObject root = JsonParser.parseReader(new java.io.InputStreamReader(stream)).getAsJsonObject();
                JsonArray elements = root.getAsJsonArray("elements");
                if (elements == null) {
                    return edges;
                }

                for (JsonElement element : elements) {
                    JsonObject obj = element.getAsJsonObject();
                    float[] from = readVec3(obj.getAsJsonArray("from"));
                    float[] to = readVec3(obj.getAsJsonArray("to"));
                    float[][] corners = buildCorners(from, to);

                    if (obj.has("rotation")) {
                        JsonObject rot = obj.getAsJsonObject("rotation");
                        float angle = rot.get("angle").getAsFloat();
                        String axis = rot.get("axis").getAsString();
                        float[] origin = readVec3(rot.getAsJsonArray("origin"));
                        for (int i = 0; i < corners.length; i++) {
                            corners[i] = rotatePoint(corners[i], origin, axis, angle);
                        }
                    }

                    int[][] edgeIndices = {
                        {0, 1}, {1, 2}, {2, 3}, {3, 0},
                        {4, 5}, {5, 6}, {6, 7}, {7, 4},
                        {0, 4}, {1, 5}, {2, 6}, {3, 7}
                    };

                    for (int[] e : edgeIndices) {
                        float[] a = corners[e[0]];
                        float[] b = corners[e[1]];
                        edges.add(new EdgeSegment3D(
                            a[0] / 16.0f, a[1] / 16.0f, a[2] / 16.0f,
                            b[0] / 16.0f, b[1] / 16.0f, b[2] / 16.0f));
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error building model outline for {}: ", modelLoc, e);
        }
        return edges;
    }

    private static ResourceLocation getPosterModelLocation(int posterNumber) {
        String modelName = switch (posterNumber) {
            case 1 -> "poster1";
            case 2 -> "poster2";
            case 3 -> "poster3";
            case 4 -> "poster4";
            case 5 -> "poster5";
            case 6 -> "poster6";
            case 7 -> "poster7";
            case 8 -> "poster8";
            case 9 -> "poster9";
            case 10 -> "poster10";
            case 11 -> "oldposter";
            case 12 -> "paperposter1";
            case 13 -> "paperposter2";
            default -> "poster1";
        };
        return ResourceLocation.fromNamespaceAndPath(Marallyzen.MODID, "models/block/" + modelName + ".json");
    }

    private static float[] readVec3(JsonArray array) {
        return new float[] { array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat() };
    }

    private static float[][] buildCorners(float[] from, float[] to) {
        return new float[][] {
            {from[0], from[1], from[2]},
            {to[0], from[1], from[2]},
            {to[0], to[1], from[2]},
            {from[0], to[1], from[2]},
            {from[0], from[1], to[2]},
            {to[0], from[1], to[2]},
            {to[0], to[1], to[2]},
            {from[0], to[1], to[2]}
        };
    }

    private static float[] rotatePoint(float[] p, float[] origin, String axis, float angleDeg) {
        double angle = Math.toRadians(angleDeg);
        double x = p[0] - origin[0];
        double y = p[1] - origin[1];
        double z = p[2] - origin[2];

        double rx = x;
        double ry = y;
        double rz = z;

        if ("x".equals(axis)) {
            ry = y * Math.cos(angle) - z * Math.sin(angle);
            rz = y * Math.sin(angle) + z * Math.cos(angle);
        } else if ("y".equals(axis)) {
            rx = x * Math.cos(angle) + z * Math.sin(angle);
            rz = -x * Math.sin(angle) + z * Math.cos(angle);
        } else if ("z".equals(axis)) {
            rx = x * Math.cos(angle) - y * Math.sin(angle);
            ry = x * Math.sin(angle) + y * Math.cos(angle);
        }

        return new float[] {
            (float) (rx + origin[0]),
            (float) (ry + origin[1]),
            (float) (rz + origin[2])
        };
    }

    private static EdgeSegment3D rotateEdgeForFacing(EdgeSegment3D edge, Direction facing) {
        float[] a = rotatePointY(edge.x1, edge.y1, edge.z1, facing);
        float[] b = rotatePointY(edge.x2, edge.y2, edge.z2, facing);
        return new EdgeSegment3D(a[0], a[1], a[2], b[0], b[1], b[2]);
    }

    private static boolean shouldSkipChainBoundary(EdgeSegment3D edge, boolean keepTop, boolean keepBottom) {
        boolean top = isTopY(edge.y1) && isTopY(edge.y2);
        boolean bottom = isBottomY(edge.y1) && isBottomY(edge.y2);
        if (top && !keepTop) {
            return true;
        }
        if (bottom && !keepBottom) {
            return true;
        }
        return false;
    }

    private static boolean isTopY(float y) {
        return Math.abs(y - 1.0f) < 1.0e-4f;
    }

    private static boolean isBottomY(float y) {
        return Math.abs(y) < 1.0e-4f;
    }

    private static boolean isOuterPlaneEdge(EdgeSegment3D edge) {
        return (isPlane(edge.x1, edge.x2, 0.0f) || isPlane(edge.x1, edge.x2, 1.0f)
            || isPlane(edge.z1, edge.z2, 0.0f) || isPlane(edge.z1, edge.z2, 1.0f));
    }

    private static boolean isPlane(float a, float b, float target) {
        return Math.abs(a - target) < 1.0e-4f && Math.abs(b - target) < 1.0e-4f;
    }

    private static float[] rotatePointY(float x, float y, float z, Direction facing) {
        double angle = switch (facing) {
            case EAST -> Math.PI / 2.0;
            case SOUTH -> Math.PI;
            case WEST -> -Math.PI / 2.0;
            default -> 0.0;
        };
        double cx = 0.5;
        double cz = 0.5;
        double dx = x - cx;
        double dz = z - cz;
        double rx = dx * Math.cos(angle) + dz * Math.sin(angle);
        double rz = -dx * Math.sin(angle) + dz * Math.cos(angle);
        return new float[] { (float) (rx + cx), y, (float) (rz + cz) };
    }
    private static List<EdgeSegment> getOrBuildOutline(ResourceLocation textureLoc) {
        return OUTLINE_CACHE.computeIfAbsent(textureLoc, InteractiveBlockOutlineRenderer::buildOutline);
    }

    private static List<EdgeSegment> buildOutline(ResourceLocation textureLoc) {
        List<EdgeSegment> edges = new ArrayList<>();

        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getResourceManager() == null) {
                LOGGER.warn("ResourceManager not available, cannot load outline for {}", textureLoc);
                return edges;
            }

            Resource resource = mc.getResourceManager().getResource(textureLoc).orElse(null);
            if (resource == null) {
                LOGGER.warn("Texture not found: {}", textureLoc);
                return edges;
            }

            try (InputStream stream = resource.open();
                 NativeImage image = NativeImage.read(NativeImage.Format.RGBA, stream)) {

                int width = image.getWidth();
                int height = image.getHeight();

                for (int y = 0; y <= height; y++) {
                    int startX = -1;
                    for (int x = 0; x < width; x++) {
                        boolean topOpaque = (y > 0) && ((image.getPixelRGBA(x, y - 1) >> 24) & 0xFF) >= ALPHA_THRESHOLD;
                        boolean bottomOpaque = (y < height) && ((image.getPixelRGBA(x, y) >> 24) & 0xFF) >= ALPHA_THRESHOLD;
                        boolean hasEdge = topOpaque != bottomOpaque;

                        if (hasEdge) {
                            if (startX == -1) {
                                startX = x;
                            }
                        } else if (startX != -1) {
                            float px1 = (float) startX / width * 16.0f;
                            float px2 = (float) x / width * 16.0f;
                            float py = (float) y / height * 16.0f;
                            edges.add(new EdgeSegment(px1, py, px2, py));
                            startX = -1;
                        }
                    }
                    if (startX != -1) {
                        float px1 = (float) startX / width * 16.0f;
                        float px2 = 16.0f;
                        float py = (float) y / height * 16.0f;
                        edges.add(new EdgeSegment(px1, py, px2, py));
                    }
                }

                for (int x = 0; x <= width; x++) {
                    int startY = -1;
                    for (int y = 0; y < height; y++) {
                        boolean leftOpaque = (x > 0) && ((image.getPixelRGBA(x - 1, y) >> 24) & 0xFF) >= ALPHA_THRESHOLD;
                        boolean rightOpaque = (x < width) && ((image.getPixelRGBA(x, y) >> 24) & 0xFF) >= ALPHA_THRESHOLD;
                        boolean hasEdge = leftOpaque != rightOpaque;

                        if (hasEdge) {
                            if (startY == -1) {
                                startY = y;
                            }
                        } else if (startY != -1) {
                            float px = (float) x / width * 16.0f;
                            float py1 = (float) startY / height * 16.0f;
                            float py2 = (float) y / height * 16.0f;
                            edges.add(new EdgeSegment(px, py1, px, py2));
                            startY = -1;
                        }
                    }
                    if (startY != -1) {
                        float px = (float) x / width * 16.0f;
                        float py1 = (float) startY / height * 16.0f;
                        float py2 = 16.0f;
                        edges.add(new EdgeSegment(px, py1, px, py2));
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to load outline for {}: {}", textureLoc, e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Error building outline for {}: ", textureLoc, e);
        }

        return edges;
    }

    private static void renderLine(VertexConsumer consumer, Matrix4f matrix,
                                   float x1, float y1, float z1,
                                   float x2, float y2, float z2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len > 0.0f) {
            dx /= len;
            dy /= len;
            dz /= len;
        }

        consumer.addVertex(matrix, x1, y1, z1)
            .setColor(OUTLINE_R, OUTLINE_G, OUTLINE_B, OUTLINE_A)
            .setNormal(dx, dy, dz);
        consumer.addVertex(matrix, x2, y2, z2)
            .setColor(OUTLINE_R, OUTLINE_G, OUTLINE_B, OUTLINE_A)
            .setNormal(dx, dy, dz);
    }

    private static int renderChainSilhouetteFromQuads(BlockState state, VertexConsumer consumer, Matrix4f matrix,
                                                      boolean chainTopCap, boolean chainBottomCap) {
        Minecraft mc = Minecraft.getInstance();
        BakedModel model = mc.getBlockRenderer().getBlockModel(state);
        Map<String, EdgeInfo> edges = new HashMap<>();

        for (RenderType renderType : model.getRenderTypes(state, RandomSource.create(0), ModelData.EMPTY)) {
            collectQuadEdges(model.getQuads(state, null, RandomSource.create(0), ModelData.EMPTY, renderType), edges);
            for (Direction dir : Direction.values()) {
                collectQuadEdges(model.getQuads(state, dir, RandomSource.create(0), ModelData.EMPTY, renderType), edges);
            }
        }

        int drawn = 0;
        for (EdgeInfo info : edges.values()) {
            if (info.coplanar) {
                continue;
            }
            EdgeSegment3D edge = info.edge;
            if (shouldSkipChainBoundary(edge, chainTopCap, chainBottomCap)) {
                continue;
            }
            if (isOuterPlaneEdge(edge)) {
                continue;
            }
            renderLine(consumer, matrix, edge.x1, edge.y1, edge.z1, edge.x2, edge.y2, edge.z2);
            drawn++;
        }

        if (drawn == 0) {
            List<EdgeSegment3D> modelEdges = getOrBuildModelOutline(
                ResourceLocation.fromNamespaceAndPath("minecraft", "models/block/chain.json"));
            for (EdgeSegment3D edge : modelEdges) {
                if (shouldSkipChainBoundary(edge, chainTopCap, chainBottomCap)) {
                    continue;
                }
                if (isOuterPlaneEdge(edge)) {
                    continue;
                }
                renderLine(consumer, matrix, edge.x1, edge.y1, edge.z1, edge.x2, edge.y2, edge.z2);
                drawn++;
            }
        }
        return drawn;
    }

    private static void renderModelOutlineFromQuads(BlockState state, VertexConsumer consumer, Matrix4f matrix) {
        Minecraft mc = Minecraft.getInstance();
        BakedModel model = mc.getBlockRenderer().getBlockModel(state);
        Map<String, EdgeInfo> edges = new HashMap<>();

        for (RenderType renderType : model.getRenderTypes(state, RandomSource.create(0), ModelData.EMPTY)) {
            collectQuadEdges(model.getQuads(state, null, RandomSource.create(0), ModelData.EMPTY, renderType), edges);
            for (Direction dir : Direction.values()) {
                collectQuadEdges(model.getQuads(state, dir, RandomSource.create(0), ModelData.EMPTY, renderType), edges);
            }
        }

        for (EdgeInfo info : edges.values()) {
            if (info.coplanar) {
                continue;
            }
            EdgeSegment3D edge = info.edge;
            renderLine(consumer, matrix, edge.x1, edge.y1, edge.z1, edge.x2, edge.y2, edge.z2);
        }
    }

    private static void collectQuadEdges(List<BakedQuad> quads,
                                         Map<String, EdgeInfo> edges) {
        if (quads == null || quads.isEmpty()) {
            return;
        }
        for (BakedQuad quad : quads) {
            int[] data = quad.getVertices();
            if (data.length < 32) {
                continue;
            }
            Vertex v0 = Vertex.from(data, 0);
            Vertex v1 = Vertex.from(data, 1);
            Vertex v2 = Vertex.from(data, 2);
            Vertex v3 = Vertex.from(data, 3);
            float[] normal = normalForDirection(quad.getDirection());
            addEdge(v0, v1, normal, edges);
            addEdge(v1, v2, normal, edges);
            addEdge(v2, v3, normal, edges);
            addEdge(v3, v0, normal, edges);
        }
    }

    private static void addEdge(Vertex a, Vertex b, float[] normal,
                                Map<String, EdgeInfo> edges) {
        String key = edgeKey(a, b);
        EdgeInfo existing = edges.get(key);
        if (existing == null) {
            edges.put(key, new EdgeInfo(new EdgeSegment3D(a.x, a.y, a.z, b.x, b.y, b.z), normal, false));
            return;
        }
        if (dot(existing.normal, normal) > 0.999f) {
            existing.coplanar = true;
        }
    }

    private static String edgeKey(Vertex a, Vertex b) {
        long ax = Math.round(a.x * 10000.0);
        long ay = Math.round(a.y * 10000.0);
        long az = Math.round(a.z * 10000.0);
        long bx = Math.round(b.x * 10000.0);
        long by = Math.round(b.y * 10000.0);
        long bz = Math.round(b.z * 10000.0);
        if (ax > bx || (ax == bx && (ay > by || (ay == by && az > bz)))) {
            long tx = ax; long ty = ay; long tz = az;
            ax = bx; ay = by; az = bz;
            bx = tx; by = ty; bz = tz;
        }
        return ax + "," + ay + "," + az + "|" + bx + "," + by + "," + bz;
    }

    private static float[] normalForDirection(Direction dir) {
        if (dir == null) {
            return new float[] {0.0f, 0.0f, 0.0f};
        }
        return switch (dir) {
            case UP -> new float[] {0.0f, 1.0f, 0.0f};
            case DOWN -> new float[] {0.0f, -1.0f, 0.0f};
            case NORTH -> new float[] {0.0f, 0.0f, -1.0f};
            case SOUTH -> new float[] {0.0f, 0.0f, 1.0f};
            case WEST -> new float[] {-1.0f, 0.0f, 0.0f};
            case EAST -> new float[] {1.0f, 0.0f, 0.0f};
        };
    }

    private static float dot(float[] a, float[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static final class EdgeInfo {
        final EdgeSegment3D edge;
        final float[] normal;
        boolean coplanar;

        private EdgeInfo(EdgeSegment3D edge, float[] normal, boolean coplanar) {
            this.edge = edge;
            this.normal = normal;
            this.coplanar = coplanar;
        }
    }
}

