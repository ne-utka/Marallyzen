package neutka.marallys.marallyzen.cutscene.world;

import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Captured world state for cutscene playback (initial snapshot + diffs).
 */
public class CutsceneWorldTrack {
    private final Header header;
    private final List<ChunkSnapshot> chunks;
    private final List<BlockChange> blockChanges;
    private final List<BlockEntityChange> blockEntityChanges;
    private final List<WeatherChange> weatherChanges;
    private final List<ParticleEvent> particleEvents;
    private final List<SeekFrame> seekFrames;

    public CutsceneWorldTrack(
        Header header,
        List<ChunkSnapshot> chunks,
        List<BlockChange> blockChanges,
        List<BlockEntityChange> blockEntityChanges,
        List<WeatherChange> weatherChanges,
        List<ParticleEvent> particleEvents,
        List<SeekFrame> seekFrames
    ) {
        this.header = Objects.requireNonNull(header, "header");
        this.chunks = chunks == null ? new ArrayList<>() : new ArrayList<>(chunks);
        this.blockChanges = blockChanges == null ? new ArrayList<>() : new ArrayList<>(blockChanges);
        this.blockEntityChanges = blockEntityChanges == null ? new ArrayList<>() : new ArrayList<>(blockEntityChanges);
        this.weatherChanges = weatherChanges == null ? new ArrayList<>() : new ArrayList<>(weatherChanges);
        this.particleEvents = particleEvents == null ? new ArrayList<>() : new ArrayList<>(particleEvents);
        this.seekFrames = seekFrames == null ? new ArrayList<>() : new ArrayList<>(seekFrames);
    }

    public Header getHeader() {
        return header;
    }

    public List<ChunkSnapshot> getChunks() {
        return new ArrayList<>(chunks);
    }

    public List<BlockChange> getBlockChanges() {
        return new ArrayList<>(blockChanges);
    }

    public List<BlockEntityChange> getBlockEntityChanges() {
        return new ArrayList<>(blockEntityChanges);
    }

    public List<WeatherChange> getWeatherChanges() {
        return new ArrayList<>(weatherChanges);
    }

    public List<ParticleEvent> getParticleEvents() {
        return new ArrayList<>(particleEvents);
    }

    public List<SeekFrame> getSeekFrames() {
        return new ArrayList<>(seekFrames);
    }

    public record Header(
        String dimension,
        long startWorldTime,
        int minX,
        int minY,
        int minZ,
        int maxX,
        int maxY,
        int maxZ,
        int tickRate
    ) {
    }

    public record ChunkSnapshot(
        int chunkX,
        int chunkZ,
        List<SectionData> sections,
        List<CompoundTag> blockEntities,
        List<LightSectionData> skyLight,
        List<LightSectionData> blockLight
    ) {
    }

    public record SectionData(int sectionIndex, byte[] data) {
    }

    public record LightSectionData(int sectionIndex, byte[] data) {
    }

    public record BlockChange(long tick, long pos, CompoundTag stateTag, CompoundTag blockEntityTag) {
    }

    public record BlockEntityChange(long tick, long pos, CompoundTag tag) {
    }

    public record WeatherChange(
        long tick,
        long worldTime,
        boolean raining,
        float rainLevel,
        boolean thundering,
        float thunderLevel
    ) {
    }

    public record ParticleEvent(
        long tick,
        String typeId,
        double x,
        double y,
        double z,
        double dx,
        double dy,
        double dz,
        int count,
        byte[] optionsData
    ) {
    }

    public record SeekFrame(
        long tick,
        int blockIndex,
        int blockEntityIndex,
        int weatherIndex,
        int particleIndex,
        List<ChunkSnapshot> chunks
    ) {
    }
}
