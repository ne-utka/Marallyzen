package neutka.marallys.marallyzen.trigger.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.trigger.TriggerModule;
import neutka.marallys.marallyzen.trigger.TriggerModuleRegistry;

@EventBusSubscriber(modid = Marallyzen.MODID)
public final class TriggerEventHandlers {
    private TriggerEventHandlers() {
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        TriggerModule module = TriggerModuleRegistry.get(player.level().getServer());
        if (module == null) {
            return;
        }
        if (module.onLeftClickBlock(event)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        TriggerModule module = TriggerModuleRegistry.get(player.level().getServer());
        if (module == null) {
            return;
        }
        if (module.onRightClickBlock(event)) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        TriggerModule module = TriggerModuleRegistry.get(player.level().getServer());
        if (module == null) {
            return;
        }
        module.onPlayerTick(player);
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        TriggerModule module = TriggerModuleRegistry.get(level.getServer());
        if (module == null) {
            return;
        }
        if (module.isProtected(level, event.getPos(), event.getPlayer())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        TriggerModule module = TriggerModuleRegistry.get(level.getServer());
        if (module == null) {
            return;
        }
        if (module.isProtected(level, event.getPos(), event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        TriggerModule module = TriggerModuleRegistry.get(level.getServer());
        if (module == null) {
            return;
        }
        var source = event.getExplosion().getDirectSourceEntity();
        event.getAffectedBlocks().removeIf(pos -> module.isProtected(level, pos, source));
    }
}
