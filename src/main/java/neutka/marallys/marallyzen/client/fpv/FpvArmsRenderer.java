package neutka.marallys.marallyzen.client.fpv;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

public final class FpvArmsRenderer {
    private static final Identifier DEFAULT_WIDE_SKIN = Identifier.withDefaultNamespace("textures/entity/steve.png");
    private static final Identifier DEFAULT_SLIM_SKIN = Identifier.withDefaultNamespace("textures/entity/alex.png");

    private static final float ARM_WIDTH_WIDE = 0.22f;
    private static final float ARM_WIDTH_SLIM = 0.18f;
    private static final float ARM_HEIGHT = 0.46f;
    private static final float ARM_DEPTH = 0.22f;

    private static final float TORSO_WIDTH = 0.55f;
    private static final float TORSO_HEIGHT = 0.70f;
    private static final float TORSO_DEPTH = 0.28f;

    private FpvArmsRenderer() {
    }

    public static boolean renderWorld(RenderLevelStageEvent.AfterEntities event,
                                      AbstractClientPlayer player,
                                      EmoteFpvInterpreter interpreter,
                                      FpvPhaseController.Sample phase) {
        if (event == null || player == null || interpreter == null || phase == null) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        SkinState skin = resolveSkin(player);
        Identifier texture = skin.texture;
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffer.getBuffer(RenderTypes.entityTranslucent(texture));
        float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        int packedLight = mc.getEntityRenderDispatcher().getPackedLightCoords(player, partial);

        Vec3 p = player.getPosition(partial);
        float yaw = player.getViewYRot(partial);
        float pitch = player.getViewXRot(partial);
        float headPitch = interpreter.getHeadPitch(partial);
        float headYaw = interpreter.getHeadYaw(partial);
        float headRoll = interpreter.getHeadRoll(partial);

        pose.pushPose();
        pose.translate(p.x, p.y + player.getEyeHeight() - 0.20f, p.z);
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-yaw));
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(pitch * 0.12f));
        pose.translate(0.0f, -0.12f, -0.16f);

        renderTorso(pose, vc, packedLight, headPitch, headYaw, headRoll, phase);
        renderArm(pose, vc, packedLight, true, skin.slim, headPitch, headYaw, headRoll, phase);
        renderArm(pose, vc, packedLight, false, skin.slim, headPitch, headYaw, headRoll, phase);

        pose.popPose();
        buffer.endBatch();
        return true;
    }

    private static SkinState resolveSkin(AbstractClientPlayer player) {
        SkinState state = new SkinState();
        try {
            var skin = player.getSkin();
            if (skin != null && skin.body() != null) {
                state.texture = skin.body().texturePath();
                state.slim = skin.model() == PlayerModelType.SLIM;
            }
        } catch (Exception ignored) {
        }
        if (state.texture == null) {
            state.texture = state.slim ? DEFAULT_SLIM_SKIN : DEFAULT_WIDE_SKIN;
        }
        return state;
    }

    private static void renderTorso(PoseStack pose,
                                    VertexConsumer vc,
                                    int packedLight,
                                    float headPitch,
                                    float headYaw,
                                    float headRoll,
                                    FpvPhaseController.Sample phase) {
        pose.pushPose();
        pose.translate(0.0f, -0.62f, -0.42f);
        pose.mulPose(com.mojang.math.Axis.YP.rotation(headYaw * 0.22f));
        pose.mulPose(com.mojang.math.Axis.XP.rotation(headPitch * 0.18f));
        pose.mulPose(com.mojang.math.Axis.ZP.rotation(headRoll * 0.15f));
        pose.mulPose(com.mojang.math.Axis.XP.rotation(-phase.down() * 0.28f));
        emitBox(pose.last(), vc, packedLight, TORSO_WIDTH, TORSO_HEIGHT, TORSO_DEPTH);
        pose.popPose();
    }

    private static void renderArm(PoseStack pose,
                                  VertexConsumer vc,
                                  int packedLight,
                                  boolean right,
                                  boolean slim,
                                  float headPitch,
                                  float headYaw,
                                  float headRoll,
                                  FpvPhaseController.Sample phase) {
        float side = right ? 1.0f : -1.0f;
        float shake = phase.shake() * 0.18f;
        float grab = phase.grab() * 0.22f;
        float down = phase.down() * 0.40f;
        float spin = phase.valveSpin() * 0.30f;

        pose.pushPose();
        pose.translate(0.30f * side, -0.48f, -0.44f);
        pose.mulPose(com.mojang.math.Axis.YP.rotation(headYaw * 0.33f));
        pose.mulPose(com.mojang.math.Axis.XP.rotation(headPitch * 0.28f));
        pose.mulPose(com.mojang.math.Axis.ZP.rotation(headRoll * 0.38f * side));
        pose.mulPose(com.mojang.math.Axis.XP.rotation(-0.48f - grab - down + (right ? shake : -shake)));
        pose.mulPose(com.mojang.math.Axis.YP.rotation(0.12f * side + spin * side));
        pose.mulPose(com.mojang.math.Axis.ZP.rotation(0.08f * side));
        float armWidth = slim ? ARM_WIDTH_SLIM : ARM_WIDTH_WIDE;
        emitBox(pose.last(), vc, packedLight, armWidth, ARM_HEIGHT, ARM_DEPTH);
        pose.popPose();
    }

    private static void emitBox(PoseStack.Pose p,
                                VertexConsumer vc,
                                int packedLight,
                                float w,
                                float h,
                                float d) {
        float x0 = -w * 0.5f;
        float x1 = w * 0.5f;
        float y0 = -h;
        float y1 = 0.0f;
        float z0 = -d * 0.5f;
        float z1 = d * 0.5f;
        int light = packedLight;
        int overlay = OverlayTexture.NO_OVERLAY;

        emit(vc, p, x0, y1, z0, 0, 0, light, overlay, 0, 0, -1);
        emit(vc, p, x0, y0, z0, 0, 1, light, overlay, 0, 0, -1);
        emit(vc, p, x1, y0, z0, 1, 1, light, overlay, 0, 0, -1);
        emit(vc, p, x1, y1, z0, 1, 0, light, overlay, 0, 0, -1);

        emit(vc, p, x1, y1, z1, 0, 0, light, overlay, 0, 0, 1);
        emit(vc, p, x1, y0, z1, 0, 1, light, overlay, 0, 0, 1);
        emit(vc, p, x0, y0, z1, 1, 1, light, overlay, 0, 0, 1);
        emit(vc, p, x0, y1, z1, 1, 0, light, overlay, 0, 0, 1);

        emit(vc, p, x0, y1, z1, 0, 0, light, overlay, -1, 0, 0);
        emit(vc, p, x0, y0, z1, 0, 1, light, overlay, -1, 0, 0);
        emit(vc, p, x0, y0, z0, 1, 1, light, overlay, -1, 0, 0);
        emit(vc, p, x0, y1, z0, 1, 0, light, overlay, -1, 0, 0);

        emit(vc, p, x1, y1, z0, 0, 0, light, overlay, 1, 0, 0);
        emit(vc, p, x1, y0, z0, 0, 1, light, overlay, 1, 0, 0);
        emit(vc, p, x1, y0, z1, 1, 1, light, overlay, 1, 0, 0);
        emit(vc, p, x1, y1, z1, 1, 0, light, overlay, 1, 0, 0);
    }

    private static void emit(VertexConsumer v,
                             PoseStack.Pose pose,
                             float x,
                             float y,
                             float z,
                             float u,
                             float vv,
                             int light,
                             int overlay,
                             float nx,
                             float ny,
                             float nz) {
        v.addVertex(pose, x, y, z)
            .setColor(1.0f, 1.0f, 1.0f, 1.0f)
            .setUv(u, vv)
            .setOverlay(overlay)
            .setLight(light)
            .setNormal(pose, nx, ny, nz);
    }

    private static final class SkinState {
        private Identifier texture;
        private boolean slim;
    }
}
