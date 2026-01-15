package neutka.marallys.marallyzen;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import java.util.Map;

import neutka.marallys.marallyzen.client.gui.DialogScreen;
import neutka.marallys.marallyzen.client.cutscene.editor.CutsceneEditorKeyBindings;
import neutka.marallys.marallyzen.blocks.MarallyzenBlockEntities;
import neutka.marallys.marallyzen.client.renderer.GeckoNpcFallbackRenderer;
import neutka.marallys.marallyzen.client.renderer.InteractiveChainBlockEntityRenderer;
import neutka.marallys.marallyzen.client.renderer.OldTvBlockEntityRenderer;

@Mod(value = Marallyzen.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class MarallyzenClient {
    public MarallyzenClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            Marallyzen.LOGGER.info("Marallyzen client setup; player={}", Minecraft.getInstance().getUser().getName());

            // Check if Emotecraft is available (NeoForge version uses io.github.kosmx.* packages)
            try {
                Class.forName("io.github.kosmx.emotes.main.EmoteHolder");
                Class.forName("io.github.kosmx.emotes.main.mixinFunctions.IPlayerEntity");
                Marallyzen.LOGGER.info("✔ Emotecraft API FOUND (NeoForge version)");
            } catch (ClassNotFoundException e) {
                Marallyzen.LOGGER.error("✘ Emotecraft API MISSING - NPC emotes will not work", e);
            } catch (Throwable t) {
                Marallyzen.LOGGER.error("✘ Emotecraft API check failed", t);
            }

            // Load cutscenes
            neutka.marallys.marallyzen.client.camera.SceneLoader.loadScenes();
            Marallyzen.LOGGER.info("Loaded {} cutscenes", neutka.marallys.marallyzen.client.camera.SceneLoader.getAllScenes().size());

            // Poster blocks use masked transparency (holes). Force CUTOUT render layer to avoid translucent blending artifacts.
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.POSTER_1.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.POSTER_2.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.POSTER_3.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.POSTER_4.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.POSTER_5.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.POSTER_6.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.POSTER_7.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.POSTER_8.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.POSTER_9.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.POSTER_10.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.OLD_POSTER.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.PAPER_POSTER_1.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.PAPER_POSTER_2.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.INTERACTIVE_CHAIN.get(), RenderType.cutout());
        });
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        // Client-side DenizenCore tick (if needed in the future)
        // For now, scripts run server-side only
        
        // Tick narration manager (updates narration overlay state and alpha interpolation)
        neutka.marallys.marallyzen.client.narration.NarrationManager.getInstance().tick();
        
        // Tick screen fade manager
        neutka.marallys.marallyzen.client.cutscene.ScreenFadeManager.getInstance().tick();
        
        // Tick eyes close manager
        neutka.marallys.marallyzen.client.cutscene.EyesCloseManager.getInstance().tick();

        // Tick dictaphone client visibility + despawn logic
        neutka.marallys.marallyzen.client.ClientDictaphoneManager.clientTick();
        
        // Tick cutscene editor if open
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.screen instanceof neutka.marallys.marallyzen.client.cutscene.editor.CutsceneEditorScreen editorScreen) {
            editorScreen.tick();
        } else {
            // Keep recording even when the editor screen is closed.
            var recorder = neutka.marallys.marallyzen.client.cutscene.editor.CutsceneRecorder.getInstance();
            if (recorder.isRecording()) {
                recorder.tick();
            }
        }
    }

    @SubscribeEvent
    static void onClientTickPre(ClientTickEvent.Pre event) {
        // Hide client-side blocks before rendering to avoid flicker on server block updates.
        neutka.marallys.marallyzen.client.ClientPosterManager.tick();
    }

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Use GeckoLib renderer when available, otherwise fallback to a no-op renderer.
        if (isGeckoLibAvailable()) {
            event.registerEntityRenderer(Marallyzen.GECKO_NPC.get(), neutka.marallys.marallyzen.client.GeckoNpcRenderer::new);
        } else {
            event.registerEntityRenderer(Marallyzen.GECKO_NPC.get(), GeckoNpcFallbackRenderer::new);
        }
        
        // Register poster entity renderer
        event.registerEntityRenderer(Marallyzen.POSTER_ENTITY.get(), neutka.marallys.marallyzen.client.renderer.PosterEntityRenderer::new);
        event.registerEntityRenderer(Marallyzen.DICTAPHONE_ENTITY.get(), neutka.marallys.marallyzen.client.renderer.DictaphoneEntityRenderer::new);
        event.registerBlockEntityRenderer(
                MarallyzenBlockEntities.INTERACTIVE_CHAIN_BE.get(),
                InteractiveChainBlockEntityRenderer::new
        );
        event.registerBlockEntityRenderer(
                MarallyzenBlockEntities.OLD_TV_BE.get(),
                OldTvBlockEntityRenderer::new
        );
    }

    private static boolean isGeckoLibAvailable() {
        try {
            Class.forName("software.bernie.geckolib.GeckoLib");
            return true;
        } catch (ClassNotFoundException e) {
            Marallyzen.LOGGER.warn("GeckoLib not found; Gecko NPCs will render as invisible fallback.");
            return false;
        }
    }
    
    @SubscribeEvent
    static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        // Clear flashlight state cache on disconnect
        neutka.marallys.marallyzen.client.FlashlightStateCache.clear();
        
        // Clear client poster entities on disconnect
        // Execute on render thread to avoid OpenGL errors with Sodium
        net.minecraft.client.Minecraft.getInstance().execute(() -> {
            neutka.marallys.marallyzen.client.ClientPosterManager.clearAll();
            neutka.marallys.marallyzen.client.ClientDictaphoneManager.clearAll();
        });
    }

    /**
     * Opens a dialog HUD on the client.
     */
    public static void openDialog(String dialogId, String title, Map<String, String> buttons, java.util.UUID npcEntityUuid) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            if (minecraft.level == null) {
                return;
            }
            
            // Find the NPC entity by UUID
            net.minecraft.world.entity.Entity npcEntity = null;
            for (net.minecraft.world.entity.Entity entity : minecraft.level.entitiesForRendering()) {
                if (entity.getUUID().equals(npcEntityUuid)) {
                    npcEntity = entity;
                    break;
                }
            }
            
            if (npcEntity != null) {
                // Update dialog content and trigger state transition
                neutka.marallys.marallyzen.client.gui.DialogHud dialogHud = neutka.marallys.marallyzen.client.gui.DialogHud.getInstance();
                dialogHud.updateDialogContent(dialogId, title, buttons, npcEntity);
            } else {
                Marallyzen.LOGGER.warn("Could not find NPC entity with UUID: {}", npcEntityUuid);
            }
        });
    }
}
