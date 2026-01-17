package neutka.marallys.marallyzen.npc;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import net.neoforged.neoforge.registries.DeferredHolder;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.npc.GeckoNpcEntity;
import neutka.marallys.marallyzen.npc.NpcExpressionManager;
import net.minecraft.world.phys.AABB;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing NPCs.
 * Handles loading, spawning, and tracking NPC entities.
 */
public class NpcRegistry {
    private final Map<String, NpcData> npcDataMap = new ConcurrentHashMap<>();
    private final Map<String, Entity> spawnedNpcs = new ConcurrentHashMap<>();
    private final Map<Entity, String> entityToNpcId = new ConcurrentHashMap<>();
    private final Map<Entity, WaypointAI> npcAIs = new ConcurrentHashMap<>();
    private final Map<Entity, Integer> npcAiErrors = new ConcurrentHashMap<>();
    private static final int VALK_DEFAULT_RADIUS = 5;
    private static final int VALK_DEFAULT_POINTS = 8;
    private static final int VALK_DEFAULT_WAIT_TICKS = 20;
    private static final double VALK_DEFAULT_SPEED = 0.3;
    private static final String VALK_PATTERN_RANDOM = "random";
    private static final String VALK_PATTERN_CIRCLE = "circle";
    private static final String VALK_PATTERN_SQUARE = "square";

    /**
     * Loads NPC data from JSON files in config/marallyzen/npcs/
     */
    public void loadNpcs() {
        // TODO: Implement JSON loading (will be done next)
        // For now, this is a placeholder
    }

    /**
     * Gets NPC data by ID.
     */
    public NpcData getNpcData(String id) {
        return npcDataMap.get(id);
    }

    /**
     * Registers NPC data.
     */
    public void registerNpcData(NpcData data) {
        npcDataMap.put(data.getId(), data);
    }

    public void unregisterNpcData(String id) {
        if (id == null || id.isEmpty()) {
            return;
        }
        npcDataMap.remove(id);
    }

    public String createNpcCopyId(String baseId) {
        NpcData original = npcDataMap.get(baseId);
        if (original == null) {
            throw new IllegalArgumentException("NPC not found: " + baseId);
        }
        int index = 1;
        String candidate = baseId + "_copy" + index;
        while (npcDataMap.containsKey(candidate) || spawnedNpcs.containsKey(candidate)) {
            index++;
            candidate = baseId + "_copy" + index;
        }
        NpcData clone = cloneNpcData(candidate, original);
        registerNpcData(clone);
        return candidate;
    }

    /**
     * Spawns an NPC entity in the world.
     */
    public Entity spawnNpc(String npcId, ServerLevel level, BlockPos pos) {
        return spawnNpc(npcId, level, pos, null);
    }
    
    /**
     * Spawns an NPC entity in the world with optional rotation from a player.
     * If player is provided, NPC will spawn with the same rotation as the player.
     */
    public Entity spawnNpc(String npcId, ServerLevel level, BlockPos pos, ServerPlayer sourcePlayer) {
        float yaw = 0.0f;
        float pitch = 0.0f;

        if (sourcePlayer != null) {
            yaw = sourcePlayer.getYRot();
            pitch = sourcePlayer.getXRot();
        }
        return spawnNpcInternal(npcId, level, pos, yaw, pitch, sourcePlayer);
    }

    public Entity spawnNpc(String npcId, ServerLevel level, BlockPos pos, float yaw, float pitch) {
        return spawnNpcInternal(npcId, level, pos, yaw, pitch, null);
    }

    private Entity spawnNpcInternal(String npcId, ServerLevel level, BlockPos pos, float yaw, float pitch, ServerPlayer sourcePlayer) {
        NpcData data = npcDataMap.get(npcId);
        if (data == null) {
            throw new IllegalArgumentException("NPC not found: " + npcId);
        }


        Entity entity;

        if (shouldUseGeckoNpc(data)) {
            entity = createGeckoEntity(level, data);
        } else {
            entity = createFallbackEntity(level, data);
        }

        if (entity == null) {
            throw new IllegalStateException("Failed to create entity for NPC: " + npcId);
        }

        // Set position and rotation
        double spawnX = pos.getX() + 0.5;
        double spawnY = computeSpawnY(level, pos);
        double spawnZ = pos.getZ() + 0.5;
        if (sourcePlayer != null) {
            spawnX = sourcePlayer.getX();
            spawnY = sourcePlayer.getY();
            spawnZ = sourcePlayer.getZ();
            yaw = sourcePlayer.getYRot();
            pitch = sourcePlayer.getXRot();
        }
        entity.moveTo(spawnX, spawnY, spawnZ, yaw, pitch);
        entity.setYRot(yaw);
        entity.setXRot(pitch);
        entity.yRotO = yaw;
        entity.xRotO = pitch;
        
        // If entity is a LivingEntity, also set body rotation to match player
        if (entity instanceof LivingEntity livingEntity) {
            float bodyYaw = yaw;
            float headYaw = yaw;
            if (sourcePlayer != null) {
                bodyYaw = sourcePlayer.getYRot();
                headYaw = sourcePlayer.getYHeadRot();
            }
            livingEntity.setYRot(yaw);
            livingEntity.setXRot(pitch);
            livingEntity.yRotO = yaw;
            livingEntity.xRotO = pitch;
            livingEntity.yBodyRot = bodyYaw;
            livingEntity.yHeadRot = headYaw;
            livingEntity.yBodyRotO = bodyYaw;
            livingEntity.yHeadRotO = headYaw;
        }
        
        // If entity is a ServerPlayer (fake player), send player info packet first
        if (entity instanceof ServerPlayer fakePlayer) {
            // Send player info packet to all players before adding entity
            // This is required so clients know about the player before the entity is added
            ClientboundPlayerInfoUpdatePacket playerInfoPacket = 
                ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(fakePlayer));
            
            for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
                if (player.connection != null) {
                    player.connection.send(playerInfoPacket);
                }
            }
        }
        
        level.addFreshEntity(entity);

        if (level instanceof ServerLevel serverLevel) {
            NpcSavedData.get(serverLevel).putState(
                    npcId,
                    new NpcState(serverLevel.dimension(), entity.blockPosition(), entity.getYRot(), npcId, null, null)
            );
        }
        
        // After adding entity, ensure all nearby players can see it
        if (entity instanceof ServerPlayer fakePlayer) {
            // Force update for all players in the same dimension
            for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
                if (player.level() == level && player.connection != null) {
                    // The entity should now be visible to all players
                    // The ChunkMap will automatically send the spawn packet
                }
            }
        }

        // Apply health and invulnerability settings
        if (entity instanceof LivingEntity livingEntity) {
            // Set max health first if specified
            if (data.getMaxHealth() != null) {
                AttributeInstance maxHealthAttr = livingEntity.getAttribute(Attributes.MAX_HEALTH);
                if (maxHealthAttr != null) {
                    maxHealthAttr.setBaseValue(data.getMaxHealth());
                }
            }
            
            // Set current health if specified, otherwise set to max health
            if (data.getHealth() != null) {
                livingEntity.setHealth(data.getHealth().floatValue());
            } else if (data.getMaxHealth() != null) {
                livingEntity.setHealth(data.getMaxHealth().floatValue());
            } else {
                // Set to max health by default
                AttributeInstance maxHealthAttr = livingEntity.getAttribute(Attributes.MAX_HEALTH);
                if (maxHealthAttr != null) {
                    livingEntity.setHealth((float) maxHealthAttr.getValue());
                }
            }
            
            // Set invulnerable (default: true for NPCs)
            boolean shouldBeInvulnerable = data.getInvulnerable() != null ? data.getInvulnerable() : true;
            livingEntity.setInvulnerable(shouldBeInvulnerable);
            
            // Set custom name (name tag) - show NPC name by default
            String npcName = data.getName() != null ? data.getName() : data.getId();
            livingEntity.setCustomName(net.minecraft.network.chat.Component.literal(npcName));
            boolean showNameTag = data.getShowNameTag() != null ? data.getShowNameTag() : true;
            livingEntity.setCustomNameVisible(showNameTag);
            applyNameTagVisibility(level, livingEntity, showNameTag);
        }

        WildfireNpcIntegration.applyNpcSettings(entity, data, level);

        if (entity instanceof GeckoNpcEntity geckoEntity) {
            geckoEntity.setNpcId(npcId);
            geckoEntity.setGeckolibModel(data.getGeckolibModel());
            geckoEntity.setGeckolibAnimation(data.getGeckolibAnimation());
            geckoEntity.setGeckolibTexture(data.getGeckolibTexture());
            NpcExpressionManager.applyDefaultExpression(geckoEntity, data);
        }
        if (entity instanceof NpcEntity npcEntity) {
            npcEntity.setNpcId(npcId);
            npcEntity.setAppearanceId(npcId);
        }

        spawnedNpcs.put(npcId, entity);
        entityToNpcId.put(entity, npcId);

        // Create AI if NPC has waypoints or valk is enabled
        if (Boolean.TRUE.equals(data.getValk())) {
            String pattern = normalizeValkPattern(data.getValkPattern());
            List<NpcData.Waypoint> waypoints = buildValkWaypoints(
                    level,
                    data.getSpawnPos() != null ? data.getSpawnPos() : pos,
                    data.getValkRadius() != null ? data.getValkRadius() : VALK_DEFAULT_RADIUS,
                    pattern,
                    VALK_DEFAULT_POINTS,
                    VALK_DEFAULT_WAIT_TICKS,
                    VALK_DEFAULT_SPEED
            );
            data.setWaypoints(waypoints);
            data.setWaypointsLoop(true);
            WaypointAI ai = new WaypointAI(entity, waypoints, level);
            ai.setLoop(true);
            npcAIs.put(entity, ai);
        }
        else if (!data.getWaypoints().isEmpty()) {
            WaypointAI ai = new WaypointAI(entity, data.getWaypoints(), level);
            ai.setLoop(data.isWaypointsLoop());
            npcAIs.put(entity, ai);
        }

        // Register default animation if specified
        if (data.getDefaultAnimation() != null && !data.getDefaultAnimation().isEmpty()) {
            // Schedule default animation registration and playback after a short delay to ensure entity is fully spawned
            level.getServer().execute(() -> {
                if (entity.isAlive() && !entity.isRemoved()) {
                    NpcDefaultAnimationHandler.registerNpcDefaultAnimation(entity, data.getDefaultAnimation());
                    // Play default animation immediately
                    NpcAnimationHandler.sendAnimationToNearbyPlayers(entity, data.getDefaultAnimation(), 32);
                }
            });
        }

        // Give item to NPC on spawn (e.g., merchant with locator)
        if (entity instanceof LivingEntity livingEntity) {
            // Schedule item equip after a short delay to ensure entity is fully spawned
            level.getServer().execute(() -> {
                if (livingEntity.isAlive() && !livingEntity.isRemoved()) {
                    // Give locator to merchant in main hand
                    if (npcId.equals("merchant")) {
                        try {
                            net.minecraft.resources.ResourceLocation itemLocation = net.minecraft.resources.ResourceLocation.parse("marallyzen:locator");
                            net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(itemLocation).ifPresentOrElse(
                                    item -> {
                                        net.minecraft.world.item.ItemStack itemStack = new net.minecraft.world.item.ItemStack(item);
                                        
                                        // Use setItemSlot for all LivingEntity types (including ServerPlayer)
                                        livingEntity.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, itemStack);
                                        
                                        // For ServerPlayer, send equipment update packet to nearby players
                                        if (livingEntity instanceof ServerPlayer fakePlayer) {
                                            java.util.List<com.mojang.datafixers.util.Pair<net.minecraft.world.entity.EquipmentSlot, net.minecraft.world.item.ItemStack>> equipment = new java.util.ArrayList<>();
                                            equipment.add(com.mojang.datafixers.util.Pair.of(net.minecraft.world.entity.EquipmentSlot.MAINHAND, itemStack));
                                            net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket packet = 
                                                    new net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket(fakePlayer.getId(), equipment);
                                            
                                            // Send to all nearby players
                                            for (net.minecraft.server.level.ServerPlayer nearbyPlayer : level.players()) {
                                                if (nearbyPlayer != fakePlayer && nearbyPlayer.distanceToSqr(fakePlayer) < 64 * 64) {
                                                    nearbyPlayer.connection.send(packet);
                                                }
                                            }
                                            Marallyzen.LOGGER.info("NpcRegistry: Equipped locator to merchant NPC (ServerPlayer) in main hand and synced to nearby players");
                                        } else {
                                            Marallyzen.LOGGER.info("NpcRegistry: Equipped locator to merchant NPC (LivingEntity) in main hand");
                                        }
                                    },
                                    () -> Marallyzen.LOGGER.warn("NpcRegistry: Item 'marallyzen:locator' not found in registry")
                            );
                        } catch (Exception e) {
                            Marallyzen.LOGGER.error("NpcRegistry: Failed to equip locator to merchant NPC", e);
                        }
                    }
                }
            });
        }

        return entity;
    }


    public Map<String, NpcStateStore.NpcState> captureNpcStates() {
        Map<String, NpcStateStore.NpcState> states = new HashMap<>();
        for (Map.Entry<String, Entity> entry : spawnedNpcs.entrySet()) {
            Entity entity = entry.getValue();
            if (entity == null || entity.isRemoved()) {
                continue;
            }
            states.put(entry.getKey(), new NpcStateStore.NpcState(
                    entity.level().dimension(),
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    entity.getYRot(),
                    entity.getXRot()
            ));
        }
        return states;
    }

    public int spawnConfiguredNpcs(ServerLevel level, Map<String, NpcStateStore.NpcState> states) {
        if (level == null) {
            return 0;
        }
        Map<String, Entity> existingEntities = scanExistingNpcEntities(level);
        Set<String> disabled = NpcStateStore.loadDisabled();
        int spawned = 0;
        for (NpcData data : npcDataMap.values()) {
            if (disabled.contains(data.getId())) {
                continue;
            }
            Entity existing = existingEntities.get(data.getId());
            if (existing != null) {
                registerExistingNpcEntity(existing);
                continue;
            }
            if (getNpc(data.getId()) != null) {
                continue;
            }
            NpcStateStore.NpcState state = states != null ? states.get(data.getId()) : null;
            if (state != null) {
                ServerLevel stateLevel = level.getServer().getLevel(state.dimension());
                if (stateLevel != null) {
                    try {
                        BlockPos statePos = BlockPos.containing(state.x(), state.y(), state.z());
                        if (!stateLevel.hasChunkAt(statePos)) {
                            continue;
                        }
                        spawnNpc(data.getId(), stateLevel, BlockPos.containing(state.x(), state.y(), state.z()), state.yaw(), state.pitch());
                        spawned++;
                        continue;
                    } catch (Exception e) {
                        Marallyzen.LOGGER.warn("Failed to spawn NPC {} from state, falling back to config.", data.getId(), e);
                    }
                }
            }
            if (data.getSpawnPos() == null) {
                continue;
            }
            try {
                if (!level.hasChunkAt(data.getSpawnPos())) {
                    continue;
                }
                spawnNpc(data.getId(), level, data.getSpawnPos());
                spawned++;
            } catch (Exception e) {
                Marallyzen.LOGGER.error("Failed to auto-spawn NPC: {}", data.getId(), e);
            }
        }
        return spawned;
    }

    public void refreshNpcAis() {
        npcAIs.clear();
        for (Map.Entry<String, Entity> entry : spawnedNpcs.entrySet()) {
            String npcId = entry.getKey();
            Entity entity = entry.getValue();
            if (entity == null || entity.isRemoved()) {
                continue;
            }
            NpcData data = npcDataMap.get(npcId);
            if (data == null) {
                continue;
            }
            if (Boolean.TRUE.equals(data.getValk())) {
                String pattern = normalizeValkPattern(data.getValkPattern());
                List<NpcData.Waypoint> waypoints = buildValkWaypoints(
                        entity.level(),
                        data.getSpawnPos() != null ? data.getSpawnPos() : entity.blockPosition(),
                        data.getValkRadius() != null ? data.getValkRadius() : VALK_DEFAULT_RADIUS,
                        pattern,
                        VALK_DEFAULT_POINTS,
                        VALK_DEFAULT_WAIT_TICKS,
                        VALK_DEFAULT_SPEED
                );
                data.setWaypoints(waypoints);
                data.setWaypointsLoop(true);
                WaypointAI ai = new WaypointAI(entity, waypoints, entity.level());
                ai.setLoop(true);
                npcAIs.put(entity, ai);
            } else if (!data.getWaypoints().isEmpty()) {
                WaypointAI ai = new WaypointAI(entity, data.getWaypoints(), entity.level());
                ai.setLoop(data.isWaypointsLoop());
                npcAIs.put(entity, ai);
            }
        }
    }

    public int despawnMissingNpcs() {
        int removed = 0;
        for (String npcId : new ArrayList<>(spawnedNpcs.keySet())) {
            if (!npcDataMap.containsKey(npcId)) {
                despawnNpc(npcId);
                removed++;
            }
        }
        return removed;
    }

    /**
     * Creates a fallback entity when GeckoLib is not available.
     */
    private Entity createFallbackEntity(ServerLevel level, NpcData data) {
        EntityType<?> entityType = data.getEntityType();
        if (entityType == null) {
            // Default to ArmorStand if no type specified
            entityType = EntityType.ARMOR_STAND;
        }

        // Special handling for player entities - create fake player with skin support
        if (entityType == EntityType.PLAYER) {
            return createFakePlayer(level, data);
        }

        Entity entity = entityType.create(level);
        if (entity == null) {
            throw new IllegalStateException("Failed to create entity for NPC: " + data.getId());
        }

        return entity;
    }

    private boolean shouldUseGeckoNpc(NpcData data) {
        return data != null && data.getGeckolibModel() != null;
    }

    private Entity createGeckoEntity(ServerLevel level, NpcData data) {
        GeckoNpcEntity entity = Marallyzen.GECKO_NPC.get().create(level);
        if (entity == null) {
            return null;
        }
        return entity;
    }

    private static final Gson gson = new Gson();
    
    /**
     * Extracts plain text from JSON Component string or returns the string as-is.
     * Also ensures the name is within Minecraft's 16 character limit for player names.
     */
    private String extractPlainName(String nameOrJson, String fallbackId) {
        if (nameOrJson == null || nameOrJson.isEmpty()) {
            return fallbackId != null ? fallbackId.substring(0, Math.min(fallbackId.length(), 16)) : "NPC";
        }
        
        // Try to parse as JSON
        String trimmed = nameOrJson.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            try {
                // Try to extract "text" field from JSON
                JsonElement element = gson.fromJson(trimmed, JsonElement.class);
                
                if (element != null && element.isJsonObject()) {
                    JsonObject obj = element.getAsJsonObject();
                    if (obj.has("text") && obj.get("text").isJsonPrimitive()) {
                        String text = obj.get("text").getAsString();
                        // Limit to 16 characters for GameProfile
                        return text.length() > 16 ? text.substring(0, 16) : text;
                    }
                } else if (element != null && element.isJsonArray()) {
                    // For arrays, try to get text from first element
                    JsonArray arr = element.getAsJsonArray();
                    if (arr.size() > 0 && arr.get(0).isJsonObject()) {
                        JsonObject first = arr.get(0).getAsJsonObject();
                        if (first.has("text") && first.get("text").isJsonPrimitive()) {
                            String text = first.get("text").getAsString();
                            return text.length() > 16 ? text.substring(0, 16) : text;
                        }
                    }
                }
            } catch (Exception e) {
                // If JSON parsing fails, fall through to plain text handling
            }
        }
        
        // Not JSON or parsing failed - use as plain text, but limit to 16 characters
        return nameOrJson.length() > 16 ? nameOrJson.substring(0, 16) : nameOrJson;
    }
    
    /**
     * Creates a fake player entity with optional skin.
     */
    private Entity createFakePlayer(ServerLevel level, NpcData data) {
        // Extract plain name from JSON (if it's JSON) and ensure it's <= 16 characters
        // GameProfile names must be <= 16 characters
        String plainName = extractPlainName(data.getName(), data.getId());
        String texture = data.getSkinTexture();
        String signature = data.getSkinSignature();
        
        // Get skin model (default or slim)
        String model = data.getSkinModel() != null ? data.getSkinModel() : "default";
        
        // Create GameProfile with skin
        GameProfile gameProfile = FakePlayerEntity.createGameProfileWithSkin(plainName, texture, signature, model);
        
        // Create fake player entity
        MinecraftServer server = level.getServer();
        FakePlayerEntity fakePlayer = new FakePlayerEntity(server, level, gameProfile);
        fakePlayer.addTag("marallyzen_npc");
        
        // Apply name tag visibility for player-type NPCs
        boolean showNameTag = data.getShowNameTag() != null ? data.getShowNameTag() : true;
        fakePlayer.setCustomName(net.minecraft.network.chat.Component.literal(plainName));
        fakePlayer.setCustomNameVisible(showNameTag);
        applyNameTagVisibility(level, fakePlayer, showNameTag);
        applyNpcTabTeam(level, fakePlayer);
        
        return fakePlayer;
    }

    private void applyNpcTabTeam(ServerLevel level, ServerPlayer fakePlayer) {
        if (level == null || fakePlayer == null) {
            return;
        }
        Scoreboard scoreboard = level.getScoreboard();
        String teamName = "marallyzen_npc_tab";
        PlayerTeam team = scoreboard.getPlayerTeam(teamName);
        if (team == null) {
            team = scoreboard.addPlayerTeam(teamName);
        }
        String scoreboardEntry = fakePlayer.getScoreboardName();
        PlayerTeam current = scoreboard.getPlayersTeam(scoreboardEntry);
        if (current != team) {
            scoreboard.addPlayerToTeam(scoreboardEntry, team);
        }
        String profileName = fakePlayer.getGameProfile().getName();
        if (profileName != null && !profileName.isEmpty() && !profileName.equals(scoreboardEntry)) {
            PlayerTeam currentByName = scoreboard.getPlayersTeam(profileName);
            if (currentByName != team) {
                scoreboard.addPlayerToTeam(profileName, team);
            }
        }
        String uuidEntry = fakePlayer.getUUID().toString();
        PlayerTeam currentByUuid = scoreboard.getPlayersTeam(uuidEntry);
        if (currentByUuid != team) {
            scoreboard.addPlayerToTeam(uuidEntry, team);
        }
    }

    /**
     * Applies name tag visibility using scoreboard teams to fully hide player nameplates when needed.
     */
    private void applyNameTagVisibility(ServerLevel level, Entity entity, boolean showNameTag) {
        if (level == null || entity == null) {
            return;
        }
        Scoreboard scoreboard = level.getScoreboard();
        String teamName = "marallyzen_hidden_names";
        PlayerTeam team = scoreboard.getPlayerTeam(teamName);
        if (team == null) {
            team = scoreboard.addPlayerTeam(teamName);
        }
        team.setNameTagVisibility(Team.Visibility.NEVER);
        String entry = entity.getScoreboardName();
        if (!showNameTag) {
            PlayerTeam current = scoreboard.getPlayersTeam(entry);
            if (current != team) {
                scoreboard.addPlayerToTeam(entry, team);
            }
        } else {
            PlayerTeam current = scoreboard.getPlayersTeam(entry);
            if (current == team) {
                scoreboard.removePlayerFromTeam(entry);
            }
        }
    }

    /**
     * Checks if GeckoLib is available (when dependency issues are resolved).
     */
    private boolean isGeckoLibAvailable() {
        try {
            Class.forName("software.bernie.geckolib.GeckoLib");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Despawns an NPC entity.
     */
    public void despawnNpc(String npcId) {
        Entity entity = spawnedNpcs.remove(npcId);
        if (entity != null) {
            entityToNpcId.remove(entity);
            npcAIs.remove(entity); // Remove AI
            entity.remove(Entity.RemovalReason.DISCARDED);
        }
        // Clean up proximity tracking
        // Note: Custom HUD proximity overlay is cleared automatically when player leaves range
    }

    /**
     * Despawns all currently spawned NPC entities.
     */
    public void despawnAllNpcs() {
        for (String npcId : new ArrayList<>(spawnedNpcs.keySet())) {
            despawnNpc(npcId);
        }
    }

    /**
     * Gets the NPC ID for an entity, if it's a registered NPC.
     */
    public String getNpcId(Entity entity) {
        return entityToNpcId.get(entity);
    }

    /**
     * Gets NPC entity by UUID if registered.
     */
    public Entity getNpcByUuid(java.util.UUID uuid) {
        if (uuid == null) return null;
        for (Entity entity : spawnedNpcs.values()) {
            if (uuid.equals(entity.getUUID())) {
                return entity;
            }
        }
        return null;
    }

    /**
     * Checks if an entity is a registered NPC.
     */
    public boolean isNpc(Entity entity) {
        return entityToNpcId.containsKey(entity);
    }

    /**
     * Gets an NPC entity by its ID.
     */
    public Entity getNpc(String npcId) {
        return spawnedNpcs.get(npcId);
    }

    /**
     * Gets all registered NPC data.
     */
    public Collection<NpcData> getAllNpcData() {
        return npcDataMap.values();
    }

    /**
     * Spawns all NPCs that have a configured spawn position.
     * Returns the count of successfully spawned NPCs.
     */
    public int spawnConfiguredNpcs(ServerLevel level) {
        if (level == null) {
            return 0;
        }
        Map<String, Entity> existingEntities = scanExistingNpcEntities(level);
        Set<String> disabled = NpcStateStore.loadDisabled();
        int spawned = 0;
        for (NpcData data : npcDataMap.values()) {
            if (disabled.contains(data.getId())) {
                continue;
            }
            if (data.getSpawnPos() == null) {
                continue;
            }
            Entity existing = existingEntities.get(data.getId());
            if (existing != null) {
                registerExistingNpcEntity(existing);
                continue;
            }
            if (getNpc(data.getId()) != null) {
                continue;
            }
            try {
                if (!level.hasChunkAt(data.getSpawnPos())) {
                    continue;
                }
                spawnNpc(data.getId(), level, data.getSpawnPos());
                spawned++;
            } catch (Exception e) {
                Marallyzen.LOGGER.error("Failed to auto-spawn NPC: {}", data.getId(), e);
            }
        }
        return spawned;
    }

    /**
     * Gets all spawned NPC entities.
     */
    public Collection<Entity> getSpawnedNpcs() {
        return spawnedNpcs.values();
    }

    public Entity spawnNpcFromState(String npcId, ServerLevel level, NpcState state) {
        if (npcId == null || level == null || state == null) {
            return null;
        }
        BlockPos pos = state.pos();
        Entity entity = spawnNpc(npcId, level, pos, state.yaw(), 0.0f);
        if (entity instanceof NpcEntity npcEntity && state.appearanceId() != null) {
            npcEntity.setAppearanceId(state.appearanceId());
        }
        return entity;
    }

    public void registerExistingNpcEntity(Entity entity) {
        if (entity == null) {
            return;
        }
        String npcId = null;
        if (entity instanceof NpcEntity npcEntity) {
            npcId = npcEntity.getNpcId();
        } else if (entity instanceof GeckoNpcEntity geckoEntity) {
            npcId = geckoEntity.getNpcId();
        }
        if (npcId == null || npcId.isEmpty()) {
            return;
        }
        spawnedNpcs.putIfAbsent(npcId, entity);
        entityToNpcId.putIfAbsent(entity, npcId);

        NpcData data = npcDataMap.get(npcId);
        if (data == null) {
            return;
        }
        if (entity instanceof LivingEntity livingEntity) {
            boolean shouldBeInvulnerable = data.getInvulnerable() != null ? data.getInvulnerable() : true;
            livingEntity.setInvulnerable(shouldBeInvulnerable);

            String npcName = data.getName() != null ? data.getName() : data.getId();
            livingEntity.setCustomName(net.minecraft.network.chat.Component.literal(npcName));
            boolean showNameTag = data.getShowNameTag() != null ? data.getShowNameTag() : true;
            livingEntity.setCustomNameVisible(showNameTag);
            applyNameTagVisibility((ServerLevel) entity.level(), livingEntity, showNameTag);
        }
    }

    public void sendPlayerInfoTo(ServerPlayer player) {
        if (player == null || player.connection == null) {
            return;
        }
        List<ServerPlayer> fakePlayers = new ArrayList<>();
        for (Entity entity : spawnedNpcs.values()) {
            if (entity instanceof ServerPlayer serverPlayer) {
                fakePlayers.add(serverPlayer);
            }
        }
        if (fakePlayers.isEmpty()) {
            return;
        }
        ClientboundPlayerInfoUpdatePacket packet =
                ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(fakePlayers);
        player.connection.send(packet);
        player.getServer().execute(() -> {
            for (ServerPlayer fakePlayer : fakePlayers) {
                forceRespawnForPlayer(player, fakePlayer);
                // sendRemoveFromTab(player, fakePlayer);
            }
        });
    }

    private static final java.lang.reflect.Field TRACKED_ENTITIES_FIELD = findTrackedEntitiesField();
    private static final java.lang.reflect.Field TRACKED_ENTITY_FIELD = findTrackedEntityField();
    private static boolean trackedEntityLogOnce = false;
    private static boolean trackedEntityMapLogOnce = false;

    private static java.lang.reflect.Field findTrackedEntitiesField() {
        try {
            java.lang.reflect.Field field = ChunkMap.class.getDeclaredField("entityMap");
            field.setAccessible(true);
            return field;
        } catch (Exception ignored) {
        }
        for (java.lang.reflect.Field field : ChunkMap.class.getDeclaredFields()) {
            if (it.unimi.dsi.fastutil.ints.Int2ObjectMap.class.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                return field;
            }
        }
        return null;
    }

    private static java.lang.reflect.Field findTrackedEntityField() {
        try {
            Class<?> trackedClass = null;
            for (Class<?> nested : ChunkMap.class.getDeclaredClasses()) {
                if ("TrackedEntity".equals(nested.getSimpleName())) {
                    trackedClass = nested;
                    break;
                }
            }
            if (trackedClass == null) {
                return null;
            }
            for (java.lang.reflect.Field field : trackedClass.getDeclaredFields()) {
                if (ServerEntity.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    return field;
                }
            }
        } catch (Exception e) {
            // Return null and warn later.
        }
        return null;
    }

    private static void forceRespawnForPlayer(ServerPlayer viewer, Entity entity) {
        if (viewer == null || entity == null || !(entity.level() instanceof ServerLevel level)) {
            return;
        }
        if (TRACKED_ENTITY_FIELD == null) {
            if (!trackedEntityLogOnce) {
                trackedEntityLogOnce = true;
                Marallyzen.LOGGER.warn("NpcRegistry: Unable to locate tracked entity field; player NPCs may not respawn for joiners.");
            }
            return;
        }
        ChunkMap tracker = level.getChunkSource().chunkMap;
        Object tracked = null;
        if (TRACKED_ENTITIES_FIELD != null) {
            try {
                Object mapObj = TRACKED_ENTITIES_FIELD.get(tracker);
                if (mapObj instanceof it.unimi.dsi.fastutil.ints.Int2ObjectMap<?> map) {
                    tracked = map.get(entity.getId());
                }
            } catch (Exception e) {
                if (!trackedEntityMapLogOnce) {
                    trackedEntityMapLogOnce = true;
                    Marallyzen.LOGGER.warn("NpcRegistry: Failed to access tracked entity map; player NPCs may not respawn for joiners.", e);
                }
            }
        } else if (!trackedEntityMapLogOnce) {
            trackedEntityMapLogOnce = true;
            Marallyzen.LOGGER.warn("NpcRegistry: Unable to locate tracked entity map; player NPCs may not respawn for joiners.");
        }
        if (tracked == null) {
            return;
        }
        try {
            ServerEntity serverEntity = (ServerEntity) TRACKED_ENTITY_FIELD.get(tracked);
            if (serverEntity == null) {
                return;
            }
            serverEntity.removePairing(viewer);
            serverEntity.addPairing(viewer);
        } catch (Exception e) {
            if (!trackedEntityLogOnce) {
                trackedEntityLogOnce = true;
                Marallyzen.LOGGER.warn("NpcRegistry: Failed to re-pair NPC entity for viewer.", e);
            }
        }
    }

    private void hideFakePlayerFromTab(ServerLevel level, ServerPlayer fakePlayer) {
        if (level == null || fakePlayer == null || level.getServer() == null) {
            return;
        }
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            sendRemoveFromTab(player, fakePlayer);
        }
    }

    private void sendRemoveFromTab(ServerPlayer viewer, ServerPlayer fakePlayer) {
        if (viewer == null || viewer.connection == null || fakePlayer == null) {
            return;
        }
        ClientboundPlayerInfoRemovePacket removePacket =
                new ClientboundPlayerInfoRemovePacket(List.of(fakePlayer.getUUID()));
        viewer.connection.send(removePacket);
    }

    /**
     * Tick all NPC AIs.
     */
    public void tickAIs() {
        for (Map.Entry<Entity, WaypointAI> entry : npcAIs.entrySet()) {
            Entity entity = entry.getKey();
            WaypointAI ai = entry.getValue();
            if (entity == null || entity.isRemoved()) {
                continue;
            }
            if (entity.level() != null && !entity.level().hasChunkAt(entity.blockPosition())) {
                continue;
            }
            try {
                ai.tick();
            } catch (Exception e) {
                int count = npcAiErrors.getOrDefault(entity, 0) + 1;
                npcAiErrors.put(entity, count);
                npcAIs.remove(entity);
                if (count == 1) {
                    String npcId = getNpcId(entity);
                    Marallyzen.LOGGER.warn("NpcRegistry: AI error for NPC {} ({}). Disabling AI for this NPC.", npcId, entity.getUUID(), e);
                }
            }
        }
    }

    /**
     * Get AI for a specific NPC entity.
     */
    public WaypointAI getAI(Entity entity) {
        return npcAIs.get(entity);
    }

    /**
     * Get AI for a specific NPC by ID.
     */
    public WaypointAI getAI(String npcId) {
        Entity entity = spawnedNpcs.get(npcId);
        return entity != null ? npcAIs.get(entity) : null;
    }

    /**
     * Set whether waypoints should loop for an NPC.
     */
    public void setWaypointLoop(String npcId, boolean loop) {
        WaypointAI ai = getAI(npcId);
        if (ai != null) {
            ai.setLoop(loop);
        }
    }

    /**
     * Replace NPC waypoints at runtime and (re)start the waypoint AI.
     */
    public void setNpcWaypoints(String npcId, List<NpcData.Waypoint> waypoints, boolean loop) {
        NpcData data = npcDataMap.get(npcId);
        if (data != null) {
            data.setWaypoints(waypoints);
            data.setWaypointsLoop(loop);
        }

        Entity entity = spawnedNpcs.get(npcId);
        if (entity == null) {
            return;
        }

        WaypointAI ai = new WaypointAI(entity, waypoints, entity.level());
        ai.setLoop(loop);
        npcAIs.put(entity, ai);
    }

    private static List<NpcData.Waypoint> buildValkWaypoints(Level level, BlockPos center, int radius, String pattern, int points, int waitTicks, double speed) {
        if (VALK_PATTERN_SQUARE.equals(pattern)) {
            return buildSquareWaypoints(level, center, radius, waitTicks, speed);
        }
        if (VALK_PATTERN_CIRCLE.equals(pattern)) {
            return buildCircleWaypoints(level, center, radius, points, waitTicks, speed);
        }
        List<NpcData.Waypoint> waypoints = new ArrayList<>();
        if (radius < 1 || points < 1) {
            return waypoints;
        }
        Random random = new Random();
        for (int i = 0; i < points; i++) {
            int dx = random.nextInt(radius * 2 + 1) - radius;
            int dz = random.nextInt(radius * 2 + 1) - radius;
            BlockPos target = center.offset(dx, 0, dz);
            waypoints.add(new NpcData.Waypoint(resolveGroundWaypoint(level, target), waitTicks, speed));
        }
        return waypoints;
    }

    private static List<NpcData.Waypoint> buildSquareWaypoints(Level level, BlockPos center, int radius, int waitTicks, double speed) {
        List<NpcData.Waypoint> waypoints = new ArrayList<>();
        int r = Math.max(1, radius);
        waypoints.add(new NpcData.Waypoint(resolveGroundWaypoint(level, center.offset(r, 0, r)), waitTicks, speed));
        waypoints.add(new NpcData.Waypoint(resolveGroundWaypoint(level, center.offset(r, 0, -r)), waitTicks, speed));
        waypoints.add(new NpcData.Waypoint(resolveGroundWaypoint(level, center.offset(-r, 0, -r)), waitTicks, speed));
        waypoints.add(new NpcData.Waypoint(resolveGroundWaypoint(level, center.offset(-r, 0, r)), waitTicks, speed));
        return waypoints;
    }

    private static List<NpcData.Waypoint> buildCircleWaypoints(Level level, BlockPos center, int radius, int points, int waitTicks, double speed) {
        List<NpcData.Waypoint> waypoints = new ArrayList<>();
        int r = Math.max(1, radius);
        int count = Math.max(8, points);
        BlockPos last = null;
        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2.0 * i) / count;
            int x = center.getX() + (int) Math.round(Math.cos(angle) * r);
            int z = center.getZ() + (int) Math.round(Math.sin(angle) * r);
            BlockPos pos = resolveGroundWaypoint(level, new BlockPos(x, center.getY(), z));
            if (last != null && last.equals(pos)) {
                continue;
            }
            waypoints.add(new NpcData.Waypoint(pos, waitTicks, speed));
            last = pos;
        }
        return waypoints;
    }

    private static BlockPos resolveGroundWaypoint(Level level, BlockPos target) {
        if (level == null) {
            return target;
        }
        int baseY = target.getY();
        BlockPos.MutableBlockPos scan = new BlockPos.MutableBlockPos(target.getX(), baseY + 2, target.getZ());
        for (int y = baseY + 2; y >= baseY - 8; y--) {
            scan.setY(y);
            if (!level.getBlockState(scan).getCollisionShape(level, scan).isEmpty()) {
                return scan.immutable();
            }
        }
        return target;
    }

    private static double computeSpawnY(Level level, BlockPos pos) {
        if (level == null || pos == null) {
            return pos != null ? pos.getY() : 0.0;
        }
        var shape = level.getBlockState(pos).getCollisionShape(level, pos);
        if (!shape.isEmpty()) {
            return pos.getY() + shape.max(Direction.Axis.Y);
        }
        BlockPos below = pos.below();
        var belowShape = level.getBlockState(below).getCollisionShape(level, below);
        if (!belowShape.isEmpty()) {
            return below.getY() + belowShape.max(Direction.Axis.Y);
        }
        return pos.getY();
    }

    private static String normalizeValkPattern(String pattern) {
        if (pattern == null) {
            return VALK_PATTERN_RANDOM;
        }
        String lower = pattern.toLowerCase(Locale.ROOT);
        if (VALK_PATTERN_CIRCLE.equals(lower) || VALK_PATTERN_SQUARE.equals(lower) || VALK_PATTERN_RANDOM.equals(lower)) {
            return lower;
        }
        return VALK_PATTERN_RANDOM;
    }

    private Map<String, Entity> scanExistingNpcEntities(ServerLevel level) {
        Map<String, Entity> result = new HashMap<>();
        AABB box = new AABB(
            -3.0E7, level.getMinBuildHeight(), -3.0E7,
            3.0E7, level.getMaxBuildHeight(), 3.0E7
        );
        for (GeckoNpcEntity entity : level.getEntitiesOfClass(GeckoNpcEntity.class, box)) {
            if (entity == null || entity.isRemoved()) {
                continue;
            }
            String npcId = entity.getNpcId();
            if (npcId == null || npcId.isEmpty()) {
                String resolved = resolveNpcIdFromName(entity);
                if (resolved != null) {
                    entity.setNpcId(resolved);
                    npcId = resolved;
                }
            }
            if (npcId != null && !npcId.isEmpty() && !result.containsKey(npcId)) {
                result.put(npcId, entity);
            }
        }
        for (NpcEntity entity : level.getEntitiesOfClass(NpcEntity.class, box)) {
            if (entity == null || entity.isRemoved()) {
                continue;
            }
            String npcId = entity.getNpcId();
            if (npcId == null || npcId.isEmpty()) {
                String resolved = resolveNpcIdFromName(entity);
                if (resolved != null) {
                    entity.setNpcId(resolved);
                    npcId = resolved;
                }
            }
            if (npcId != null && !npcId.isEmpty() && !result.containsKey(npcId)) {
                result.put(npcId, entity);
            }
        }
        return result;
    }

    private String resolveNpcIdFromName(Entity entity) {
        if (entity == null) {
            return null;
        }
        net.minecraft.network.chat.Component customName = entity.getCustomName();
        String name = customName != null ? customName.getString() : null;
        if (name == null || name.isBlank()) {
            return null;
        }
        String match = null;
        for (NpcData data : npcDataMap.values()) {
            if (data == null) {
                continue;
            }
            String dataName = data.getName();
            String dataId = data.getId();
            boolean nameMatches = dataName != null && !dataName.isBlank() && name.equalsIgnoreCase(dataName);
            boolean idMatches = dataId != null && !dataId.isBlank() && name.equalsIgnoreCase(dataId);
            if (nameMatches || idMatches) {
                if (match != null && !match.equals(dataId)) {
                    return null;
                }
                match = dataId;
            }
        }
        return match;
    }

    /**
     * Force an NPC to move to a specific waypoint index.
     */
    public void moveNpcToWaypoint(String npcId, int waypointIndex) {
        WaypointAI ai = getAI(npcId);
        if (ai != null) {
            ai.moveToWaypoint(waypointIndex);
        }
    }

    /**
     * Check if an NPC has active waypoints.
     */
    public boolean hasWaypoints(String npcId) {
        WaypointAI ai = getAI(npcId);
        return ai != null;
    }

    /**
     * Clears all registered NPC data (but keeps spawned entities).
     * Used for reloading NPC configurations.
     */
    public void clearNpcData() {
        npcDataMap.clear();
    }

    public void resetRuntimeState() {
        spawnedNpcs.clear();
        entityToNpcId.clear();
        npcAIs.clear();
        npcAiErrors.clear();
    }

    private static NpcData cloneNpcData(String newId, NpcData original) {
        NpcData clone = new NpcData(newId);
        clone.setName(original.getName());
        clone.setEntityType(original.getEntityType());
        clone.setSpawnPos(original.getSpawnPos());
        clone.setGeckolibModel(original.getGeckolibModel());
        clone.setGeckolibAnimation(original.getGeckolibAnimation());
        clone.setGeckolibTexture(original.getGeckolibTexture());
        clone.setGeckolibExpression(original.getGeckolibExpression());
        clone.setGeckolibTalkExpression(original.getGeckolibTalkExpression());
        clone.setSkinTexture(original.getSkinTexture());
        clone.setSkinSignature(original.getSkinSignature());
        clone.setSkinModel(original.getSkinModel());
        clone.setDialogScript(original.getDialogScript());
        clone.setCutscene(original.getCutscene());
        clone.setDefaultAnimation(original.getDefaultAnimation());
        clone.setWaypoints(new ArrayList<>(original.getWaypoints()));
        clone.setWaypointsLoop(original.isWaypointsLoop());
        clone.setMaxHealth(original.getMaxHealth());
        clone.setHealth(original.getHealth());
        clone.setInvulnerable(original.getInvulnerable());
        clone.setProximityText(original.getProximityText());
        clone.setProximityRange(original.getProximityRange());
        clone.setShowNameTag(original.getShowNameTag());
        clone.setShowProximityTextInActionBar(original.getShowProximityTextInActionBar());
        clone.setShowProximityTextInChat(original.getShowProximityTextInChat());
        clone.setValk(original.getValk());
        clone.setValkRadius(original.getValkRadius());
        clone.setValkPattern(original.getValkPattern());
        clone.setLookAtPlayers(original.getLookAtPlayers());
        clone.setAiSettings(cloneAiSettings(original.getAiSettings()));
        clone.setWildfireSettings(cloneWildfireSettings(original.getWildfireSettings()));
        clone.setMetadata(new HashMap<>(original.getMetadata()));
        return clone;
    }

    private static NpcData.AiSettings cloneAiSettings(NpcData.AiSettings original) {
        if (original == null) {
            return null;
        }
        NpcData.AiSettings copy = new NpcData.AiSettings();
        copy.setEnabled(original.isEnabled());
        copy.setSystemPrompt(original.getSystemPrompt());
        copy.setModel(original.getModel());
        copy.setTemperature(original.getTemperature());
        copy.setMaxTokens(original.getMaxTokens());
        copy.setOptionCount(original.getOptionCount());
        copy.setMemoryTurns(original.getMemoryTurns());
        return copy;
    }

    private static NpcData.WildfireSettings cloneWildfireSettings(NpcData.WildfireSettings original) {
        if (original == null) {
            return null;
        }
        NpcData.WildfireSettings copy = new NpcData.WildfireSettings();
        copy.setEnabled(original.getEnabled());
        copy.setGender(original.getGender());
        copy.setBustSize(original.getBustSize());
        copy.setBreastPhysics(original.getBreastPhysics());
        copy.setBounceMultiplier(original.getBounceMultiplier());
        copy.setFloppiness(original.getFloppiness());
        copy.setShowBreastsInArmor(original.getShowBreastsInArmor());
        copy.setCleavage(original.getCleavage());
        copy.setUniboob(original.getUniboob());
        copy.setXOffset(original.getXOffset());
        copy.setYOffset(original.getYOffset());
        copy.setZOffset(original.getZOffset());
        return copy;
    }
}
