package neutka.marallys.marallyzen.denizen.objects;

import com.denizenscript.denizencore.flags.AbstractFlagTracker;
import com.denizenscript.denizencore.flags.FlaggableObject;
import com.denizenscript.denizencore.objects.Fetchable;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ObjectTagProcessor;
import com.denizenscript.denizencore.tags.TagContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import neutka.marallys.marallyzen.denizen.storage.MarallyzenFlagStore;
import neutka.marallys.marallyzen.objects.LocationTag;
import neutka.marallys.marallyzen.util.PermissionHelper;

import java.util.UUID;

public class EntityTag implements ObjectTag, FlaggableObject {
    private final Entity entity;

    public EntityTag(Entity entity) {
        this.entity = entity;
    }

    public Entity getEntity() {
        return entity;
    }

    public UUID getUUID() {
        return entity.getUUID();
    }

    @Override
    public String getPrefix() {
        return "entity";
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
        return "e@" + getUUID();
    }

    @Override
    public String identifySimple() {
        return identify();
    }

    @Override
    public Object getJavaObject() {
        return entity;
    }

    @Override
    public AbstractFlagTracker getFlagTracker() {
        if (entity instanceof ServerPlayer player) {
            return MarallyzenFlagStore.getPlayerTracker(player.getUUID());
        }
        return MarallyzenFlagStore.getEntityTracker(getUUID());
    }

    @Override
    public void reapplyTracker(AbstractFlagTracker tracker) {
    }

    @Override
    public String getReasonNotFlaggable() {
        return "entity not available";
    }

    @Override
    public ObjectTag getObjectAttribute(Attribute attribute) {
        return tagProcessor.getObjectAttribute(this, attribute);
    }

    @Fetchable("e")
    public static EntityTag valueOf(String string, TagContext context) {
        return null;
    }

    public static boolean matches(String string) {
        return string != null && string.startsWith("e@");
    }

    public static ObjectTagProcessor<EntityTag> tagProcessor = new ObjectTagProcessor<>();

    public static void register() {
        AbstractFlagTracker.registerFlagHandlers(tagProcessor);

        tagProcessor.registerTag(ElementTag.class, "name", (attribute, object) -> {
            return new ElementTag(object.entity.getName().getString(), true);
        });

        tagProcessor.registerTag(ElementTag.class, ElementTag.class, "has_permission", (attribute, object, permission) -> {
            if (object.entity instanceof ServerPlayer player) {
                return new ElementTag(PermissionHelper.hasPermission(player, permission.asString()));
            }
            return new ElementTag(false);
        });

        tagProcessor.registerTag(LocationTag.class, "location", (attribute, object) -> {
            return new LocationTag(object.entity.position(), object.entity.level());
        });
    }
}
