package neutka.marallys.marallyzen.instance;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import neutka.marallys.marallyzen.Marallyzen;

@EventBusSubscriber(modid = Marallyzen.MODID, bus = EventBusSubscriber.Bus.GAME)
public class InstanceRestrictionHandler {
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        if (InstanceSessionManager.getInstance().isPlayerRestricted(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (InstanceSessionManager.getInstance().isPlayerRestricted(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        var player = event.getPlayer();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (InstanceSessionManager.getInstance().isPlayerRestricted(serverPlayer)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (InstanceSessionManager.getInstance().isPlayerRestricted(player)) {
            player.closeContainer();
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        InstanceSessionManager manager = InstanceSessionManager.getInstance();
        manager.tickPendingTeleport(player);
        if (!manager.isPlayerRestricted(player)) {
            return;
        }
        if (player.containerMenu != player.inventoryMenu) {
            player.closeContainer();
        }
    }

    @SubscribeEvent
    public static void onCommand(CommandEvent event) {
        var parse = event.getParseResults();
        if (parse == null) {
            return;
        }
        var source = parse.getContext().getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (InstanceSessionManager.getInstance().isPlayerRestricted(player) && !player.hasPermissions(2)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        InstanceSessionManager manager = InstanceSessionManager.getInstance();
        PlayerState state = manager.getPlayerState(player.getUUID());
        if (state == PlayerState.LEAVE_PENDING || state == PlayerState.LOGIN_RESTORE_PENDING) {
            return;
        }
        if (manager.isPlayerInSession(player.getUUID()) && !isInstanceDimension(player) && !manager.isPendingInstanceRespawn(player)) {
            manager.leaveSession(player, "dimension_escape");
            return;
        }
        if (!manager.isPlayerInSession(player.getUUID()) || (!isInstanceDimension(player) && !manager.isPendingInstanceRespawn(player))) {
            manager.clearRestrictions(player);
        }
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!InstanceSessionManager.getInstance().isPlayerRestricted(player)) {
            return;
        }
        event.setDamageMultiplier(0.0f);
        event.setCanceled(true);
    }

    private static boolean isInstanceDimension(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        var key = player.level().dimension().location();
        return Marallyzen.MODID.equals(key.getNamespace()) && key.getPath().startsWith("instance/");
    }
}
