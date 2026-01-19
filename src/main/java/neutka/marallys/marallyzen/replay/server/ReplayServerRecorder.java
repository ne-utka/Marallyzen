package neutka.marallys.marallyzen.replay.server;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import io.netty.buffer.Unpooled;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.replay.ReplayChunkSectionData;
import neutka.marallys.marallyzen.replay.ReplayChunkSnapshot;
import neutka.marallys.marallyzen.replay.ReplayEntityFrame;
import neutka.marallys.marallyzen.replay.ReplayEntityInfo;
import neutka.marallys.marallyzen.replay.ReplayServerSnapshot;
import neutka.marallys.marallyzen.replay.ReplayServerTrack;
import neutka.marallys.marallyzen.replay.ReplaySettings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ReplayServerRecorder {
    private static final Map<UUID, ReplayServerSession> SESSIONS = new HashMap<>();

    private ReplayServerRecorder() {
    }

    public static boolean isRecording(ServerPlayer player) {
        if (!neutka.marallys.marallyzen.replay.LegacyReplayGate.isLegacyReplayEnabled()) {
            return false;
        }
        return player != null && SESSIONS.containsKey(player.getUUID());
    }

    public static void start(ServerPlayer player, String replayId, int keyframeInterval) {
        if (!neutka.marallys.marallyzen.replay.LegacyReplayGate.isLegacyReplayEnabled()) {
            return;
        }
        if (player == null || replayId == null || replayId.isEmpty()) {
            return;
        }
        if (SESSIONS.containsKey(player.getUUID())) {
            return;
        }
        ServerLevel level = player.serverLevel();
        ReplayServerSession session = new ReplayServerSession(player.getUUID(), replayId, level,
            Math.max(1, keyframeInterval));
        session.startTick = level.getGameTime();
        SESSIONS.put(player.getUUID(), session);
        Marallyzen.LOGGER.info("Replay server recording started: {} (player={})", replayId, player.getName().getString());
    }

    public static ReplayServerResult stop(ServerPlayer player) {
        if (!neutka.marallys.marallyzen.replay.LegacyReplayGate.isLegacyReplayEnabled()) {
            return null;
        }
        if (player == null) {
            return null;
        }
        ReplayServerSession session = SESSIONS.remove(player.getUUID());
        if (session == null) {
            return null;
        }
        Marallyzen.LOGGER.info("Replay server recording stopped: {} (player={}) snapshots={}",
            session.replayId, player.getName().getString(), session.track.getSnapshots().size());
        return new ReplayServerResult(session.replayId, session.keyframeInterval, session.track);
    }

    public static void pause(ServerPlayer player) {
        if (!neutka.marallys.marallyzen.replay.LegacyReplayGate.isLegacyReplayEnabled()) {
            return;
        }
        ReplayServerSession session = player == null ? null : SESSIONS.get(player.getUUID());
        if (session == null || session.paused) {
            return;
        }
        session.paused = true;
        session.pauseStartTick = session.level.getGameTime();
    }

    public static void resume(ServerPlayer player) {
        if (!neutka.marallys.marallyzen.replay.LegacyReplayGate.isLegacyReplayEnabled()) {
            return;
        }
        ReplayServerSession session = player == null ? null : SESSIONS.get(player.getUUID());
        if (session == null || !session.paused) {
            return;
        }
        long pauseDuration = session.level.getGameTime() - session.pauseStartTick;
        session.totalPauseTicks += pauseDuration;
        session.paused = false;
    }

    public static void tick(ServerLevel level) {
        if (!neutka.marallys.marallyzen.replay.LegacyReplayGate.isLegacyReplayEnabled()) {
            return;
        }
        if (level == null || SESSIONS.isEmpty()) {
            return;
        }
        List<UUID> toRemove = new ArrayList<>();
        for (ReplayServerSession session : SESSIONS.values()) {
            if (session.level != level) {
                continue;
            }
            if (session.paused) {
                continue;
            }
            long currentTick = level.getGameTime();
            long relativeTick = currentTick - session.startTick - session.totalPauseTicks;
            if (relativeTick < 0) {
                continue;
            }
            if (relativeTick > ReplaySettings.MAX_DURATION_TICKS) {
                toRemove.add(session.playerId);
                continue;
            }
            if (relativeTick % session.keyframeInterval != 0) {
                continue;
            }
            session.track.addSnapshot(captureSnapshot(level, session.track, relativeTick));
            ServerPlayer recorder = level.getServer().getPlayerList().getPlayer(session.playerId);
            captureChunkSnapshots(level, session.track, recorder, relativeTick, session.chunkRadius);
        }
        for (UUID id : toRemove) {
            ReplayServerSession session = SESSIONS.remove(id);
            if (session != null) {
                Marallyzen.LOGGER.warn("Replay server recording auto-stopped (max duration): {}", session.replayId);
            }
        }
    }

    private static ReplayServerSnapshot captureSnapshot(ServerLevel level, ReplayServerTrack track, long relativeTick) {
        long tick = relativeTick;
        long worldTime = level.getDayTime();
        boolean raining = level.isRaining();
        float rainLevel = level.getRainLevel(1.0f);
        boolean thundering = level.isThundering();
        float thunderLevel = level.getThunderLevel(1.0f);
        List<ReplayEntityFrame> frames = new ArrayList<>();

        for (Entity entity : level.getEntities().getAll()) {
            if (entity == null || entity.isRemoved()) {
                continue;
            }
            UUID id = entity.getUUID();
            if (!track.getEntities().containsKey(id)) {
                track.addEntityInfo(buildEntityInfo(entity));
            }
            Vec3 pos = entity.position();
            float yaw = entity.getYRot();
            float pitch = entity.getXRot();
            float headYaw = yaw;
            float bodyYaw = yaw;
            if (entity instanceof LivingEntity living) {
                headYaw = living.getYHeadRot();
                bodyYaw = living.yBodyRot;
            }
            frames.add(new ReplayEntityFrame(id, pos, yaw, pitch, headYaw, bodyYaw));
        }

        return new ReplayServerSnapshot(tick, worldTime, raining, rainLevel, thundering, thunderLevel, frames);
    }

    private static void captureChunkSnapshots(ServerLevel level, ReplayServerTrack track, ServerPlayer player,
                                              long relativeTick, int radius) {
        if (level == null || track == null || player == null) {
            return;
        }
        ChunkPos center = player.chunkPosition();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int chunkX = center.x + dx;
                int chunkZ = center.z + dz;
                var access = level.getChunkSource().getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
                if (access instanceof LevelChunk levelChunk) {
                    track.addChunkSnapshot(captureChunkSnapshot(level, levelChunk, relativeTick));
                }
            }
        }
    }

    private static ReplayChunkSnapshot captureChunkSnapshot(ServerLevel level, LevelChunk chunk, long relativeTick) {
        List<ReplayChunkSectionData> sections = new ArrayList<>();
        LevelChunkSection[] chunkSections = chunk.getSections();
        for (int i = 0; i < chunkSections.length; i++) {
            LevelChunkSection section = chunkSections[i];
            if (section == null) {
                continue;
            }
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer(section.getSerializedSize()));
            section.write(buffer);
            byte[] data = new byte[buffer.readableBytes()];
            buffer.readBytes(data);
            sections.add(new ReplayChunkSectionData(i, data));
        }
        List<CompoundTag> blockEntities = new ArrayList<>();
        for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
            if (blockEntity == null || blockEntity.isRemoved()) {
                continue;
            }
            blockEntities.add(blockEntity.saveWithFullMetadata(level.registryAccess()));
        }
        ChunkPos pos = chunk.getPos();
        return new ReplayChunkSnapshot(relativeTick, pos.x, pos.z, sections, blockEntities);
    }

    private static ReplayEntityInfo buildEntityInfo(Entity entity) {
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        String entityTypeId = typeId != null ? typeId.toString() : "minecraft:pig";
        boolean isPlayer = entity instanceof net.minecraft.world.entity.player.Player;
        String name = entity.getName().getString();
        String skinValue = null;
        String skinSignature = null;
        if (isPlayer && entity instanceof net.minecraft.world.entity.player.Player player) {
            GameProfile profile = player.getGameProfile();
            if (profile != null) {
                Property textures = profile.getProperties().get("textures").stream().findFirst().orElse(null);
                if (textures != null) {
                    skinValue = textures.value();
                    skinSignature = textures.signature();
                }
            }
        }
        return new ReplayEntityInfo(entity.getUUID(), entityTypeId, isPlayer, name, skinValue, skinSignature);
    }

    private static final class ReplayServerSession {
        private final UUID playerId;
        private final String replayId;
        private final ServerLevel level;
        private final int keyframeInterval;
        private final int chunkRadius;
        private final ReplayServerTrack track;
        private long startTick;
        private boolean paused;
        private long pauseStartTick;
        private long totalPauseTicks;

        private ReplayServerSession(UUID playerId, String replayId, ServerLevel level, int keyframeInterval) {
            this.playerId = playerId;
            this.replayId = replayId;
            this.level = level;
            this.keyframeInterval = keyframeInterval;
            this.chunkRadius = ReplaySettings.DEFAULT_CHUNK_RADIUS;
            this.track = new ReplayServerTrack(level.dimension().location().toString());
        }
    }
}
