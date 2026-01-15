package neutka.marallys.marallyzen.denizen.events;

import com.denizenscript.denizencore.events.ScriptEvent;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import neutka.marallys.marallyzen.MarallyzenScriptEntryData;
import neutka.marallys.marallyzen.denizen.objects.EntityTag;
import neutka.marallys.marallyzen.denizen.objects.PlayerTag;

public class PlayerRightClicksPlayerScriptEvent extends ScriptEvent {
    public static PlayerRightClicksPlayerScriptEvent instance;
    private ServerPlayer player;
    private ServerPlayer target;
    private PlayerInteractEvent event;

    public PlayerRightClicksPlayerScriptEvent() {
        instance = this;
        registerCouldMatcher("player right clicks player");
    }

    public PlayerRightClicksPlayerScriptEvent setPlayer(ServerPlayer player) {
        this.player = player;
        return this;
    }

    public PlayerRightClicksPlayerScriptEvent setTarget(ServerPlayer target) {
        this.target = target;
        return this;
    }

    public PlayerRightClicksPlayerScriptEvent setEvent(PlayerInteractEvent event) {
        this.event = event;
        return this;
    }

    public void fireFor(ServerPlayer player, ServerPlayer target, PlayerInteractEvent event) {
        this.player = player;
        this.target = target;
        this.event = event;
        fire();
    }

    @Override
    public boolean matches(ScriptPath path) {
        return player != null && target != null;
    }

    @Override
    public ScriptEntryData getScriptEntryData() {
        MarallyzenScriptEntryData data = new MarallyzenScriptEntryData();
        if (player != null) {
            data.setPlayer(new PlayerTag(player));
        }
        if (target != null) {
            data.setEntity(new EntityTag(target));
        }
        return data;
    }

    @Override
    public ObjectTag getContext(String name) {
        if ("player".equals(name) && player != null) {
            return new PlayerTag(player);
        }
        if ("entity".equals(name) && target != null) {
            return new EntityTag(target);
        }
        if ("hand".equals(name) && event != null) {
            try {
                java.lang.reflect.Method method = event.getClass().getMethod("getHand");
                Object hand = method.invoke(event);
                if (hand != null) {
                    return new ElementTag(hand.toString(), true);
                }
            } catch (Exception ignored) {
            }
        }
        return super.getContext(name);
    }

    @Override
    public void cancellationChanged() {
        if (event != null) {
            // PlayerInteractEvent doesn't expose setCanceled in all NeoForge mappings, so reflect.
            try {
                java.lang.reflect.Method method = event.getClass().getMethod("setCanceled", boolean.class);
                method.invoke(event, cancelled);
                return;
            } catch (Exception ignored) {
            }
            try {
                java.lang.reflect.Method method = event.getClass().getMethod("setCancellationResult", net.minecraft.world.InteractionResult.class);
                if (cancelled) {
                    method.invoke(event, net.minecraft.world.InteractionResult.FAIL);
                }
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void run(ScriptPath path) {
        neutka.marallys.marallyzen.Marallyzen.LOGGER.info(
                "PlayerRightClicksPlayerScriptEvent: running path '{}' for {} -> {}",
                path.event,
                player != null ? player.getName().getString() : "null",
                target != null ? target.getName().getString() : "null");
        super.run(path);
    }
}
