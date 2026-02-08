package neutka.marallys.marallyzen.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import neutka.marallys.marallyzen.blocks.OldTvBlock;
import neutka.marallys.marallyzen.blocks.OldTvBlockEntity;
import neutka.marallys.marallyzen.client.OldTvMediaManager;

public class OldTvBlockEntityRenderer implements BlockEntityRenderer<OldTvBlockEntity, BlockEntityRenderState> {
    private static final float SCREEN_MIN_X = 2.0f / 16.0f;
    private static final float SCREEN_MAX_X = 14.0f / 16.0f;
    private static final float SCREEN_MIN_Y = 3.0f / 16.0f;
    private static final float SCREEN_MAX_Y = 13.0f / 16.0f;
    private static final float FRONT_Z = 1.0f / 16.0f;
    private static final float BACK_Z = 15.0f / 16.0f;
    private static final float FACE_OFFSET = 0.001f;

    public OldTvBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public BlockEntityRenderState createRenderState() {
        return new BlockEntityRenderState();
    }

    @Override
    public void extractRenderState(OldTvBlockEntity blockEntity, BlockEntityRenderState renderState, float partialTick,
                                   Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(blockEntity, renderState, breakProgress);
    }

    @Override
    public void submit(BlockEntityRenderState renderState, PoseStack poseStack, SubmitNodeCollector renderTasks,
                       CameraRenderState cameraState) {
        if (renderState.blockState == null || renderState.blockPos == null) {
            return;
        }
        Level level = net.minecraft.client.Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        BlockState state = renderState.blockState;
        if (!state.hasProperty(OldTvBlock.ON) || !state.getValue(OldTvBlock.ON)) {
            return;
        }
        Identifier texture = OldTvMediaManager.getTexture(renderState.blockPos, level.dimension(), level.getGameTime());
        if (texture == null) {
            return;
        }
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        RenderType renderType = RenderTypes.entityTranslucent(texture);
        int packedLight = renderState.lightCoords;
        int packedOverlay = OverlayTexture.NO_OVERLAY;

        renderTasks.submitCustomGeometry(poseStack, renderType, (pose, consumer) -> {
            switch (facing) {
                case NORTH -> renderNorth(consumer, pose, packedLight, packedOverlay);
                case SOUTH -> renderSouth(consumer, pose, packedLight, packedOverlay);
                case WEST -> renderWest(consumer, pose, packedLight, packedOverlay);
                case EAST -> renderEast(consumer, pose, packedLight, packedOverlay);
                default -> {
                }
            }
        });
    }

    private static void renderNorth(VertexConsumer consumer, PoseStack.Pose pose, int light, int overlay) {
        float z = FRONT_Z - FACE_OFFSET;
        emit(consumer, pose, SCREEN_MAX_X, SCREEN_MAX_Y, z, 1.0f, 0.0f, light, overlay, 0.0f, 0.0f, -1.0f);
        emit(consumer, pose, SCREEN_MAX_X, SCREEN_MIN_Y, z, 1.0f, 1.0f, light, overlay, 0.0f, 0.0f, -1.0f);
        emit(consumer, pose, SCREEN_MIN_X, SCREEN_MIN_Y, z, 0.0f, 1.0f, light, overlay, 0.0f, 0.0f, -1.0f);
        emit(consumer, pose, SCREEN_MIN_X, SCREEN_MAX_Y, z, 0.0f, 0.0f, light, overlay, 0.0f, 0.0f, -1.0f);
    }

    private static void renderSouth(VertexConsumer consumer, PoseStack.Pose pose, int light, int overlay) {
        float z = BACK_Z + FACE_OFFSET;
        emit(consumer, pose, SCREEN_MIN_X, SCREEN_MAX_Y, z, 1.0f, 0.0f, light, overlay, 0.0f, 0.0f, 1.0f);
        emit(consumer, pose, SCREEN_MIN_X, SCREEN_MIN_Y, z, 1.0f, 1.0f, light, overlay, 0.0f, 0.0f, 1.0f);
        emit(consumer, pose, SCREEN_MAX_X, SCREEN_MIN_Y, z, 0.0f, 1.0f, light, overlay, 0.0f, 0.0f, 1.0f);
        emit(consumer, pose, SCREEN_MAX_X, SCREEN_MAX_Y, z, 0.0f, 0.0f, light, overlay, 0.0f, 0.0f, 1.0f);
    }

    private static void renderWest(VertexConsumer consumer, PoseStack.Pose pose, int light, int overlay) {
        float x = FRONT_Z - FACE_OFFSET;
        emit(consumer, pose, x, SCREEN_MAX_Y, SCREEN_MIN_X, 1.0f, 0.0f, light, overlay, -1.0f, 0.0f, 0.0f);
        emit(consumer, pose, x, SCREEN_MIN_Y, SCREEN_MIN_X, 1.0f, 1.0f, light, overlay, -1.0f, 0.0f, 0.0f);
        emit(consumer, pose, x, SCREEN_MIN_Y, SCREEN_MAX_X, 0.0f, 1.0f, light, overlay, -1.0f, 0.0f, 0.0f);
        emit(consumer, pose, x, SCREEN_MAX_Y, SCREEN_MAX_X, 0.0f, 0.0f, light, overlay, -1.0f, 0.0f, 0.0f);
    }

    private static void renderEast(VertexConsumer consumer, PoseStack.Pose pose, int light, int overlay) {
        float x = BACK_Z + FACE_OFFSET;
        emit(consumer, pose, x, SCREEN_MAX_Y, SCREEN_MAX_X, 1.0f, 0.0f, light, overlay, 1.0f, 0.0f, 0.0f);
        emit(consumer, pose, x, SCREEN_MIN_Y, SCREEN_MAX_X, 1.0f, 1.0f, light, overlay, 1.0f, 0.0f, 0.0f);
        emit(consumer, pose, x, SCREEN_MIN_Y, SCREEN_MIN_X, 0.0f, 1.0f, light, overlay, 1.0f, 0.0f, 0.0f);
        emit(consumer, pose, x, SCREEN_MAX_Y, SCREEN_MIN_X, 0.0f, 0.0f, light, overlay, 1.0f, 0.0f, 0.0f);
    }

    private static void emit(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z,
                             float u, float v, int light, int overlay, float nx, float ny, float nz) {
        int lightU = light & 0xFFFF;
        int lightV = (light >> 16) & 0xFFFF;
        int overlayU = overlay & 0xFFFF;
        int overlayV = (overlay >> 16) & 0xFFFF;
        consumer.addVertex(pose.pose(), x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setUv1(overlayU, overlayV)
                .setUv2(lightU, lightV)
                .setNormal(nx, ny, nz);
    }
}







