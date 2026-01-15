package neutka.marallys.marallyzen.denizen.events;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import neutka.marallys.marallyzen.Marallyzen;

@EventBusSubscriber(modid = Marallyzen.MODID, bus = EventBusSubscriber.Bus.GAME)
public class DenizenEventBridge {

    @SubscribeEvent
    public static void onPlayerRightClickEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!(event.getTarget() instanceof ServerPlayer target)) {
            return;
        }
        fireRightClickPlayer(player, target, event);
    }

    @SubscribeEvent
    public static void onPlayerRightClickEntitySpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!(event.getTarget() instanceof ServerPlayer target)) {
            return;
        }
        fireRightClickPlayer(player, target, event);
    }

    private static void fireRightClickPlayer(ServerPlayer player, ServerPlayer target, PlayerInteractEvent event) {
        Marallyzen.LOGGER.info("DenizenEventBridge: right click player {} -> {} (event={})",
                player.getName().getString(),
                target.getName().getString(),
                event.getClass().getSimpleName());
        PlayerRightClicksPlayerScriptEvent evt = PlayerRightClicksPlayerScriptEvent.instance;
        if (evt == null) {
            Marallyzen.LOGGER.warn("DenizenEventBridge: PlayerRightClicksPlayerScriptEvent instance is null");
            return;
        }
        evt.fireFor(player, target, event);
    }

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        if (player == null) {
            return;
        }
        PlayerChatsScriptEvent evt = PlayerChatsScriptEvent.instance;
        if (evt == null) {
            Marallyzen.LOGGER.warn("DenizenEventBridge: PlayerChatsScriptEvent instance is null");
            return;
        }
        evt.fireFor(player, event);
    }
}
