package neutka.marallys.marallyzen.mirror;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class MirrorSavedData extends SavedData {
    public static final String DATA_NAME = "marallyzen_mirror_links";
    private static final String KEY_LINKS = "mirrorLinks";

    public static final Codec<MirrorSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(BlockPos.CODEC, BlockPos.CODEC)
                    .optionalFieldOf(KEY_LINKS, Map.of())
                    .forGetter(data -> data.mirrorLinks)
    ).apply(instance, links -> {
        MirrorSavedData data = new MirrorSavedData();
        data.mirrorLinks.putAll(links);
        return data;
    }));

    public static final SavedDataType<MirrorSavedData> TYPE =
            new SavedDataType<>(DATA_NAME, MirrorSavedData::new, CODEC);

    private final Map<BlockPos, BlockPos> mirrorLinks = new HashMap<>();

    public static MirrorSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public Map<BlockPos, BlockPos> links() {
        return Collections.unmodifiableMap(mirrorLinks);
    }

    public BlockPos getLinked(BlockPos pos) {
        return mirrorLinks.get(pos);
    }

    public void putLink(BlockPos a, BlockPos b) {
        if (a == null || b == null) {
            return;
        }
        mirrorLinks.put(a.immutable(), b.immutable());
        setDirty();
    }

    public void removeLink(BlockPos pos) {
        if (pos == null) {
            return;
        }
        mirrorLinks.remove(pos);
        setDirty();
    }
}

