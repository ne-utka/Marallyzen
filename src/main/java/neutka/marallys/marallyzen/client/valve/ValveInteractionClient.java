package neutka.marallys.marallyzen.client.valve;

import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.client.emote.ClientEmoteHandler;
import neutka.marallys.marallyzen.client.fpv.MarallyzenRenderContext;
import neutka.marallys.marallyzen.network.ValveQteDownAckPacket;
import neutka.marallys.marallyzen.network.NetworkHelper;

@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT)
public final class ValveInteractionClient {
    private static boolean active;
    private static Vec3 targetPos = Vec3.ZERO;
    private static float targetYaw;
    private static float targetPitch;
    private static int grabTicks;
    private static int shakeTicks;
    private static int downTicks;
    private static int downTicksRemaining;
    private static boolean prevHideGui;
    private static boolean hideGuiCaptured;
    private static CameraType prevCameraType;
    private static boolean cameraCaptured;
    private static boolean downActive;
    private static boolean downEmoteStarted;
    private static int postDoneDelayTicks;

    private ValveInteractionClient() {
    }

    public static boolean isBlockingInput() {
        return active;
    }

    public static float getTargetYaw() {
        return targetYaw;
    }

    public static float getTargetPitch() {
        return targetPitch;
    }

    public static void start(Vec3 targetPos, float targetYaw, float targetPitch, int grabTicks, int shakeTicks, int downTicks) {
        ValveInteractionClient.targetPos = targetPos;
        ValveInteractionClient.targetYaw = targetYaw;
        ValveInteractionClient.targetPitch = targetPitch;
        ValveInteractionClient.grabTicks = Math.max(1, grabTicks);
        ValveInteractionClient.shakeTicks = Math.max(1, shakeTicks);
        ValveInteractionClient.downTicks = Math.max(1, downTicks);
        ValveInteractionClient.downTicksRemaining = 0;
        ValveInteractionClient.downActive = false;
        ValveInteractionClient.downEmoteStarted = false;
        ValveInteractionClient.postDoneDelayTicks = 0;
        ValveInteractionClient.active = true;

        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.player != null) {
            captureHideGui(mc);
            captureCameraType(mc);
            forceFirstPerson(mc);
            applyPlayerLock(mc.player);
            MarallyzenRenderContext.setFpvEmoteEnabled(true);
            MarallyzenRenderContext.setCurrentEmoteId(
                net.minecraft.resources.Identifier.fromNamespaceAndPath("marallyzen", "valve_grab")
            );
            ClientEmoteHandler.handle(mc.player.getUUID(), "valve_grab", false);
        }
    }

    public static void startDown(int downTicks) {
        ValveInteractionClient.downTicks = Math.max(1, downTicks);
        ValveInteractionClient.downTicksRemaining = ValveInteractionClient.downTicks;
        ValveInteractionClient.downActive = true;
        ValveInteractionClient.downEmoteStarted = false;
        ValveInteractionClient.postDoneDelayTicks = 0;
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.player != null) {
            NetworkHelper.sendToServer(new ValveQteDownAckPacket(0));
        }
    }

    public static void startDownFail() {
        if (downActive) {
            return;
        }
        ValveInteractionClient.downTicks = Math.max(1, ValveInteractionClient.downTicks);
        ValveInteractionClient.downTicksRemaining = ValveInteractionClient.downTicks;
        ValveInteractionClient.downActive = true;
        ValveInteractionClient.downEmoteStarted = false;
        ValveInteractionClient.postDoneDelayTicks = 0;
    }

    public static void cancel() {
        active = false;
        downActive = false;
        downEmoteStarted = false;
        postDoneDelayTicks = 0;
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            MarallyzenRenderContext.setFpvEmoteEnabled(false);
            restoreHideGui(mc);
            restoreCameraType(mc);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!active) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) {
            active = false;
            restoreHideGui(mc);
            return;
        }
        forceFirstPerson(mc);
        LocalPlayer player = mc.player;
        applyPlayerLock(player);

        if (downActive) {
            downTicksRemaining--;
            if (!downEmoteStarted && downTicksRemaining > 0) {
                ClientEmoteHandler.stop(player);
                MarallyzenRenderContext.setFpvEmoteEnabled(true);
                neutka.marallys.marallyzen.client.fpv.FpvEmoteInvoker.play(player, "valve_done");
                downEmoteStarted = true;
            }
            if (downTicksRemaining <= 0) {
                downActive = false;
                downEmoteStarted = false;
                postDoneDelayTicks = 10;
            }
            return;
        }

        if (postDoneDelayTicks > 0) {
            postDoneDelayTicks--;
            if (postDoneDelayTicks <= 0) {
                active = false;
                MarallyzenRenderContext.setFpvEmoteEnabled(false);
                restoreHideGui(mc);
                restoreCameraType(mc);
            }
            return;
        }
    }

    private static void applyPlayerLock(LocalPlayer player) {
        player.setDeltaMovement(Vec3.ZERO);
        player.setSprinting(false);
        Minecraft mc = Minecraft.getInstance();
        player.setXRot(targetPitch);
        player.setYRot(targetYaw);
        player.xRotO = targetPitch;
        player.yRotO = targetYaw;
        player.setYHeadRot(targetYaw);
        player.yHeadRotO = targetYaw;
        player.setYBodyRot(targetYaw);
        player.yBodyRotO = targetYaw;
        player.setPos(targetPos.x, targetPos.y, targetPos.z);
    }

    private static void captureHideGui(Minecraft mc) {
        if (mc == null || mc.options == null || hideGuiCaptured) {
            return;
        }
        prevHideGui = mc.options.hideGui;
        mc.options.hideGui = true;
        hideGuiCaptured = true;
    }

    private static void restoreHideGui(Minecraft mc) {
        if (!hideGuiCaptured || mc == null || mc.options == null) {
            hideGuiCaptured = false;
            return;
        }
        mc.options.hideGui = prevHideGui;
        hideGuiCaptured = false;
    }

    private static void captureCameraType(Minecraft mc) {
        if (mc == null || mc.options == null || cameraCaptured) {
            return;
        }
        prevCameraType = mc.options.getCameraType();
        cameraCaptured = true;
    }

    private static void forceFirstPerson(Minecraft mc) {
        if (mc == null || mc.options == null) {
            return;
        }
        mc.options.setCameraType(CameraType.FIRST_PERSON);
    }

    private static void restoreCameraType(Minecraft mc) {
        if (!cameraCaptured || mc == null || mc.options == null) {
            cameraCaptured = false;
            return;
        }
        mc.options.setCameraType(prevCameraType);
        cameraCaptured = false;
    }

}



