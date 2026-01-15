package neutka.marallys.marallyzen.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.audio.MarallyzenSounds;
import neutka.marallys.marallyzen.entity.DictaphoneEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class ClientDictaphoneManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientDictaphoneManager.class);
    private static final Map<BlockPos, DictaphoneEntity> clientDictaphones = new HashMap<>();
    private static final Map<BlockPos, BlockState> originalBlockStates = new HashMap<>();
    private static final Set<BlockPos> hiddenBlocks = new HashSet<>();
    private static final Map<BlockPos, Long> returnScheduleTicks = new HashMap<>();
    private static final Set<BlockPos> playbackActive = new HashSet<>();
    private static BlockPos activeNarrationPos;
    private static int nextClientEntityId = -1;
    private static Method cachedBlockChanged;
    private static Method cachedSetBlockDirty;

    private ClientDictaphoneManager() {}

    public static void createClientDictaphone(BlockPos pos, BlockState originalBlockState) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel clientLevel)) {
            LOGGER.warn("Cannot create client dictaphone: not in a client level");
            return;
        }

        if (clientDictaphones.containsKey(pos)) {
            return;
        }

        markHidden(pos);
        forceRebuild(clientLevel, pos, originalBlockState);
        activeNarrationPos = pos;

        DictaphoneEntity entity = new DictaphoneEntity(Marallyzen.DICTAPHONE_ENTITY.get(), clientLevel);
        entity.setClientOnly(true);
        entity.setId(nextClientEntityId--);
        entity.initializeFromBlock(pos, originalBlockState);

        clientDictaphones.put(pos, entity);
        originalBlockStates.put(pos, originalBlockState);
        boolean added = false;
        try {
            var method = clientLevel.getClass().getDeclaredMethod("addEntity", net.minecraft.world.entity.Entity.class);
            method.setAccessible(true);
            method.invoke(clientLevel, entity);
            added = true;
        } catch (ReflectiveOperationException ignored) {
            // Ignore and fall back.
        }
        if (!added) {
            try {
                var method = clientLevel.getClass().getDeclaredMethod("addEntity", int.class, net.minecraft.world.entity.Entity.class);
                method.setAccessible(true);
                method.invoke(clientLevel, entity.getId(), entity);
                added = true;
            } catch (ReflectiveOperationException ignored) {
                // Ignore and fall back.
            }
        }
        if (!added) {
            added = clientLevel.addFreshEntity(entity);
        }
        if (!added) {
            LOGGER.warn("ClientDictaphoneManager: failed to add dictaphone entity id={} at {}", entity.getId(), pos);
        }
        LOGGER.info("ClientDictaphoneManager: spawned client dictaphone at {} id={} state={}",
            pos, entity.getId(), entity.getCurrentState());

    }

    public static void removeClientDictaphone(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        DictaphoneEntity existing = clientDictaphones.remove(pos);
        BlockState originalState = originalBlockStates.remove(pos);
        hiddenBlocks.remove(pos);
        returnScheduleTicks.remove(pos);
        playbackActive.remove(pos);
        if (pos != null && pos.equals(activeNarrationPos)) {
            activeNarrationPos = null;
        }
        if (mc.level instanceof ClientLevel clientLevel) {
            if (originalState == null) {
                originalState = clientLevel.getBlockState(pos);
            }
            forceRebuild(clientLevel, pos, originalState);
        }
        if (existing != null) {
            existing.discard();
        }
    }

    public static boolean hasClientDictaphone(BlockPos pos) {
        return hiddenBlocks.contains(pos);
    }

    public static void clearAll() {
        for (DictaphoneEntity entity : clientDictaphones.values()) {
            entity.discard();
        }
        clientDictaphones.clear();
        originalBlockStates.clear();
        hiddenBlocks.clear();
        playbackActive.clear();
    }

    public static void clientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel clientLevel) || mc.player == null) {
            return;
        }
        if (clientDictaphones.isEmpty()) {
            return;
        }

        long gameTime = clientLevel.getGameTime();
        if (!returnScheduleTicks.isEmpty()) {
            var scheduleIterator = returnScheduleTicks.entrySet().iterator();
            while (scheduleIterator.hasNext()) {
                var entry = scheduleIterator.next();
                if (gameTime < entry.getValue()) {
                    continue;
                }
                BlockPos pos = entry.getKey();
                DictaphoneEntity entity = clientDictaphones.get(pos);
                if (entity != null) {
                    entity.startReturningClient();
                }
                scheduleIterator.remove();
            }
        }

        double maxDistanceSqr = 64.0;
        var player = mc.player;
        var iterator = clientDictaphones.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            BlockPos pos = entry.getKey();
            double distanceSqr = player.distanceToSqr(
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5
            );
            if (distanceSqr <= maxDistanceSqr) {
                continue;
            }

            DictaphoneEntity entity = entry.getValue();
            if (entity != null) {
                entity.startReturningClient();
            }
        }
    }

    public static void markHidden(BlockPos pos) {
        hiddenBlocks.add(pos);
    }

    public static boolean isHidden(BlockPos pos) {
        return hiddenBlocks.contains(pos);
    }

    public static void onNarrationComplete() {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel clientLevel)) {
            return;
        }
        if (activeNarrationPos == null) {
            return;
        }
        playbackActive.remove(activeNarrationPos);
        playStopCue(clientLevel, activeNarrationPos);
        DictaphoneEntity entity = clientDictaphones.get(activeNarrationPos);
        if (entity == null) {
            return;
        }
        returnScheduleTicks.put(activeNarrationPos, clientLevel.getGameTime() + 40L);
    }

    public static void onNarrationStart() {
        if (activeNarrationPos == null) {
            return;
        }
        playbackActive.add(activeNarrationPos);
    }

    public static boolean isPlaybackActive(BlockPos pos) {
        return playbackActive.contains(pos);
    }

    public static void playStartCue(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel clientLevel)) {
            return;
        }
        playLocalSound(clientLevel, pos, MarallyzenSounds.DICTAPHONE_START.get());
    }

    private static void playStopCue(ClientLevel level, BlockPos pos) {
        playLocalSound(level, pos, MarallyzenSounds.DICTAPHONE_STOP.get());
    }

    private static void playLocalSound(ClientLevel level, BlockPos pos, net.minecraft.sounds.SoundEvent sound) {
        level.playLocalSound(
            pos.getX() + 0.5,
            pos.getY() + 0.5,
            pos.getZ() + 0.5,
            sound,
            SoundSource.BLOCKS,
            1.0f,
            1.0f,
            false
        );
    }

    private static void forceRebuild(ClientLevel level, BlockPos pos, BlockState state) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.levelRenderer == null) {
            return;
        }
        try {
            if (cachedBlockChanged == null) {
                cachedBlockChanged = mc.levelRenderer.getClass().getMethod(
                    "blockChanged", ClientLevel.class, BlockPos.class, BlockState.class, BlockState.class, int.class
                );
                cachedBlockChanged.setAccessible(true);
            }
        } catch (ReflectiveOperationException ignored) {
            cachedBlockChanged = null;
        }
        try {
            if (cachedSetBlockDirty == null) {
                cachedSetBlockDirty = mc.levelRenderer.getClass().getMethod(
                    "setBlockDirty", BlockPos.class, BlockState.class, BlockState.class
                );
                cachedSetBlockDirty.setAccessible(true);
            }
        } catch (ReflectiveOperationException ignored) {
            cachedSetBlockDirty = null;
        }

        if (cachedBlockChanged != null) {
            try {
                cachedBlockChanged.invoke(mc.levelRenderer, level, pos, state, state, 0);
            } catch (ReflectiveOperationException ignored) {
                // Ignore and fall back.
            }
        }
        if (cachedSetBlockDirty != null) {
            try {
                cachedSetBlockDirty.invoke(mc.levelRenderer, pos, state, state);
            } catch (ReflectiveOperationException ignored) {
                // Ignore and fall back.
            }
        }
        level.setSectionDirtyWithNeighbors(
            SectionPos.blockToSectionCoord(pos.getX()),
            SectionPos.blockToSectionCoord(pos.getY()),
            SectionPos.blockToSectionCoord(pos.getZ())
        );
    }
}
