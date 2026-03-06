package neutka.marallys.marallyzen.npc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;

public record NpcState(
        ResourceKey<Level> dimension,
        BlockPos pos,
        float yaw,
        String appearanceId,
        String aiState,
        String dialogState,
        String currentReplayId,
        int replayFrameIndex,
        boolean replayRunning
) {
    public static final String KEY_DIMENSION = "dimension";
    public static final String KEY_POS = "pos";
    public static final String KEY_YAW = "yaw";
    public static final String KEY_APPEARANCE = "appearance";
    public static final String KEY_AI = "ai_state";
    public static final String KEY_DIALOG = "dialog_state";
    public static final String KEY_REPLAY_ID = "replay_id";
    public static final String KEY_REPLAY_FRAME = "replay_frame";
    public static final String KEY_REPLAY_RUNNING = "replay_running";
    public static final Codec<NpcState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceKey.codec(Registries.DIMENSION).fieldOf(KEY_DIMENSION).forGetter(NpcState::dimension),
            BlockPos.CODEC.fieldOf(KEY_POS).forGetter(NpcState::pos),
            Codec.FLOAT.optionalFieldOf(KEY_YAW, 0.0f).forGetter(NpcState::yaw),
            Codec.STRING.optionalFieldOf(KEY_APPEARANCE).forGetter(state -> Optional.ofNullable(state.appearanceId())),
            Codec.STRING.optionalFieldOf(KEY_AI).forGetter(state -> Optional.ofNullable(state.aiState())),
            Codec.STRING.optionalFieldOf(KEY_DIALOG).forGetter(state -> Optional.ofNullable(state.dialogState())),
            Codec.STRING.optionalFieldOf(KEY_REPLAY_ID).forGetter(state -> Optional.ofNullable(state.currentReplayId())),
            Codec.INT.optionalFieldOf(KEY_REPLAY_FRAME, 0).forGetter(NpcState::replayFrameIndex),
            Codec.BOOL.optionalFieldOf(KEY_REPLAY_RUNNING, false).forGetter(NpcState::replayRunning)
    ).apply(instance, (dimension, pos, yaw, appearance, ai, dialog, replayId, replayFrame, replayRunning) ->
            new NpcState(
                    dimension,
                    pos,
                    yaw,
                    appearance.orElse(null),
                    ai.orElse(null),
                    dialog.orElse(null),
                    replayId.orElse(null),
                    replayFrame,
                    replayRunning
            )
    ));

    public NpcState(
            ResourceKey<Level> dimension,
            BlockPos pos,
            float yaw,
            String appearanceId,
            String aiState,
            String dialogState
    ) {
        this(dimension, pos, yaw, appearanceId, aiState, dialogState, null, 0, false);
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString(KEY_DIMENSION, dimension.identifier().toString());
        tag.putLong(KEY_POS, pos.asLong());
        tag.putFloat(KEY_YAW, yaw);
        if (appearanceId != null) {
            tag.putString(KEY_APPEARANCE, appearanceId);
        }
        if (aiState != null) {
            tag.putString(KEY_AI, aiState);
        }
        if (dialogState != null) {
            tag.putString(KEY_DIALOG, dialogState);
        }
        if (currentReplayId != null && !currentReplayId.isBlank()) {
            tag.putString(KEY_REPLAY_ID, currentReplayId);
        }
        tag.putInt(KEY_REPLAY_FRAME, replayFrameIndex);
        tag.putBoolean(KEY_REPLAY_RUNNING, replayRunning);
        return tag;
    }

    public static NpcState fromTag(CompoundTag tag) {
        if (tag == null || !tag.contains(KEY_DIMENSION)) {
            return null;
        }
        String dimString = tag.getString(KEY_DIMENSION).orElse(null);
        if (dimString == null) {
            return null;
        }
        Identifier dimId = Identifier.parse(dimString);
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimId);
        BlockPos pos = BlockPos.of(tag.getLong(KEY_POS).orElse(0L));
        float yaw = tag.getFloat(KEY_YAW).orElse(0.0f);
        String appearance = tag.getString(KEY_APPEARANCE).orElse(null);
        String ai = tag.getString(KEY_AI).orElse(null);
        String dialog = tag.getString(KEY_DIALOG).orElse(null);
        String replayId = tag.getString(KEY_REPLAY_ID).orElse(null);
        int replayFrame = tag.getInt(KEY_REPLAY_FRAME).orElse(0);
        boolean replayRunning = tag.getBoolean(KEY_REPLAY_RUNNING).orElse(false);
        return new NpcState(dimension, pos, yaw, appearance, ai, dialog, replayId, replayFrame, replayRunning);
    }
}




