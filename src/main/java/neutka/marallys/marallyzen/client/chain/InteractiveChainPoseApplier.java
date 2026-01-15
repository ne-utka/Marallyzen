package neutka.marallys.marallyzen.client.chain;

import net.minecraft.Util;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import neutka.marallys.marallyzen.Marallyzen;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class InteractiveChainPoseApplier {
    private static final Map<UUID, PoseSnapshot> SNAPSHOTS = new ConcurrentHashMap<>();

    private InteractiveChainPoseApplier() {}

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        if (!(event.getEntity() instanceof AbstractClientPlayer player)) {
            return;
        }
        InteractiveChainPoseManager.ChainPose pose = InteractiveChainPoseManager.getPose(player, Util.getMillis());
        if (pose == null) {
            return;
        }
        PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();
        PoseSnapshot snapshot = new PoseSnapshot(model);
        SNAPSHOTS.put(player.getUUID(), snapshot);

        float fade = pose.fade();
        float chainAngle = pose.chainAngle() * fade;
        float chainSide = pose.chainSideAngle() * fade;
        float chainVelocity = pose.chainVelocity() * fade;

        model.body.xRot += chainAngle;
        model.body.zRot += chainSide;

        model.head.xRot += chainAngle * 0.25f;
        model.head.zRot += chainSide * 0.2f;

        model.rightArm.xRot += chainAngle * 0.4f;
        model.leftArm.xRot += chainAngle * 0.4f;
        model.rightArm.yRot += chainSide * 0.6f;
        model.leftArm.yRot += -chainSide * 0.6f;
        model.rightArm.zRot += chainSide * 0.35f;
        model.leftArm.zRot += -chainSide * 0.35f;
        model.rightArm.yRot -= 0.25f * fade;
        model.leftArm.yRot += 0.25f * fade;
        model.rightArm.zRot += 0.18f * fade;
        model.leftArm.zRot -= 0.18f * fade;

        float timeSec = Util.getMillis() / 1000.0f;
        float legBase = -chainAngle * 0.35f;
        float legSwing = (float) Math.sin(timeSec * 3.0f) * chainVelocity * 0.1f;

        model.rightLeg.xRot += legBase + legSwing;
        model.leftLeg.xRot += legBase - legSwing;
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        if (!(event.getEntity() instanceof AbstractClientPlayer player)) {
            return;
        }
        PoseSnapshot snapshot = SNAPSHOTS.remove(player.getUUID());
        if (snapshot == null) {
            return;
        }
        snapshot.restore(event.getRenderer().getModel());
    }

    private static final class PoseSnapshot {
        private final float bodyX;
        private final float bodyY;
        private final float bodyZ;
        private final float headX;
        private final float headY;
        private final float headZ;
        private final float rightArmX;
        private final float rightArmY;
        private final float rightArmZ;
        private final float leftArmX;
        private final float leftArmY;
        private final float leftArmZ;
        private final float rightLegX;
        private final float rightLegY;
        private final float rightLegZ;
        private final float leftLegX;
        private final float leftLegY;
        private final float leftLegZ;

        private PoseSnapshot(PlayerModel<?> model) {
            this.bodyX = model.body.xRot;
            this.bodyY = model.body.yRot;
            this.bodyZ = model.body.zRot;
            this.headX = model.head.xRot;
            this.headY = model.head.yRot;
            this.headZ = model.head.zRot;
            this.rightArmX = model.rightArm.xRot;
            this.rightArmY = model.rightArm.yRot;
            this.rightArmZ = model.rightArm.zRot;
            this.leftArmX = model.leftArm.xRot;
            this.leftArmY = model.leftArm.yRot;
            this.leftArmZ = model.leftArm.zRot;
            this.rightLegX = model.rightLeg.xRot;
            this.rightLegY = model.rightLeg.yRot;
            this.rightLegZ = model.rightLeg.zRot;
            this.leftLegX = model.leftLeg.xRot;
            this.leftLegY = model.leftLeg.yRot;
            this.leftLegZ = model.leftLeg.zRot;
        }

        private void restore(PlayerModel<?> model) {
            model.body.xRot = bodyX;
            model.body.yRot = bodyY;
            model.body.zRot = bodyZ;
            model.head.xRot = headX;
            model.head.yRot = headY;
            model.head.zRot = headZ;
            model.rightArm.xRot = rightArmX;
            model.rightArm.yRot = rightArmY;
            model.rightArm.zRot = rightArmZ;
            model.leftArm.xRot = leftArmX;
            model.leftArm.yRot = leftArmY;
            model.leftArm.zRot = leftArmZ;
            model.rightLeg.xRot = rightLegX;
            model.rightLeg.yRot = rightLegY;
            model.rightLeg.zRot = rightLegZ;
            model.leftLeg.xRot = leftLegX;
            model.leftLeg.yRot = leftLegY;
            model.leftLeg.zRot = leftLegZ;
        }
    }
}
