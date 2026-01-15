package neutka.marallys.marallyzen.client.cutscene.editor;

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

/**
 * Key bindings for cutscene editor.
 */
@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class CutsceneEditorKeyBindings {
    
    public static final KeyMapping OPEN_EDITOR = new KeyMapping(
        "key.marallyzen.cutscene_editor",
        KeyConflictContext.IN_GAME,
        KeyModifier.NONE,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_K,
        "key.categories.marallyzen"
    );

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_EDITOR);
    }
}

@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
class CutsceneEditorKeyBindingsHandler {
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (CutsceneEditorKeyBindings.OPEN_EDITOR.consumeClick()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.level != null) {
                CutsceneEditorScreen activeFixed = CutsceneEditorScreen.getActiveFixedRecordingScreen();
                if (activeFixed != null && activeFixed.isFixedRecordingActive()) {
                    activeFixed.stopRecordingFromKeybind();
                } else if (activeFixed != null) {
                    mc.setScreen(activeFixed);
                } else {
                    // Open editor screen
                    mc.setScreen(new CutsceneEditorScreen(CutsceneEditorScreen.getLastEditorData()));
                }
            }
        }
    }
}
