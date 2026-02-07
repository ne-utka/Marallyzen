package neutka.marallys.marallyzen.client.quest;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import neutka.marallys.marallyzen.Marallyzen;

@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT)
public class QuestZoneMagnetiteOutlineRenderer {
    private static final int OUTLINE_COLOR = 0xD48E03;

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent.AfterEntities event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        BlockPos pos = QuestZonePromptHud.getInstance().activeMagnetitePos();
        if (pos == null) {
            return;
        }
        BlockState state = mc.level.getBlockState(pos);
        if (!state.is(Blocks.LODESTONE)) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.position();

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        VoxelShape shape = state.getShape(mc.level, pos);
        AABB box = shape.isEmpty() ? new AABB(pos) : shape.bounds().move(pos);
        box = box.inflate(0.002);

        float r = ((OUTLINE_COLOR >> 16) & 0xFF) / 255.0f;
        float g = ((OUTLINE_COLOR >> 8) & 0xFF) / 255.0f;
        float b = (OUTLINE_COLOR & 0xFF) / 255.0f;

        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        renderLineBox(poseStack, buffer.getBuffer(RenderTypes.lines()), box, r, g, b, 0.85f);
        buffer.endBatch(RenderTypes.lines());
        poseStack.popPose();
    }

    private static void renderLineBox(PoseStack poseStack, VertexConsumer consumer, AABB box,
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






