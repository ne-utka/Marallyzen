package neutka.marallys.marallyzen.quest;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.loading.FMLPaths;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.network.NarratePacket;
import neutka.marallys.marallyzen.network.NetworkHelper;
import neutka.marallys.marallyzen.quest.QuestCategoryColors;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class QuestManager {
    private static final QuestManager INSTANCE = new QuestManager();

    private final QuestConditionEvaluator conditionEvaluator = new QuestConditionEvaluator();
    private final QuestProgressTracker progressTracker = new QuestProgressTracker();
    private final QuestRewardHandler rewardHandler = new QuestRewardHandler();
    private final QuestTriggerSystem triggerSystem = new QuestTriggerSystem(conditionEvaluator, progressTracker, rewardHandler);
    private final QuestZoneIndex zoneIndex = new QuestZoneIndex();

    private final Map<String, QuestDefinition> definitions = new HashMap<>();
    private final Map<String, QuestZoneDefinition> zones = new HashMap<>();
    private final Map<UUID, QuestPlayerData> playerData = new HashMap<>();
    private final Map<UUID, QuestPlayerRuntime> runtime = new HashMap<>();
    private final Set<String> zoneTriggerIds = new HashSet<>();
    private final java.util.List<ScheduledQuestEvent> scheduledEvents = new java.util.ArrayList<>();
    private int timeCheckTicks;
    private static final int TIME_CHECK_INTERVAL = 20;
    private static final double SIDE_QUEST_NOTIFY_RADIUS = 32.0;

    private MinecraftServer server;
    private boolean initialized;

    public static QuestManager getInstance() {
        return INSTANCE;
    }

    public void initialize(MinecraftServer server) {
        this.server = server;
        loadDefinitions();
        loadZones();
        playerData.clear();
        playerData.putAll(QuestStateStore.load());
        ensureQuestAdvancements(server);
        initialized = true;
        Marallyzen.LOGGER.info("QuestManager initialized: {} quest(s), {} zone(s)", definitions.size(), zones.size());
    }

    public void reload(MinecraftServer server) {
        this.server = server;
        loadDefinitions();
        loadZones();
        ensureQuestAdvancements(server);
        if (server != null) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                QuestPlayerData data = getOrCreatePlayerData(player.getUUID());
                QuestUIBridge.sendFullSync(player, definitions, data, zones);
            }
        }
        Marallyzen.LOGGER.info("QuestManager reloaded: {} quest(s), {} zone(s)", definitions.size(), zones.size());
    }

    public void shutdown() {
        QuestStateStore.save(playerData);
        initialized = false;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void onPlayerLogin(ServerPlayer player) {
        if (!initialized || player == null) {
            return;
        }
        QuestPlayerData data = getOrCreatePlayerData(player.getUUID());
        boolean firstLogin = !data.hasJoinedBefore();
        data.setHasJoinedBefore(true);
        QuestUIBridge.sendFullSync(player, definitions, data, zones);
        if (firstLogin) {
            fireEvent(player, new QuestEvent("first_login", Map.of(), player.blockPosition()));
        }
        fireEvent(player, new QuestEvent("player_login", Map.of(), player.blockPosition()));
    }

    public void onPlayerLogout(ServerPlayer player) {
        if (!initialized || player == null) {
            return;
        }
        runtime.remove(player.getUUID());
        QuestStateStore.save(playerData);
    }

    public void onNpcDialog(ServerPlayer player, String npcId) {
        if (npcId == null || npcId.isBlank()) {
            return;
        }
        fireEvent(player, new QuestEvent("npc_dialog", Map.of("npcId", npcId), player.blockPosition()));
    }

    public void onEntityKilled(ServerPlayer player, String entityId) {
        fireEvent(player, new QuestEvent("kill_entity", Map.of("entity", entityId), player.blockPosition()));
    }

    public void onItemUse(ServerPlayer player, String itemId) {
        fireEvent(player, new QuestEvent("item_use", Map.of("item", itemId), player.blockPosition()));
    }

    public void onItemPickup(ServerPlayer player, String itemId) {
        fireEvent(player, new QuestEvent("item_pickup", Map.of("item", itemId), player.blockPosition()));
    }

    public void fireCustomEvent(ServerPlayer player, String eventId, Map<String, String> data) {
        if (eventId == null || eventId.isBlank()) {
            return;
        }
        Map<String, String> payload = new HashMap<>();
        if (data != null) {
            payload.putAll(data);
        }
        payload.put("eventId", eventId);
        fireEvent(player, new QuestEvent("custom", payload, player.blockPosition()));
    }

    public void onServerTick() {
        if (!initialized) {
            return;
        }
        timeCheckTicks++;
        if (timeCheckTicks >= TIME_CHECK_INTERVAL) {
            timeCheckTicks = 0;
            if (server != null) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    updateTimeWeather(player);
                }
            }
        }
        if (scheduledEvents.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        var iterator = scheduledEvents.iterator();
        while (iterator.hasNext()) {
            ScheduledQuestEvent scheduled = iterator.next();
            if (now < scheduled.fireAtMs) {
                continue;
            }
            iterator.remove();
            ServerPlayer player = server != null ? server.getPlayerList().getPlayer(scheduled.playerId) : null;
            if (player != null) {
                fireEvent(player, scheduled.event);
            }
        }
    }

    public void onPlayerMove(ServerPlayer player) {
        if (!initialized || player == null) {
            return;
        }
        QuestPlayerRuntime runtimeData = runtime.computeIfAbsent(player.getUUID(), key -> new QuestPlayerRuntime());
        BlockPos currentPos = player.blockPosition();
        ResourceKey<net.minecraft.world.level.Level> dimension = player.level().dimension();
        if (runtimeData.lastDimension != null && currentPos.equals(runtimeData.lastPos) && dimension.equals(runtimeData.lastDimension)) {
            return;
        }
        if (runtimeData.lastDimension != null && !dimension.equals(runtimeData.lastDimension)) {
            fireEvent(player, new QuestEvent("dimension", Map.of("dimension", dimension.location().toString()), currentPos));
        }
        runtimeData.lastPos = currentPos;
        runtimeData.lastDimension = dimension;

        var biomeKey = player.level().getBiome(currentPos).unwrapKey().map(key -> key.location().toString()).orElse("");
        if (!biomeKey.isBlank() && !biomeKey.equals(runtimeData.lastBiomeId)) {
            runtimeData.lastBiomeId = biomeKey;
            fireEvent(player, new QuestEvent("biome", Map.of("biome", biomeKey), currentPos));
        }

        Set<String> currentZones = new HashSet<>();
        for (QuestZoneDefinition zone : zoneIndex.getZonesAt(currentPos)) {
            if (!dimension.equals(zone.dimension())) {
                continue;
            }
            if (!zoneTriggerIds.contains(zone.id())) {
                continue;
            }
            if (zone.contains(currentPos)) {
                currentZones.add(zone.id());
            }
        }

        Set<String> entered = new HashSet<>(currentZones);
        entered.removeAll(runtimeData.activeZones);
        runtimeData.activeZones = currentZones;
        for (String zoneId : entered) {
            Marallyzen.LOGGER.info("QuestManager: zone_enter {} at {} for {}", zoneId, currentPos, player.getGameProfile().getName());
            fireEvent(player, new QuestEvent("zone_enter", Map.of("zoneId", zoneId), currentPos));
        }
    }

    public QuestInstance startQuest(ServerPlayer player, String questId) {
        if (!initialized || player == null) {
            return null;
        }
        QuestDefinition definition = definitions.get(questId);
        if (definition == null) {
            return null;
        }
        QuestPlayerData data = getOrCreatePlayerData(player.getUUID());
        QuestCategory category = definition.resolvedCategory();
        if (!isParallelAllowed(category)) {
            String activeId = data.activeQuestId();
            if (activeId != null && !activeId.equals(questId)) {
                QuestInstance activeInstance = data.quests().get(activeId);
                QuestDefinition activeDefinition = definitions.get(activeId);
                if (activeInstance == null || activeDefinition == null || activeInstance.state() != QuestInstance.State.ACTIVE) {
                    data.setActiveQuestId(null);
                } else {
                    return null;
                }
            }
        }
        QuestInstance instance = data.quests().computeIfAbsent(questId, id -> new QuestInstance(id, definition.version()));

        if (!canStartQuest(definition, instance)) {
            return null;
        }
        if (!conditionEvaluator.evaluateAll(player, data, definition.conditions())) {
            return null;
        }
        instance.setDefinitionVersion(definition.version());
        instance.setState(QuestInstance.State.ACTIVE);
        instance.setCurrentStepIndex(0);
        instance.stepProgress().clear();
        instance.setStartedAt(System.currentTimeMillis());
        if (isParallelAllowed(category)) {
            data.addActiveFarmQuest(questId);
        } else {
            data.setActiveQuestId(questId);
        }
        scheduleStepTimers(player, definition, instance);
        QuestUIBridge.sendFullSync(player, definitions, data, zones);
        return instance;
    }

    public void completeQuest(ServerPlayer player, QuestInstance instance, QuestDefinition definition) {
        if (instance == null || definition == null) {
            return;
        }
        instance.setState(QuestInstance.State.COMPLETED);
        instance.setCompletedAt(System.currentTimeMillis());
        int rewardCount = definition.rewards() != null ? definition.rewards().size() : 0;
        Marallyzen.LOGGER.info(
                "QuestManager: quest '{}' completed by {}, rewards={}",
                instance.questId(),
                player != null ? player.getGameProfile().getName() : "null",
                rewardCount
        );
        rewardHandler.applyRewards(player, getOrCreatePlayerData(player.getUUID()), definition);
        if (player != null && rewardCount > 0) {
            sendQuestRewardNarration(player, definition);
        }
        sendQuestAdvancementMessage(player, definition);
        clearActiveQuestState(player, definition, instance);
        QuestUIBridge.sendFullSync(player, definitions, getOrCreatePlayerData(player.getUUID()), zones);
    }

    public void failQuest(ServerPlayer player, QuestInstance instance) {
        if (instance == null) {
            return;
        }
        instance.setState(QuestInstance.State.FAILED);
        QuestDefinition definition = definitions.get(instance.questId());
        clearActiveQuestState(player, definition, instance);
        QuestUIBridge.sendFullSync(player, definitions, getOrCreatePlayerData(player.getUUID()), zones);
    }

    public void selectActiveQuest(ServerPlayer player, String questId) {
        if (!initialized || player == null || questId == null || questId.isBlank()) {
            return;
        }
        QuestPlayerData data = getOrCreatePlayerData(player.getUUID());
        QuestInstance instance = data.quests().get(questId);
        if (instance == null || instance.state() != QuestInstance.State.ACTIVE) {
            return;
        }
        if (questId.equals(data.activeQuestId())) {
            return;
        }
        data.setActiveQuestId(questId);
        QuestUIBridge.sendFullSync(player, definitions, data, zones);
    }

    private void fireEvent(ServerPlayer player, QuestEvent event) {
        if (!initialized || player == null || event == null) {
            return;
        }
        try {
            QuestPlayerData data = getOrCreatePlayerData(player.getUUID());
            boolean changed = triggerSystem.handleEvent(player, data, definitions, event, this);
            if (changed) {
                QuestUIBridge.sendFullSync(player, definitions, data, zones);
            }
        } catch (Exception e) {
            Marallyzen.LOGGER.error("QuestManager: error handling event {}", event.type(), e);
        }
    }

    private void updateTimeWeather(ServerPlayer player) {
        QuestPlayerRuntime runtimeData = runtime.computeIfAbsent(player.getUUID(), key -> new QuestPlayerRuntime());
        long time = player.level().getDayTime() % 24000L;
        boolean isDay = time >= 0 && time < 12000;
        if (runtimeData.lastIsDay == null || runtimeData.lastIsDay != isDay) {
            runtimeData.lastIsDay = isDay;
            fireEvent(player, new QuestEvent("time_of_day", Map.of("value", isDay ? "day" : "night"), player.blockPosition()));
        }
        String weather = player.level().isThundering() ? "storm" : (player.level().isRaining() ? "rain" : "clear");
        if (runtimeData.lastWeather == null || !runtimeData.lastWeather.equals(weather)) {
            runtimeData.lastWeather = weather;
            fireEvent(player, new QuestEvent("weather", Map.of("value", weather), player.blockPosition()));
        }
    }

    private boolean canStartQuest(QuestDefinition definition, QuestInstance instance) {
        if (instance.state() == QuestInstance.State.ACTIVE) {
            return false;
        }
        if (instance.state() == QuestInstance.State.COMPLETED && !definition.flags().repeatable()) {
            boolean isDaily = definition.resolvedCategory() == QuestCategory.DAILY || definition.flags().daily();
            if (!isDaily) {
                return false;
            }
            long lastDay = instance.completedAt() / 86_400_000L;
            long currentDay = System.currentTimeMillis() / 86_400_000L;
            return currentDay > lastDay;
        }
        return true;
    }

    private QuestPlayerData getOrCreatePlayerData(UUID uuid) {
        return playerData.computeIfAbsent(uuid, key -> new QuestPlayerData());
    }

    private void loadDefinitions() {
        definitions.clear();
        Path dir = FMLPaths.CONFIGDIR.get().resolve(Marallyzen.MODID).resolve("quests");
        definitions.putAll(QuestDefinitionLoader.loadDefinitions(dir));
    }

    private void ensureQuestAdvancements(MinecraftServer server) {
        if (server == null || definitions.isEmpty()) {
            return;
        }
        Path worldRoot = server.getWorldPath(LevelResource.ROOT);
        Path mainDatapacksDir = server.getWorldPath(LevelResource.DATAPACK_DIR);
        Path packDir = mainDatapacksDir.resolve("marallyzen-quests");
        boolean wroteAny = ensureQuestAdvancementsInDir(packDir);

        var repo = server.getPackRepository();
        repo.reload();
        java.util.List<String> selected = new ArrayList<>(repo.getSelectedIds());
        java.util.List<String> available = new ArrayList<>();
        for (var pack : repo.getAvailablePacks()) {
            available.add(pack.getId());
        }

        String packId = findPackId(repo, "marallyzen-quests");
        if (packId == null && available.contains("file/marallyzen-quests")) {
            packId = "file/marallyzen-quests";
        }
        if (packId == null && available.contains("marallyzen-quests")) {
            packId = "marallyzen-quests";
        }
        if (packId != null && !selected.contains(packId)) {
            selected.add(packId);
        }
        try {
            repo.setSelected(selected);
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("QuestManager: failed to update selected datapacks", e);
        }
        if (packId != null) {
            server.getCommands().performPrefixedCommand(
                    server.createCommandSourceStack(),
                    "datapack enable \"" + packId + "\""
            );
        }
        Marallyzen.LOGGER.info(
                "QuestManager: quest advancements datapack mainDir={}, packDir={}, wroteAny={}, availablePacks={}, selectedPacks={}, packId={}",
                mainDatapacksDir, packDir, wroteAny, available, selected, packId
        );
        Marallyzen.LOGGER.info("QuestManager: quest datapack worldRoot={}", worldRoot);
        if (packId != null || wroteAny) {
            server.reloadResources(selected)
                    .whenComplete((ignored, error) -> {
                        if (error != null) {
                            Marallyzen.LOGGER.warn("QuestManager: quest datapack reload failed", error);
                            return;
                        }
                        server.execute(() -> {
                            ResourceLocation rootId = ResourceLocation.parse(Marallyzen.MODID + ":root");
                            boolean rootLoaded = server.getAdvancements().get(rootId) != null;
                            boolean questLoaded = false;
                            ResourceLocation sampleQuestId = findSampleQuestAdvancement();
                            if (sampleQuestId != null) {
                                questLoaded = server.getAdvancements().get(sampleQuestId) != null;
                            }
                            logQuestResourceState(server, sampleQuestId);
                            Marallyzen.LOGGER.info(
                                    "QuestManager: quest advancements loaded root={}, sampleQuest={}, sampleLoaded={}",
                                    rootLoaded,
                                    sampleQuestId != null ? sampleQuestId.toString() : "none",
                                    questLoaded
                            );
                        });
                    });
        }
    }

    private boolean ensureQuestAdvancementsInDir(Path packDir) {
        boolean wroteAny = false;
        try {
            Files.createDirectories(packDir);
            Path meta = packDir.resolve("pack.mcmeta");
            int packFormat = SharedConstants.getCurrentVersion().getPackVersion(PackType.SERVER_DATA);
            String metaJson = "{\n"
                    + "  \"pack\": {\n"
                    + "    \"pack_format\": " + packFormat + ",\n"
                    + "    \"description\": \"Marallyzen quest advancements\"\n"
                    + "  }\n"
                    + "}\n";
            Files.write(meta, metaJson.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("QuestManager: failed to write pack.mcmeta in {}", packDir, e);
            return false;
        }

        wroteAny |= ensureRootAdvancement(packDir);

        for (QuestDefinition definition : definitions.values()) {
            if (definition.rewards() == null) {
                continue;
            }
            for (QuestRewardDef reward : definition.rewards()) {
                if (reward == null || !"advancement".equals(reward.type())) {
                    continue;
                }
                String id = QuestJsonUtils.getString(reward.data(), "id", "");
                if (id.isBlank()) {
                    continue;
                }
                String resolvedId = id.contains(":") ? id : Marallyzen.MODID + ":" + id;
                ResourceLocation location = safeParseLocation(resolvedId);
                if (location == null) {
                    continue;
                }
                String packPath = "data/" + location.getNamespace() + "/advancement/" + location.getPath() + ".json";
                try {
                    Path target = packDir.resolve(packPath.replace("/", java.io.File.separator));
                    Files.createDirectories(target.getParent());
                    String content = buildQuestAdvancement(definition, resolvedId);
                    Files.writeString(target, content, StandardCharsets.UTF_8);
                    wroteAny = true;
                } catch (Exception e) {
                    Marallyzen.LOGGER.warn("QuestManager: failed to write advancement {} to {}", resolvedId, packDir, e);
                }
            }
        }
        return wroteAny;
    }

    private boolean ensureRootAdvancement(Path packDir) {
        String packPath = "data/" + Marallyzen.MODID + "/advancement/root.json";
        try {
            Path target = packDir.resolve(packPath.replace("/", java.io.File.separator));
            Files.createDirectories(target.getParent());
            String content = buildRootAdvancement();
            if (Files.exists(target)) {
                String existing = Files.readString(target, StandardCharsets.UTF_8);
                if (existing.equals(content)) {
                    return false;
                }
            }
            Files.writeString(target, content, StandardCharsets.UTF_8);
            return true;
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("QuestManager: failed to write root advancement to {}", packDir, e);
            return false;
        }
    }

    private ResourceLocation safeParseLocation(String id) {
        try {
            return ResourceLocation.parse(id);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String buildQuestAdvancement(QuestDefinition definition, String resolvedId) {
        String title = definition != null ? definition.title() : resolvedId;
        String desc = definition != null ? definition.description() : "";
        String titleJson = buildTextComponentJson(title);
        String descJson = buildTextComponentJson(desc);
        return "{\n"
                + "  \"parent\": \"" + Marallyzen.MODID + ":root\",\n"
                + "  \"display\": {\n"
                + "    \"icon\": {\"item\": \"minecraft:paper\"},\n"
                + "    \"title\": " + titleJson + ",\n"
                + "    \"description\": " + descJson + ",\n"
                + "    \"frame\": \"challenge\",\n"
                + "    \"show_toast\": true,\n"
                + "    \"announce_to_chat\": false,\n"
                + "    \"hidden\": false\n"
                + "  },\n"
                + "  \"criteria\": {\n"
                + "    \"impossible\": {\"trigger\": \"minecraft:impossible\"}\n"
                + "  }\n"
                + "}\n";
    }

    private String buildRootAdvancement() {
        return "{\n"
                + "  \"display\": {\n"
                + "    \"icon\": {\"item\": \"minecraft:book\"},\n"
                + "    \"title\": {\"text\": \"Marallys\"},\n"
                + "    \"description\": {\"text\": \"\\u041a\\u0432\\u0435\\u0441\\u0442\\u044b Marallys\"},\n"
                + "    \"background\": \"minecraft:textures/gui/advancements/backgrounds/stone.png\",\n"
                + "    \"frame\": \"task\",\n"
                + "    \"show_toast\": false,\n"
                + "    \"announce_to_chat\": false,\n"
                + "    \"hidden\": false\n"
                + "  },\n"
                + "  \"criteria\": {\n"
                + "    \"impossible\": {\"trigger\": \"minecraft:impossible\"}\n"
                + "  }\n"
                + "}\n";
    }

    private ResourceLocation findSampleQuestAdvancement() {
        for (QuestDefinition definition : definitions.values()) {
            if (definition.rewards() == null) {
                continue;
            }
            for (QuestRewardDef reward : definition.rewards()) {
                if (reward == null || !"advancement".equals(reward.type())) {
                    continue;
                }
                String id = QuestJsonUtils.getString(reward.data(), "id", "");
                if (id.isBlank()) {
                    continue;
                }
                String resolvedId = id.contains(":") ? id : Marallyzen.MODID + ":" + id;
                ResourceLocation location = safeParseLocation(resolvedId);
                if (location != null) {
                    return location;
                }
            }
        }
        return null;
    }

    private void logQuestResourceState(MinecraftServer server, ResourceLocation sampleQuestId) {
        ResourceManager resources = server.getResourceManager();
        ResourceLocation rootAdvancementsPath = ResourceLocation.fromNamespaceAndPath(Marallyzen.MODID, "advancements/root.json");
        ResourceLocation rootAdvancementPath = ResourceLocation.fromNamespaceAndPath(Marallyzen.MODID, "advancement/root.json");
        boolean rootAdvancementsResource = resources.getResource(rootAdvancementsPath).isPresent();
        boolean rootAdvancementResource = resources.getResource(rootAdvancementPath).isPresent();
        boolean sampleAdvancementsResource = false;
        boolean sampleAdvancementResource = false;
        if (sampleQuestId != null) {
            String sampleAdvancementsPath = "advancements/" + sampleQuestId.getPath() + ".json";
            String sampleAdvancementPath = "advancement/" + sampleQuestId.getPath() + ".json";
            ResourceLocation sampleAdvancementsResId = ResourceLocation.fromNamespaceAndPath(sampleQuestId.getNamespace(), sampleAdvancementsPath);
            ResourceLocation sampleAdvancementResId = ResourceLocation.fromNamespaceAndPath(sampleQuestId.getNamespace(), sampleAdvancementPath);
            sampleAdvancementsResource = resources.getResource(sampleAdvancementsResId).isPresent();
            sampleAdvancementResource = resources.getResource(sampleAdvancementResId).isPresent();
        }
        int advancementsCount = resources.listResources("advancements", loc -> Marallyzen.MODID.equals(loc.getNamespace())).size();
        int advancementCount = resources.listResources("advancement", loc -> Marallyzen.MODID.equals(loc.getNamespace())).size();
        Marallyzen.LOGGER.info(
                "QuestManager: quest resources rootAdvancements={}, rootAdvancement={}, sampleAdvancements={}, sampleAdvancement={}, countAdvancements={}, countAdvancement={}",
                rootAdvancementsResource,
                rootAdvancementResource,
                sampleAdvancementsResource,
                sampleAdvancementResource,
                advancementsCount,
                advancementCount
        );
    }

    private void sendQuestAdvancementMessage(ServerPlayer player, QuestDefinition definition) {
        if (player == null || definition == null) {
            return;
        }
        String title = definition.title();
        if (title == null || title.isBlank()) {
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        QuestCategory category = definition.resolvedCategory();
        if (category == QuestCategory.FARM) {
            return;
        }
        String playerName = player.getGameProfile().getName();
        TextColor prime = TextColor.fromRgb(0xD48E03);
        String prefix = getQuestMessagePrefix(category);
        MutableComponent message = Component.literal(playerName + " " + prefix)
                .append(Component.literal(title).withStyle(Style.EMPTY.withColor(prime)));
        switch (category) {
            case DAILY -> player.sendSystemMessage(message);
            case SIDE -> notifyNearbyPlayers(player, message);
            case STORY, EVENT -> {
                for (ServerPlayer online : server.getPlayerList().getPlayers()) {
                    online.sendSystemMessage(message);
                }
                player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
            }
            default -> {
            }
        }
    }

    private void sendQuestRewardNarration(ServerPlayer player, QuestDefinition definition) {
        if (player == null || definition == null) {
            return;
        }
        Component questTitle = resolveQuestTitle(definition.title());
        int color = getQuestTitleColor(definition.resolvedCategory());
        MutableComponent message = Component.literal("Вы получили награду за выполнение квеста ")
                .append(questTitle.copy().withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color))))
                .append(Component.literal("! Проверьте инвентарь."));
        NetworkHelper.sendToPlayer(player, new NarratePacket(message, null, 5, 100, 5));
    }

    private Component resolveQuestTitle(String title) {
        if (title == null || title.isBlank()) {
            return Component.empty();
        }
        String trimmed = title.trim();
        if (trimmed.startsWith("quest.") || trimmed.startsWith("advancement.")) {
            return Component.translatable(trimmed);
        }
        return Component.literal(trimmed);
    }

    private int getQuestTitleColor(QuestCategory category) {
        return QuestCategoryColors.getColor(category);
    }

    private void notifyNearbyPlayers(ServerPlayer player, Component message) {
        for (ServerPlayer online : player.getServer().getPlayerList().getPlayers()) {
            if (!online.level().dimension().equals(player.level().dimension())) {
                continue;
            }
            if (online.distanceToSqr(player) > SIDE_QUEST_NOTIFY_RADIUS * SIDE_QUEST_NOTIFY_RADIUS) {
                continue;
            }
            online.sendSystemMessage(message);
        }
    }

    private String getQuestMessagePrefix(QuestCategory category) {
        return switch (category) {
            case STORY, EVENT -> "\u0432\u044b\u043f\u043e\u043b\u043d\u0438\u043b \u0441\u044e\u0436\u0435\u0442\u043d\u044b\u0439 \u043a\u0432\u0435\u0441\u0442: ";
            case DAILY -> "\u0432\u044b\u043f\u043e\u043b\u043d\u0438\u043b \u0435\u0436\u0435\u0434\u043d\u0435\u0432\u043d\u044b\u0439 \u043a\u0432\u0435\u0441\u0442: ";
            default -> "\u0432\u044b\u043f\u043e\u043b\u043d\u0438\u043b \u043a\u0432\u0435\u0441\u0442: ";
        };
    }

    private boolean isParallelAllowed(QuestCategory category) {
        return category == QuestCategory.FARM || category == QuestCategory.DAILY;
    }

    private void clearActiveQuestState(ServerPlayer player, QuestDefinition definition, QuestInstance instance) {
        if (player == null || definition == null || instance == null) {
            return;
        }
        QuestPlayerData data = getOrCreatePlayerData(player.getUUID());
        QuestCategory category = definition.resolvedCategory();
        if (isParallelAllowed(category)) {
            data.removeActiveFarmQuest(instance.questId());
            return;
        }
        if (instance.questId().equals(data.activeQuestId())) {
            data.setActiveQuestId(null);
        }
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String buildTextComponentJson(String value) {
        if (value == null || value.isBlank()) {
            return "{\"text\":\"\"}";
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("quest.") || trimmed.startsWith("advancement.")) {
            return "{\"translate\":\"" + escapeJson(trimmed) + "\"}";
        }
        return "{\"text\":\"" + escapeJson(trimmed) + "\"}";
    }

    private String findPackId(net.minecraft.server.packs.repository.PackRepository repo, String folderName) {
        for (var pack : repo.getAvailablePacks()) {
            String id = pack.getId();
            if (id != null && id.contains(folderName)) {
                return id;
            }
        }
        return null;
    }

    private void loadZones() {
        zones.clear();
        zoneIndex.clear();
        zoneTriggerIds.clear();
        Path dir = FMLPaths.CONFIGDIR.get().resolve(Marallyzen.MODID).resolve("zones");
        zones.putAll(QuestZoneLoader.loadZones(dir));
        for (QuestZoneDefinition zone : zones.values()) {
            zoneIndex.indexZone(zone);
        }
        for (QuestDefinition definition : definitions.values()) {
            for (QuestTriggerDef trigger : definition.triggers()) {
                collectZoneTrigger(trigger);
            }
            for (QuestStep step : definition.steps()) {
                for (QuestTriggerDef trigger : step.triggers()) {
                    collectZoneTrigger(trigger);
                }
            }
        }
        if (!zones.isEmpty()) {
            Marallyzen.LOGGER.info("QuestManager loaded {} zone(s): {}", zones.size(), zones.keySet());
            Marallyzen.LOGGER.info("QuestManager active zone triggers: {}", zoneTriggerIds);
        } else {
            Marallyzen.LOGGER.info("QuestManager loaded 0 zones from {}", dir);
        }
    }

    private void collectZoneTrigger(QuestTriggerDef trigger) {
        if (trigger == null || trigger.type() == null) {
            return;
        }
        if (!"zone_enter".equals(trigger.type())) {
            return;
        }
        String zoneId = QuestJsonUtils.getString(trigger.data(), "zoneId", "");
        if (!zoneId.isBlank()) {
            zoneTriggerIds.add(zoneId);
        }
    }

    public void onStepChanged(ServerPlayer player, QuestDefinition definition, QuestInstance instance) {
        if (player == null || definition == null || instance == null) {
            return;
        }
        scheduleStepTimers(player, definition, instance);
    }

    private void scheduleStepTimers(ServerPlayer player, QuestDefinition definition, QuestInstance instance) {
        QuestStep step = definition.getStep(instance.currentStepIndex());
        if (step == null) {
            return;
        }
        for (QuestTriggerDef trigger : step.triggers()) {
            if (trigger == null || !"timer".equals(trigger.type())) {
                continue;
            }
            long seconds = QuestJsonUtils.getLong(trigger.data(), "seconds", 0L);
            if (seconds <= 0) {
                continue;
            }
            String timerId = QuestJsonUtils.getString(trigger.data(), "id", "");
            QuestEvent event = new QuestEvent("timer", Map.of("id", timerId), player.blockPosition());
            scheduledEvents.add(new ScheduledQuestEvent(player.getUUID(), System.currentTimeMillis() + seconds * 1000L, event));
        }
    }

    private static class QuestPlayerRuntime {
        private BlockPos lastPos = BlockPos.ZERO;
        private ResourceKey<net.minecraft.world.level.Level> lastDimension;
        private Set<String> activeZones = new HashSet<>();
        private String lastBiomeId;
        private Boolean lastIsDay;
        private String lastWeather;
    }

    private static class ScheduledQuestEvent {
        private final UUID playerId;
        private final long fireAtMs;
        private final QuestEvent event;

        private ScheduledQuestEvent(UUID playerId, long fireAtMs, QuestEvent event) {
            this.playerId = playerId;
            this.fireAtMs = fireAtMs;
            this.event = event;
        }
    }
}



