package neutka.marallys.marallyzen.cutscene.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.neoforged.fml.loading.FMLPaths;
import neutka.marallys.marallyzen.Marallyzen;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Storage for cutscene world tracks (compressed NBT).
 */
public final class CutsceneWorldStorage {
    private static final int FORMAT_VERSION = 1;
    private static final String TAG_VERSION = "version";
    private static final String TAG_HEADER = "header";
    private static final String TAG_CHUNKS = "chunks";
    private static final String TAG_BLOCKS = "block_changes";
    private static final String TAG_BLOCK_ENTITIES = "block_entity_changes";
    private static final String TAG_WEATHER = "weather_changes";
    private static final String TAG_PARTICLES = "particle_events";
    private static final String TAG_SEEK_FRAMES = "seek_frames";

    private CutsceneWorldStorage() {
    }

    public static Path getWorldTrackPath(String sceneId) {
        return FMLPaths.CONFIGDIR.get()
            .resolve(Marallyzen.MODID)
            .resolve("scenes")
            .resolve(sceneId + ".world.nbt");
    }

    public static void save(String sceneId, CutsceneWorldTrack track) throws IOException {
        if (sceneId == null || sceneId.isBlank() || track == null) {
            return;
        }
        Path path = getWorldTrackPath(sceneId);
        if (!Files.exists(path.getParent())) {
            Files.createDirectories(path.getParent());
        }
        CompoundTag root = write(track);
        writeCompressed(root, path);
    }

    public static CutsceneWorldTrack load(String sceneId) throws IOException {
        if (sceneId == null || sceneId.isBlank()) {
            return null;
        }
        Path path = getWorldTrackPath(sceneId);
        if (!Files.exists(path)) {
            return null;
        }
        CompoundTag root = readCompressed(path);
        return read(root);
    }

    public static byte[] encode(CutsceneWorldTrack track) throws IOException {
        if (track == null) {
            return new byte[0];
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        writeCompressed(write(track), output);
        return output.toByteArray();
    }

    public static CutsceneWorldTrack decode(byte[] payload) throws IOException {
        if (payload == null || payload.length == 0) {
            return null;
        }
        try (ByteArrayInputStream input = new ByteArrayInputStream(payload)) {
            CompoundTag root = NbtIo.readCompressed(input, NbtAccounter.unlimitedHeap());
            return read(root);
        }
    }

    private static CompoundTag write(CutsceneWorldTrack track) {
        CompoundTag root = new CompoundTag();
        root.putInt(TAG_VERSION, FORMAT_VERSION);
        root.put(TAG_HEADER, writeHeader(track.getHeader()));
        root.put(TAG_CHUNKS, writeChunks(track.getChunks()));
        root.put(TAG_BLOCKS, writeBlockChanges(track.getBlockChanges()));
        root.put(TAG_BLOCK_ENTITIES, writeBlockEntityChanges(track.getBlockEntityChanges()));
        root.put(TAG_WEATHER, writeWeatherChanges(track.getWeatherChanges()));
        root.put(TAG_PARTICLES, writeParticles(track.getParticleEvents()));
        root.put(TAG_SEEK_FRAMES, writeSeekFrames(track.getSeekFrames()));
        return root;
    }

    private static CutsceneWorldTrack read(CompoundTag root) {
        if (root == null || root.isEmpty()) {
            return null;
        }
        CutsceneWorldTrack.Header header = readHeader(root.getCompound(TAG_HEADER));
        List<CutsceneWorldTrack.ChunkSnapshot> chunks =
            readChunks(root.getList(TAG_CHUNKS, Tag.TAG_COMPOUND));
        List<CutsceneWorldTrack.BlockChange> blocks =
            readBlockChanges(root.getList(TAG_BLOCKS, Tag.TAG_COMPOUND));
        List<CutsceneWorldTrack.BlockEntityChange> blockEntities =
            readBlockEntityChanges(root.getList(TAG_BLOCK_ENTITIES, Tag.TAG_COMPOUND));
        List<CutsceneWorldTrack.WeatherChange> weather =
            readWeatherChanges(root.getList(TAG_WEATHER, Tag.TAG_COMPOUND));
        List<CutsceneWorldTrack.ParticleEvent> particles =
            readParticles(root.getList(TAG_PARTICLES, Tag.TAG_COMPOUND));
        List<CutsceneWorldTrack.SeekFrame> seekFrames =
            readSeekFrames(root.getList(TAG_SEEK_FRAMES, Tag.TAG_COMPOUND));
        return new CutsceneWorldTrack(header, chunks, blocks, blockEntities, weather, particles, seekFrames);
    }

    private static CompoundTag writeHeader(CutsceneWorldTrack.Header header) {
        CompoundTag tag = new CompoundTag();
        tag.putString("dimension", header.dimension());
        tag.putLong("start_time", header.startWorldTime());
        tag.putInt("min_x", header.minX());
        tag.putInt("min_y", header.minY());
        tag.putInt("min_z", header.minZ());
        tag.putInt("max_x", header.maxX());
        tag.putInt("max_y", header.maxY());
        tag.putInt("max_z", header.maxZ());
        tag.putInt("tick_rate", header.tickRate());
        return tag;
    }

    private static CutsceneWorldTrack.Header readHeader(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return new CutsceneWorldTrack.Header("", 0L, 0, 0, 0, 0, 0, 0, 20);
        }
        return new CutsceneWorldTrack.Header(
            tag.getString("dimension"),
            tag.getLong("start_time"),
            tag.getInt("min_x"),
            tag.getInt("min_y"),
            tag.getInt("min_z"),
            tag.getInt("max_x"),
            tag.getInt("max_y"),
            tag.getInt("max_z"),
            tag.getInt("tick_rate")
        );
    }

    private static ListTag writeChunks(List<CutsceneWorldTrack.ChunkSnapshot> chunks) {
        ListTag list = new ListTag();
        for (CutsceneWorldTrack.ChunkSnapshot chunk : chunks) {
            CompoundTag tag = new CompoundTag();
            tag.putInt("x", chunk.chunkX());
            tag.putInt("z", chunk.chunkZ());
            tag.put("sections", writeSections(chunk.sections()));
            tag.put("block_entities", writeBlockEntities(chunk.blockEntities()));
            tag.put("sky_light", writeLightSections(chunk.skyLight()));
            tag.put("block_light", writeLightSections(chunk.blockLight()));
            list.add(tag);
        }
        return list;
    }

    private static List<CutsceneWorldTrack.ChunkSnapshot> readChunks(ListTag list) {
        List<CutsceneWorldTrack.ChunkSnapshot> chunks = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            int x = tag.getInt("x");
            int z = tag.getInt("z");
            List<CutsceneWorldTrack.SectionData> sections =
                readSections(tag.getList("sections", Tag.TAG_COMPOUND));
            List<CompoundTag> blockEntities =
                readBlockEntities(tag.getList("block_entities", Tag.TAG_COMPOUND));
            List<CutsceneWorldTrack.LightSectionData> skyLight =
                readLightSections(tag.getList("sky_light", Tag.TAG_COMPOUND));
            List<CutsceneWorldTrack.LightSectionData> blockLight =
                readLightSections(tag.getList("block_light", Tag.TAG_COMPOUND));
            chunks.add(new CutsceneWorldTrack.ChunkSnapshot(x, z, sections, blockEntities, skyLight, blockLight));
        }
        return chunks;
    }

    private static ListTag writeSections(List<CutsceneWorldTrack.SectionData> sections) {
        ListTag list = new ListTag();
        if (sections == null) {
            return list;
        }
        for (CutsceneWorldTrack.SectionData section : sections) {
            CompoundTag tag = new CompoundTag();
            tag.putInt("index", section.sectionIndex());
            tag.putByteArray("data", section.data());
            list.add(tag);
        }
        return list;
    }

    private static List<CutsceneWorldTrack.SectionData> readSections(ListTag list) {
        List<CutsceneWorldTrack.SectionData> sections = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            sections.add(new CutsceneWorldTrack.SectionData(tag.getInt("index"), tag.getByteArray("data")));
        }
        return sections;
    }

    private static ListTag writeLightSections(List<CutsceneWorldTrack.LightSectionData> sections) {
        ListTag list = new ListTag();
        if (sections == null) {
            return list;
        }
        for (CutsceneWorldTrack.LightSectionData section : sections) {
            CompoundTag tag = new CompoundTag();
            tag.putInt("index", section.sectionIndex());
            tag.putByteArray("data", section.data());
            list.add(tag);
        }
        return list;
    }

    private static List<CutsceneWorldTrack.LightSectionData> readLightSections(ListTag list) {
        List<CutsceneWorldTrack.LightSectionData> sections = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            sections.add(new CutsceneWorldTrack.LightSectionData(tag.getInt("index"), tag.getByteArray("data")));
        }
        return sections;
    }

    private static ListTag writeBlockEntities(List<CompoundTag> blockEntities) {
        ListTag list = new ListTag();
        if (blockEntities == null) {
            return list;
        }
        for (CompoundTag tag : blockEntities) {
            if (tag != null) {
                list.add(tag);
            }
        }
        return list;
    }

    private static List<CompoundTag> readBlockEntities(ListTag list) {
        List<CompoundTag> blockEntities = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            blockEntities.add(list.getCompound(i));
        }
        return blockEntities;
    }

    private static ListTag writeBlockChanges(List<CutsceneWorldTrack.BlockChange> changes) {
        ListTag list = new ListTag();
        for (CutsceneWorldTrack.BlockChange change : changes) {
            CompoundTag tag = new CompoundTag();
            tag.putLong("tick", change.tick());
            tag.putLong("pos", change.pos());
            tag.put("state", change.stateTag());
            if (change.blockEntityTag() != null && !change.blockEntityTag().isEmpty()) {
                tag.put("block_entity", change.blockEntityTag());
            }
            list.add(tag);
        }
        return list;
    }

    private static List<CutsceneWorldTrack.BlockChange> readBlockChanges(ListTag list) {
        List<CutsceneWorldTrack.BlockChange> changes = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            long tick = tag.getLong("tick");
            long pos = tag.getLong("pos");
            CompoundTag stateTag = tag.getCompound("state");
            CompoundTag beTag = tag.contains("block_entity") ? tag.getCompound("block_entity") : null;
            changes.add(new CutsceneWorldTrack.BlockChange(tick, pos, stateTag, beTag));
        }
        return changes;
    }

    private static ListTag writeBlockEntityChanges(List<CutsceneWorldTrack.BlockEntityChange> changes) {
        ListTag list = new ListTag();
        for (CutsceneWorldTrack.BlockEntityChange change : changes) {
            CompoundTag tag = new CompoundTag();
            tag.putLong("tick", change.tick());
            tag.putLong("pos", change.pos());
            tag.put("tag", change.tag());
            list.add(tag);
        }
        return list;
    }

    private static List<CutsceneWorldTrack.BlockEntityChange> readBlockEntityChanges(ListTag list) {
        List<CutsceneWorldTrack.BlockEntityChange> changes = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            changes.add(new CutsceneWorldTrack.BlockEntityChange(
                tag.getLong("tick"),
                tag.getLong("pos"),
                tag.getCompound("tag")
            ));
        }
        return changes;
    }

    private static ListTag writeWeatherChanges(List<CutsceneWorldTrack.WeatherChange> changes) {
        ListTag list = new ListTag();
        for (CutsceneWorldTrack.WeatherChange change : changes) {
            CompoundTag tag = new CompoundTag();
            tag.putLong("tick", change.tick());
            tag.putLong("world_time", change.worldTime());
            tag.putBoolean("raining", change.raining());
            tag.putFloat("rain_level", change.rainLevel());
            tag.putBoolean("thundering", change.thundering());
            tag.putFloat("thunder_level", change.thunderLevel());
            list.add(tag);
        }
        return list;
    }

    private static List<CutsceneWorldTrack.WeatherChange> readWeatherChanges(ListTag list) {
        List<CutsceneWorldTrack.WeatherChange> changes = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            changes.add(new CutsceneWorldTrack.WeatherChange(
                tag.getLong("tick"),
                tag.getLong("world_time"),
                tag.getBoolean("raining"),
                tag.getFloat("rain_level"),
                tag.getBoolean("thundering"),
                tag.getFloat("thunder_level")
            ));
        }
        return changes;
    }

    private static ListTag writeParticles(List<CutsceneWorldTrack.ParticleEvent> events) {
        ListTag list = new ListTag();
        for (CutsceneWorldTrack.ParticleEvent event : events) {
            CompoundTag tag = new CompoundTag();
            tag.putLong("tick", event.tick());
            tag.putString("type", event.typeId());
            tag.putDouble("x", event.x());
            tag.putDouble("y", event.y());
            tag.putDouble("z", event.z());
            tag.putDouble("dx", event.dx());
            tag.putDouble("dy", event.dy());
            tag.putDouble("dz", event.dz());
            tag.putInt("count", event.count());
            if (event.optionsData() != null && event.optionsData().length > 0) {
                tag.putByteArray("data", event.optionsData());
            }
            list.add(tag);
        }
        return list;
    }

    private static List<CutsceneWorldTrack.ParticleEvent> readParticles(ListTag list) {
        List<CutsceneWorldTrack.ParticleEvent> events = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            events.add(new CutsceneWorldTrack.ParticleEvent(
                tag.getLong("tick"),
                tag.getString("type"),
                tag.getDouble("x"),
                tag.getDouble("y"),
                tag.getDouble("z"),
                tag.getDouble("dx"),
                tag.getDouble("dy"),
                tag.getDouble("dz"),
                tag.getInt("count"),
                tag.contains("data") ? tag.getByteArray("data") : new byte[0]
            ));
        }
        return events;
    }

    private static ListTag writeSeekFrames(List<CutsceneWorldTrack.SeekFrame> frames) {
        ListTag list = new ListTag();
        for (CutsceneWorldTrack.SeekFrame frame : frames) {
            CompoundTag tag = new CompoundTag();
            tag.putLong("tick", frame.tick());
            tag.putInt("block_index", frame.blockIndex());
            tag.putInt("block_entity_index", frame.blockEntityIndex());
            tag.putInt("weather_index", frame.weatherIndex());
            tag.putInt("particle_index", frame.particleIndex());
            tag.put("chunks", writeChunks(frame.chunks()));
            list.add(tag);
        }
        return list;
    }

    private static List<CutsceneWorldTrack.SeekFrame> readSeekFrames(ListTag list) {
        List<CutsceneWorldTrack.SeekFrame> frames = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            frames.add(new CutsceneWorldTrack.SeekFrame(
                tag.getLong("tick"),
                tag.getInt("block_index"),
                tag.getInt("block_entity_index"),
                tag.getInt("weather_index"),
                tag.getInt("particle_index"),
                readChunks(tag.getList("chunks", Tag.TAG_COMPOUND))
            ));
        }
        return frames;
    }

    private static void writeCompressed(CompoundTag tag, Path path) throws IOException {
        try (OutputStream output = Files.newOutputStream(path)) {
            writeCompressed(tag, output);
        }
    }

    private static void writeCompressed(CompoundTag tag, OutputStream output) throws IOException {
        NbtIo.writeCompressed(tag, output);
    }

    private static CompoundTag readCompressed(Path path) throws IOException {
        try (InputStream input = Files.newInputStream(path)) {
            return NbtIo.readCompressed(input, NbtAccounter.unlimitedHeap());
        }
    }
}
