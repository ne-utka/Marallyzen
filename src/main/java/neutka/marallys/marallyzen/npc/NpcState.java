package neutka.marallys.marallyzen.npc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public record NpcState(
        ResourceKey<Level> dimension,
        BlockPos pos,
        float yaw,
        String appearanceId,
        String aiState,
        String dialogState
) {
    public static final String KEY_DIMENSION = "dimension";
    public static final String KEY_POS = "pos";
    public static final String KEY_YAW = "yaw";
    public static final String KEY_APPEARANCE = "appearance";
    public static final String KEY_AI = "ai_state";
    public static final String KEY_DIALOG = "dialog_state";

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString(KEY_DIMENSION, dimension.location().toString());
        tag.put(KEY_POS, net.minecraft.nbt.NbtUtils.writeBlockPos(pos));
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
        return tag;
    }

    public static NpcState fromTag(CompoundTag tag) {
        if (tag == null || !tag.contains(KEY_DIMENSION)) {
            return null;
        }
        ResourceLocation dimId = ResourceLocation.parse(tag.getString(KEY_DIMENSION));
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimId);
        BlockPos pos = net.minecraft.nbt.NbtUtils.readBlockPos(tag, KEY_POS).orElse(BlockPos.ZERO);
        float yaw = tag.contains(KEY_YAW) ? tag.getFloat(KEY_YAW) : 0.0f;
        String appearance = tag.contains(KEY_APPEARANCE) ? tag.getString(KEY_APPEARANCE) : null;
        String ai = tag.contains(KEY_AI) ? tag.getString(KEY_AI) : null;
        String dialog = tag.contains(KEY_DIALOG) ? tag.getString(KEY_DIALOG) : null;
        return new NpcState(dimension, pos, yaw, appearance, ai, dialog);
    }
}
