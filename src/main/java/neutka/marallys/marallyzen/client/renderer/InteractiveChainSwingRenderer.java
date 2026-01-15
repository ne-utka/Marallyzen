package neutka.marallys.marallyzen.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.blocks.MarallyzenBlocks;
import neutka.marallys.marallyzen.client.chain.InteractiveChainClientHider;
import neutka.marallys.marallyzen.client.chain.InteractiveChainSwingVisuals;
import org.joml.Quaternionf;

import java.util.Map;

/**
 * Renders swinging chains without relying on block entity rendering.
 */
@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class InteractiveChainSwingRenderer {
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) {
            return;
        }

        Map<BlockPos, InteractiveChainSwingVisuals.SwingStateView> snapshot = InteractiveChainSwingVisuals.getSnapshot();
        if (snapshot.isEmpty()) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Vec3 camPos = event.getCamera().getPosition();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        BlockState chainState = Blocks.CHAIN.defaultBlockState().setValue(ChainBlock.AXIS, Direction.Axis.Y);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        for (Map.Entry<BlockPos, InteractiveChainSwingVisuals.SwingStateView> entry : snapshot.entrySet()) {
            BlockPos root = entry.getKey();

            InteractiveChainSwingVisuals.SwingStateView state = entry.getValue();
            Vec3 anchor = state.anchor();
            if (anchor == null) {
                continue;
            }
            long nowMillis = Util.getMillis();
            Vec3 offset = InteractiveChainSwingVisuals.getInterpolatedOffset(root, nowMillis);
            if (offset == null || offset.lengthSqr() < 1.0E-6) {
                offset = new Vec3(0.0, -1.0, 0.0);
            } else {
                offset = offset.normalize();
            }

            int length = InteractiveChainClientHider.getHiddenLength(root);
            if (length <= 0) {
                length = getChainLength(level, root);
            }
            if (length <= 0) {
                continue;
            }

            Quaternionf rotation = rotationFromTo(new Vec3(0.0, -1.0, 0.0), offset);

            poseStack.pushPose();
            poseStack.translate(-camPos.x, -camPos.y, -camPos.z);
            poseStack.translate(anchor.x, anchor.y, anchor.z);
            poseStack.mulPose(rotation);
            poseStack.translate(-0.5, -0.5, -0.5);

            for (int i = 0; i < length; i++) {
                poseStack.pushPose();
                poseStack.translate(0.0, -i, 0.0);
                int light = LevelRenderer.getLightColor(level, root.below(i));
                mc.getBlockRenderer().renderSingleBlock(chainState, poseStack, bufferSource, light, OverlayTexture.NO_OVERLAY);
                poseStack.popPose();
            }

            poseStack.popPose();

            if (InteractiveChainSwingVisuals.isSettled(root, nowMillis)) {
                InteractiveChainClientHider.restoreChain(level, root);
                InteractiveChainSwingVisuals.clear(root);
            }
        }

    }

    private static int getChainLength(Level level, BlockPos root) {
        int length = 0;
        BlockPos current = root;
        while (level.getBlockState(current).getBlock() == MarallyzenBlocks.INTERACTIVE_CHAIN.get()) {
            length++;
            current = current.below();
        }
        return length;
    }

    private static Quaternionf rotationFromTo(Vec3 from, Vec3 to) {
        Vec3 f = from.normalize();
        Vec3 t = to.normalize();
        double dot = Mth.clamp(f.dot(t), -1.0, 1.0);
        if (dot > 0.9999) {
            return new Quaternionf();
        }
        if (dot < -0.9999) {
            Vec3 axis = new Vec3(1.0, 0.0, 0.0);
            if (Math.abs(f.x) > 0.9) {
                axis = new Vec3(0.0, 0.0, 1.0);
            }
            return new Quaternionf().fromAxisAngleRad((float) axis.x, (float) axis.y, (float) axis.z, (float) Math.PI);
        }
        Vec3 axis = f.cross(t);
        float angle = (float) Math.acos(dot);
        return new Quaternionf().fromAxisAngleRad((float) axis.x, (float) axis.y, (float) axis.z, angle);
    }

}
