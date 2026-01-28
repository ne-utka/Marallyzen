package neutka.marallys.marallyzen.client.lever;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.client.emote.ClientEmoteHandler;
import neutka.marallys.marallyzen.client.fpv.MarallyzenRenderContext;

@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class LeverInteractionClient {
    private static boolean active;
    private static Vec3 targetPos = Vec3.ZERO;
    private static float targetYaw;
    private static float targetPitch;
    private static int grabTicks;
    private static int shakeTicks;
    private static int downTicks;
    private static int tick;
    private static boolean moveActive;
    private static int moveTicks;
    private static boolean prevHideGui;
    private static boolean hideGuiCaptured;

    private LeverInteractionClient() {
    }

    public static boolean isBlockingInput() {
        return active || moveActive;
    }

    public static boolean isMoveActive() {
        return moveActive;
    }

    public static void startMove(Vec3 targetPos, float targetYaw, float targetPitch, int moveTicks) {
        LeverInteractionClient.targetPos = targetPos;
        LeverInteractionClient.targetYaw = targetYaw;
        LeverInteractionClient.targetPitch = targetPitch;
        LeverInteractionClient.moveTicks = Math.max(1, moveTicks);
        LeverInteractionClient.moveActive = true;
    }

    public static void start(Vec3 targetPos, float targetYaw, float targetPitch, int grabTicks, int shakeTicks, int downTicks) {
        LeverInteractionClient.targetPos = targetPos;
        LeverInteractionClient.targetYaw = targetYaw;
        LeverInteractionClient.targetPitch = targetPitch;
        LeverInteractionClient.grabTicks = Math.max(1, grabTicks);
        LeverInteractionClient.shakeTicks = Math.max(1, shakeTicks);
        LeverInteractionClient.downTicks = Math.max(1, downTicks);
        LeverInteractionClient.tick = 0;
        LeverInteractionClient.active = true;
        LeverInteractionClient.moveActive = false;

        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.player != null) {
            captureHideGui(mc);
            applyPlayerLock(mc.player);
            MarallyzenRenderContext.setFpvEmoteEnabled(true);
            MarallyzenRenderContext.setCurrentEmoteId(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("marallyzen", "lever_grab_shake")
            );
            ClientEmoteHandler.handle(mc.player.getUUID(), "lever_grab_shake", false);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (moveActive) {
            moveTicks--;
            if (moveTicks <= 0) {
                moveActive = false;
            }
        }
        if (!active) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) {
            active = false;
            restoreHideGui(mc);
            return;
        }
        LocalPlayer player = mc.player;
        applyPlayerLock(player);

        tick++;
        if (tick == grabTicks + shakeTicks) {
            MarallyzenRenderContext.setCurrentEmoteId(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("marallyzen", "lever_down")
            );
            ClientEmoteHandler.handle(player.getUUID(), "lever_down", false);
        } else if (tick >= grabTicks + shakeTicks + downTicks) {
            active = false;
            MarallyzenRenderContext.setFpvEmoteEnabled(false);
            restoreHideGui(mc);
        }
    }

    private static void applyPlayerLock(LocalPlayer player) {
        player.setDeltaMovement(Vec3.ZERO);
        player.setSprinting(false);
        player.setXRot(targetPitch);
        player.setYRot(targetYaw);
        player.xRotO = targetPitch;
        player.yRotO = targetYaw;
        player.setYHeadRot(targetYaw);
        player.setYBodyRot(targetYaw);
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

    @SubscribeEvent
    public static void onMovementInputUpdate(MovementInputUpdateEvent event) {
        if (!moveActive) {
            return;
        }
        var input = event.getInput();
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.player != null) {
            LocalPlayer player = mc.player;
            Vec3 toTarget = targetPos.subtract(player.position());
            Vec3 flat = new Vec3(toTarget.x, 0.0, toTarget.z);
            double dist = flat.length();
            if (dist <= 0.06) {
                moveActive = false;
                input.forwardImpulse = 0.0f;
                input.leftImpulse = 0.0f;
                input.up = false;
                input.down = false;
                input.left = false;
                input.right = false;
                return;
            }
            float yaw = (float) (Math.toDegrees(Math.atan2(flat.z, flat.x)) - 90.0f);
            targetYaw = yaw;
            player.setYRot(targetYaw);
            player.setXRot(targetPitch);
            player.yHeadRot = targetYaw;
            player.yBodyRot = targetYaw;
        }

        input.forwardImpulse = 1.0f;
        input.leftImpulse = 0.0f;
        input.jumping = false;
        input.shiftKeyDown = false;
        input.up = true;
        input.down = false;
        input.left = false;
        input.right = false;
    }
}
