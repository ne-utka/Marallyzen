package neutka.marallys.marallyzen.replay;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLPaths;
import neutka.marallys.marallyzen.Marallyzen;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ReplayStorage {
    private static final int FORMAT_VERSION = 1;
    private static final String TAG_HEADER = "header";
    private static final String TAG_MARKERS = "markers";
    private static final String TAG_SERVER = "server";
    private static final String TAG_CLIENT = "client";

    private ReplayStorage() {
    }

    public static Path getReplayDir() {
        return FMLPaths.CONFIGDIR.get().resolve(Marallyzen.MODID).resolve("replays");
    }

    public static Path getReplayPath(String replayId) {
        return getReplayDir().resolve(replayId + ".replay.nbt");
    }

    public static Path getServerTrackPath(String replayId) {
        return getReplayDir().resolve(replayId + ".server.nbt");
    }

    public static Path getClientTrackPath(String replayId) {
        return getReplayDir().resolve(replayId + ".client.nbt");
    }

    public static void saveReplay(ReplayData data) throws IOException {
        ensureReplayDir();
        CompoundTag root = new CompoundTag();
        root.put(TAG_HEADER, writeHeader(data.getHeader()));
        root.put(TAG_MARKERS, writeMarkers(data.getMarkers()));
        if (data.getServerTrack() != null) {
            root.put(TAG_SERVER, writeServerTrack(data.getServerTrack()));
        }
        if (data.getClientTrack() != null) {
            root.put(TAG_CLIENT, writeClientTrack(data.getClientTrack()));
        }
        writeCompressed(root, getReplayPath(data.getId()));
    }

    public static void saveServerTrack(String replayId, ReplayHeader header,
                                       List<ReplayMarker> markers, ReplayServerTrack track) throws IOException {
        ensureReplayDir();
        CompoundTag root = new CompoundTag();
        root.put(TAG_HEADER, writeHeader(header));
        root.put(TAG_MARKERS, writeMarkers(markers));
        root.put(TAG_SERVER, writeServerTrack(track));
        writeCompressed(root, getServerTrackPath(replayId));
    }

    public static void saveClientTrack(String replayId, ReplayHeader header,
                                       List<ReplayMarker> markers, ReplayClientTrack track) throws IOException {
        ensureReplayDir();
        CompoundTag root = new CompoundTag();
        root.put(TAG_HEADER, writeHeader(header));
        root.put(TAG_MARKERS, writeMarkers(markers));
        root.put(TAG_CLIENT, writeClientTrack(track));
        writeCompressed(root, getClientTrackPath(replayId));
    }

    public static ReplayData loadReplay(Path path) throws IOException {
        String id = stripExtension(path.getFileName().toString());
        return loadReplay(path, id);
    }

    public static ReplayData loadReplay(String replayId) throws IOException {
        if (replayId == null || replayId.isEmpty()) {
            return null;
        }
        Path combined = getReplayPath(replayId);
        if (Files.exists(combined)) {
            return loadReplay(combined, replayId);
        }
        Path serverPath = getServerTrackPath(replayId);
        Path clientPath = getClientTrackPath(replayId);
        ReplayData serverData = Files.exists(serverPath) ? loadReplay(serverPath, replayId) : null;
        ReplayData clientData = Files.exists(clientPath) ? loadReplay(clientPath, replayId) : null;
        return ReplayMerger.merge(serverData, clientData, replayId);
    }

    public static ReplayData loadReplay(Path path, String replayId) throws IOException {
        CompoundTag root = readCompressed(path);
        ReplayHeader header = readHeader(root.getCompound(TAG_HEADER));
        List<ReplayMarker> markers = readMarkers(root.getList(TAG_MARKERS, Tag.TAG_COMPOUND));
        ReplayServerTrack serverTrack = root.contains(TAG_SERVER) ? readServerTrack(root.getCompound(TAG_SERVER)) : null;
        ReplayClientTrack clientTrack = root.contains(TAG_CLIENT) ? readClientTrack(root.getCompound(TAG_CLIENT)) : null;
        return new ReplayData(replayId, header, markers, serverTrack, clientTrack);
    }

    private static void ensureReplayDir() throws IOException {
        Path dir = getReplayDir();
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
    }

    private static String stripExtension(String name) {
        int idx = name.lastIndexOf('.');
        if (idx <= 0) {
            return name;
        }
        return name.substring(0, idx);
    }

    private static CompoundTag writeHeader(ReplayHeader header) {
        CompoundTag tag = new CompoundTag();
        int version = header != null ? header.version() : FORMAT_VERSION;
        int tickRate = header != null ? header.tickRate() : ReplaySettings.DEFAULT_TICK_RATE;
        int keyframeInterval = header != null ? header.keyframeInterval() : ReplaySettings.DEFAULT_KEYFRAME_INTERVAL;
        long duration = header != null ? header.durationTicks() : 0L;
        String dimension = header != null ? header.dimension() : "";
        tag.putInt("version", version);
        tag.putInt("tick_rate", tickRate);
        tag.putInt("keyframe_interval", keyframeInterval);
        tag.putLong("duration", duration);
        tag.putString("dimension", dimension == null ? "" : dimension);
        return tag;
    }

    private static ReplayHeader readHeader(CompoundTag tag) {
        int version = tag.getInt("version");
        int tickRate = tag.getInt("tick_rate");
        int keyframeInterval = tag.getInt("keyframe_interval");
        long duration = tag.getLong("duration");
        String dimension = tag.getString("dimension");
        return new ReplayHeader(version, tickRate, keyframeInterval, duration, dimension);
    }

    private static ListTag writeMarkers(List<ReplayMarker> markers) {
        ListTag list = new ListTag();
        if (markers == null) {
            return list;
        }
        for (ReplayMarker marker : markers) {
            if (marker == null) {
                continue;
            }
            CompoundTag tag = new CompoundTag();
            tag.putLong("time", marker.time());
            tag.putString("label", marker.label() == null ? "" : marker.label());
            list.add(tag);
        }
        return list;
    }

    private static List<ReplayMarker> readMarkers(ListTag list) {
        java.util.ArrayList<ReplayMarker> markers = new java.util.ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            long time = tag.getLong("time");
            String label = tag.getString("label");
            markers.add(new ReplayMarker(time, label));
        }
        return markers;
    }

    private static CompoundTag writeServerTrack(ReplayServerTrack track) {
        CompoundTag tag = new CompoundTag();
        tag.putString("dimension", track.getDimension() == null ? "" : track.getDimension());

        ListTag entities = new ListTag();
        for (ReplayEntityInfo info : track.getEntities().values()) {
            CompoundTag infoTag = new CompoundTag();
            infoTag.putString("uuid", info.id().toString());
            infoTag.putString("type", info.entityTypeId() == null ? "" : info.entityTypeId());
            infoTag.putBoolean("player", info.player());
            infoTag.putString("name", info.name() == null ? "" : info.name());
            if (info.skinValue() != null) {
                infoTag.putString("skin_value", info.skinValue());
            }
            if (info.skinSignature() != null) {
                infoTag.putString("skin_signature", info.skinSignature());
            }
            entities.add(infoTag);
        }
        tag.put("entities", entities);

        ListTag snapshots = new ListTag();
        for (ReplayServerSnapshot snapshot : track.getSnapshots()) {
            CompoundTag snapTag = new CompoundTag();
            snapTag.putLong("tick", snapshot.tick());
            snapTag.putLong("world_time", snapshot.worldTime());
            snapTag.putBoolean("raining", snapshot.raining());
            snapTag.putFloat("rain_level", snapshot.rainLevel());
            snapTag.putBoolean("thundering", snapshot.thundering());
            snapTag.putFloat("thunder_level", snapshot.thunderLevel());
            ListTag entityFrames = new ListTag();
            for (ReplayEntityFrame frame : snapshot.entities()) {
                CompoundTag frameTag = new CompoundTag();
                frameTag.putString("uuid", frame.id().toString());
                frameTag.put("pos", writeVec3(frame.position()));
                frameTag.putFloat("yaw", frame.yaw());
                frameTag.putFloat("pitch", frame.pitch());
                frameTag.putFloat("head_yaw", frame.headYaw());
                frameTag.putFloat("body_yaw", frame.bodyYaw());
                entityFrames.add(frameTag);
            }
            snapTag.put("entities", entityFrames);
            snapshots.add(snapTag);
        }
        tag.put("snapshots", snapshots);

        ListTag chunkSnapshots = new ListTag();
        for (ReplayChunkSnapshot snapshot : track.getChunkSnapshots()) {
            if (snapshot == null) {
                continue;
            }
            CompoundTag chunkTag = new CompoundTag();
            chunkTag.putLong("tick", snapshot.tick());
            chunkTag.putInt("x", snapshot.chunkX());
            chunkTag.putInt("z", snapshot.chunkZ());
            ListTag sections = new ListTag();
            if (snapshot.sections() != null) {
                for (ReplayChunkSectionData section : snapshot.sections()) {
                    if (section == null) {
                        continue;
                    }
                    CompoundTag sectionTag = new CompoundTag();
                    sectionTag.putInt("index", section.sectionIndex());
                    sectionTag.putByteArray("data", section.data());
                    sections.add(sectionTag);
                }
            }
            chunkTag.put("sections", sections);
            ListTag blockEntities = new ListTag();
            if (snapshot.blockEntities() != null) {
                for (CompoundTag blockEntity : snapshot.blockEntities()) {
                    if (blockEntity != null) {
                        blockEntities.add(blockEntity.copy());
                    }
                }
            }
            chunkTag.put("block_entities", blockEntities);
            chunkSnapshots.add(chunkTag);
        }
        tag.put("chunk_snapshots", chunkSnapshots);
        return tag;
    }

    private static ReplayServerTrack readServerTrack(CompoundTag tag) {
        ReplayServerTrack track = new ReplayServerTrack(tag.getString("dimension"));
        ListTag entities = tag.getList("entities", Tag.TAG_COMPOUND);
        for (int i = 0; i < entities.size(); i++) {
            CompoundTag infoTag = entities.getCompound(i);
            UUID id = UUID.fromString(infoTag.getString("uuid"));
            String type = infoTag.getString("type");
            boolean player = infoTag.getBoolean("player");
            String name = infoTag.getString("name");
            String skinValue = infoTag.contains("skin_value") ? infoTag.getString("skin_value") : null;
            String skinSignature = infoTag.contains("skin_signature") ? infoTag.getString("skin_signature") : null;
            track.addEntityInfo(new ReplayEntityInfo(id, type, player, name, skinValue, skinSignature));
        }
        ListTag snapshots = tag.getList("snapshots", Tag.TAG_COMPOUND);
        for (int i = 0; i < snapshots.size(); i++) {
            CompoundTag snapTag = snapshots.getCompound(i);
            long tick = snapTag.getLong("tick");
            long worldTime = snapTag.getLong("world_time");
            boolean raining = snapTag.getBoolean("raining");
            float rainLevel = snapTag.getFloat("rain_level");
            boolean thundering = snapTag.getBoolean("thundering");
            float thunderLevel = snapTag.getFloat("thunder_level");
            ListTag entityFrames = snapTag.getList("entities", Tag.TAG_COMPOUND);
            java.util.ArrayList<ReplayEntityFrame> frames = new java.util.ArrayList<>();
            for (int j = 0; j < entityFrames.size(); j++) {
                CompoundTag frameTag = entityFrames.getCompound(j);
                UUID id = UUID.fromString(frameTag.getString("uuid"));
                Vec3 pos = readVec3(frameTag.getCompound("pos"));
                float yaw = frameTag.getFloat("yaw");
                float pitch = frameTag.getFloat("pitch");
                float headYaw = frameTag.getFloat("head_yaw");
                float bodyYaw = frameTag.getFloat("body_yaw");
                frames.add(new ReplayEntityFrame(id, pos, yaw, pitch, headYaw, bodyYaw));
            }
            track.addSnapshot(new ReplayServerSnapshot(tick, worldTime, raining, rainLevel, thundering, thunderLevel, frames));
        }
        ListTag chunkSnapshots = tag.getList("chunk_snapshots", Tag.TAG_COMPOUND);
        for (int i = 0; i < chunkSnapshots.size(); i++) {
            CompoundTag chunkTag = chunkSnapshots.getCompound(i);
            long tick = chunkTag.getLong("tick");
            int x = chunkTag.getInt("x");
            int z = chunkTag.getInt("z");
            ListTag sectionsTag = chunkTag.getList("sections", Tag.TAG_COMPOUND);
            List<ReplayChunkSectionData> sections = new java.util.ArrayList<>();
            for (int j = 0; j < sectionsTag.size(); j++) {
                CompoundTag sectionTag = sectionsTag.getCompound(j);
                int index = sectionTag.getInt("index");
                byte[] data = sectionTag.getByteArray("data");
                sections.add(new ReplayChunkSectionData(index, data));
            }
            ListTag blockEntitiesTag = chunkTag.getList("block_entities", Tag.TAG_COMPOUND);
            List<CompoundTag> blockEntities = new java.util.ArrayList<>();
            for (int j = 0; j < blockEntitiesTag.size(); j++) {
                blockEntities.add(blockEntitiesTag.getCompound(j).copy());
            }
            track.addChunkSnapshot(new ReplayChunkSnapshot(tick, x, z, sections, blockEntities));
        }
        return track;
    }

    private static CompoundTag writeClientTrack(ReplayClientTrack track) {
        CompoundTag tag = new CompoundTag();
        ListTag frames = new ListTag();
        for (ReplayClientFrame frame : track.getFrames()) {
            CompoundTag frameTag = new CompoundTag();
            frameTag.putLong("tick", frame.getTick());
            ReplayCameraFrame camera = frame.getCamera();
            if (camera != null) {
                CompoundTag camTag = new CompoundTag();
                camTag.put("pos", writeVec3(camera.position()));
                camTag.putFloat("yaw", camera.yaw());
                camTag.putFloat("pitch", camera.pitch());
                camTag.putFloat("fov", camera.fov());
                frameTag.put("camera", camTag);
            }
            CompoundTag worldVisuals = frame.getWorldVisuals();
            frameTag.put("world_visuals", worldVisuals == null ? new CompoundTag() : worldVisuals.copy());

            ListTag entityVisuals = new ListTag();
            for (Map.Entry<UUID, CompoundTag> entry : frame.getEntityVisuals().entrySet()) {
                CompoundTag entryTag = new CompoundTag();
                entryTag.putString("uuid", entry.getKey().toString());
                entryTag.put("data", entry.getValue().copy());
                entityVisuals.add(entryTag);
            }
            frameTag.put("entity_visuals", entityVisuals);
            frames.add(frameTag);
        }
        tag.put("frames", frames);
        return tag;
    }

    private static ReplayClientTrack readClientTrack(CompoundTag tag) {
        ReplayClientTrack track = new ReplayClientTrack();
        ListTag frames = tag.getList("frames", Tag.TAG_COMPOUND);
        for (int i = 0; i < frames.size(); i++) {
            CompoundTag frameTag = frames.getCompound(i);
            long tick = frameTag.getLong("tick");
            ReplayCameraFrame camera = null;
            if (frameTag.contains("camera")) {
                CompoundTag camTag = frameTag.getCompound("camera");
                Vec3 pos = readVec3(camTag.getCompound("pos"));
                float yaw = camTag.getFloat("yaw");
                float pitch = camTag.getFloat("pitch");
                float fov = camTag.getFloat("fov");
                camera = new ReplayCameraFrame(pos, yaw, pitch, fov);
            }
            CompoundTag worldVisuals = frameTag.contains("world_visuals")
                ? frameTag.getCompound("world_visuals")
                : new CompoundTag();

            Map<UUID, CompoundTag> entityVisuals = new HashMap<>();
            ListTag entityList = frameTag.getList("entity_visuals", Tag.TAG_COMPOUND);
            for (int j = 0; j < entityList.size(); j++) {
                CompoundTag entryTag = entityList.getCompound(j);
                UUID id = UUID.fromString(entryTag.getString("uuid"));
                CompoundTag data = entryTag.getCompound("data");
                entityVisuals.put(id, data.copy());
            }
            track.addFrame(new ReplayClientFrame(tick, camera, entityVisuals, worldVisuals));
        }
        return track;
    }

    private static CompoundTag writeVec3(Vec3 vec) {
        CompoundTag tag = new CompoundTag();
        Vec3 safe = vec == null ? Vec3.ZERO : vec;
        tag.putDouble("x", safe.x);
        tag.putDouble("y", safe.y);
        tag.putDouble("z", safe.z);
        return tag;
    }

    private static Vec3 readVec3(CompoundTag tag) {
        double x = tag.getDouble("x");
        double y = tag.getDouble("y");
        double z = tag.getDouble("z");
        return new Vec3(x, y, z);
    }

    private static void writeCompressed(CompoundTag tag, Path path) throws IOException {
        try (OutputStream output = Files.newOutputStream(path)) {
            NbtIo.writeCompressed(tag, output);
        }
    }

    private static CompoundTag readCompressed(Path path) throws IOException {
        try (InputStream input = Files.newInputStream(path)) {
            return NbtIo.readCompressed(input, NbtAccounter.unlimitedHeap());
        }
    }
}
