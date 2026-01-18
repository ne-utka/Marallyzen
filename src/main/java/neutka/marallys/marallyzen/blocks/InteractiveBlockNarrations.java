package neutka.marallys.marallyzen.blocks;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
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
        return posterBlockMessage();
    }

    public static Component posterBlockMessage() {
        return Component.literal("\u041d\u0430\u0436\u043c\u0438\u0442\u0435 ")
            .append(pkm())
            .append(Component.literal(" \u0447\u0442\u043e\u0431\u044b \u043f\u043e\u0441\u043c\u043e\u0442\u0440\u0435\u0442\u044c \u043f\u043e\u0441\u0442\u0435\u0440"));
    }

    public static Component posterFlipToFrontMessage() {
        return Component.literal("\u041d\u0430\u0436\u043c\u0438\u0442\u0435 ")
            .append(pkm())
            .append(Component.literal(" \u0447\u0442\u043e\u0431\u044b \u043f\u043e\u0432\u0435\u0440\u043d\u0443\u0442\u044c \u0438 \u043f\u043e\u0441\u043c\u043e\u0442\u0440\u0435\u0442\u044c \u043b\u0438\u0446\u0435\u0432\u0443\u044e \u0441\u0442\u043e\u0440\u043e\u043d\u0443"));
    }

    public static Component posterFlipToBackMessage() {
        return Component.literal("\u041d\u0430\u0436\u043c\u0438\u0442\u0435 ")
            .append(pkm())
            .append(Component.literal(" \u0447\u0442\u043e\u0431\u044b \u043f\u043e\u0432\u0435\u0440\u043d\u0443\u0442\u044c \u0438 \u043f\u043e\u0441\u043c\u043e\u0442\u0440\u0435\u0442\u044c \u043e\u0431\u0440\u0430\u0442\u043d\u0443\u044e \u0441\u0442\u043e\u0440\u043e\u043d\u0443"));
    }

    public static Component mirrorMessage() {
        return Component.translatable("narration.marallyzen.mirror_instruction", pkm());
    }

    public static Component oldLaptopMessage() {
        return rmbPrompt("\u0412\u043a\u043b.");
    }

    public static Component oldTvMessage() {
        return rmbPrompt("\u0412\u043a\u043b.");
    }

    public static Component oldTvTurnOnMessage() {
        return rmbPrompt("\u0412\u043a\u043b.");
    }

    public static Component oldTvTurnOffMessage() {
        return rmbPrompt("\u0412\u044b\u043a\u043b.");
    }

    public static Component chainInstructionMessage() {
        return Component.translatable("narration.marallyzen.interactive_chain_instruction", pkm());
    }

    public static Component dictaphoneMessage() {
        return Component.translatable("narration.marallyzen.dictaphone_listen", pkm());
    }

    public static Component rmbPrompt(String label) {
        Component separator = Component.literal(" >> ")
            .withStyle(style -> style.withColor(TextColor.fromRgb(0x555555)));
        Component text = Component.literal(label)
            .withStyle(style -> style.withColor(TextColor.fromRgb(0xFFFFFF)));
        return pkm().copy().append(Component.literal(" ")).append(separator).append(text);
    }
}
