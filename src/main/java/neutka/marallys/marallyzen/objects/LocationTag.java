package neutka.marallys.marallyzen.objects;

import com.denizenscript.denizencore.objects.Fetchable;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.objects.core.VectorObject;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.tags.ObjectTagProcessor;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import net.minecraft.core.Position;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import neutka.marallys.marallyzen.denizen.objects.PlayerTag;
import neutka.marallys.marallyzen.objects.WorldTag;

/**
 * LocationTag implementation for NeoForge.
 * Represents a 3D location in the world.
 */
public class LocationTag implements ObjectTag, VectorObject {
    
    private double x, y, z;
    private float yaw, pitch;
    private String world;
    private Level level;

    public LocationTag(double x, double y, double z) {
        this(x, y, z, 0, 0, null, null);
    }

    public LocationTag(double x, double y, double z, float yaw, float pitch) {
        this(x, y, z, yaw, pitch, null, null);
    }

    public LocationTag(double x, double y, double z, String world) {
        this(x, y, z, 0, 0, world, null);
    }

    public LocationTag(double x, double y, double z, float yaw, float pitch, String world) {
        this(x, y, z, yaw, pitch, world, null);
    }

    public LocationTag(double x, double y, double z, float yaw, float pitch, String world, Level level) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.world = world;
        this.level = level;
    }

    public LocationTag(Position position) {
        this(position.x(), position.y(), position.z());
    }

    public LocationTag(Vec3 vec) {
        this(vec.x(), vec.y(), vec.z());
    }

    public LocationTag(Vec3 vec, Level level) {
        this(vec.x(), vec.y(), vec.z(), 0, 0, level != null ? level.dimension().location().toString() : null, level);
    }

    @Override
    public double getX() {
        return x;
    }

    @Override
    public double getY() {
        return y;
    }

    @Override
    public double getZ() {
        return z;
    }

    @Override
    public void setX(double x) {
        this.x = x;
    }

    @Override
    public void setY(double y) {
        this.y = y;
    }

    @Override
    public void setZ(double z) {
        this.z = z;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public String getWorld() {
        return world;
    }

    public Level getLevel() {
        return level;
    }

    @Override
    public String getPrefix() {
        return "Location";
    }

    @Override
    public ObjectTag setPrefix(String prefix) {
        return this;
    }

    @Override
    public String identify() {
        if (world != null) {
            return "l@" + x + "," + y + "," + z + "," + pitch + "," + yaw + "," + world;
        }
        return "l@" + x + "," + y + "," + z;
    }

    @Override
    public String identifySimple() {
        return identify();
    }

    @Override
    public String toString() {
        return identify();
    }

    @Override
    public <T extends ObjectTag> T asType(Class<T> type, TagContext context) {
        if (type == LocationTag.class) {
            return type.cast(this);
        }
        return null;
    }

    @Override
    public boolean isUnique() {
        // LocationTag is generic - locations are descriptions, not unique instances
        return false;
    }

    @Override
    public VectorObject duplicate() {
        return new LocationTag(x, y, z, yaw, pitch, world, level);
    }

    @Override
    public ObjectTag getObjectAttribute(Attribute attribute) {
        return tagProcessor.getObjectAttribute(this, attribute);
    }

    @Fetchable("l")
    public static LocationTag valueOf(String string, TagContext context) {
        if (string == null) {
            return null;
        }
        if (string.startsWith("l@")) {
            string = string.substring("l@".length());
        }
        String[] parts = string.split(",");
        if (parts.length < 3) {
            return null;
        }
        try {
            double x = Double.parseDouble(parts[0]);
            double y = Double.parseDouble(parts[1]);
            double z = Double.parseDouble(parts[2]);
            float pitch = parts.length > 3 ? Float.parseFloat(parts[3]) : 0f;
            float yaw = parts.length > 4 ? Float.parseFloat(parts[4]) : 0f;
            String world = parts.length > 5 ? parts[5] : null;
            return new LocationTag(x, y, z, yaw, pitch, world);
        }
        catch (NumberFormatException ex) {
            return null;
        }
    }

    public static boolean matches(String string) {
        return string != null && (string.startsWith("l@") || CoreUtilities.contains(string, ','));
    }

    public static ObjectTagProcessor<LocationTag> tagProcessor = new ObjectTagProcessor<>();

    public static void register() {
        tagProcessor.registerTag(LocationTag.class, ElementTag.class, "add", (attribute, object, offset) -> {
            String[] parts = offset.asString().split("[, ]+");
            if (parts.length < 3) {
                attribute.echoError("add[...] requires 3 numbers.");
                return null;
            }
            try {
                double ox = Double.parseDouble(parts[0]);
                double oy = Double.parseDouble(parts[1]);
                double oz = Double.parseDouble(parts[2]);
                return new LocationTag(object.x + ox, object.y + oy, object.z + oz, object.yaw, object.pitch, object.world, object.level);
            }
            catch (NumberFormatException ex) {
                attribute.echoError("Invalid add[...] numbers.");
                return null;
            }
        });

        tagProcessor.registerTag(ListTag.class, ElementTag.class, "find_players_within", (attribute, object, radiusElement) -> {
            if (object.level == null) {
                return new ListTag();
            }
            double radius = radiusElement.asDouble();
            double radiusSq = radius * radius;
            ListTag result = new ListTag();
            for (var player : object.level.players()) {
                if (player.distanceToSqr(object.x, object.y, object.z) <= radiusSq) {
                    if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                        result.addObject(new PlayerTag(serverPlayer));
                    }
                }
            }
            return result;
        });

        tagProcessor.registerTag(LocationTag.class, "block", (attribute, object) -> {
            return new LocationTag(Math.floor(object.x), Math.floor(object.y), Math.floor(object.z),
                    object.yaw, object.pitch, object.world, object.level);
        });

        tagProcessor.registerTag(ElementTag.class, "xyz", (attribute, object) -> {
            String value = (int) Math.floor(object.x) + "," + (int) Math.floor(object.y) + "," + (int) Math.floor(object.z);
            return new ElementTag(value, true);
        });

        tagProcessor.registerTag(WorldTag.class, "world", (attribute, object) -> {
            return new WorldTag(object.level, object.world);
        });
    }
}
