package neutka.marallys.marallyzen.objects;

import com.denizenscript.denizencore.objects.Fetchable;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ObjectTagProcessor;
import com.denizenscript.denizencore.tags.TagContext;
import net.minecraft.world.level.Level;

public class WorldTag implements ObjectTag {
    private final Level level;
    private final String worldId;

    public WorldTag(Level level, String worldId) {
        this.level = level;
        this.worldId = worldId;
    }

    public String getName() {
        if (level != null) {
            if (level.dimension() == Level.NETHER) {
                return "world_nether";
            }
            if (level.dimension() == Level.END) {
                return "world_the_end";
            }
            if (level.dimension() == Level.OVERWORLD) {
                return "world";
            }
        }
        if (worldId != null) {
            return worldId;
        }
        return "world";
    }

    @Override
    public String getPrefix() {
        return "world";
    }

    @Override
    public ObjectTag setPrefix(String prefix) {
        return this;
    }

    @Override
    public boolean isUnique() {
        return false;
    }

    @Override
    public String identify() {
        return "w@" + getName();
    }

    @Override
    public String identifySimple() {
        return identify();
    }

    @Override
    public Object getJavaObject() {
        return level;
    }

    @Override
    public ObjectTag getObjectAttribute(Attribute attribute) {
        return tagProcessor.getObjectAttribute(this, attribute);
    }

    @Fetchable("w")
    public static WorldTag valueOf(String string, TagContext context) {
        if (string == null) {
            return null;
        }
        if (string.startsWith("w@")) {
            string = string.substring("w@".length());
        }
        return new WorldTag(null, string);
    }

    public static boolean matches(String string) {
        return string != null;
    }

    public static ObjectTagProcessor<WorldTag> tagProcessor = new ObjectTagProcessor<>();

    public static void register() {
        tagProcessor.registerTag(ElementTag.class, "name", (attribute, object) -> {
            return new ElementTag(object.getName(), true);
        });
    }
}
