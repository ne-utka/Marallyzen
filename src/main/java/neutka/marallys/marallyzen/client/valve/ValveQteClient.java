package neutka.marallys.marallyzen.client.valve;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.LeverBlock;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.client.emote.ClientEmoteHandler;
import neutka.marallys.marallyzen.client.fpv.FpvQteState;
import neutka.marallys.marallyzen.blocks.InteractiveValveBlockEntity;
import software.bernie.geckolib.constant.DataTickets;

@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT)
public final class ValveQteClient {
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
    private static BlockPos valvePos;
    private static int pendingValveAnimTicks;
    private static boolean valveAnimPrimed;
    private static boolean acceptingInput;
    private static final int DOWN_EMOTE_DELAY_TICKS = 0;

    private ValveQteClient() {
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

    public static void start(BlockPos valvePos, int sequenceLength, int stepIndex, boolean expectRight, int windowTicks, int shakeLoopTicks, int grabTicks) {
        FpvQteState.onValveStart();
        ValveQteClient.active = true;
        ValveQteClient.hudVisible = true;
        ValveQteClient.finishing = false;
        ValveQteClient.valvePos = valvePos;
        ValveQteClient.pendingValveAnimTicks = 0;
        ValveQteClient.valveAnimPrimed = false;
        ValveQteClient.acceptingInput = true;
        ValveQteClient.sequenceLength = Math.max(1, sequenceLength);
        ValveQteClient.stepIndex = Math.max(0, stepIndex);
        ValveQteClient.expectRight = expectRight;
        ValveQteClient.windowTicks = Math.max(1, windowTicks);
        ValveQteClient.stepTicks = 0;
        ValveQteClient.successFlashTicks = 0;
        ValveQteClient.failFlashTicks = 0;
        ValveQteClient.failedButton = -1;
        ValveQteClient.shakeLoopTicks = Math.max(1, shakeLoopTicks);
        ValveQteClient.shakeLoopDelay = 0;
        ValveQteClient.shakeLoopCountdown = ValveQteClient.shakeLoopTicks;

        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.player != null) {
            Inventory inv = mc.player.getInventory();
            lockedHotbarSlot = inv.getSelectedSlot();
        }
    }

    public static void step(int sequenceLength, int stepIndex, boolean expectRight, int windowTicks, boolean failedReset, int failedButton) {
        ValveQteClient.sequenceLength = Math.max(1, sequenceLength);
        ValveQteClient.stepIndex = Math.max(0, stepIndex);
        ValveQteClient.expectRight = expectRight;
        ValveQteClient.windowTicks = Math.max(1, windowTicks);
        ValveQteClient.stepTicks = 0;
        if (failedReset) {
            failFlashTicks = 18;
            successFlashTicks = 0;
            ValveQteClient.failedButton = failedButton;
        } else {
            successFlashTicks = 12;
            failFlashTicks = 0;
            ValveQteClient.failedButton = -1;
        }
    }

    public static void finishSuccess(int downTicks, long downStartTick) {
        FpvQteState.onValveSuccess();
        finishing = true;
        hudVisible = false;
        acceptingInput = false;
        successFlashTicks = 12;
        failFlashTicks = 0;
        failedButton = -1;
        lockedHotbarSlot = -1;
        pendingDownTicks = Math.max(1, downTicks);
        pendingDownStartTick = downStartTick;
        if (pendingDownStartTick <= 0L) {
            Minecraft mc = Minecraft.getInstance();
            long now = mc != null && mc.level != null ? mc.level.getGameTime() : 0L;
            if (mc != null && mc.player != null) {
                ClientEmoteHandler.stop(mc.player);
            }
            valveAnimPrimed = triggerValveDownAnim(pendingDownTicks);
            pendingDownStartTick = now + DOWN_EMOTE_DELAY_TICKS;
        }
    }

    public static void finishFail() {
        FpvQteState.onValveFail();
        active = false;
        hudVisible = false;
        finishing = false;
        acceptingInput = false;
        successFlashTicks = 0;
        failFlashTicks = 18;
        failedButton = -1;
        lockedHotbarSlot = -1;
        valvePos = null;
        pendingValveAnimTicks = 0;
        valveAnimPrimed = false;
        ValveInteractionClient.startDownFail();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!active) {
            if (pendingDownTicks > 0 && pendingDownStartTick > 0 && mc != null && mc.level != null) {
                if (mc.level.getGameTime() >= pendingDownStartTick) {
                    startDownWithValveAnim(pendingDownTicks);
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
                startDownWithValveAnim(pendingDownTicks);
                pendingDownTicks = 0;
                pendingDownStartTick = 0L;
                active = false;
                finishing = false;
                FpvQteState.onQteClosed();
            }
        }
        if (pendingValveAnimTicks > 0) {
            if (triggerValveDownAnim(pendingDownTicks > 0 ? pendingDownTicks : 1)) {
                pendingValveAnimTicks = 0;
            } else {
                pendingValveAnimTicks--;
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

        // Spin is now triggered on player input, not on a timer.
    }

    private static void startDownWithValveAnim(int downTicks) {
        if (!valveAnimPrimed && !triggerValveDownAnim(downTicks)) {
            pendingValveAnimTicks = 10;
        }
        ValveInteractionClient.startDown(downTicks);
        valveAnimPrimed = false;
        valvePos = null;
    }

    private static boolean triggerValveDownAnim(int downTicks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null || valvePos == null) {
            return false;
        }
        var be = mc.level.getBlockEntity(valvePos);
        if (be instanceof InteractiveValveBlockEntity valveEntity) {
            valveEntity.setAnimData(DataTickets.USE_TICKS, Math.max(1, downTicks));
            valveEntity.triggerAnim("controller", "spin");
            var state = mc.level.getBlockState(valvePos);
            if (state.getBlock() instanceof LeverBlock
                && !state.getValue(LeverBlock.POWERED)) {
                mc.level.setBlock(valvePos, state.setValue(LeverBlock.POWERED, true), 3);
            }
            return true;
        }
        return false;
    }

    private static void triggerValveWobbleAnim(int wobbleTicks, boolean rightClick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null || valvePos == null) {
            return;
        }
        var be = mc.level.getBlockEntity(valvePos);
        if (be instanceof InteractiveValveBlockEntity valveEntity) {
            valveEntity.setAnimData(DataTickets.USE_TICKS, Math.max(1, wobbleTicks));
            valveEntity.triggerAnim("controller", rightClick ? "wobble_right" : "wobble_left");
        }
    }

    public static void onLocalInput(boolean rightClick) {
        if (!active || !acceptingInput || finishing) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) {
            return;
        }
        neutka.marallys.marallyzen.client.fpv.MarallyzenRenderContext.setCurrentEmoteId(
            net.minecraft.resources.Identifier.fromNamespaceAndPath("marallyzen", "valve_spin")
        );
        FpvQteState.onValveInputPulse();
        ClientEmoteHandler.handle(mc.player.getUUID(), "valve_spin", false);
        triggerValveWobbleAnim(shakeLoopTicks, rightClick);
    }
}



