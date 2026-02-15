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
import neutka.marallys.marallyzen.entity.TransparentGlowItemFrameEntity;
import neutka.marallys.marallyzen.entity.TransparentItemFrameEntity;
import neutka.marallys.marallyzen.entity.GoalDisplayEntity;
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
                    .build(net.minecraft.resources.ResourceKey.create(
                        net.minecraft.core.registries.Registries.ENTITY_TYPE,
                        net.minecraft.resources.Identifier.fromNamespaceAndPath(Marallyzen.MODID, "gecko_npc")
                    ))
    );

    
    // Poster entity
    public static final net.neoforged.neoforge.registries.DeferredHolder<EntityType<?>, EntityType<neutka.marallys.marallyzen.entity.PosterEntity>> POSTER_ENTITY = ENTITIES.register(
            "poster",
            () -> EntityType.Builder.<neutka.marallys.marallyzen.entity.PosterEntity>of(neutka.marallys.marallyzen.entity.PosterEntity::new, MobCategory.MISC)
                    .sized(1.0f, 1.0f)
                    .build(net.minecraft.resources.ResourceKey.create(
                        net.minecraft.core.registries.Registries.ENTITY_TYPE,
                        net.minecraft.resources.Identifier.fromNamespaceAndPath(MODID, "poster")
                    ))
    );

    public static final net.neoforged.neoforge.registries.DeferredHolder<EntityType<?>, EntityType<neutka.marallys.marallyzen.entity.DictaphoneEntity>> DICTAPHONE_ENTITY = ENTITIES.register(
            "dictaphone",
            () -> EntityType.Builder.<neutka.marallys.marallyzen.entity.DictaphoneEntity>of(neutka.marallys.marallyzen.entity.DictaphoneEntity::new, MobCategory.MISC)
                    .sized(1.0f, 1.0f)
                    .build(net.minecraft.resources.ResourceKey.create(
                        net.minecraft.core.registries.Registries.ENTITY_TYPE,
                        net.minecraft.resources.Identifier.fromNamespaceAndPath(MODID, "dictaphone")
                    ))
    );

    public static final net.neoforged.neoforge.registries.DeferredHolder<EntityType<?>, EntityType<neutka.marallys.marallyzen.entity.DecoratedPotCarryEntity>> DECORATED_POT_ENTITY = ENTITIES.register(
            "decorated_pot",
            () -> EntityType.Builder.<neutka.marallys.marallyzen.entity.DecoratedPotCarryEntity>of(neutka.marallys.marallyzen.entity.DecoratedPotCarryEntity::new, MobCategory.MISC)
                    .sized(1.0f, 1.0f)
                    .build(net.minecraft.resources.ResourceKey.create(
                        net.minecraft.core.registries.Registries.ENTITY_TYPE,
                        net.minecraft.resources.Identifier.fromNamespaceAndPath(MODID, "decorated_pot")
                    ))
    );

    public static final net.neoforged.neoforge.registries.DeferredHolder<EntityType<?>, EntityType<GoalDisplayEntity>> GOAL_DISPLAY_ENTITY = ENTITIES.register(
            "goal_display",
            () -> EntityType.Builder.<GoalDisplayEntity>of(GoalDisplayEntity::new, MobCategory.MISC)
                    .sized(0.1f, 0.1f)
                    .build(net.minecraft.resources.ResourceKey.create(
                            net.minecraft.core.registries.Registries.ENTITY_TYPE,
                            net.minecraft.resources.Identifier.fromNamespaceAndPath(MODID, "goal_display")
                    ))
    );

    public static final net.neoforged.neoforge.registries.DeferredHolder<EntityType<?>, EntityType<TransparentItemFrameEntity>> TRANSPARENT_ITEM_FRAME_ENTITY = ENTITIES.register(
            "transparent_item_frame",
            () -> EntityType.Builder.<TransparentItemFrameEntity>of(TransparentItemFrameEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(10)
                    .updateInterval(Integer.MAX_VALUE)
                    .build(net.minecraft.resources.ResourceKey.create(
                            net.minecraft.core.registries.Registries.ENTITY_TYPE,
                            net.minecraft.resources.Identifier.fromNamespaceAndPath(MODID, "transparent_item_frame")
                    ))
    );

    public static final net.neoforged.neoforge.registries.DeferredHolder<EntityType<?>, EntityType<TransparentGlowItemFrameEntity>> TRANSPARENT_GLOW_ITEM_FRAME_ENTITY = ENTITIES.register(
            "transparent_glow_item_frame",
            () -> EntityType.Builder.<TransparentGlowItemFrameEntity>of(TransparentGlowItemFrameEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(10)
                    .updateInterval(Integer.MAX_VALUE)
                    .build(net.minecraft.resources.ResourceKey.create(
                            net.minecraft.core.registries.Registries.ENTITY_TYPE,
                            net.minecraft.resources.Identifier.fromNamespaceAndPath(MODID, "transparent_glow_item_frame")
                    ))
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
        neutka.marallys.marallyzen.goals.GoalProgressEngine.getInstance().initialize(event.getServer());
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        var registry = NpcClickHandler.getRegistry();
        registry.resetRuntimeState();
        NpcLoader.loadNpcsFromDirectory(registry);
        NpcSpawner.bootstrap(event.getServer().overworld(), registry);
        LOGGER.info("Marallyzen server started. Loaded {} NPCs, spawning via chunk loader.", registry.getAllNpcData().size());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        var registry = NpcClickHandler.getRegistry();
        neutka.marallys.marallyzen.npc.NpcSpawner.resetBootstrap();
        neutka.marallys.marallyzen.quest.QuestManager.getInstance().shutdown();
        neutka.marallys.marallyzen.goals.GoalProgressEngine.getInstance().shutdown();
        LOGGER.info("Marallyzen server stopping. Saved {} NPC state(s).", registry.captureNpcStates().size());
    }
}


