package neutka.marallys.marallyzen.mirror;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import neutka.marallys.marallyzen.blocks.MarallyzenBlocks;
import neutka.marallys.marallyzen.network.NarratePacket;
import neutka.marallys.marallyzen.network.NetworkHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class MirrorRegistry {
    private static final int NARRATION_FADE_IN = 3;
    private static final int NARRATION_STAY = 40;
    private static final int NARRATION_FADE_OUT = 4;

    private static final Map<UUID, PendingSelection> PENDING_SELECTIONS = new HashMap<>();

    private MirrorRegistry() {
    }

    public static void armPeer(ServerPlayer player) {
        if (player == null) {
            return;
        }
        PENDING_SELECTIONS.put(player.getUUID(), new PendingSelection(player.level().dimension(), Mode.LINK_FIRST, null));
        sendNarration(player, Component.literal("\u00A7b[Mirror] \u041a\u043b\u0438\u043a\u043d\u0438\u0442\u0435 \u043f\u043e \u043f\u0435\u0440\u0432\u043e\u043c\u0443 \u0437\u0435\u0440\u043a\u0430\u043b\u0443"));
    }

    public static void armUnlink(ServerPlayer player) {
        if (player == null) {
            return;
        }
        PENDING_SELECTIONS.put(player.getUUID(), new PendingSelection(player.level().dimension(), Mode.UNLINK, null));
        sendNarration(player, Component.literal("\u00A7e[Mirror] \u041a\u043b\u0438\u043a\u043d\u0438\u0442\u0435 \u043f\u043e \u0437\u0435\u0440\u043a\u0430\u043b\u0443 \u0434\u043b\u044f \u0440\u0430\u0437\u044a\u0435\u0434\u0438\u043d\u0435\u043d\u0438\u044f"));
    }

    public static boolean handlePendingClick(ServerPlayer player, ServerLevel level, BlockPos clickedPos) {
        if (player == null || level == null || clickedPos == null) {
            return false;
        }
        PendingSelection pending = PENDING_SELECTIONS.get(player.getUUID());
        if (pending == null) {
            return false;
        }
        if (!level.dimension().equals(pending.dimension())) {
            PENDING_SELECTIONS.remove(player.getUUID());
            sendNarration(player, Component.literal("\u00A7c[Mirror] \u041e\u0436\u0438\u0434\u0430\u043d\u0438\u0435 \u043a\u043b\u0438\u043a\u0430 \u0441\u0431\u0440\u043e\u0448\u0435\u043d\u043e"));
            return true;
        }

        switch (pending.mode()) {
            case LINK_FIRST -> {
                PENDING_SELECTIONS.put(player.getUUID(), new PendingSelection(level.dimension(), Mode.LINK_SECOND, clickedPos.immutable()));
                sendNarration(player, Component.literal("\u00A7b[Mirror] \u041a\u043b\u0438\u043a\u043d\u0438\u0442\u0435 \u043f\u043e \u0432\u0442\u043e\u0440\u043e\u043c\u0443 \u0437\u0435\u0440\u043a\u0430\u043b\u0443"));
                return true;
            }
            case LINK_SECOND -> {
                BlockPos first = pending.firstPos();
                if (first == null || first.equals(clickedPos)) {
                    sendNarration(player, Component.literal("\u00A7c[Mirror] \u0412\u044b\u0431\u0435\u0440\u0438\u0442\u0435 \u0434\u0440\u0443\u0433\u043e\u0435 \u0437\u0435\u0440\u043a\u0430\u043b\u043e"));
                    return true;
                }
                link(level, first, clickedPos);
                PENDING_SELECTIONS.remove(player.getUUID());
                sendNarration(player, Component.literal("\u00A7a[Mirror] \u0417\u0435\u0440\u043a\u0430\u043b\u0430 \u0443\u0441\u043f\u0435\u0448\u043d\u043e \u0441\u043e\u0435\u0434\u0438\u043d\u0435\u043d\u044b"));
                return true;
            }
            case UNLINK -> {
                if (getLinked(level, clickedPos) != null) {
                    unlink(level, clickedPos);
                    sendNarration(player, Component.literal("\u00A7c[Mirror] \u0421\u0432\u044f\u0437\u044c \u0437\u0435\u0440\u043a\u0430\u043b \u0443\u0434\u0430\u043b\u0435\u043d\u0430"));
                } else {
                    sendNarration(player, Component.literal("\u00A7c[Mirror] \u0423 \u0437\u0435\u0440\u043a\u0430\u043b\u0430 \u043d\u0435\u0442 \u0441\u0432\u044f\u0437\u0438"));
                }
                PENDING_SELECTIONS.remove(player.getUUID());
                return true;
            }
        }
        return false;
    }

    public static void link(ServerLevel level, BlockPos a, BlockPos b) {
        if (level == null || a == null || b == null || a.equals(b)) {
            return;
        }
        MirrorSavedData data = MirrorSavedData.get(level);
        unlink(level, a);
        unlink(level, b);
        data.putLink(a, b);
        data.putLink(b, a);
    }

    public static void unlink(ServerLevel level, BlockPos a) {
        if (level == null || a == null) {
            return;
        }
        MirrorSavedData data = MirrorSavedData.get(level);
        BlockPos b = data.getLinked(a);
        data.removeLink(a);
        if (b != null) {
            data.removeLink(b);
        }
    }

    public static BlockPos getLinked(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) {
            return null;
        }
        return MirrorSavedData.get(level).getLinked(pos);
    }

    public static boolean isMirror(Level level, BlockPos pos) {
        return level != null && pos != null && level.getBlockState(pos).getBlock() == MarallyzenBlocks.MIRROR.get();
    }

    public static void sendNarration(ServerPlayer player, Component text) {
        if (player == null || text == null) {
            return;
        }
        NetworkHelper.sendToPlayer(player, new NarratePacket(text, null, NARRATION_FADE_IN, NARRATION_STAY, NARRATION_FADE_OUT));
    }

    private enum Mode {
        LINK_FIRST,
        LINK_SECOND,
        UNLINK
    }

    private record PendingSelection(net.minecraft.resources.ResourceKey<Level> dimension, Mode mode, BlockPos firstPos) {
    }
}

