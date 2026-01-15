package neutka.marallys.marallyzen.items;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.audio.MarallyzenSounds;
import neutka.marallys.marallyzen.blocks.InteractiveBlockTargeting;
import neutka.marallys.marallyzen.client.narration.NarrationManager;
import neutka.marallys.marallyzen.client.narration.NarrationOverlay;
import neutka.marallys.marallyzen.util.NarrationIcons;

@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class FlashlightItem extends Item {
    private static final String ON_KEY = "flashlight_on";
    private static ItemStack lastSelectedStack = ItemStack.EMPTY;
    private static Boolean lastNarrationOnState = null;

    public FlashlightItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            boolean isOn = isOn(stack);
            boolean newState = !isOn;
            setOn(stack, newState);
            neutka.marallys.marallyzen.server.FlashlightStateManager.updateFlashlightState(
                serverPlayer,
                newState
            );
            level.playSound(null, player.blockPosition(),
                newState ? MarallyzenSounds.FLASHLIGHT_TURN_ON.get() : MarallyzenSounds.FLASHLIGHT_TURN_OFF.get(),
                SoundSource.PLAYERS, 1.0f, 1.0f);
        }

        return InteractionResultHolder.success(stack);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        ItemStack mainHand = mc.player.getMainHandItem();
        boolean holdingFlashlight = mainHand.getItem() instanceof FlashlightItem;
        NarrationManager manager = NarrationManager.getInstance();

        if (holdingFlashlight) {
            boolean isOn = isOn(mainHand);
            if (isTargetingInteractiveBlock(mc)) {
                if (isFlashlightNarrationText(manager.getActive() != null ? manager.getActive().getText() : null)) {
                    manager.startNarrationFadeOut();
                }
                return;
            }
            if (manager.getActive() != null
                && !isFlashlightNarrationText(manager.getActive().getText())) {
                return;
            }
            if (!ItemStack.isSameItem(mainHand, lastSelectedStack)) {
                lastSelectedStack = mainHand.copy();
                lastNarrationOnState = isOn;
                startFlashlightNarration(manager, isOn);
            } else if (!isFlashlightNarrationActive(manager, isOn)) {
                lastNarrationOnState = isOn;
                startFlashlightNarration(manager, isOn);
            } else if (shouldRestartFlashlightNarration(manager)) {
                lastNarrationOnState = isOn;
                startFlashlightNarration(manager, isOn);
            }
        } else {
            lastSelectedStack = ItemStack.EMPTY;
            lastNarrationOnState = null;
            if (isFlashlightNarrationText(manager.getActive() != null ? manager.getActive().getText() : null)) {
                manager.startNarrationFadeOut();
            }
        }
    }

    private static void startFlashlightNarration(NarrationManager manager, boolean isOn) {
        manager.startNarration(buildNarration(isOn), null, 5, 999999, 3);
    }

    private static boolean shouldRestartFlashlightNarration(NarrationManager manager) {
        NarrationOverlay active = manager.getActive();
        Boolean lastState = lastNarrationOnState;
        if (lastState == null || !isFlashlightNarrationActive(manager, lastState)) {
            return true;
        }
        return active.getState() == NarrationOverlay.State.FADE_OUT;
    }

    private static boolean isFlashlightNarrationActive(NarrationManager manager, boolean isOn) {
        NarrationOverlay active = manager.getActive();
        if (active == null || active.getText() == null) {
            return false;
        }
        return active.getText().getString().equals(buildNarration(isOn).getString());
    }

    private static boolean isTargetingInteractiveBlock(net.minecraft.client.Minecraft mc) {
        HitResult hit = mc.hitResult;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        BlockHitResult blockHit = (BlockHitResult) hit;
        return InteractiveBlockTargeting.getType(mc.level.getBlockState(blockHit.getBlockPos()))
            != InteractiveBlockTargeting.Type.NONE;
    }

    private static Component buildNarration(boolean isOn) {
        Component pkm = NarrationIcons.rmb();
        String key = isOn
            ? "narration.marallyzen.flashlight_turn_off"
            : "narration.marallyzen.flashlight_turn_on";
        return Component.translatable(key, pkm);
    }

    private static boolean isFlashlightNarrationText(Component text) {
        if (text == null) {
            return false;
        }
        String value = text.getString();
        return value.equals(buildNarration(true).getString())
            || value.equals(buildNarration(false).getString());
    }

    public static boolean isOn(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return false;
        }
        return data.copyTag().getBoolean(ON_KEY);
    }

    public static void setOn(ItemStack stack, boolean on) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        var tag = data.copyTag();
        tag.putBoolean(ON_KEY, on);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            net.minecraft.client.renderer.item.ItemProperties.register(
                MarallyzenItems.FLASHLIGHT.get(),
                ResourceLocation.fromNamespaceAndPath("marallyzen", "flashlight_on"),
                (stack, level, entity, seed) -> isOn(stack) ? 1.0f : 0.0f
            );
        });
    }
}
