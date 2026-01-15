package neutka.marallys.marallyzen.denizen.objects;

import com.denizenscript.denizencore.flags.AbstractFlagTracker;
import com.denizenscript.denizencore.flags.FlaggableObject;
import com.denizenscript.denizencore.objects.Fetchable;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ObjectTagProcessor;
import com.denizenscript.denizencore.tags.TagContext;
import net.minecraft.server.level.ServerPlayer;
import neutka.marallys.marallyzen.denizen.storage.MarallyzenFlagStore;
import neutka.marallys.marallyzen.util.PermissionHelper;
import neutka.marallys.marallyzen.objects.LocationTag;
import neutka.marallys.marallyzen.denizen.objects.ItemTag;

import java.util.UUID;

public class PlayerTag implements ObjectTag, FlaggableObject {
    private final ServerPlayer player;

    public PlayerTag(ServerPlayer player) {
        this.player = player;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public UUID getUUID() {
        return player.getUUID();
    }

    @Override
    public String getPrefix() {
        return "player";
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
        return "p@" + getUUID();
    }

    @Override
    public String identifySimple() {
        return identify();
    }

    @Override
    public Object getJavaObject() {
        return player;
    }

    @Override
    public AbstractFlagTracker getFlagTracker() {
        return MarallyzenFlagStore.getPlayerTracker(getUUID());
    }

    @Override
    public void reapplyTracker(AbstractFlagTracker tracker) {
    }

    @Override
    public String getReasonNotFlaggable() {
        return "player not available";
    }

    @Override
    public ObjectTag getObjectAttribute(Attribute attribute) {
        return tagProcessor.getObjectAttribute(this, attribute);
    }

    @Fetchable("p")
    public static PlayerTag valueOf(String string, TagContext context) {
        if (string == null) {
            return null;
        }
        if (string.startsWith("p@")) {
            string = string.substring("p@".length());
        }
        UUID uuid = null;
        try {
            uuid = UUID.fromString(string);
        }
        catch (IllegalArgumentException ignored) {
        }
        ServerPlayer player = MarallyzenServerLookup.getPlayer(uuid != null ? uuid.toString() : string);
        return player != null ? new PlayerTag(player) : null;
    }

    public static boolean matches(String string) {
        return string != null;
    }

    public static ObjectTagProcessor<PlayerTag> tagProcessor = new ObjectTagProcessor<>();

    public static void register() {
        AbstractFlagTracker.registerFlagHandlers(tagProcessor);

        tagProcessor.registerTag(ElementTag.class, "name", (attribute, object) -> {
            return new ElementTag(object.player.getName().getString(), true);
        });

        tagProcessor.registerTag(ElementTag.class, "is_sneaking", (attribute, object) -> {
            return new ElementTag(object.player.isCrouching());
        });

        tagProcessor.registerTag(ElementTag.class, "is_op", (attribute, object) -> {
            return new ElementTag(object.player.hasPermissions(2));
        });

        tagProcessor.registerTag(ElementTag.class, ElementTag.class, "has_permission", (attribute, object, permission) -> {
            return new ElementTag(PermissionHelper.hasPermission(object.player, permission.asString()));
        });

        tagProcessor.registerTag(ElementTag.class, ElementTag.class, "in_group", (attribute, object, group) -> {
            String groupName = group.asString();
            if (groupName.isEmpty()) {
                return new ElementTag(false);
            }
            if (object.player.getTeam() != null && object.player.getTeam().getName().equalsIgnoreCase(groupName)) {
                return new ElementTag(true);
            }
            return new ElementTag(PermissionHelper.hasPermission(object.player, groupName)
                    || PermissionHelper.hasPermission(object.player, "group." + groupName));
        });

        tagProcessor.registerTag(LocationTag.class, "location", (attribute, object) -> {
            return new LocationTag(object.player.position(), object.player.level());
        });

        tagProcessor.registerTag(ItemTag.class, "item_in_hand", (attribute, object) -> {
            return new ItemTag(object.player.getMainHandItem());
        });
    }
}
