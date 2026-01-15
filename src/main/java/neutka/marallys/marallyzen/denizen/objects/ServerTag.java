package neutka.marallys.marallyzen.denizen.objects;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.flags.AbstractFlagTracker;
import com.denizenscript.denizencore.flags.FlaggableObject;
import com.denizenscript.denizencore.objects.Fetchable;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ObjectTagProcessor;
import com.denizenscript.denizencore.tags.TagContext;

public class ServerTag implements ObjectTag, FlaggableObject {
    private static final ServerTag INSTANCE = new ServerTag();

    private ServerTag() {
    }

    public static ServerTag get() {
        return INSTANCE;
    }

    @Override
    public String getPrefix() {
        return "server";
    }

    @Override
    public ObjectTag setPrefix(String prefix) {
        return this;
    }

    @Override
    public boolean isUnique() {
        return true;
    }

    @Override
    public String identify() {
        return "server";
    }

    @Override
    public String identifySimple() {
        return identify();
    }

    @Override
    public Object getJavaObject() {
        return this;
    }

    @Override
    public AbstractFlagTracker getFlagTracker() {
        return DenizenCore.serverFlagMap;
    }

    @Override
    public void reapplyTracker(AbstractFlagTracker tracker) {
    }

    @Override
    public String getReasonNotFlaggable() {
        return "server flags unavailable";
    }

    @Override
    public ObjectTag getObjectAttribute(Attribute attribute) {
        return tagProcessor.getObjectAttribute(this, attribute);
    }

    @Fetchable("server")
    public static ServerTag valueOf(String string, TagContext context) {
        if (string == null) {
            return null;
        }
        if ("server".equalsIgnoreCase(string) || "server".equalsIgnoreCase(string.replace("server@", ""))) {
            return INSTANCE;
        }
        return null;
    }

    public static boolean matches(String string) {
        return string != null && "server".equalsIgnoreCase(string);
    }

    public static ObjectTagProcessor<ServerTag> tagProcessor = new ObjectTagProcessor<>();

    public static void register() {
        AbstractFlagTracker.registerFlagHandlers(tagProcessor);

        tagProcessor.registerTag(ListTag.class, "online_players", (attribute, object) -> {
            return new ListTag(MarallyzenServerLookup.getOnlinePlayers(), PlayerTag::new);
        });

        tagProcessor.registerTag(PlayerTag.class, ElementTag.class, "match_player", (attribute, object, name) -> {
            if (name == null) {
                return null;
            }
            var player = MarallyzenServerLookup.getPlayer(name.asString());
            return player != null ? new PlayerTag(player) : null;
        });

        tagProcessor.registerTag(PlayerTag.class, ElementTag.class, "match_offline_player", (attribute, object, name) -> {
            if (name == null) {
                return null;
            }
            var player = MarallyzenServerLookup.getPlayer(name.asString());
            return player != null ? new PlayerTag(player) : null;
        });
    }
}
