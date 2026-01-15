package neutka.marallys.marallyzen.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.client.fpv.MarallyzenFpvController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    private static final double FPV_FORWARD_OFFSET = 0.32;
    private static java.lang.reflect.Method cachedSetPosition;

    @Inject(method = "setup", at = @At("TAIL"), require = 0)
    private void marallyzen$fpvEmote(BlockGetter level, Entity entity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        var cameraController = neutka.marallys.marallyzen.client.camera.CameraManager.getInstance().getCameraController();
        if (neutka.marallys.marallyzen.client.cutscene.editor.CutscenePreviewPlayer.isPreviewActive()) {
            float time = neutka.marallys.marallyzen.client.cutscene.editor.CutscenePreviewPlayer.getActivePreviewTime()
                + (float) tickDelta;
            if (neutka.marallys.marallyzen.client.cutscene.editor.CutscenePreviewPlayer.applyPreviewCamera(time, cameraController)) {
                Camera camera = (Camera) (Object) this;
                applyCameraPosition(camera, cameraController.getPosition());
                return;
            }
        }
        if (cameraController.isActive()) {
            Camera camera = (Camera) (Object) this;
            applyCameraPosition(camera, cameraController.getInterpolatedPosition(tickDelta));
            return;
        }
        if (!(entity instanceof Player player)) {
            return;
        }
        boolean shouldApply = MarallyzenFpvController.shouldApply(player);
        if (shouldApply && !thirdPerson) {
            Camera camera = (Camera) (Object) this;
            Vec3 forward = player.getLookAngle();
            if (forward.lengthSqr() > 1.0E-6) {
                Vec3 pos = camera.getPosition();
                Vec3 shifted = pos.add(forward.normalize().scale(FPV_FORWARD_OFFSET));
                applyCameraPosition(camera, shifted);
            }
        }
        // Camera application is handled in FpvEventHandler based on interpreter state.
        // Keeping this hook minimal to honor the requested injection point.
    }

    private static void applyCameraPosition(Camera camera, Vec3 position) {
        try {
            if (cachedSetPosition == null) {
                cachedSetPosition = resolveSetPosition(camera.getClass());
            }
            if (cachedSetPosition == null) {
                return;
            }
            if (cachedSetPosition.getParameterCount() == 3) {
                cachedSetPosition.invoke(camera, position.x, position.y, position.z);
            } else {
                cachedSetPosition.invoke(camera, position);
            }
        } catch (Exception ignored) {
        }
    }

    private static java.lang.reflect.Method resolveSetPosition(Class<?> cameraClass) {
        try {
            java.lang.reflect.Method method = cameraClass.getMethod("setPosition", double.class, double.class, double.class);
            method.setAccessible(true);
            return method;
        } catch (Exception ignored) {
        }
        try {
            java.lang.reflect.Method method = cameraClass.getMethod("setPosition", Vec3.class);
            method.setAccessible(true);
            return method;
        } catch (Exception ignored) {
        }
        try {
            java.lang.reflect.Method method = cameraClass.getMethod("setPosition", org.joml.Vector3f.class);
            method.setAccessible(true);
            return method;
        } catch (Exception ignored) {
        }
        return null;
    }
}
