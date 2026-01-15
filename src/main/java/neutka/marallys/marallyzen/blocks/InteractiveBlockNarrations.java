package neutka.marallys.marallyzen.blocks;

import net.minecraft.network.chat.Component;
import neutka.marallys.marallyzen.util.NarrationIcons;

/**
 * Shared narration builders for interactive blocks.
 */
public final class InteractiveBlockNarrations {
    private InteractiveBlockNarrations() {}

    private static Component pkm() {
        return NarrationIcons.rmb();
    }

    public static Component posterMessage() {
        return Component.literal("Нажмите ")
            .append(pkm())
            .append(Component.literal(" для переключения плаката"));
    }

    public static Component mirrorMessage() {
        return Component.translatable("narration.marallyzen.mirror_instruction", pkm());
    }

    public static Component oldLaptopMessage() {
        return Component.translatable("narration.marallyzen.old_laptop_instruction", pkm());
    }

    public static Component oldTvMessage() {
        return oldTvTurnOnMessage();
    }

    public static Component oldTvTurnOnMessage() {
        return Component.translatable("narration.marallyzen.old_tv_turn_on", pkm());
    }

    public static Component oldTvTurnOffMessage() {
        return Component.translatable("narration.marallyzen.old_tv_turn_off", pkm());
    }

    public static Component chainInstructionMessage() {
        return Component.translatable("narration.marallyzen.interactive_chain_instruction", pkm());
    }
    public static Component dictaphoneMessage() {
        return Component.translatable("narration.marallyzen.dictaphone_listen", pkm());
    }
}




