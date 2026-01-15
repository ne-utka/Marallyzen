package neutka.marallys.marallyzen.denizen.events;

import com.denizenscript.denizencore.events.ScriptEvent;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.ServerChatEvent;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.MarallyzenScriptEntryData;
import neutka.marallys.marallyzen.denizen.objects.PlayerTag;

public class PlayerChatsScriptEvent extends ScriptEvent {
    public static PlayerChatsScriptEvent instance;
    private ServerPlayer player;
    private ServerChatEvent event;
    private String message;

    public PlayerChatsScriptEvent() {
        instance = this;
        registerCouldMatcher("player chats");
        registerSwitches("bukkit_priority");
    }

    public void fireFor(ServerPlayer player, ServerChatEvent event) {
        this.player = player;
        this.event = event;
        this.message = event != null ? event.getRawText() : null;
        fire();
    }

    @Override
    public boolean matches(ScriptPath path) {
        return player != null && message != null;
    }

    @Override
    public ScriptEntryData getScriptEntryData() {
        MarallyzenScriptEntryData data = new MarallyzenScriptEntryData();
        if (player != null) {
            data.setPlayer(new PlayerTag(player));
        }
        return data;
    }

    @Override
    public ObjectTag getContext(String name) {
        if ("player".equals(name) && player != null) {
            return new PlayerTag(player);
        }
        if ("message".equals(name)) {
            return new ElementTag(message == null ? "" : message, true);
        }
        return super.getContext(name);
    }

    @Override
    public void cancellationChanged() {
        if (event == null) {
            return;
        }
        try {
            java.lang.reflect.Method method = event.getClass().getMethod("setCanceled", boolean.class);
            method.invoke(event, cancelled);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void run(ScriptPath path) {
        Marallyzen.LOGGER.info("PlayerChatsScriptEvent: running path '{}' for {}",
                path.event,
                player != null ? player.getName().getString() : "null");
        super.run(path);
    }
}
