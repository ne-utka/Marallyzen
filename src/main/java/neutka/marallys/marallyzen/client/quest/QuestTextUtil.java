package neutka.marallys.marallyzen.client.quest;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

public final class QuestTextUtil {
    private QuestTextUtil() {
    }

    public static Component resolve(String keyOrText) {
        if (keyOrText == null || keyOrText.isBlank()) {
            return Component.empty();
        }
        if (I18n.exists(keyOrText)) {
            return Component.translatable(keyOrText);
        }
        return Component.literal(keyOrText);
    }
}
