package neutka.marallys.marallyzen.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ClipContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import neutka.marallys.marallyzen.entity.DecoratedPotCarryEntity;

public class DecoratedPotCarryEntityRenderer extends EntityRenderer<DecoratedPotCarryEntity> {
    private static final double BE_RENDER_DISTANCE_SQ = 128.0 * 128.0;
    public DecoratedPotCarryEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(DecoratedPotCarryEntity entity, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(DecoratedPotCarryEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/decorated_pot.png");
    }

    @Override
    public void render(DecoratedPotCarryEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        Player localPlayer = Minecraft.getInstance().player;
        if (localPlayer != null && entity.getMode() == DecoratedPotCarryEntity.Mode.CARRIED
            && entity.isCarriedBy(localPlayer.getUUID())) {
            Vec3 eye = localPlayer.getEyePosition(partialTick);
            Vec3 look = localPlayer.getLookAngle().normalize();
            Vec3 desired = eye.add(look.scale(DecoratedPotCarryEntity.CARRY_DISTANCE));
            HitResult hit = localPlayer.level().clip(new ClipContext(
                eye,
                desired,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                localPlayer
            ));
            Vec3 targetCenter;
            if (hit.getType() == HitResult.Type.BLOCK) {
                Vec3 horizontal = new Vec3(look.x, 0.0, look.z).normalize();
                double halfX = DecoratedPotCarryEntity.POT_BOUNDS.maxX - 0.5;
                double halfZ = DecoratedPotCarryEntity.POT_BOUNDS.maxZ - 0.5;
                double extra = DecoratedPotCarryEntity.CARRY_WALL_PADDING + DecoratedPotCarryEntity.COLLISION_EPS;
                double push = extra;
                if (horizontal.lengthSqr() >= 1.0E-6) {
                    push += Math.abs(horizontal.x) * halfX + Math.abs(horizontal.z) * halfZ;
                }
                targetCenter = hit.getLocation().subtract(look.scale(push));
            } else {
                targetCenter = desired;
            }
            targetCenter = targetCenter.add(0.0, -0.3, 0.0);
            double minY = localPlayer.getY() + DecoratedPotCarryEntity.CARRY_MIN_Y_OFFSET;
            if (targetCenter.y < minY) {
                targetCenter = new Vec3(targetCenter.x, minY, targetCenter.z);
            }
            Vec3 targetPivot = targetCenter.subtract(0.5, 0.0, 0.5);
            double baseX = Mth.lerp(partialTick, entity.xo, entity.getX());
            double baseY = Mth.lerp(partialTick, entity.yo, entity.getY());
            double baseZ = Mth.lerp(partialTick, entity.zo, entity.getZ());
            poseStack.translate(-baseX, -baseY, -baseZ);
            poseStack.translate(targetPivot.x, targetPivot.y, targetPivot.z);
        }
        poseStack.translate(0.5, 0.0, 0.5);
        if (entity.getMode() == DecoratedPotCarryEntity.Mode.CARRIED) {
            poseStack.mulPose(Axis.YP.rotationDegrees(entity.getPotYaw()));
        }
        poseStack.translate(-0.5, 0.0, -0.5);
        renderPot(entity, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private void renderPot(DecoratedPotCarryEntity entity, float partialTick, PoseStack poseStack,
                           MultiBufferSource bufferSource, int packedLight) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        int renderLight = LevelRenderer.getLightColor(level, BlockPos.containing(entity.getX(), entity.getY(), entity.getZ()));
        Vec3 camPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        double dx = camPos.x - entity.getX();
        double dy = camPos.y - entity.getY();
        double dz = camPos.z - entity.getZ();
        double distSq = dx * dx + dy * dy + dz * dz;

        if (distSq > BE_RENDER_DISTANCE_SQ) {
            BlockState stateToRender = entity.getStoredBlockState();
            if (stateToRender == null || stateToRender.isAir()) {
                stateToRender = Blocks.DECORATED_POT.defaultBlockState();
            }
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                stateToRender,
                poseStack,
                bufferSource,
                renderLight,
                OverlayTexture.NO_OVERLAY
            );
            return;
        }

        BlockEntity blockEntity = entity.getOrCreateRenderEntity(level);
        if (blockEntity == null) {
            BlockState stateToRender = entity.getStoredBlockState();
            if (stateToRender == null || stateToRender.isAir()) {
                stateToRender = Blocks.DECORATED_POT.defaultBlockState();
            }
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                stateToRender,
                poseStack,
                bufferSource,
                renderLight,
                OverlayTexture.NO_OVERLAY
            );
            return;
        }
        var dispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
        var renderer = dispatcher.getRenderer(blockEntity);
        if (renderer == null) {
            BlockState stateToRender = entity.getStoredBlockState();
            if (stateToRender == null || stateToRender.isAir()) {
                stateToRender = Blocks.DECORATED_POT.defaultBlockState();
            }
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                stateToRender,
                poseStack,
                bufferSource,
                renderLight,
                OverlayTexture.NO_OVERLAY
            );
            return;
        }
        renderer.render(
            blockEntity,
            partialTick,
            poseStack,
            bufferSource,
            renderLight,
            OverlayTexture.NO_OVERLAY
        );
    }

}
