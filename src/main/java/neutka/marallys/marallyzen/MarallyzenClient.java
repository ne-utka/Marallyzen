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
import neutka.marallys.marallyzen.client.director.DirectorReplayOverlayBridge;
import neutka.marallys.marallyzen.client.director.DirectorReplayBrowserScreen;
import neutka.marallys.marallyzen.blocks.MarallyzenBlockEntities;
import neutka.marallys.marallyzen.client.renderer.DecoratedPotCarryEntityRenderer;
import neutka.marallys.marallyzen.client.renderer.GeckoNpcFallbackRenderer;
import neutka.marallys.marallyzen.client.renderer.InteractiveChainBlockEntityRenderer;
import neutka.marallys.marallyzen.client.renderer.InteractiveLeverBlockEntityRenderer;
import neutka.marallys.marallyzen.client.renderer.OldTvBlockEntityRenderer;
import neutka.marallys.marallyzen.replay.ReplayCompat;
import neutka.marallys.marallyzen.replay.ReplayReturnManager;
import neutka.marallys.marallyzen.replay.ReplayStartQueue;
import neutka.marallys.marallyzen.replay.client.ReplayEmoteVisualChannel;
import neutka.marallys.marallyzen.replay.client.ReplayVisualChannelRegistry;

@Mod(value = Marallyzen.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class MarallyzenClient {
    private static String lastBlurScreenLogged = "";
    private static boolean lastReplayActive = false;

    public MarallyzenClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            Marallyzen.LOGGER.info("Marallyzen client setup; player={}", Minecraft.getInstance().getUser().getName());
            disableBlurForDirectorUi();

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

            // Load replay camera tracks
            neutka.marallys.marallyzen.replay.camera.ReplayCameraTrackLoader.loadTracks();
            Marallyzen.LOGGER.info("Loaded {} replay camera tracks", neutka.marallys.marallyzen.replay.camera.ReplayCameraTrackLoader.getAllTracks().size());

            // Load replay timelines
            neutka.marallys.marallyzen.replay.timeline.TimelineLoader.loadTracks();
            neutka.marallys.marallyzen.replay.timeline.TimelineActionRegistry.registerDefaults();
            Marallyzen.LOGGER.info("Loaded {} replay timelines", neutka.marallys.marallyzen.replay.timeline.TimelineLoader.getAllTracks().size());

            ReplayVisualChannelRegistry.register(new ReplayEmoteVisualChannel());

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
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.BANK_SIGN.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.BAR_SIGN.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.BARREL_FULL.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.BARREL_FULL_PILE.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.COACH.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.LARGE_CACTUS_POT.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.MEDIUM_CACTUS_POT.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.MINI_CACTUS_POT.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.WEST_TABLE_BAR.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.WEST_CHAIR_BAR.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.WOODEN_BUCKET.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.DRYING_FISH_RACK.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.FISH.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.FISHING_NET_WALL_DECORATION.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.FISHING_ROD.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.FISHING_ROD_RACK.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.FISH_BOX.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.FISH_BOX_EMPTY.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.FISH_PILE.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.FISH_PRIZE_WALL_DECORATION.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.LEANING_FISHING_ROD.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.TAVERN_BENCH.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.TAVERN_BIG_KEG.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.TAVERN_BIG_KEG2.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.TAVERN_BIG_TABLE.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.TAVERN_KEG.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.TAVERN_KEG2.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.TAVERN_KEG_SUPPORT.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.TAVERN_KEG_SUPPORT_DOUBLE.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.TAVERN_MULTIPLE_BOTTLES.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.TAVERN_MURAL_SHELF.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.TAVERN_PILE_BOTTLES.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.TAVERN_RED_BOTTLE.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.TAVERN_SMALL_GREEN_BOTTLE.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.TAVERN_STOOL.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.TAVERN_TABLE.get(), RenderType.cutout());
        });
    }

    private static void disableBlurForDirectorUi() {
        try {
            Class<?> configClass = Class.forName("eu.midnightdust.blur.config.BlurConfig");
            var field = configClass.getField("forceDisabledScreens");
            Object value = field.get(null);
            if (value instanceof java.util.List<?> list) {
                String screenName = "neutka.marallys.marallyzen.client.director.DirectorReplayBrowserScreen";
                if (!list.contains(screenName)) {
                    @SuppressWarnings("unchecked")
                    java.util.List<String> mutable = (java.util.List<String>) list;
                    mutable.add(screenName);
                    Marallyzen.LOGGER.info("Blur disabled for director UI.");
                }
            }
        } catch (ClassNotFoundException e) {
            // Blur is not installed.
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("Failed to disable blur for director UI.", e);
        }
    }

    private static void updateBlurDisableForCurrentScreen(Minecraft mc) {
        if (mc.screen == null) {
            return;
        }
        boolean shouldDisable = mc.screen instanceof DirectorReplayBrowserScreen || ReplayCompat.isReplayActive();
        if (!shouldDisable) {
            return;
        }
        try {
            Class<?> configClass = Class.forName("eu.midnightdust.blur.config.BlurConfig");
            var field = configClass.getField("forceDisabledScreens");
            Object value = field.get(null);
            if (value instanceof java.util.List<?> list) {
                @SuppressWarnings("unchecked")
                java.util.List<String> mutable = (java.util.List<String>) list;
                String name = mc.screen.getClass().getCanonicalName();
                if (name != null && !name.equals(lastBlurScreenLogged)) {
                    Marallyzen.LOGGER.info("Replay UI screen detected: {}", name);
                    lastBlurScreenLogged = name;
                }
                addScreenAndParentsToBlurList(mutable, mc.screen.getClass());
            }
            forceDisableBlurAnimations();
        } catch (ClassNotFoundException e) {
            // Blur is not installed.
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("Failed to disable blur for current screen.", e);
        }
    }

    private static void addScreenAndParentsToBlurList(java.util.List<String> list, Class<?> type) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            String name = current.getCanonicalName();
            if (name != null && !list.contains(name)) {
                list.add(name);
            }
            current = current.getSuperclass();
        }
    }

    private static void forceDisableBlurAnimations() {
        try {
            Class<?> blurClass = Class.forName("eu.midnightdust.blur.Blur");
            Object blurAnimation = blurClass.getField("blurAnimation").get(null);
            Object backgroundAnimation = blurClass.getField("backgroundAnimation").get(null);
            if (blurAnimation != null) {
                setBlurAnimationDisabled(blurAnimation);
            }
            if (backgroundAnimation != null) {
                setBlurAnimationDisabled(backgroundAnimation);
            }
            Class<?> configClass = Class.forName("eu.midnightdust.blur.config.BlurConfig");
            configClass.getField("useGradient").setBoolean(null, false);
        } catch (ClassNotFoundException e) {
            // Blur is not installed.
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("Failed to force-disable blur animations.", e);
        }
    }

    private static void setBlurAnimationDisabled(Object animation) throws ReflectiveOperationException {
        Class<?> animClass = animation.getClass();
        animClass.getField("enabled").setBoolean(animation, false);
        animClass.getField("fadeProgress").setFloat(animation, 0.0f);
        animClass.getField("fadeTimeState").setFloat(animation, 0.0f);
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        // Client-side DenizenCore tick (if needed in the future)
        // For now, scripts run server-side only

        // Tick narration manager (updates narration overlay state and alpha interpolation)
        neutka.marallys.marallyzen.client.narration.NarrationManager.getInstance().tick();
        
        // Tick poster narration hints (block vs active entity)
        neutka.marallys.marallyzen.client.PosterNarrationService.tick();
        
        // Tick screen fade manager
        neutka.marallys.marallyzen.client.cutscene.ScreenFadeManager.getInstance().tick();
        
        // Tick eyes close manager
        neutka.marallys.marallyzen.client.cutscene.EyesCloseManager.getInstance().tick();

        // Tick dictaphone client visibility + despawn logic
        neutka.marallys.marallyzen.client.ClientDictaphoneManager.clientTick();
        
        // Tick cutscene editor if open
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        updateBlurDisableForCurrentScreen(mc);
        ReplayCompat.tryOverrideReplayViewerLoadButton(mc.screen);
        if (ReplayCompat.isReplayAvailable()) {
            ReplayCompat.runReplayModTasks();
        }
        ReplayStartQueue.tick();
        boolean replayActive = ReplayCompat.isReplayActive();
        DirectorReplayOverlayBridge.updateReplayState(lastReplayActive, replayActive);
        neutka.marallys.marallyzen.client.director.DirectorOverlayHud.tick();
        var directorTimeSource = neutka.marallys.marallyzen.director.ReplayTimeSourceHolder.get();
        if (directorTimeSource != null && neutka.marallys.marallyzen.director.DirectorRuntime.isPreviewing()) {
            neutka.marallys.marallyzen.director.DirectorRuntime.tick(directorTimeSource.getTimestamp());
        }
        ReplayReturnManager.getInstance().onReplayStateChanged(lastReplayActive, replayActive);
        ReplayReturnManager.getInstance().tick(mc, replayActive);
        lastReplayActive = replayActive;
        if (neutka.marallys.marallyzen.replay.LegacyReplayGate.isLegacyReplayEnabled()) {
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

        // Tick replay camera director (ReplayMod-backed)
        neutka.marallys.marallyzen.replay.camera.ReplayCameraDirector.getInstance().tick();

        // Tick replay timeline scheduler (ReplayMod-backed)
        neutka.marallys.marallyzen.replay.timeline.TimelineScheduler.getInstance().tick();
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
        event.registerEntityRenderer(Marallyzen.DECORATED_POT_ENTITY.get(), DecoratedPotCarryEntityRenderer::new);
        event.registerBlockEntityRenderer(
                MarallyzenBlockEntities.INTERACTIVE_CHAIN_BE.get(),
                InteractiveChainBlockEntityRenderer::new
        );
        event.registerBlockEntityRenderer(
                MarallyzenBlockEntities.OLD_TV_BE.get(),
                OldTvBlockEntityRenderer::new
        );
        event.registerBlockEntityRenderer(
                MarallyzenBlockEntities.INTERACTIVE_LEVER_BE.get(),
                InteractiveLeverBlockEntityRenderer::new
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
        neutka.marallys.marallyzen.client.animation.LeverShakeAnimationClient.clear();

        // Clear client poster entities on disconnect
        // Execute on render thread to avoid OpenGL errors with Sodium
        net.minecraft.client.Minecraft.getInstance().execute(() -> {
            neutka.marallys.marallyzen.client.ClientPosterManager.clearAll();
            neutka.marallys.marallyzen.client.ClientDictaphoneManager.clearAll();
            neutka.marallys.marallyzen.client.ClientRadioManager.clearAll();
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
