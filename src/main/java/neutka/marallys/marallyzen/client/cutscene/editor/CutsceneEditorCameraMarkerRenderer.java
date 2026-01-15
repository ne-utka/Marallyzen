package neutka.marallys.marallyzen.client.cutscene.editor;

import com.mojang.blaze3d.vertex.PoseStack;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class CutsceneEditorCameraMarkerRenderer {
    private static final double MARKER_HALF_SIZE = 0.22;

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof CutsceneEditorScreen editorScreen)) {
            return;
        }
        CutsceneEditorData data = editorScreen.getEditorData();
        if (data == null || mc.level == null || mc.player == null) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();

        CutsceneEditorData.CameraKeyframe fixed = findFixedCamera(data.getKeyframes());
        for (CutsceneEditorData.EditorKeyframe keyframe : data.getKeyframes()) {
            if (!(keyframe instanceof CutsceneEditorData.CameraKeyframe cameraKf)) {
                continue;
            }
            Vec3 pos = cameraKf.getPosition();
            if (pos == null) {
                continue;
            }
            boolean isFixed = fixed != null && fixed == cameraKf;
            float r = isFixed ? 0.2f : 1.0f;
            float g = isFixed ? 1.0f : 0.8f;
            float b = isFixed ? 0.2f : 0.2f;
            float a = 0.9f;
            AABB box = new AABB(
                pos.x - MARKER_HALF_SIZE, pos.y - MARKER_HALF_SIZE, pos.z - MARKER_HALF_SIZE,
                pos.x + MARKER_HALF_SIZE, pos.y + MARKER_HALF_SIZE, pos.z + MARKER_HALF_SIZE
            );
            LevelRenderer.renderLineBox(poseStack, buffer.getBuffer(RenderType.lines()), box, r, g, b, a);
        }

        buffer.endBatch(RenderType.lines());
        poseStack.popPose();
    }

    private static CutsceneEditorData.CameraKeyframe findFixedCamera(List<CutsceneEditorData.EditorKeyframe> keyframes) {
        Map<Integer, List<CutsceneEditorData.CameraKeyframe>> groups = new HashMap<>();
        for (CutsceneEditorData.EditorKeyframe keyframe : keyframes) {
            if (keyframe instanceof CutsceneEditorData.CameraKeyframe cameraKf && keyframe.getGroupId() >= 0) {
                groups.computeIfAbsent(keyframe.getGroupId(), k -> new java.util.ArrayList<>()).add(cameraKf);
            }
        }
        CutsceneEditorData.CameraKeyframe lastSingle = null;
        long lastTime = Long.MIN_VALUE;
        for (var entry : groups.entrySet()) {
            List<CutsceneEditorData.CameraKeyframe> frames = entry.getValue();
            if (frames.size() != 1) {
                continue;
            }
            CutsceneEditorData.CameraKeyframe cam = frames.get(0);
            if (cam.getTime() >= lastTime) {
                lastTime = cam.getTime();
                lastSingle = cam;
            }
        }
        return lastSingle;
    }
}
