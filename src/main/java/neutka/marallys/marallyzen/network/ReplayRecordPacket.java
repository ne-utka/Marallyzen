package neutka.marallys.marallyzen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.replay.ReplayHeader;
import neutka.marallys.marallyzen.replay.ReplayMarker;
import neutka.marallys.marallyzen.replay.ReplayServerTrack;
import neutka.marallys.marallyzen.replay.ReplaySettings;
import neutka.marallys.marallyzen.replay.ReplayStorage;
import neutka.marallys.marallyzen.replay.server.ReplayServerRecorder;
import neutka.marallys.marallyzen.replay.server.ReplayServerResult;

import java.util.List;

public record ReplayRecordPacket(String replayId, byte action, int keyframeInterval) implements CustomPacketPayload {
    public static final byte ACTION_START = 0;
    public static final byte ACTION_STOP = 1;
    public static final byte ACTION_PAUSE = 2;
    public static final byte ACTION_RESUME = 3;

    public static final CustomPacketPayload.Type<ReplayRecordPacket> TYPE =
        new CustomPacketPayload.Type<>(MarallyzenNetwork.id("replay_record"));

    private static final StreamCodec<RegistryFriendlyByteBuf, Byte> BYTE_CODEC = StreamCodec.of(
        (buf, value) -> buf.writeByte(value),
        RegistryFriendlyByteBuf::readByte
    );

    private static final StreamCodec<RegistryFriendlyByteBuf, Integer> INT_CODEC = StreamCodec.of(
        (buf, value) -> buf.writeInt(value),
        RegistryFriendlyByteBuf::readInt
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ReplayRecordPacket> STREAM_CODEC = StreamCodec.composite(
        NetworkCodecs.STRING,
        ReplayRecordPacket::replayId,
        BYTE_CODEC,
        ReplayRecordPacket::action,
        INT_CODEC,
        ReplayRecordPacket::keyframeInterval,
        ReplayRecordPacket::new
    );

    @Override
    public CustomPacketPayload.Type<ReplayRecordPacket> type() {
        return TYPE;
    }

    public static void handle(ReplayRecordPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!neutka.marallys.marallyzen.replay.LegacyReplayGate.isLegacyReplayEnabled()) {
                return;
            }
            if (!(context.player() instanceof net.minecraft.server.level.ServerPlayer player)) {
                return;
            }
            switch (packet.action()) {
                case ACTION_START -> ReplayServerRecorder.start(player, packet.replayId(), packet.keyframeInterval());
                case ACTION_PAUSE -> ReplayServerRecorder.pause(player);
                case ACTION_RESUME -> ReplayServerRecorder.resume(player);
                case ACTION_STOP -> {
                    ReplayServerResult result = ReplayServerRecorder.stop(player);
                    if (result != null) {
                        ReplayServerTrack track = result.track();
                        long duration = track.getSnapshots().isEmpty()
                            ? 0
                            : track.getSnapshots().get(track.getSnapshots().size() - 1).tick();
                        ReplayHeader header = new ReplayHeader(
                            1,
                            ReplaySettings.DEFAULT_TICK_RATE,
                            result.keyframeInterval(),
                            duration,
                            track.getDimension()
                        );
                        try {
                            ReplayStorage.saveServerTrack(result.replayId(), header, List.<ReplayMarker>of(), track);
                        } catch (Exception e) {
                            Marallyzen.LOGGER.error("Failed to save server replay track: {}", result.replayId(), e);
                        }
                    }
                }
                default -> {
                }
            }
        });
    }
}
