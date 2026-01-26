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

    private static final Map<UUID, DecoratedPotCarryEntity> active = new HashMap<>();

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
        return true;
    }

    public static boolean tryPickupEntity(ServerPlayer player, DecoratedPotCarryEntity entity, net.minecraft.world.phys.Vec3 grabOffset) {
        if (player == null || entity == null) {
            return false;
        }
        if (entity.isRemoved() || entity.level() != player.level()) {
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
        return true;
    }

    public static void handleAction(ServerPlayer player, byte action) {
        DecoratedPotCarryEntity entity = getCarriedEntity(player);
        if (entity == null) {
            return;
        }
        if (action == ACTION_PLACE) {
            entity.dropFromCarry(player);
            active.remove(player.getUUID());
        } else if (action == ACTION_THROW) {
            entity.updateCarriedPosition(player);
            entity.throwFromPlayer(player);
            active.remove(player.getUUID());
        }
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
