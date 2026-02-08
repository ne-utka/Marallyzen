package neutka.marallys.marallyzen.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.blocks.DictaphoneBlock;
import neutka.marallys.marallyzen.blocks.MarallyzenBlocks;
import neutka.marallys.marallyzen.client.ClientDictaphoneManager;
import neutka.marallys.marallyzen.entity.DictaphoneEntity;

public class DictaphoneEntityRenderer extends EntityRenderer<DictaphoneEntity, DictaphoneEntityRenderer.RenderState> {
    public DictaphoneEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    private static Vec3 computeFlightPosition(Vec3 start, Vec3 target, float progress) {
        float liftPhase = 0.35f;
        double liftY = start.y + 0.6;
        if (target != null) {
            liftY = Math.max(liftY, target.y);
        }
        Vec3 liftPos = new Vec3(start.x, liftY, start.z);
        if (progress < liftPhase) {
            float local = progress / liftPhase;
            float t = Mth.clamp(local, 0.0f, 1.0f);
            float liftEased = t * t * (3.0f - 2.0f * t);
            return start.lerp(liftPos, liftEased);
        }
        float local = (progress - liftPhase) / (1.0f - liftPhase);
        float t = Mth.clamp(local, 0.0f, 1.0f);
        float moveEased = t * t * (3.0f - 2.0f * t);
        return liftPos.lerp(target, moveEased);
    }

    private static float computeRotationT(float progress, float liftPhase) {
        float local = (progress - liftPhase) / (1.0f - liftPhase);
        float t = Mth.clamp(local, 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);
    }

    public Identifier getTextureLocation(DictaphoneEntity entity) {
        return Identifier.fromNamespaceAndPath(Marallyzen.MODID, "textures/block/dictaphone.png");
    }

    @Override
    public RenderState createRenderState() {
        return new RenderState();
    }

    @Override
    public void extractRenderState(DictaphoneEntity entity, RenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.entity = entity;
        state.partialTick = partialTick;
    }

    @Override
    public void submit(RenderState state, PoseStack poseStack, SubmitNodeCollector renderTasks, CameraRenderState cameraState) {
        DictaphoneEntity entity = state.entity;
        if (entity == null) {
            return;
        }
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        renderDictaphone(entity, state.partialTick, poseStack, bufferSource, state.lightCoords);
        bufferSource.endBatch();
        super.submit(state, poseStack, renderTasks, cameraState);
    }

    private void renderDictaphone(DictaphoneEntity entity, float partialTick, PoseStack poseStack,
                                  MultiBufferSource.BufferSource bufferSource, int packedLight) {
        DictaphoneEntity.State state = entity.getCurrentState();
        Vec3 start = entity.getStartPosition();
        Vec3 target = entity.getTargetPosition();
        Vec3 renderPos = entity.position();
        Vec3 adjustedTarget = null;
        if (target != null) {
            adjustedTarget = new Vec3(target.x, target.y + 0.5, target.z);
        }

        float flyElapsed = (entity.tickCount + partialTick) - entity.getAnimationStartTick();
        float flyProgress = Mth.clamp(flyElapsed / DictaphoneEntity.FLY_OUT_DURATION_TICKS, 0.0f, 1.0f);
        boolean forceFlyOut = start != null && adjustedTarget != null && flyProgress < 1.0f;

        if (forceFlyOut) {
            renderPos = computeFlightPosition(start, adjustedTarget, flyProgress);
        } else if (state == DictaphoneEntity.State.VIEWING && adjustedTarget != null) {
            renderPos = adjustedTarget;
        } else if (state == DictaphoneEntity.State.RETURNING) {
            Vec3 returnStart = adjustedTarget != null ? adjustedTarget : entity.getReturnStartPosition();
            Vec3 origin = start != null ? start : entity.position();
            if (returnStart != null && origin != null) {
                float elapsed = (entity.tickCount + partialTick) - entity.getReturnAnimationStartTick();
                float progress = Mth.clamp(elapsed / DictaphoneEntity.RETURN_DURATION_TICKS, 0.0f, 1.0f);
                renderPos = computeFlightPosition(origin, returnStart, 1.0f - progress);
            }
        }

        double baseX = Mth.lerp(partialTick, entity.xo, entity.getX());
        double baseY = Mth.lerp(partialTick, entity.yo, entity.getY());
        double baseZ = Mth.lerp(partialTick, entity.zo, entity.getZ());

        poseStack.pushPose();
        poseStack.translate(renderPos.x - baseX, renderPos.y - baseY, renderPos.z - baseZ);

        BlockState original = entity.getOriginalBlockState();
        BlockState stateToRender = MarallyzenBlocks.DICTAPHONE.get().defaultBlockState();
        BlockPos originPos = entity.getOriginPos();
        boolean animated = originPos != null && ClientDictaphoneManager.isPlaybackActive(originPos);
        if (stateToRender.hasProperty(DictaphoneBlock.ANIMATED)) {
            stateToRender = stateToRender.setValue(DictaphoneBlock.ANIMATED, animated);
        }
        if (original != null && original.hasProperty(HorizontalDirectionalBlock.FACING)) {
            if (state == DictaphoneEntity.State.RETURNING) {
                stateToRender = stateToRender.setValue(HorizontalDirectionalBlock.FACING,
                    original.getValue(HorizontalDirectionalBlock.FACING));
            } else {
                stateToRender = stateToRender.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH);
            }
        }

        float liftPhase = 0.35f;
        float returnProgress = 0.0f;
        if (state == DictaphoneEntity.State.RETURNING) {
            float returnElapsed = (entity.tickCount + partialTick) - entity.getReturnAnimationStartTick();
            returnProgress = Mth.clamp(returnElapsed / DictaphoneEntity.RETURN_DURATION_TICKS, 0.0f, 1.0f);
        }
        boolean useBillboard = state == DictaphoneEntity.State.VIEWING
            || forceFlyOut
            || state == DictaphoneEntity.State.RETURNING;
        if (useBillboard) {
            // Billboard like posters: always face the camera.
            if (state == DictaphoneEntity.State.VIEWING) {
                float swayX = Mth.sin((entity.tickCount + partialTick) * 0.035f) * 6.0f;
                float swayY = Mth.cos((entity.tickCount + partialTick) * 0.0455f) * 6.0f;
                poseStack.mulPose(Axis.XP.rotationDegrees(swayX));
                poseStack.mulPose(Axis.YP.rotationDegrees(swayY));
            }
            poseStack.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));
            // Rotate so the top face points toward the camera.
            float rotT = 1.0f;
            if (forceFlyOut) {
                rotT = computeRotationT(flyProgress, liftPhase);
            } else if (state == DictaphoneEntity.State.RETURNING) {
                rotT = computeRotationT(1.0f - returnProgress, liftPhase);
            }
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0f * rotT));
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0f * rotT));
        } else if (stateToRender.hasProperty(HorizontalDirectionalBlock.FACING)) {
            Direction facing = stateToRender.getValue(HorizontalDirectionalBlock.FACING);
            float rot = switch (facing) {
                case EAST -> 90.0f;
                case SOUTH -> 180.0f;
                case WEST -> 270.0f;
                default -> 0.0f;
            };
            poseStack.translate(0.5, 0.0, 0.5);
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-rot));
            poseStack.translate(-0.5, 0.0, -0.5);
        }

        // Center block model on the entity position.
        poseStack.translate(-0.5, 0.0, -0.5);

        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
            stateToRender,
            poseStack,
            bufferSource,
            packedLight,
            OverlayTexture.NO_OVERLAY
        );

        poseStack.popPose();
    }

    public static final class RenderState extends EntityRenderState {
        private DictaphoneEntity entity;
        private float partialTick;
    }
}


