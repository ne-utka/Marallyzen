package neutka.marallys.marallyzen.client.quest;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import neutka.marallys.marallyzen.Marallyzen;

@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class QuestZoneMagnetiteOutlineRenderer {
    private static final int OUTLINE_COLOR = 0xD48E03;

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
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
        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        VoxelShape shape = state.getShape(mc.level, pos);
        AABB box = shape.isEmpty() ? new AABB(pos) : shape.bounds().move(pos);
        box = box.inflate(0.002);

        float r = ((OUTLINE_COLOR >> 16) & 0xFF) / 255.0f;
        float g = ((OUTLINE_COLOR >> 8) & 0xFF) / 255.0f;
        float b = (OUTLINE_COLOR & 0xFF) / 255.0f;

        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        LevelRenderer.renderLineBox(poseStack, buffer.getBuffer(RenderType.lines()), box, r, g, b, 0.85f);
        buffer.endBatch(RenderType.lines());
        poseStack.popPose();
    }
}
