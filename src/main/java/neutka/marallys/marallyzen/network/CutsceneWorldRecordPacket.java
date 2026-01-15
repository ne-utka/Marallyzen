package neutka.marallys.marallyzen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.cutscene.world.CutsceneWorldStorage;
import neutka.marallys.marallyzen.cutscene.world.CutsceneWorldTrack;
import neutka.marallys.marallyzen.cutscene.world.server.CutsceneWorldRecorder;

import java.util.ArrayList;
import java.util.List;

public record CutsceneWorldRecordPacket(
    String sceneId,
    byte action,
    int chunkRadius,
    boolean useCenter,
    int centerChunkX,
    int centerChunkZ
) implements CustomPacketPayload {
    public static final byte ACTION_START = 0;
    public static final byte ACTION_STOP = 1;
    public static final byte ACTION_PAUSE = 2;
    public static final byte ACTION_RESUME = 3;

    public static final CustomPacketPayload.Type<CutsceneWorldRecordPacket> TYPE =
        new CustomPacketPayload.Type<>(MarallyzenNetwork.id("cutscene_world_record"));

    private static final StreamCodec<RegistryFriendlyByteBuf, Byte> BYTE_CODEC = StreamCodec.of(
        RegistryFriendlyByteBuf::writeByte,
        RegistryFriendlyByteBuf::readByte
    );

    private static final StreamCodec<RegistryFriendlyByteBuf, Integer> INT_CODEC = StreamCodec.of(
        RegistryFriendlyByteBuf::writeInt,
        RegistryFriendlyByteBuf::readInt
    );

    private static final StreamCodec<RegistryFriendlyByteBuf, Boolean> BOOL_CODEC = StreamCodec.of(
        RegistryFriendlyByteBuf::writeBoolean,
        RegistryFriendlyByteBuf::readBoolean
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, CutsceneWorldRecordPacket> STREAM_CODEC =
        StreamCodec.composite(
            NetworkCodecs.STRING,
            CutsceneWorldRecordPacket::sceneId,
            BYTE_CODEC,
            CutsceneWorldRecordPacket::action,
            INT_CODEC,
            CutsceneWorldRecordPacket::chunkRadius,
            BOOL_CODEC,
            CutsceneWorldRecordPacket::useCenter,
            INT_CODEC,
            CutsceneWorldRecordPacket::centerChunkX,
            INT_CODEC,
            CutsceneWorldRecordPacket::centerChunkZ,
            CutsceneWorldRecordPacket::new
        );

    public CutsceneWorldRecordPacket(String sceneId, byte action, int chunkRadius) {
        this(sceneId, action, chunkRadius, false, 0, 0);
    }

    @Override
    public CustomPacketPayload.Type<CutsceneWorldRecordPacket> type() {
        return TYPE;
    }

    public static void handle(CutsceneWorldRecordPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof net.minecraft.server.level.ServerPlayer player)) {
                return;
            }
            switch (packet.action()) {
                case ACTION_START -> {
                    if (packet.useCenter()) {
                        CutsceneWorldRecorder.start(player, packet.sceneId(), packet.chunkRadius(),
                            packet.centerChunkX(), packet.centerChunkZ());
                    } else {
                        CutsceneWorldRecorder.start(player, packet.sceneId(), packet.chunkRadius());
                    }
                }
                case ACTION_PAUSE -> CutsceneWorldRecorder.pause(player);
                case ACTION_RESUME -> CutsceneWorldRecorder.resume(player);
                case ACTION_STOP -> {
                    CutsceneWorldTrack track = CutsceneWorldRecorder.stop(player);
                    if (track != null) {
                        sendWorldTrack(player, packet.sceneId(), track);
                    }
                }
                default -> {
                }
            }
        });
    }

    private static void sendWorldTrack(net.minecraft.server.level.ServerPlayer player, String sceneId,
                                       CutsceneWorldTrack track) {
        try {
            byte[] payload = CutsceneWorldStorage.encode(track);
            if (payload.length == 0) {
                return;
            }
            List<byte[]> parts = split(payload, 48_000);
            int total = parts.size();
            for (int i = 0; i < total; i++) {
                CutsceneWorldTrackPacket packet = new CutsceneWorldTrackPacket(sceneId, i, total, parts.get(i));
                NetworkHelper.sendToPlayer(player, packet);
            }
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("Failed to send cutscene world track: {}", sceneId, e);
        }
    }

    private static List<byte[]> split(byte[] data, int chunkSize) {
        List<byte[]> parts = new ArrayList<>();
        int offset = 0;
        while (offset < data.length) {
            int len = Math.min(chunkSize, data.length - offset);
            byte[] part = new byte[len];
            System.arraycopy(data, offset, part, 0, len);
            parts.add(part);
            offset += len;
        }
        return parts;
    }
}
