package neutka.marallys.marallyzen.trigger.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import neutka.marallys.marallyzen.activity.EasingRegistry;
import neutka.marallys.marallyzen.network.TriggerAnimationStartPacket;
import neutka.marallys.marallyzen.trigger.blueprint.TriggerBlockStateCodec;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TriggerAnimationClient {
    private static final TriggerAnimationClient INSTANCE = new TriggerAnimationClient();

    private final Map<String, RunningAnimation> animations = new ConcurrentHashMap<>();

    private TriggerAnimationClient() {
    }

    public static TriggerAnimationClient getInstance() {
        return INSTANCE;
    }

    public void start(TriggerAnimationStartPacket packet) {
        if (packet == null || packet.instanceId() == null || packet.instanceId().isBlank()) {
            return;
        }
        ResourceKey<Level> dimension;
        try {
            dimension = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, Identifier.parse(packet.dimensionId()));
        } catch (Exception e) {
            dimension = Level.OVERWORLD;
        }
        List<VisualBlock> blocks = new ArrayList<>(packet.blocks().size());
        for (TriggerAnimationStartPacket.BlockVisual block : packet.blocks()) {
            blocks.add(new VisualBlock(block.localPos(), TriggerBlockStateCodec.decode(block.state())));
        }
        animations.put(packet.instanceId(), new RunningAnimation(
                packet.instanceId(),
                dimension,
                packet.origin(),
                packet.spawnOffset(),
                Math.max(1, packet.durationTicks()),
                packet.animationType(),
                packet.easing(),
                blocks
        ));
    }

    public void stop(String instanceId) {
        if (instanceId == null || instanceId.isBlank()) {
            return;
        }
        animations.remove(instanceId);
    }

    public void tick() {
        if (animations.isEmpty()) {
            return;
        }
        Iterator<RunningAnimation> iterator = animations.values().iterator();
        while (iterator.hasNext()) {
            RunningAnimation animation = iterator.next();
            animation.ageTicks++;
            if (animation.ageTicks > animation.durationTicks + 2) {
                iterator.remove();
            }
        }
    }

    public void render(PoseStack poseStack, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || animations.isEmpty()) {
            return;
        }
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.position();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);
        for (RunningAnimation animation : animations.values()) {
            if (!mc.level.dimension().equals(animation.dimension)) {
                continue;
            }
            float t = Math.min(1.0F, (animation.ageTicks + partialTick) / (float) animation.durationTicks);
            float eased = EasingRegistry.apply(animation.easing, t);
            double shiftX = animation.spawnOffset.getX() * (1.0D - eased);
            double shiftY = animation.spawnOffset.getY() * (1.0D - eased);
            double shiftZ = animation.spawnOffset.getZ() * (1.0D - eased);

            for (VisualBlock block : animation.blocks) {
                if (block.state.isAir()) {
                    continue;
                }
                double x = animation.origin.getX() + block.localPos.getX() + shiftX;
                double y = animation.origin.getY() + block.localPos.getY() + shiftY;
                double z = animation.origin.getZ() + block.localPos.getZ() + shiftZ;

                poseStack.pushPose();
                poseStack.translate(x, y, z);
                mc.getBlockRenderer().renderSingleBlock(
                        block.state,
                        poseStack,
                        bufferSource,
                        15728880,
                        OverlayTexture.NO_OVERLAY
                );
                poseStack.popPose();
            }
        }
        bufferSource.endBatch();
        poseStack.popPose();
    }

    public void clear() {
        animations.clear();
    }

    private static final class RunningAnimation {
        private final String instanceId;
        private final ResourceKey<Level> dimension;
        private final BlockPos origin;
        private final BlockPos spawnOffset;
        private final int durationTicks;
        private final String animationType;
        private final String easing;
        private final List<VisualBlock> blocks;
        private int ageTicks;

        private RunningAnimation(
                String instanceId,
                ResourceKey<Level> dimension,
                BlockPos origin,
                BlockPos spawnOffset,
                int durationTicks,
                String animationType,
                String easing,
                List<VisualBlock> blocks
        ) {
            this.instanceId = instanceId;
            this.dimension = dimension;
            this.origin = origin;
            this.spawnOffset = spawnOffset;
            this.durationTicks = durationTicks;
            this.animationType = animationType;
            this.easing = easing;
            this.blocks = blocks;
        }
    }

    private static final class VisualBlock {
        private final BlockPos localPos;
        private final BlockState state;

        private VisualBlock(BlockPos localPos, BlockState state) {
            this.localPos = localPos;
            this.state = state;
        }
    }
}
