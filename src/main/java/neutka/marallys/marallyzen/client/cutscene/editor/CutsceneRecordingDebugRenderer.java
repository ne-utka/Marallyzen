package neutka.marallys.marallyzen.client.cutscene.editor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import neutka.marallys.marallyzen.Marallyzen;
import org.joml.Matrix4f;

import java.util.List;

@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class CutsceneRecordingDebugRenderer {
    private static final double MARKER_HALF_SIZE = 0.22;

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        CutsceneRecorder recorder = CutsceneRecorder.getInstance();
        if (!recorder.isRecording()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        VertexConsumer lineConsumer = buffer.getBuffer(RenderType.lines());
        Matrix4f matrix = poseStack.last().pose();

        List<CutsceneRecorder.RecordedFrame> frames = recorder.getRecordedFrames();
        Vec3 lastPos = null;
        for (CutsceneRecorder.RecordedFrame frame : frames) {
            Vec3 pos = frame.getPosition();
            if (pos == null) {
                continue;
            }
            if (lastPos != null) {
                renderLine(lineConsumer, matrix, lastPos, pos, 0.2f, 0.7f, 1.0f, 0.7f);
            }
            lastPos = pos;
        }

        Vec3 fixedCamera = findFixedCameraPosition();
        if (fixedCamera != null) {
            AABB camBox = new AABB(
                fixedCamera.x - MARKER_HALF_SIZE, fixedCamera.y - MARKER_HALF_SIZE, fixedCamera.z - MARKER_HALF_SIZE,
                fixedCamera.x + MARKER_HALF_SIZE, fixedCamera.y + MARKER_HALF_SIZE, fixedCamera.z + MARKER_HALF_SIZE
            );
            LevelRenderer.renderLineBox(poseStack, buffer.getBuffer(RenderType.lines()), camBox, 0.2f, 1.0f, 0.2f, 1.0f);
        }

        Vec3 currentPos = mc.player.getEyePosition();
        AABB box = new AABB(
            currentPos.x - MARKER_HALF_SIZE, currentPos.y - MARKER_HALF_SIZE, currentPos.z - MARKER_HALF_SIZE,
            currentPos.x + MARKER_HALF_SIZE, currentPos.y + MARKER_HALF_SIZE, currentPos.z + MARKER_HALF_SIZE
        );
        LevelRenderer.renderLineBox(poseStack, buffer.getBuffer(RenderType.lines()), box, 1.0f, 0.3f, 0.3f, 1.0f);

        buffer.endBatch(RenderType.lines());
        poseStack.popPose();
    }

    private static void renderLine(VertexConsumer consumer, Matrix4f matrix, Vec3 start, Vec3 end,
                                   float r, float g, float b, float a) {
        float dx = (float) (end.x - start.x);
        float dy = (float) (end.y - start.y);
        float dz = (float) (end.z - start.z);
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len > 0.0f) {
            dx /= len;
            dy /= len;
            dz /= len;
        }
        consumer.addVertex(matrix, (float) start.x, (float) start.y, (float) start.z)
            .setColor(r, g, b, a)
            .setNormal(dx, dy, dz);
        consumer.addVertex(matrix, (float) end.x, (float) end.y, (float) end.z)
            .setColor(r, g, b, a)
            .setNormal(dx, dy, dz);
    }

    private static Vec3 findFixedCameraPosition() {
        CutsceneEditorData data = CutsceneEditorScreen.getLastEditorData();
        if (data == null) {
            return null;
        }
        java.util.Map<Integer, java.util.List<CutsceneEditorData.CameraKeyframe>> groups = new java.util.HashMap<>();
        for (CutsceneEditorData.EditorKeyframe keyframe : data.getKeyframes()) {
            if (keyframe instanceof CutsceneEditorData.CameraKeyframe cameraKf && keyframe.getGroupId() >= 0) {
                groups.computeIfAbsent(keyframe.getGroupId(), k -> new java.util.ArrayList<>()).add(cameraKf);
            }
        }
        CutsceneEditorData.CameraKeyframe lastSingle = null;
        long lastTime = Long.MIN_VALUE;
        for (var entry : groups.entrySet()) {
            java.util.List<CutsceneEditorData.CameraKeyframe> frames = entry.getValue();
            if (frames.size() != 1) {
                continue;
            }
            CutsceneEditorData.CameraKeyframe cam = frames.get(0);
            if (cam.getTime() >= lastTime) {
                lastTime = cam.getTime();
                lastSingle = cam;
            }
        }
        return lastSingle != null ? lastSingle.getPosition() : null;
    }
}
