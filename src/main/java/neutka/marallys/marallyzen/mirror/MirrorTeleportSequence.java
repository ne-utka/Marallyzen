package neutka.marallys.marallyzen.mirror;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.network.NetworkHelper;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = Marallyzen.MODID)
public final class MirrorTeleportSequence {
    public static final int NEGATIVE_TIME_MS = 400;
    public static final int WHITE_FADE_IN_MS = 300;
    public static final int WHITE_FADE_OUT_MS = 400;
    private static final int TELEPORT_DELAY_TICKS = Math.max(1, (NEGATIVE_TIME_MS + WHITE_FADE_IN_MS + 49) / 50);

    private static final Map<UUID, PendingTeleport> PENDING = new HashMap<>();

    private MirrorTeleportSequence() {
    }

    public static boolean start(ServerPlayer player, ServerLevel level, BlockPos sourceMirrorPos) {
        if (player == null || level == null || sourceMirrorPos == null) {
            return false;
        }
        BlockPos targetMirrorPos = MirrorRegistry.getLinked(level, sourceMirrorPos);
        if (targetMirrorPos == null) {
            return false;
        }
        if (player.isSpectator() || player.getVehicle() != null) {
            MirrorRegistry.sendNarration(player, net.minecraft.network.chat.Component.literal(
                    "\u00A7c[Mirror] \u0422\u0435\u043b\u0435\u043f\u043e\u0440\u0442 \u043d\u0435\u0434\u043e\u0441\u0442\u0443\u043f\u0435\u043d"));
            return true;
        }
        if (!MirrorRegistry.isMirror(level, targetMirrorPos)) {
            MirrorRegistry.unlink(level, sourceMirrorPos);
            MirrorRegistry.sendNarration(player, net.minecraft.network.chat.Component.literal(
                    "\u00A7c[Mirror] \u0421\u0432\u044f\u0437\u0430\u043d\u043d\u043e\u0435 \u0437\u0435\u0440\u043a\u0430\u043b\u043e \u043e\u0442\u0441\u0443\u0442\u0441\u0442\u0432\u0443\u0435\u0442"));
            return true;
        }

        PENDING.put(player.getUUID(), new PendingTeleport(
                level.dimension(),
                sourceMirrorPos.immutable(),
                targetMirrorPos.immutable(),
                player.getYRot(),
                player.getXRot(),
                level.getGameTime() + TELEPORT_DELAY_TICKS
        ));

        NetworkHelper.sendToPlayer(player, new MirrorTeleportPacket(
                targetMirrorPos,
                WHITE_FADE_IN_MS,
                WHITE_FADE_OUT_MS,
                NEGATIVE_TIME_MS
        ));
        return true;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (PENDING.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, PendingTeleport>> iterator = PENDING.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PendingTeleport> entry = iterator.next();
            PendingTeleport pending = entry.getValue();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                iterator.remove();
                continue;
            }
            ServerLevel level = event.getServer().getLevel(pending.dimension());
            if (level == null || level.getGameTime() < pending.executeAtTick()) {
                continue;
            }
            if (!MirrorRegistry.isMirror(level, pending.targetMirrorPos())) {
                MirrorRegistry.unlink(level, pending.sourceMirrorPos());
                MirrorRegistry.sendNarration(player, net.minecraft.network.chat.Component.literal(
                        "\u00A7c[Mirror] \u0421\u0432\u044f\u0437\u0430\u043d\u043d\u043e\u0435 \u0437\u0435\u0440\u043a\u0430\u043b\u043e \u043e\u0442\u0441\u0443\u0442\u0441\u0442\u0432\u0443\u0435\u0442"));
                iterator.remove();
                continue;
            }
            if (player.isSpectator() || player.getVehicle() != null) {
                iterator.remove();
                continue;
            }

            Vec3 spawnPos = computeSpawnInFront(level, pending.targetMirrorPos());
            player.teleportTo(
                    level,
                    spawnPos.x,
                    spawnPos.y,
                    spawnPos.z,
                    Set.of(),
                    pending.yaw(),
                    pending.pitch(),
                    false
            );
            player.fallDistance = 0.0f;
            iterator.remove();
        }
    }

    private static Vec3 computeSpawnInFront(ServerLevel level, BlockPos mirrorPos) {
        Direction facing = Direction.NORTH;
        var state = level.getBlockState(mirrorPos);
        if (state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            facing = state.getValue(HorizontalDirectionalBlock.FACING);
        }
        return new Vec3(
                mirrorPos.getX() + 0.5 + facing.getStepX() * 1.2,
                mirrorPos.getY() + 0.1,
                mirrorPos.getZ() + 0.5 + facing.getStepZ() * 1.2
        );
    }

    private record PendingTeleport(
            net.minecraft.resources.ResourceKey<Level> dimension,
            BlockPos sourceMirrorPos,
            BlockPos targetMirrorPos,
            float yaw,
            float pitch,
            long executeAtTick
    ) {
    }
}
