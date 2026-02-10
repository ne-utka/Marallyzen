package neutka.marallys.marallyzen.client.lever;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.client.emote.ClientEmoteHandler;
import neutka.marallys.marallyzen.client.fpv.FpvQteState;
import neutka.marallys.marallyzen.blocks.InteractiveLeverBlockEntity;
import software.bernie.geckolib.constant.DataTickets;

@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT)
public final class LeverQteClient {
    private static boolean active;
    private static int sequenceLength;
    private static int stepIndex;
    private static boolean expectRight;
    private static int windowTicks;
    private static int stepTicks;
    private static int successFlashTicks;
    private static int failFlashTicks;
    private static int failedButton = -1;
    private static int shakeLoopTicks;
    private static int shakeLoopDelay;
    private static int shakeLoopCountdown;
    private static int lockedHotbarSlot = -1;
    private static boolean hudVisible;
    private static boolean finishing;
    private static int pendingDownTicks;
    private static long pendingDownStartTick;
    private static BlockPos leverPos;
    private static int pendingLeverAnimTicks;
    private static boolean leverAnimPrimed;
    private static final int DOWN_EMOTE_DELAY_TICKS = 20;

    private LeverQteClient() {
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean isHudVisible() {
        return active && hudVisible;
    }

    public static int getSequenceLength() {
        return sequenceLength;
    }

    public static int getStepIndex() {
        return stepIndex;
    }

    public static boolean isExpectRight() {
        return expectRight;
    }

    public static float getStepProgress() {
        if (!active || windowTicks <= 0) {
            return 0.0f;
        }
        return Math.min(1.0f, stepTicks / (float) windowTicks);
    }

    public static int getSuccessFlashTicks() {
        return successFlashTicks;
    }

    public static int getFailFlashTicks() {
        return failFlashTicks;
    }

    public static int getFailedButton() {
        return failedButton;
    }

    public static void start(BlockPos leverPos, int sequenceLength, int stepIndex, boolean expectRight, int windowTicks, int shakeLoopTicks, int grabTicks) {
        FpvQteState.onLeverStart();
        LeverQteClient.active = true;
        LeverQteClient.hudVisible = true;
        LeverQteClient.finishing = false;
        LeverQteClient.leverPos = leverPos;
        LeverQteClient.pendingLeverAnimTicks = 0;
        LeverQteClient.leverAnimPrimed = false;
        LeverQteClient.sequenceLength = Math.max(1, sequenceLength);
        LeverQteClient.stepIndex = Math.max(0, stepIndex);
        LeverQteClient.expectRight = expectRight;
        LeverQteClient.windowTicks = Math.max(1, windowTicks);
        LeverQteClient.stepTicks = 0;
        LeverQteClient.successFlashTicks = 0;
        LeverQteClient.failFlashTicks = 0;
        LeverQteClient.failedButton = -1;
        LeverQteClient.shakeLoopTicks = Math.max(1, shakeLoopTicks);
        LeverQteClient.shakeLoopDelay = 0;
        LeverQteClient.shakeLoopCountdown = LeverQteClient.shakeLoopTicks;

        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.player != null) {
            Inventory inv = mc.player.getInventory();
            lockedHotbarSlot = inv.getSelectedSlot();
        }
    }

    public static void step(int sequenceLength, int stepIndex, boolean expectRight, int windowTicks, boolean failedReset, int failedButton) {
        LeverQteClient.sequenceLength = Math.max(1, sequenceLength);
        LeverQteClient.stepIndex = Math.max(0, stepIndex);
        LeverQteClient.expectRight = expectRight;
        LeverQteClient.windowTicks = Math.max(1, windowTicks);
        LeverQteClient.stepTicks = 0;
        if (failedReset) {
            failFlashTicks = 18;
            successFlashTicks = 0;
            LeverQteClient.failedButton = failedButton;
        } else {
            successFlashTicks = 12;
            failFlashTicks = 0;
            LeverQteClient.failedButton = -1;
        }
    }

    public static void finishSuccess(int downTicks, long downStartTick) {
        FpvQteState.onLeverSuccess();
        finishing = true;
        hudVisible = false;
        successFlashTicks = 12;
        failFlashTicks = 0;
        failedButton = -1;
        lockedHotbarSlot = -1;
        pendingDownTicks = Math.max(1, downTicks);
        pendingDownStartTick = downStartTick;
        if (pendingDownStartTick <= 0L) {
            Minecraft mc = Minecraft.getInstance();
            long now = mc != null && mc.level != null ? mc.level.getGameTime() : 0L;
            leverAnimPrimed = triggerLeverDownAnim(pendingDownTicks);
            pendingDownStartTick = now + DOWN_EMOTE_DELAY_TICKS;
        }
    }

    public static void finishFail() {
        FpvQteState.onLeverFail();
        active = false;
        hudVisible = false;
        finishing = false;
        successFlashTicks = 0;
        failFlashTicks = 18;
        failedButton = -1;
        lockedHotbarSlot = -1;
        leverPos = null;
        pendingLeverAnimTicks = 0;
        leverAnimPrimed = false;
        LeverInteractionClient.startDownFail();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!active) {
            if (pendingDownTicks > 0 && pendingDownStartTick > 0 && mc != null && mc.level != null) {
                if (mc.level.getGameTime() >= pendingDownStartTick) {
                    startDownWithLeverAnim(pendingDownTicks);
                    pendingDownTicks = 0;
                    pendingDownStartTick = 0L;
                    active = false;
                    finishing = false;
                    FpvQteState.onQteClosed();
                }
            }
            return;
        }
        if (mc == null || mc.player == null) {
            active = false;
            return;
        }
        if (finishing && pendingDownTicks > 0 && pendingDownStartTick > 0) {
            if (mc.level != null && mc.level.getGameTime() >= pendingDownStartTick) {
                startDownWithLeverAnim(pendingDownTicks);
                pendingDownTicks = 0;
                pendingDownStartTick = 0L;
                active = false;
                finishing = false;
                FpvQteState.onQteClosed();
            }
        }
        if (pendingLeverAnimTicks > 0) {
            if (triggerLeverDownAnim(pendingDownTicks > 0 ? pendingDownTicks : 1)) {
                pendingLeverAnimTicks = 0;
            } else {
                pendingLeverAnimTicks--;
            }
        }
        LocalPlayer player = mc.player;
        if (!finishing) {
            stepTicks++;
        }
        if (successFlashTicks > 0) {
            successFlashTicks--;
        }
        if (failFlashTicks > 0) {
            failFlashTicks--;
        }
        if (lockedHotbarSlot >= 0) {
            player.getInventory().setSelectedSlot(lockedHotbarSlot);
        }
        if (mc.screen != null) {
            mc.setScreen(null);
        }

        if (shakeLoopDelay > 0) {
            shakeLoopDelay--;
        } else {
            shakeLoopCountdown--;
            if (shakeLoopCountdown <= 0) {
                neutka.marallys.marallyzen.client.fpv.MarallyzenRenderContext.setCurrentEmoteId(
                    net.minecraft.resources.Identifier.fromNamespaceAndPath("marallyzen", "lever_shake")
                );
                ClientEmoteHandler.handle(player.getUUID(), "lever_shake", false);
                shakeLoopCountdown = shakeLoopTicks;
            }
        }
    }

    private static void startDownWithLeverAnim(int downTicks) {
        if (!leverAnimPrimed && !triggerLeverDownAnim(downTicks)) {
            pendingLeverAnimTicks = 10;
        }
        LeverInteractionClient.startDown(downTicks);
        leverAnimPrimed = false;
        leverPos = null;
    }

    private static boolean triggerLeverDownAnim(int downTicks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null || leverPos == null) {
            return false;
        }
        var be = mc.level.getBlockEntity(leverPos);
        if (be instanceof InteractiveLeverBlockEntity leverEntity) {
            leverEntity.setAnimData(DataTickets.USE_TICKS, Math.max(1, downTicks));
            leverEntity.triggerAnim("controller", "pull");
            var state = mc.level.getBlockState(leverPos);
            if (state.getBlock() instanceof net.minecraft.world.level.block.LeverBlock
                && !state.getValue(net.minecraft.world.level.block.LeverBlock.POWERED)) {
                mc.level.setBlock(leverPos, state.setValue(net.minecraft.world.level.block.LeverBlock.POWERED, true), 3);
            }
            return true;
        }
        return false;
    }
}


