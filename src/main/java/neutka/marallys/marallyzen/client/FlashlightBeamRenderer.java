package neutka.marallys.marallyzen.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import neutka.marallys.marallyzen.Marallyzen;

import java.util.Map;
import java.util.UUID;

/**
 * Renders volumetric flashlight beams for all players with active flashlights.
 * Uses proper cone geometry with segments and hand-attached positioning.
 */
@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class FlashlightBeamRenderer {
    
    // Beam parameters
    private static final float BEAM_LENGTH = 40.0f;
    private static final float BEAM_WIDTH_START = 0.08f;
    private static final float BEAM_WIDTH_END = 0.8f;
    private static final int SEGMENTS = 16;
    private static final int BEAM_COLOR = 0xFFFFFFFF; // White with full alpha
    
    // Cached geometry (generated once, reused)
    private static float[][] cachedVertices = null;
    
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        
        Camera camera = event.getCamera();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        
        float partialTick = mc.getTimer().getGameTimeDeltaPartialTick(false);
        
        // Get all active flashlight states
        Map<UUID, FlashlightStateCache.FlashlightState> states = FlashlightStateCache.getAllStates();
        
        for (Map.Entry<UUID, FlashlightStateCache.FlashlightState> entry : states.entrySet()) {
            UUID playerId = entry.getKey();
            FlashlightStateCache.FlashlightState state = entry.getValue();
            
            if (!state.enabled()) {
                continue;
            }
            
            // Find player entity
            Entity entity = findEntityByUuid(playerId, mc.level);
            if (!(entity instanceof Player player)) {
                continue;
            }
            
            // Calculate hand position and direction
            Vec3 handPos = calculateHandPosition(player, partialTick);
            Vec3 direction = calculateBeamDirection(state.yaw(), state.pitch());
            Vec3 endPos = handPos.add(direction.scale(BEAM_LENGTH));
            
            // Render beam
            renderBeam(poseStack, bufferSource, camera, handPos, endPos, BEAM_WIDTH_START, BEAM_WIDTH_END);
        }
        
        bufferSource.endBatch();
    }
    
    /**
     * Calculates hand position based on player position and eye height
     */
    private static Vec3 calculateHandPosition(Player player, float partialTick) {
        double x = Mth.lerp(partialTick, player.xo, player.getX());
        double y = Mth.lerp(partialTick, player.yo, player.getY()) + player.getEyeHeight();
        double z = Mth.lerp(partialTick, player.zo, player.getZ());
        
        // Small offset forward and to the right for hand position
        float yawRad = (float) Math.toRadians(player.getYRot());
        float offsetX = (float) (Math.sin(yawRad) * 0.3);
        float offsetZ = (float) (-Math.cos(yawRad) * 0.3);
        
        return new Vec3(x + offsetX, y - 0.1, z + offsetZ);
    }
    
    /**
     * Calculates beam direction from yaw and pitch
     */
    private static Vec3 calculateBeamDirection(float yaw, float pitch) {
        float yawRad = (float) Math.toRadians(-yaw);
        float pitchRad = (float) Math.toRadians(-pitch);
        
        float x = Mth.cos(yawRad) * Mth.cos(pitchRad);
        float y = Mth.sin(pitchRad);
        float z = Mth.sin(yawRad) * Mth.cos(pitchRad);
        
        return new Vec3(x, y, z).normalize();
    }
    
    /**
     * Renders a volumetric cone beam
     */
    private static void renderBeam(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, Camera camera,
                                   Vec3 start, Vec3 end, float widthStart, float widthEnd) {
        poseStack.pushPose();
        
        // Translate to camera-relative coordinates
        Vec3 camPos = camera.getPosition();
        double dx = start.x - camPos.x;
        double dy = start.y - camPos.y;
        double dz = start.z - camPos.z;
        
        double endDx = end.x - camPos.x;
        double endDy = end.y - camPos.y;
        double endDz = end.z - camPos.z;
        
        // Calculate direction vector
        Vec3 direction = new Vec3(endDx - dx, endDy - dy, endDz - dz).normalize();
        
        // Calculate perpendicular vectors for cross-section
        Vec3 right = new Vec3(-direction.z, 0, direction.x).normalize();
        if (right.lengthSqr() < 0.01) {
            right = new Vec3(1, 0, 0);
        }
        Vec3 up = direction.cross(right).normalize();
        
        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer buffer = bufferSource.getBuffer(FlashlightRenderType.FLASHLIGHT_BEAM);
        
        int r = (BEAM_COLOR >> 16) & 0xFF;
        int g = (BEAM_COLOR >> 8) & 0xFF;
        int b = BEAM_COLOR & 0xFF;
        int a = (BEAM_COLOR >> 24) & 0xFF;
        if (a == 0) a = 255; // Default to full alpha if not specified
        
        int light = net.minecraft.client.renderer.LightTexture.FULL_BRIGHT;
        int overlay = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
        int lightU = light & 0xFFFF;
        int lightV = (light >> 16) & 0xFFFF;
        int overlayU = overlay & 0xFFFF;
        int overlayV = (overlay >> 16) & 0xFFFF;
        
        // Generate cone geometry with segments
        float[] startRing = new float[SEGMENTS * 3];
        float[] endRing = new float[SEGMENTS * 3];
        
        for (int i = 0; i < SEGMENTS; i++) {
            float angle = (float) (i * 2.0 * Math.PI / SEGMENTS);
            float cos = Mth.cos(angle);
            float sin = Mth.sin(angle);
            
            // Start ring
            Vec3 startOffset = right.scale(cos * widthStart).add(up.scale(sin * widthStart));
            startRing[i * 3] = (float) (dx + startOffset.x);
            startRing[i * 3 + 1] = (float) (dy + startOffset.y);
            startRing[i * 3 + 2] = (float) (dz + startOffset.z);
            
            // End ring
            Vec3 endOffset = right.scale(cos * widthEnd).add(up.scale(sin * widthEnd));
            endRing[i * 3] = (float) (endDx + endOffset.x);
            endRing[i * 3 + 1] = (float) (endDy + endOffset.y);
            endRing[i * 3 + 2] = (float) (endDz + endOffset.z);
        }
        
        // Draw triangles connecting the rings
        for (int i = 0; i < SEGMENTS; i++) {
            int next = (i + 1) % SEGMENTS;
            
            int startIdx1 = i * 3;
            int startIdx2 = next * 3;
            int endIdx1 = i * 3;
            int endIdx2 = next * 3;
            
            // Triangle 1: start[i] -> start[next] -> end[i]
            addVertex(buffer, matrix, 
                startRing[startIdx1], startRing[startIdx1 + 1], startRing[startIdx1 + 2],
                r, g, b, a, lightU, lightV, overlayU, overlayV);
            addVertex(buffer, matrix,
                startRing[startIdx2], startRing[startIdx2 + 1], startRing[startIdx2 + 2],
                r, g, b, a, lightU, lightV, overlayU, overlayV);
            addVertex(buffer, matrix,
                endRing[endIdx1], endRing[endIdx1 + 1], endRing[endIdx1 + 2],
                r, g, b, a, lightU, lightV, overlayU, overlayV);
            
            // Triangle 2: start[next] -> end[next] -> end[i]
            addVertex(buffer, matrix,
                startRing[startIdx2], startRing[startIdx2 + 1], startRing[startIdx2 + 2],
                r, g, b, a, lightU, lightV, overlayU, overlayV);
            addVertex(buffer, matrix,
                endRing[endIdx2], endRing[endIdx2 + 1], endRing[endIdx2 + 2],
                r, g, b, a, lightU, lightV, overlayU, overlayV);
            addVertex(buffer, matrix,
                endRing[endIdx1], endRing[endIdx1 + 1], endRing[endIdx1 + 2],
                r, g, b, a, lightU, lightV, overlayU, overlayV);
        }
        
        poseStack.popPose();
    }
    
    /**
     * Adds a vertex to the buffer
     */
    private static void addVertex(VertexConsumer buffer, Matrix4f matrix,
                                 float x, float y, float z,
                                 int r, int g, int b, int a,
                                 int lightU, int lightV, int overlayU, int overlayV) {
        buffer.addVertex(matrix, x, y, z)
              .setColor(r, g, b, a)
              .setUv(0.0f, 0.0f)
              .setUv1(overlayU, overlayV)
              .setUv2(lightU, lightV)
              .setNormal(0.0f, 1.0f, 0.0f);
    }
    
    /**
     * Finds an entity by UUID in the level
     */
    private static Entity findEntityByUuid(UUID uuid, net.minecraft.client.multiplayer.ClientLevel level) {
        for (Entity entity : level.entitiesForRendering()) {
            if (entity.getUUID().equals(uuid)) {
                return entity;
            }
        }
        return null;
    }
}

