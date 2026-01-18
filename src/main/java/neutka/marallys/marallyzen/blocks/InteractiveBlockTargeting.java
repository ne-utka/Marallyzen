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
        RADIO,
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
        if (block == MarallyzenBlocks.RADIO.get()) {
            return Type.RADIO;
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
            case POSTER -> null;
            case MIRROR -> null;
            case OLD_LAPTOP -> null;
            case RADIO -> null;
            case OLD_TV -> null;
            case VIDEO_CAMERA -> null;
            case CHAIN -> InteractiveBlockNarrations.chainInstructionMessage();
            case DICTAPHONE -> null;
            case DICTAPHONE_SIMPLE -> null;
            case NONE -> null;
        };
    }

    public static Component getNarrationMessage(BlockState state) {
        Type type = getType(state);
        return getNarrationMessage(type);
    }
}
