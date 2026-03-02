package neutka.marallys.marallyzen.door;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class DoorSavedData extends SavedData {
    public static final String DATA_NAME = "marallyzen_doors";
    private static final String KEY_DOORS = "doors";

    public static final Codec<DoorSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, DoorState.CODEC)
                    .fieldOf(KEY_DOORS)
                    .forGetter(data -> data.doors)
    ).apply(instance, map -> {
        DoorSavedData data = new DoorSavedData();
        data.doors.putAll(map);
        return data;
    }));

    public static final SavedDataType<DoorSavedData> TYPE = new SavedDataType<>(DATA_NAME, DoorSavedData::new, CODEC);

    private final Map<String, DoorState> doors = new HashMap<>();

    public static DoorSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public Map<String, DoorState> doors() {
        return Collections.unmodifiableMap(doors);
    }

    public DoorState getDoor(String id) {
        return doors.get(id);
    }

    public void putDoor(String id, DoorState state) {
        if (id == null || id.isBlank() || state == null) {
            return;
        }
        doors.put(id, state);
        setDirty();
    }

    public void removeDoor(String id) {
        if (id == null || id.isBlank()) {
            return;
        }
        doors.remove(id);
        setDirty();
    }

    public Set<String> ids() {
        return Set.copyOf(doors.keySet());
    }

    public record DoorState(
            boolean opened,
            boolean animating,
            int animationTick,
            String animationDirection
    ) {
        public static final Codec<DoorState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("opened", true).forGetter(DoorState::opened),
                Codec.BOOL.optionalFieldOf("animating", false).forGetter(DoorState::animating),
                Codec.INT.optionalFieldOf("animationTick", 0).forGetter(DoorState::animationTick),
                Codec.STRING.optionalFieldOf("animationDirection", DoorAnimationDirection.CLOSING.name())
                        .forGetter(DoorState::animationDirection)
        ).apply(instance, DoorState::new));

        public DoorAnimationDirection direction() {
            try {
                return DoorAnimationDirection.valueOf(animationDirection);
            } catch (Exception ignored) {
                return DoorAnimationDirection.CLOSING;
            }
        }
    }
}
