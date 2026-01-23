package neutka.marallys.marallyzen.client.quest;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;
import neutka.marallys.marallyzen.Marallyzen;

@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class QuestJournalKeyBindings {
    public static final KeyMapping OPEN_JOURNAL = new KeyMapping(
        "key.marallyzen.quest_journal",
        KeyConflictContext.IN_GAME,
        KeyModifier.NONE,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_J,
        "key.categories.marallyzen"
    );
    public static final KeyMapping EXIT_INSTANCE = new KeyMapping(
        "key.marallyzen.instance_exit",
        KeyConflictContext.IN_GAME,
        KeyModifier.NONE,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_H,
        "key.categories.marallyzen"
    );

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_JOURNAL);
        event.register(EXIT_INSTANCE);
    }
}

@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
class QuestJournalKeyBindingsHandler {
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!QuestJournalKeyBindings.OPEN_JOURNAL.consumeClick()) {
            if (!QuestJournalKeyBindings.EXIT_INSTANCE.consumeClick()) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null || mc.screen != null) {
                return;
            }
            if (!neutka.marallys.marallyzen.client.instance.InstanceClientState.getInstance().isInInstance()) {
                return;
            }
            mc.setScreen(new neutka.marallys.marallyzen.client.instance.InstanceExitScreen());
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null) {
            return;
        }
        mc.setScreen(new QuestJournalScreen());
    }
}
