package neutka.marallys.marallyzen.server;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.entity.DecoratedPotCarryEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class DecoratedPotCarryManager {
    public static final byte ACTION_PLACE = 0;
    public static final byte ACTION_THROW = 1;
    private static final int PICKUP_COOLDOWN_TICKS = 6;
    private static final int THROW_COOLDOWN_TICKS = 6;
    private static final int DROP_COOLDOWN_TICKS = 6;
    private static final int MAX_PER_CHUNK = 8;

    private static final Map<UUID, DecoratedPotCarryEntity> active = new HashMap<>();
    private static final Map<UUID, Long> lastPickupTick = new HashMap<>();
    private static final Map<UUID, Long> lastThrowTick = new HashMap<>();
    private static final Map<UUID, Long> lastDropTick = new HashMap<>();

    private DecoratedPotCarryManager() {}

    public static DecoratedPotCarryEntity getCarriedEntity(ServerPlayer player) {
        if (player == null) {
            return null;
        }
        UUID id = player.getUUID();
        DecoratedPotCarryEntity entity = active.get(id);
        if (entity == null) {
            return null;
        }
        if (entity.isRemoved() || entity.level() != player.level()) {
            active.remove(id);
            return null;
        }
        return entity;
    }

    public static boolean isCarrying(ServerPlayer player) {
        return getCarriedEntity(player) != null;
    }

    public static boolean tryPickup(ServerPlayer player, BlockPos pos, BlockState state) {
        if (player == null || pos == null || state == null) {
            return false;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        if (isChunkLimitReached(level, pos)) {
            return false;
        }
        if (isOnCooldown(lastPickupTick, player.getUUID(), level.getGameTime(), PICKUP_COOLDOWN_TICKS)) {
            return false;
        }
        if (isCarrying(player)) {
            return false;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        CompoundTag blockEntityTag = new CompoundTag();
        if (blockEntity != null) {
            blockEntityTag = blockEntity.saveWithFullMetadata(level.registryAccess());
        }
        float yaw = resolveYaw(state, blockEntity, player);

        DecoratedPotCarryEntity entity = new DecoratedPotCarryEntity(
            Marallyzen.DECORATED_POT_ENTITY.get(),
            level
        );
        entity.initializeFromBlock(pos, state, blockEntityTag, yaw, player.getUUID());
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        level.removeBlockEntity(pos);
        level.addFreshEntity(entity);
        active.put(player.getUUID(), entity);
        lastPickupTick.put(player.getUUID(), level.getGameTime());
        return true;
    }

    public static boolean tryPickupEntity(ServerPlayer player, DecoratedPotCarryEntity entity, net.minecraft.world.phys.Vec3 grabOffset) {
        if (player == null || entity == null) {
            return false;
        }
        if (entity.isRemoved() || entity.level() != player.level()) {
            return false;
        }
        if (entity.level() instanceof ServerLevel level && isChunkLimitReached(level, entity.blockPosition())) {
            return false;
        }
        if (entity.level() instanceof ServerLevel level
            && isOnCooldown(lastPickupTick, player.getUUID(), level.getGameTime(), PICKUP_COOLDOWN_TICKS)) {
            return false;
        }
        if (entity.getMode() != DecoratedPotCarryEntity.Mode.RESTING) {
            return false;
        }
        if (isCarrying(player)) {
            return false;
        }
        entity.setOwnerUuid(player.getUUID());
        entity.setGrabOffset(grabOffset);
        entity.setMode(DecoratedPotCarryEntity.Mode.CARRIED);
        active.put(player.getUUID(), entity);
        if (entity.level() instanceof ServerLevel level) {
            lastPickupTick.put(player.getUUID(), level.getGameTime());
        }
        return true;
    }

    public static void handleAction(ServerPlayer player, byte action) {
        DecoratedPotCarryEntity entity = getCarriedEntity(player);
        if (entity == null) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (isChunkLimitReached(level, entity.blockPosition())) {
            return;
        }
        if (action == ACTION_PLACE) {
            if (!isOnCooldown(lastDropTick, player.getUUID(), level.getGameTime(), DROP_COOLDOWN_TICKS)) {
                entity.dropFromCarry(player);
                active.remove(player.getUUID());
                lastDropTick.put(player.getUUID(), level.getGameTime());
            }
        } else if (action == ACTION_THROW) {
            if (!isOnCooldown(lastThrowTick, player.getUUID(), level.getGameTime(), THROW_COOLDOWN_TICKS)) {
                entity.updateCarriedPosition(player);
                entity.throwFromPlayer(player);
                active.remove(player.getUUID());
                lastThrowTick.put(player.getUUID(), level.getGameTime());
            }
        }
    }

    private static boolean isOnCooldown(Map<UUID, Long> map, UUID id, long now, int cooldown) {
        Long last = map.get(id);
        return last != null && now - last < cooldown;
    }

    private static boolean isChunkLimitReached(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) {
            return false;
        }
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        int minX = (chunkX << 4);
        int minZ = (chunkZ << 4);
        int maxX = minX + 15;
        int maxZ = minZ + 15;
        var box = new net.minecraft.world.phys.AABB(
            minX, level.getMinBuildHeight(), minZ,
            maxX + 1, level.getMaxBuildHeight(), maxZ + 1
        );
        return level.getEntitiesOfClass(DecoratedPotCarryEntity.class, box, e -> !e.isRemoved()).size() >= MAX_PER_CHUNK;
    }

    private static float resolveYaw(BlockState state, BlockEntity blockEntity, ServerPlayer player) {
        if (blockEntity != null && blockEntity.getPersistentData().contains(DecoratedPotCarryEntity.ROTATION_TAG)) {
            return blockEntity.getPersistentData().getFloat(DecoratedPotCarryEntity.ROTATION_TAG);
        }
        if (state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return state.getValue(HorizontalDirectionalBlock.FACING).toYRot();
        }
        return player.getYRot();
    }
}
