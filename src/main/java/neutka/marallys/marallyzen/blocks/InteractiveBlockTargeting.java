package neutka.marallys.marallyzen.blocks;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class InteractiveBlockTargeting {
    private InteractiveBlockTargeting() {}

    public enum Type {
        POSTER,
        MIRROR,
        OLD_LAPTOP,
        OLD_TV,
        VIDEO_CAMERA,
        CHAIN,
        DICTAPHONE,
        DICTAPHONE_SIMPLE,
        NONE
    }

    public static Type getType(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof PosterBlock) {
            return Type.POSTER;
        }
        if (block == MarallyzenBlocks.MIRROR.get()) {
            return Type.MIRROR;
        }
        if (block == MarallyzenBlocks.OLD_LAPTOP.get()) {
            return Type.OLD_LAPTOP;
        }
        if (block == MarallyzenBlocks.OLD_TV.get()) {
            return Type.OLD_TV;
        }
        if (block == MarallyzenBlocks.VIDEO_CAMERA.get()) {
            return Type.VIDEO_CAMERA;
        }
        if (block == MarallyzenBlocks.INTERACTIVE_CHAIN.get()) {
            return Type.CHAIN;
        }
        if (block == MarallyzenBlocks.DICTAPHONE.get()) {
            return Type.DICTAPHONE;
        }
        if (block == MarallyzenBlocks.DICTAPHONE_SIMPLE.get()) {
            return Type.DICTAPHONE_SIMPLE;
        }
        return Type.NONE;
    }

    public static Component getNarrationMessage(Type type) {
        return switch (type) {
            case POSTER -> InteractiveBlockNarrations.posterMessage();
            case MIRROR -> InteractiveBlockNarrations.mirrorMessage();
            case OLD_LAPTOP -> InteractiveBlockNarrations.oldLaptopMessage();
            case OLD_TV -> InteractiveBlockNarrations.oldTvMessage();
            case VIDEO_CAMERA -> null;
            case CHAIN -> InteractiveBlockNarrations.chainInstructionMessage();
            case DICTAPHONE -> null;
            case DICTAPHONE_SIMPLE -> InteractiveBlockNarrations.dictaphoneMessage();
            case NONE -> null;
        };
    }

    public static Component getNarrationMessage(BlockState state) {
        Type type = getType(state);
        if (type == Type.OLD_TV) {
            if (state.hasProperty(OldTvBlock.ON) && state.getValue(OldTvBlock.ON)) {
                return InteractiveBlockNarrations.oldTvTurnOffMessage();
            }
            return InteractiveBlockNarrations.oldTvTurnOnMessage();
        }
        return getNarrationMessage(type);
    }
}
