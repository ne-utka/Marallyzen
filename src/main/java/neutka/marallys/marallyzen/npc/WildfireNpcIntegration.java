package neutka.marallys.marallyzen.npc;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.fml.ModList;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.network.NetworkHelper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.util.Locale;
import java.util.Set;

public final class WildfireNpcIntegration {
    private static final String CLASS_WILDFIRE_GENDER = "com.wildfire.main.WildfireGender";
    private static final String CLASS_ENTITY_CONFIG = "com.wildfire.main.entitydata.EntityConfig";
    private static final String CLASS_PLAYER_CONFIG = "com.wildfire.main.entitydata.PlayerConfig";
    private static final String CLASS_BREASTS = "com.wildfire.main.entitydata.Breasts";
    private static final String CLASS_GENDER_MAIN = "com.wildfire.main.Gender";
    private static final String CLASS_GENDER_LEGACY = "com.wildfire.main.config.enums.Gender";
    private static final String CLASS_CLIENTBOUND_SYNC = "com.wildfire.main.networking.ClientboundSyncPacket";

    private static final boolean AVAILABLE;
    private static final Class<?> GENDER_CLASS;
    private static final Constructor<?> CLIENTBOUND_SYNC_CTOR;
    private static final Constructor<?> PLAYER_CONFIG_CTOR;
    private static final Field PLAYER_CACHE_FIELD;
    private static final Method GET_ENTITY_CONFIG;
    private static final Method GET_OR_ADD_PLAYER;
    private static final Method GET_TRACKERS;
    private static final Method UPDATE_GENDER;
    private static final Method UPDATE_BUST_SIZE;
    private static final Method UPDATE_BREAST_PHYSICS;
    private static final Method UPDATE_BOUNCE;
    private static final Method UPDATE_FLOPPINESS;
    private static final Method UPDATE_SHOW_IN_ARMOR;
    private static final Method GET_BREASTS;
    private static final Method UPDATE_X_OFFSET;
    private static final Method UPDATE_Y_OFFSET;
    private static final Method UPDATE_Z_OFFSET;
    private static final Method UPDATE_CLEAVAGE;
    private static final Method UPDATE_UNIBOOB;

    static {
        Class<?> genderClass = null;
        Constructor<?> clientboundCtor = null;
        Constructor<?> playerConfigCtor = null;
        Field playerCacheField = null;
        Method getEntityConfig = null;
        Method getOrAddPlayer = null;
        Method getTrackers = null;
        Method updateGender = null;
        Method updateBustSize = null;
        Method updateBreastPhysics = null;
        Method updateBounce = null;
        Method updateFloppiness = null;
        Method updateShowInArmor = null;
        Method getBreasts = null;
        Method updateXOffset = null;
        Method updateYOffset = null;
        Method updateZOffset = null;
        Method updateCleavage = null;
        Method updateUniboob = null;
        boolean available = false;

        try {
            genderClass = resolveGenderClass();
            clientboundCtor = resolveClientboundCtor();
            playerConfigCtor = resolvePlayerConfigCtor();

            Class<?> entityConfigClass = Class.forName(CLASS_ENTITY_CONFIG);
            getEntityConfig = entityConfigClass.getMethod("getEntity", LivingEntity.class);

            Class<?> wildfireGenderClass = Class.forName(CLASS_WILDFIRE_GENDER);
            getOrAddPlayer = wildfireGenderClass.getMethod("getOrAddPlayerById", java.util.UUID.class);
            getTrackers = findMethod(wildfireGenderClass, "getTrackers", 1);
            playerCacheField = wildfireGenderClass.getField("PLAYER_CACHE");

            Class<?> playerConfigClass = Class.forName(CLASS_PLAYER_CONFIG);
            updateGender = findMethod(playerConfigClass, "updateGender", 1);
            updateBustSize = findMethod(playerConfigClass, "updateBustSize", 1);
            updateBreastPhysics = findMethod(playerConfigClass, "updateBreastPhysics", 1);
            updateBounce = findMethod(playerConfigClass, "updateBounceMultiplier", 1);
            updateFloppiness = findMethod(playerConfigClass, "updateFloppiness", 1);
            updateShowInArmor = findMethod(playerConfigClass, "updateShowBreastsInArmor", 1);

            getBreasts = findMethod(entityConfigClass, "getBreasts", 0);

            Class<?> breastsClass = Class.forName(CLASS_BREASTS);
            updateXOffset = findMethod(breastsClass, "updateXOffset", 1);
            updateYOffset = findMethod(breastsClass, "updateYOffset", 1);
            updateZOffset = findMethod(breastsClass, "updateZOffset", 1);
            updateCleavage = findMethod(breastsClass, "updateCleavage", 1);
            updateUniboob = findMethod(breastsClass, "updateUniboob", 1);

            available = genderClass != null && clientboundCtor != null;
        } catch (Throwable ex) {
            available = false;
        }

        AVAILABLE = available;
        GENDER_CLASS = genderClass;
        CLIENTBOUND_SYNC_CTOR = clientboundCtor;
        PLAYER_CONFIG_CTOR = playerConfigCtor;
        PLAYER_CACHE_FIELD = playerCacheField;
        GET_ENTITY_CONFIG = getEntityConfig;
        GET_OR_ADD_PLAYER = getOrAddPlayer;
        GET_TRACKERS = getTrackers;
        UPDATE_GENDER = updateGender;
        UPDATE_BUST_SIZE = updateBustSize;
        UPDATE_BREAST_PHYSICS = updateBreastPhysics;
        UPDATE_BOUNCE = updateBounce;
        UPDATE_FLOPPINESS = updateFloppiness;
        UPDATE_SHOW_IN_ARMOR = updateShowInArmor;
        GET_BREASTS = getBreasts;
        UPDATE_X_OFFSET = updateXOffset;
        UPDATE_Y_OFFSET = updateYOffset;
        UPDATE_Z_OFFSET = updateZOffset;
        UPDATE_CLEAVAGE = updateCleavage;
        UPDATE_UNIBOOB = updateUniboob;

        if (AVAILABLE) {
            logWildfireVersion();
        }
    }

    private WildfireNpcIntegration() {
    }

    public static boolean isAvailable() {
        return AVAILABLE;
    }

    public static void applyNpcSettings(Entity entity, NpcData data, ServerLevel level) {
        if (!AVAILABLE) {
            Marallyzen.LOGGER.info("Wildfire NPC: integration unavailable, skipping {}", data.getId());
            return;
        }
        if (!(entity instanceof LivingEntity living)) {
            Marallyzen.LOGGER.info("Wildfire NPC: entity {} is not LivingEntity, skipping {}", entity.getType(), data.getId());
            return;
        }

        NpcData.WildfireSettings settings = data.getWildfireSettings();
        if (settings == null) {
            Marallyzen.LOGGER.info("Wildfire NPC: no settings for {}", data.getId());
            return;
        }
        if (settings.getEnabled() != null && !settings.getEnabled()) {
            Marallyzen.LOGGER.info("Wildfire NPC: disabled in config for {}", data.getId());
            return;
        }

        try {
            Object config = getEntityConfig(living);
            if (config == null) {
                config = createPlayerConfig(entity.getUUID());
                if (config == null) {
                    Marallyzen.LOGGER.info("Wildfire NPC: no config for {} ({})", data.getId(), entity.getUUID());
                    return;
                }
                cachePlayerConfig(entity.getUUID(), config);
            }

            Marallyzen.LOGGER.info(
                    "Wildfire NPC: applying settings for {} gender={} bustSize={} physics={} offsets=({}, {}, {})",
                    data.getId(),
                    settings.getGender(),
                    settings.getBustSize(),
                    settings.getBreastPhysics(),
                    settings.getXOffset(),
                    settings.getYOffset(),
                    settings.getZOffset()
            );
            applyConfig(config, settings);
            syncConfig(entity, config);
        } catch (Throwable ex) {
            Marallyzen.LOGGER.error("Wildfire NPC integration failed for {}", data.getId(), ex);
        }
    }

    public static void syncAllToPlayer(ServerPlayer player, NpcRegistry registry) {
        if (!AVAILABLE) {
            Marallyzen.LOGGER.info("Wildfire NPC: integration unavailable, skip sync for {}", player.getName().getString());
            return;
        }
        if (CLIENTBOUND_SYNC_CTOR == null) {
            Marallyzen.LOGGER.info("Wildfire NPC: clientbound ctor missing, skip sync for {}", player.getName().getString());
            return;
        }
        if (registry == null) {
            return;
        }
        for (Entity entity : registry.getSpawnedNpcs()) {
            if (!(entity instanceof ServerPlayer npcPlayer)) {
                continue;
            }
            String npcId = registry.getNpcId(entity);
            if (npcId == null) {
                continue;
            }
            NpcData data = registry.getNpcData(npcId);
            if (data == null || data.getWildfireSettings() == null) {
                continue;
            }
            if (data.getWildfireSettings().getEnabled() != null && !data.getWildfireSettings().getEnabled()) {
                continue;
            }
            Object config = getEntityConfig(npcPlayer);
            if (config == null) {
                Marallyzen.LOGGER.info("Wildfire NPC: no config for {} when syncing to {}", npcId, player.getName().getString());
                continue;
            }
            Marallyzen.LOGGER.info("Wildfire NPC: sync {} to {}", npcId, player.getName().getString());
            CustomPacketPayload payload = createClientboundPayload(config);
            if (payload != null) {
                NetworkHelper.sendToPlayer(player, payload);
            }
        }
    }

    private static Object getEntityConfig(LivingEntity entity) {
        try {
            if (GET_ENTITY_CONFIG != null) {
                return GET_ENTITY_CONFIG.invoke(null, entity);
            }
        } catch (Throwable ignored) {
        }

        try {
            if (GET_OR_ADD_PLAYER != null) {
                return GET_OR_ADD_PLAYER.invoke(null, entity.getUUID());
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    private static void applyConfig(Object config, NpcData.WildfireSettings settings) {
        String gender = settings.getGender();
        Float bustSize = settings.getBustSize();
        if (gender == null && bustSize != null) {
            if (bustSize > 0f) {
                gender = "female";
            } else {
                gender = "male";
            }
        }

        if (gender != null) {
            Object genderEnum = resolveGenderEnum(gender);
            if (genderEnum != null && UPDATE_GENDER != null) {
                invokeSilently(config, UPDATE_GENDER, genderEnum);
            } else if (genderEnum != null) {
                setFieldSilently(config, "gender", genderEnum);
            }
        }

        if (bustSize != null) {
            if (UPDATE_BUST_SIZE != null) {
                invokeSilently(config, UPDATE_BUST_SIZE, bustSize);
            } else {
                setFieldSilently(config, "pBustSize", bustSize);
            }
        }

        if (settings.getBreastPhysics() != null) {
            if (UPDATE_BREAST_PHYSICS != null) {
                invokeSilently(config, UPDATE_BREAST_PHYSICS, settings.getBreastPhysics());
            } else {
                setFieldSilently(config, "breastPhysics", settings.getBreastPhysics());
            }
        }

        if (settings.getBounceMultiplier() != null) {
            if (UPDATE_BOUNCE != null) {
                invokeSilently(config, UPDATE_BOUNCE, settings.getBounceMultiplier());
            } else {
                setFieldSilently(config, "bounceMultiplier", settings.getBounceMultiplier());
            }
        }

        if (settings.getFloppiness() != null) {
            if (UPDATE_FLOPPINESS != null) {
                invokeSilently(config, UPDATE_FLOPPINESS, settings.getFloppiness());
            } else {
                setFieldSilently(config, "floppyMultiplier", settings.getFloppiness());
            }
        }

        if (settings.getShowBreastsInArmor() != null) {
            if (UPDATE_SHOW_IN_ARMOR != null) {
                invokeSilently(config, UPDATE_SHOW_IN_ARMOR, settings.getShowBreastsInArmor());
            } else {
                setFieldSilently(config, "showBreastsInArmor", settings.getShowBreastsInArmor());
            }
        }

        Object breasts = null;
        if (GET_BREASTS != null) {
            breasts = invokeSilently(config, GET_BREASTS);
        }
        if (breasts != null) {
            if (settings.getXOffset() != null && UPDATE_X_OFFSET != null) {
                invokeSilently(breasts, UPDATE_X_OFFSET, settings.getXOffset());
            }
            if (settings.getYOffset() != null && UPDATE_Y_OFFSET != null) {
                invokeSilently(breasts, UPDATE_Y_OFFSET, settings.getYOffset());
            }
            if (settings.getZOffset() != null && UPDATE_Z_OFFSET != null) {
                invokeSilently(breasts, UPDATE_Z_OFFSET, settings.getZOffset());
            }
            if (settings.getCleavage() != null && UPDATE_CLEAVAGE != null) {
                invokeSilently(breasts, UPDATE_CLEAVAGE, settings.getCleavage());
            }
            if (settings.getUniboob() != null && UPDATE_UNIBOOB != null) {
                invokeSilently(breasts, UPDATE_UNIBOOB, settings.getUniboob());
            }
        }
    }

    private static void syncConfig(Entity entity, Object config) {
        if (!(entity instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (CLIENTBOUND_SYNC_CTOR == null) {
            return;
        }
        CustomPacketPayload payload = createClientboundPayload(config);
        if (payload == null) {
            return;
        }
        Set<ServerPlayer> trackers = getTrackers(serverPlayer);
        if (trackers.isEmpty()) {
            NetworkHelper.sendToAll(payload);
            return;
        }
        for (ServerPlayer watcher : trackers) {
            if (watcher == null) {
                continue;
            }
            NetworkHelper.sendToPlayer(watcher, payload);
        }
    }

    private static Object resolveGenderEnum(String gender) {
        try {
            String normalized = gender.trim().toUpperCase(Locale.ROOT);
            if (GENDER_CLASS == null) {
                return null;
            }
            return Enum.valueOf((Class<Enum>) GENDER_CLASS, normalized);
        } catch (Throwable ex) {
            Marallyzen.LOGGER.warn("Wildfire NPC: unknown gender '{}'", gender);
            return null;
        }
    }

    private static Method findMethod(Class<?> clazz, String name, int paramCount) {
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == paramCount) {
                return method;
            }
        }
        Marallyzen.LOGGER.debug("Wildfire NPC integration: missing method {}#{}({})", clazz.getName(), name, paramCount);
        return null;
    }

    private static Object invokeSilently(Object target, Method method, Object... args) {
        try {
            if (method == null) {
                return null;
            }
            return method.invoke(target, args);
        } catch (InvocationTargetException ex) {
            Marallyzen.LOGGER.debug("Wildfire NPC integration: invoke failed {}", method.getName(), ex.getTargetException());
            return null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void setFieldSilently(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (NoSuchFieldException ex) {
            Marallyzen.LOGGER.debug("Wildfire NPC integration: missing field {} on {}", fieldName, target.getClass().getName());
        } catch (Throwable ignored) {
        }
    }

    private static void logWildfireVersion() {
        try {
            ModList.get().getModContainerById("wildfire_gender").ifPresent(container -> {
                String version = container.getModInfo().getVersion().toString();
                Marallyzen.LOGGER.info("Wildfire NPC integration: wildfire_gender {}", version);
            });
        } catch (Throwable ex) {
            Marallyzen.LOGGER.debug("Wildfire NPC integration: unable to resolve mod version", ex);
        }
    }

    private static Class<?> resolveGenderClass() {
        try {
            return Class.forName(CLASS_GENDER_MAIN);
        } catch (Throwable ignored) {
        }
        try {
            return Class.forName(CLASS_GENDER_LEGACY);
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Constructor<?> resolveClientboundCtor() {
        try {
            Class<?> packetClass = Class.forName(CLASS_CLIENTBOUND_SYNC);
            for (Constructor<?> ctor : packetClass.getConstructors()) {
                if (ctor.getParameterCount() == 1) {
                    return ctor;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static CustomPacketPayload createClientboundPayload(Object config) {
        try {
            if (CLIENTBOUND_SYNC_CTOR == null) {
                return null;
            }
            Object payload = CLIENTBOUND_SYNC_CTOR.newInstance(config);
            if (payload instanceof CustomPacketPayload custom) {
                return custom;
            }
        } catch (Throwable ex) {
            Marallyzen.LOGGER.debug("Wildfire NPC integration: failed to build clientbound payload", ex);
        }
        return null;
    }

    private static Set<ServerPlayer> getTrackers(Player player) {
        if (GET_TRACKERS == null) {
            return Set.of();
        }
        Object result = invokeSilently(null, GET_TRACKERS, player);
        if (result instanceof Set<?> set) {
            try {
                return (Set<ServerPlayer>) set;
            } catch (ClassCastException ignored) {
            }
        }
        return Set.of();
    }

    private static Constructor<?> resolvePlayerConfigCtor() {
        try {
            Class<?> configClass = Class.forName(CLASS_PLAYER_CONFIG);
            return configClass.getConstructor(java.util.UUID.class);
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Object createPlayerConfig(java.util.UUID uuid) {
        try {
            if (PLAYER_CONFIG_CTOR == null) {
                return null;
            }
            return PLAYER_CONFIG_CTOR.newInstance(uuid);
        } catch (Throwable ex) {
            Marallyzen.LOGGER.debug("Wildfire NPC integration: failed to create PlayerConfig", ex);
            return null;
        }
    }

    private static void cachePlayerConfig(java.util.UUID uuid, Object config) {
        if (PLAYER_CACHE_FIELD == null) {
            return;
        }
        try {
            Object cache = PLAYER_CACHE_FIELD.get(null);
            if (cache instanceof java.util.Map<?, ?> map) {
                ((java.util.Map<java.util.UUID, Object>) map).put(uuid, config);
            }
        } catch (Throwable ex) {
            Marallyzen.LOGGER.debug("Wildfire NPC integration: failed to cache PlayerConfig", ex);
        }
    }
}
