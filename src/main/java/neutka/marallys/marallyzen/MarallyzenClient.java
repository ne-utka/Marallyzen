package neutka.marallys.marallyzen;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import java.util.Map;

import neutka.marallys.marallyzen.client.gui.DialogScreen;
import neutka.marallys.marallyzen.blocks.MarallyzenBlockEntities;
import neutka.marallys.marallyzen.client.renderer.DecoratedPotCarryEntityRenderer;
import neutka.marallys.marallyzen.client.renderer.GeckoNpcFallbackRenderer;
import neutka.marallys.marallyzen.client.renderer.GoalRenderer;
import neutka.marallys.marallyzen.client.renderer.InteractiveLeverBlockEntityRenderer;
import neutka.marallys.marallyzen.client.renderer.OldTvBlockEntityRenderer;

@Mod(value = Marallyzen.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT)
public class MarallyzenClient {
    private static String lastBlurScreenLogged = "";

    public MarallyzenClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        container.registerConfig(ModConfig.Type.CLIENT, MarallyzenClientConfig.SPEC);
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

            // Poster blocks use masked transparency (holes). Force CUTOUT render layer to avoid translucent blending artifacts.
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.POSTER_1.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.POSTER_2.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.POSTER_3.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.POSTER_4.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.POSTER_5.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.POSTER_6.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.POSTER_7.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.POSTER_8.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.POSTER_9.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.POSTER_10.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.OLD_POSTER.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.PAPER_POSTER_1.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.PAPER_POSTER_2.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.BANK_SIGN.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.BAR_SIGN.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.BARREL_FULL.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.BARREL_FULL_PILE.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.COACH.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.LARGE_CACTUS_POT.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.MEDIUM_CACTUS_POT.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.MINI_CACTUS_POT.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.WEST_TABLE_BAR.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.WEST_CHAIR_BAR.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.WOODEN_BUCKET.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.DRYING_FISH_RACK.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.FISH.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.FISHING_NET_WALL_DECORATION.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.FISHING_ROD.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.FISHING_ROD_RACK.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.FISH_BOX.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.FISH_BOX_EMPTY.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.FISH_PILE.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.FISH_PRIZE_WALL_DECORATION.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.LEANING_FISHING_ROD.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.TAVERN_BENCH.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.TAVERN_BIG_KEG.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.TAVERN_BIG_KEG2.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.TAVERN_BIG_TABLE.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.TAVERN_KEG.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.TAVERN_KEG2.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.TAVERN_KEG_SUPPORT.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.TAVERN_KEG_SUPPORT_DOUBLE.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.TAVERN_MULTIPLE_BOTTLES.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.TAVERN_MURAL_SHELF.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.TAVERN_PILE_BOTTLES.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.TAVERN_RED_BOTTLE.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.TAVERN_SMALL_GREEN_BOTTLE.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.TAVERN_STOOL.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.TAVERN_TABLE.get(), ChunkSectionLayer.CUTOUT);
        });
    }

    private static void disableBlurForDirectorUi() {
        // Replay UI removed.
    }

    private static void updateBlurDisableForCurrentScreen(Minecraft mc) {
        // Replay UI removed.
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
        
        // Tick dictaphone client visibility + despawn logic
        neutka.marallys.marallyzen.client.ClientDictaphoneManager.clientTick();
        
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
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
        event.registerEntityRenderer(Marallyzen.GOAL_DISPLAY_ENTITY.get(), GoalRenderer::new);
        event.registerBlockEntityRenderer(
                MarallyzenBlockEntities.OLD_TV_BE.get(),
                OldTvBlockEntityRenderer::new
        );
        event.registerBlockEntityRenderer(
                MarallyzenBlockEntities.INTERACTIVE_LEVER_BE.get(),
                InteractiveLeverBlockEntityRenderer::new
        );
        event.registerBlockEntityRenderer(
                MarallyzenBlockEntities.INTERACTIVE_VALVE_BE.get(),
                neutka.marallys.marallyzen.client.renderer.InteractiveValveBlockEntityRenderer::new
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
