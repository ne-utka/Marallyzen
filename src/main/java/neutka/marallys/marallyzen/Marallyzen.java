package neutka.marallys.marallyzen;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

import neutka.marallys.marallyzen.npc.GeckoNpcEntity;
import neutka.marallys.marallyzen.npc.NpcClickHandler;
import neutka.marallys.marallyzen.npc.NpcLoader;
import neutka.marallys.marallyzen.npc.NpcSpawner;
import neutka.marallys.marallyzen.npc.NpcStateStore;
import neutka.marallys.marallyzen.items.MarallyzenItems;
import neutka.marallys.marallyzen.blocks.MarallyzenBlocks;
import neutka.marallys.marallyzen.audio.VoiceIntegration;
import neutka.marallys.marallyzen.audio.MarallyzenPlasmoVoiceAddon;
import neutka.marallys.marallyzen.audio.MarallyzenSounds;

@Mod(Marallyzen.MODID)
public class Marallyzen {
    public static final String MODID = "marallyzen";
    public static final Logger LOGGER = LogUtils.getLogger();

    // Entity registry
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);

    public static final net.neoforged.neoforge.registries.DeferredHolder<EntityType<?>, EntityType<GeckoNpcEntity>> GECKO_NPC = ENTITIES.register(
            "gecko_npc",
            () -> EntityType.Builder.<GeckoNpcEntity>of(GeckoNpcEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.8f)
                    .build(Marallyzen.MODID + ":gecko_npc")
    );

    
    // Poster entity
    public static final net.neoforged.neoforge.registries.DeferredHolder<EntityType<?>, EntityType<neutka.marallys.marallyzen.entity.PosterEntity>> POSTER_ENTITY = ENTITIES.register(
            "poster",
            () -> EntityType.Builder.<neutka.marallys.marallyzen.entity.PosterEntity>of(neutka.marallys.marallyzen.entity.PosterEntity::new, MobCategory.MISC)
                    .sized(1.0f, 1.0f)
                    .build(MODID + ":poster")
    );

    public static final net.neoforged.neoforge.registries.DeferredHolder<EntityType<?>, EntityType<neutka.marallys.marallyzen.entity.DictaphoneEntity>> DICTAPHONE_ENTITY = ENTITIES.register(
            "dictaphone",
            () -> EntityType.Builder.<neutka.marallys.marallyzen.entity.DictaphoneEntity>of(neutka.marallys.marallyzen.entity.DictaphoneEntity::new, MobCategory.MISC)
                    .sized(1.0f, 1.0f)
                    .build(MODID + ":dictaphone")
    );
    

    public Marallyzen(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, MarallyzenConfig.SPEC);
        DenizenService.initIfNeeded(modContainer);

        // Initialize PlasmoVoice integration
        VoiceIntegration.init();
        
        // Register PlasmoVoice addon (if available)
        MarallyzenPlasmoVoiceAddon.registerAddon();

        // Register entities
        ENTITIES.register(modEventBus);
        
        // Register blocks
        MarallyzenBlocks.BLOCKS.register(modEventBus);

        MarallyzenSounds.SOUND_EVENTS.register(modEventBus);
        
        // Register block entities
        neutka.marallys.marallyzen.blocks.MarallyzenBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        
        // Register items and creative mode tabs
        MarallyzenItems.ITEMS.register(modEventBus);
        MarallyzenItems.CREATIVE_MODE_TABS.register(modEventBus);

        // ServerStartingEvent must be registered on NeoForge.EVENT_BUS (GAME bus), not MOD bus
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
        NeoForge.EVENT_BUS.addListener(this::onServerStopping);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        neutka.marallys.marallyzen.quest.QuestManager.getInstance().initialize(event.getServer());
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        var registry = NpcClickHandler.getRegistry();
        registry.resetRuntimeState();
        NpcLoader.loadNpcsFromDirectory(registry);
        NpcSpawner.bootstrap(event.getServer().overworld(), registry);
        var states = NpcStateStore.load();
        int spawned = registry.spawnConfiguredNpcs(event.getServer().overworld(), states);
        LOGGER.info("Marallyzen server started. Loaded {} NPCs, spawned {}.", registry.getAllNpcData().size(), spawned);
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        var registry = NpcClickHandler.getRegistry();
        NpcStateStore.save(registry.captureNpcStates());
        neutka.marallys.marallyzen.npc.NpcSpawner.resetBootstrap();
        neutka.marallys.marallyzen.quest.QuestManager.getInstance().shutdown();
        LOGGER.info("Marallyzen server stopping. Saved {} NPC state(s).", registry.captureNpcStates().size());
    }
}
