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

@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class CutscenePreviewDebugRenderer {
    private static final double MARKER_HALF_SIZE = 0.2;

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        if (!CutscenePreviewPlayer.isPreviewActive()) {
            return;
        }
        CutsceneEditorData data = CutscenePreviewPlayer.getActivePreviewData();
        if (data == null) {
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
        long currentTime = CutscenePreviewPlayer.getActivePreviewTime();
        int currentKeyframeIndex = CutscenePreviewPlayer.getActivePreviewKeyframeIndex();
        VertexConsumer lineConsumer = buffer.getBuffer(RenderType.lines());
        Matrix4f matrix = poseStack.last().pose();
        Vec3 lastPos = null;

        for (CutsceneEditorData.EditorKeyframe keyframe : data.getKeyframes()) {
            if (!(keyframe instanceof CutsceneEditorData.CameraKeyframe cameraKf)) {
                continue;
            }
            Vec3 pos = cameraKf.getPosition();
            if (pos == null) {
                continue;
            }
            if (lastPos != null) {
                renderLine(lineConsumer, matrix, lastPos, pos, 0.9f, 0.3f, 1.0f, 0.8f);
            }
            lastPos = pos;
        }

        int keyframeIndex = 0;
        for (CutsceneEditorData.EditorKeyframe keyframe : data.getKeyframes()) {
            if (!(keyframe instanceof CutsceneEditorData.CameraKeyframe cameraKf)) {
                keyframeIndex++;
                continue;
            }
            Vec3 pos = cameraKf.getPosition();
            if (pos == null) {
                keyframeIndex++;
                continue;
            }
            AABB box = new AABB(
                pos.x - MARKER_HALF_SIZE, pos.y - MARKER_HALF_SIZE, pos.z - MARKER_HALF_SIZE,
                pos.x + MARKER_HALF_SIZE, pos.y + MARKER_HALF_SIZE, pos.z + MARKER_HALF_SIZE
            );

            boolean isCurrent = keyframeIndex == currentKeyframeIndex;
            boolean isPast = keyframe.getTime() <= currentTime;
            float r = isCurrent ? 0.1f : (isPast ? 0.2f : 1.0f);
            float g = isCurrent ? 0.8f : (isPast ? 1.0f : 0.8f);
            float b = isCurrent ? 1.0f : 0.2f;
            float a = isCurrent ? 1.0f : 0.9f;
            LevelRenderer.renderLineBox(poseStack, buffer.getBuffer(RenderType.lines()), box, r, g, b, a);
            keyframeIndex++;
        }

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
}
