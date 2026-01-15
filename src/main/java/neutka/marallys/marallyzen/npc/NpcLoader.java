package neutka.marallys.marallyzen.npc;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.neoforged.fml.loading.FMLPaths;

import neutka.marallys.marallyzen.Marallyzen;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads NPC configurations from JSON files.
 */
public class NpcLoader {
    private static final Gson GSON = new Gson();

    public static void loadNpcsFromDirectory(NpcRegistry registry) {
        Path npcsDir = FMLPaths.CONFIGDIR.get().resolve(Marallyzen.MODID).resolve("npcs");
        
        if (!Files.exists(npcsDir)) {
            try {
                Files.createDirectories(npcsDir);
                Marallyzen.LOGGER.info("Created NPCs directory: {}", npcsDir);
            } catch (IOException e) {
                Marallyzen.LOGGER.error("Failed to create NPCs directory", e);
                return;
            }
        }

        File[] jsonFiles = npcsDir.toFile().listFiles((dir, name) -> name.endsWith(".json"));
        if (jsonFiles == null) {
            return;
        }

        for (File file : jsonFiles) {
            try {
                NpcData data = loadNpcFromFile(file);
                if (data != null) {
                    registry.registerNpcData(data);
                    Marallyzen.LOGGER.info("Loaded NPC: {} from {}", data.getId(), file.getName());
                }
            } catch (Exception e) {
                Marallyzen.LOGGER.error("Failed to load NPC from file: {}", file.getName(), e);
            }
        }
    }

    private static NpcData loadNpcFromFile(File file) throws IOException {
        try (FileReader reader = new FileReader(file)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            
            String id = json.get("id").getAsString();
            if (id == null || id.isEmpty()) {
                // Use filename without extension as ID
                String fileName = file.getName();
                id = fileName.substring(0, fileName.lastIndexOf('.'));
            }

            NpcData data = new NpcData(id);

            // Load basic properties
            if (json.has("name")) {
                data.setName(json.get("name").getAsString());
            }

            if (json.has("entityType")) {
                String entityTypeStr = json.get("entityType").getAsString();
                ResourceLocation entityTypeLoc = ResourceLocation.parse(entityTypeStr);
                EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(entityTypeLoc);
                if (entityType != null) {
                    data.setEntityType(entityType);
                } else {
                    Marallyzen.LOGGER.warn("Unknown entity type: {} for NPC {}", entityTypeStr, id);
                }
            }

            if (json.has("spawnPos")) {
                JsonObject posJson = json.getAsJsonObject("spawnPos");
                int x = posJson.get("x").getAsInt();
                int y = posJson.get("y").getAsInt();
                int z = posJson.get("z").getAsInt();
                data.setSpawnPos(new BlockPos(x, y, z));
            }

            // Load GeckoLib resources
            if (json.has("geckolib")) {
                JsonObject geckolib = json.getAsJsonObject("geckolib");
                if (geckolib.has("model")) {
                    data.setGeckolibModel(ResourceLocation.parse(geckolib.get("model").getAsString()));
                }
                if (geckolib.has("animation")) {
                    data.setGeckolibAnimation(ResourceLocation.parse(geckolib.get("animation").getAsString()));
                }
                if (geckolib.has("texture")) {
                    data.setGeckolibTexture(ResourceLocation.parse(geckolib.get("texture").getAsString()));
                }
                if (geckolib.has("expression")) {
                    data.setGeckolibExpression(geckolib.get("expression").getAsString());
                }
                if (geckolib.has("talkExpression")) {
                    data.setGeckolibTalkExpression(geckolib.get("talkExpression").getAsString());
                }
            }

            // Load skin data
            if (json.has("skin")) {
                JsonObject skin = json.getAsJsonObject("skin");
                if (skin.has("texture")) {
                    data.setSkinTexture(safeTrim(skin.get("texture").getAsString()));
                }
                if (skin.has("signature")) {
                    data.setSkinSignature(safeTrim(skin.get("signature").getAsString()));
                }
                if (skin.has("model")) {
                    // "default" (Steve) or "slim" (Alex)
                    data.setSkinModel(safeTrim(skin.get("model").getAsString()));
                }
            }

            // Load dialog script
            if (json.has("dialogScript")) {
                data.setDialogScript(json.get("dialogScript").getAsString());
            }

            // Load cutscene
            if (json.has("cutscene")) {
                data.setCutscene(json.get("cutscene").getAsString());
            }

            // Load default animation
            if (json.has("defaultAnimation")) {
                data.setDefaultAnimation(json.get("defaultAnimation").getAsString());
            }

            // Load waypoints
            if (json.has("waypoints")) {
                JsonArray waypointsArray = json.getAsJsonArray("waypoints");
                List<NpcData.Waypoint> waypoints = new ArrayList<>();
                for (JsonElement element : waypointsArray) {
                    JsonObject wpJson = element.getAsJsonObject();
                    int x = wpJson.get("x").getAsInt();
                    int y = wpJson.get("y").getAsInt();
                    int z = wpJson.get("z").getAsInt();
                    int waitTicks = wpJson.has("waitTicks") ? wpJson.get("waitTicks").getAsInt() : 0;
                    double speed = wpJson.has("speed") ? wpJson.get("speed").getAsDouble() : 1.0;
                    waypoints.add(new NpcData.Waypoint(new BlockPos(x, y, z), waitTicks, speed));
                }
                data.setWaypoints(waypoints);
            }

            // Load waypoints loop setting
            if (json.has("waypointsLoop")) {
                data.setWaypointsLoop(json.get("waypointsLoop").getAsBoolean());
            }

            // Load health settings
            if (json.has("maxHealth")) {
                data.setMaxHealth(json.get("maxHealth").getAsDouble());
            }
            if (json.has("health")) {
                data.setHealth(json.get("health").getAsDouble());
            }
            if (json.has("invulnerable")) {
                data.setInvulnerable(json.get("invulnerable").getAsBoolean());
            }

            // Load proximity settings
            if (json.has("proximityText")) {
                data.setProximityText(json.get("proximityText").getAsString());
            }
            if (json.has("proximityRange")) {
                data.setProximityRange(json.get("proximityRange").getAsDouble());
            }
            if (json.has("showNameTag")) {
                data.setShowNameTag(json.get("showNameTag").getAsBoolean());
            }
            if (json.has("showProximityTextInActionBar")) {
                data.setShowProximityTextInActionBar(json.get("showProximityTextInActionBar").getAsBoolean());
            }
            if (json.has("showProximityTextInChat")) {
                data.setShowProximityTextInChat(json.get("showProximityTextInChat").getAsBoolean());
            }
            if (json.has("valk")) {
                data.setValk(json.get("valk").getAsBoolean());
            }
            if (json.has("valkRadius")) {
                data.setValkRadius(json.get("valkRadius").getAsInt());
            }
            if (json.has("valkPattern")) {
                data.setValkPattern(json.get("valkPattern").getAsString());
            }
            if (json.has("lookAtPlayers")) {
                data.setLookAtPlayers(json.get("lookAtPlayers").getAsBoolean());
            }

            // Load Wildfire gender settings
            if (json.has("wildfire")) {
                JsonObject wildfireJson = json.getAsJsonObject("wildfire");
                NpcData.WildfireSettings settings = new NpcData.WildfireSettings();
                if (wildfireJson.has("enabled")) {
                    settings.setEnabled(wildfireJson.get("enabled").getAsBoolean());
                }
                if (wildfireJson.has("gender")) {
                    settings.setGender(wildfireJson.get("gender").getAsString());
                }
                if (wildfireJson.has("bustSize")) {
                    settings.setBustSize(wildfireJson.get("bustSize").getAsFloat());
                }
                if (wildfireJson.has("breastPhysics")) {
                    settings.setBreastPhysics(wildfireJson.get("breastPhysics").getAsBoolean());
                }
                if (wildfireJson.has("bounceMultiplier")) {
                    settings.setBounceMultiplier(wildfireJson.get("bounceMultiplier").getAsFloat());
                }
                if (wildfireJson.has("floppiness")) {
                    settings.setFloppiness(wildfireJson.get("floppiness").getAsFloat());
                }
                if (wildfireJson.has("showBreastsInArmor")) {
                    settings.setShowBreastsInArmor(wildfireJson.get("showBreastsInArmor").getAsBoolean());
                }
                if (wildfireJson.has("cleavage")) {
                    settings.setCleavage(wildfireJson.get("cleavage").getAsFloat());
                }
                if (wildfireJson.has("uniboob")) {
                    settings.setUniboob(wildfireJson.get("uniboob").getAsBoolean());
                }
                if (wildfireJson.has("xOffset")) {
                    settings.setXOffset(wildfireJson.get("xOffset").getAsFloat());
                }
                if (wildfireJson.has("yOffset")) {
                    settings.setYOffset(wildfireJson.get("yOffset").getAsFloat());
                }
                if (wildfireJson.has("zOffset")) {
                    settings.setZOffset(wildfireJson.get("zOffset").getAsFloat());
                }
                data.setWildfireSettings(settings);
            }

            // Load AI dialog settings
            if (json.has("ai")) {
                JsonObject aiJson = json.getAsJsonObject("ai");
                NpcData.AiSettings aiSettings = new NpcData.AiSettings();
                if (aiJson.has("enabled")) {
                    aiSettings.setEnabled(aiJson.get("enabled").getAsBoolean());
                }
                if (aiJson.has("systemPrompt")) {
                    aiSettings.setSystemPrompt(aiJson.get("systemPrompt").getAsString());
                }
                if (aiJson.has("model")) {
                    aiSettings.setModel(aiJson.get("model").getAsString());
                }
                if (aiJson.has("temperature")) {
                    aiSettings.setTemperature(aiJson.get("temperature").getAsDouble());
                }
                if (aiJson.has("maxTokens")) {
                    aiSettings.setMaxTokens(aiJson.get("maxTokens").getAsInt());
                }
                if (aiJson.has("optionCount")) {
                    aiSettings.setOptionCount(aiJson.get("optionCount").getAsInt());
                }
                if (aiJson.has("memoryTurns")) {
                    aiSettings.setMemoryTurns(aiJson.get("memoryTurns").getAsInt());
                }
                data.setAiSettings(aiSettings);
            }

            // Load metadata
            if (json.has("metadata")) {
                JsonObject metadataJson = json.getAsJsonObject("metadata");
                for (String key : metadataJson.keySet()) {
                    data.getMetadata().put(key, metadataJson.get(key).getAsString());
                }
            }

            return data;
        }
    }

    public static void saveNpcToFile(NpcData data) {
        if (data == null) {
            return;
        }
        Path npcsDir = FMLPaths.CONFIGDIR.get().resolve(Marallyzen.MODID).resolve("npcs");
        try {
            Files.createDirectories(npcsDir);
        } catch (IOException e) {
            Marallyzen.LOGGER.error("Failed to create NPCs directory", e);
            return;
        }

        JsonObject json = new JsonObject();
        json.addProperty("id", data.getId());
        if (data.getName() != null) {
            json.addProperty("name", data.getName());
        }
        if (data.getEntityType() != null) {
            ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(data.getEntityType());
            if (typeId != null) {
                json.addProperty("entityType", typeId.toString());
            }
        }
        if (data.getSpawnPos() != null) {
            JsonObject pos = new JsonObject();
            pos.addProperty("x", data.getSpawnPos().getX());
            pos.addProperty("y", data.getSpawnPos().getY());
            pos.addProperty("z", data.getSpawnPos().getZ());
            json.add("spawnPos", pos);
        }
        if (data.getGeckolibModel() != null || data.getGeckolibAnimation() != null || data.getGeckolibTexture() != null
            || data.getGeckolibExpression() != null || data.getGeckolibTalkExpression() != null) {
            JsonObject geckolib = new JsonObject();
            if (data.getGeckolibModel() != null) {
                geckolib.addProperty("model", data.getGeckolibModel().toString());
            }
            if (data.getGeckolibAnimation() != null) {
                geckolib.addProperty("animation", data.getGeckolibAnimation().toString());
            }
            if (data.getGeckolibTexture() != null) {
                geckolib.addProperty("texture", data.getGeckolibTexture().toString());
            }
            if (data.getGeckolibExpression() != null) {
                geckolib.addProperty("expression", data.getGeckolibExpression());
            }
            if (data.getGeckolibTalkExpression() != null) {
                geckolib.addProperty("talkExpression", data.getGeckolibTalkExpression());
            }
            json.add("geckolib", geckolib);
        }
        if (data.getSkinTexture() != null || data.getSkinSignature() != null || data.getSkinModel() != null) {
            JsonObject skin = new JsonObject();
            if (data.getSkinTexture() != null) {
                skin.addProperty("texture", data.getSkinTexture());
            }
            if (data.getSkinSignature() != null) {
                skin.addProperty("signature", data.getSkinSignature());
            }
            if (data.getSkinModel() != null) {
                skin.addProperty("model", data.getSkinModel());
            }
            json.add("skin", skin);
        }
        if (data.getDialogScript() != null) {
            json.addProperty("dialogScript", data.getDialogScript());
        }
        if (data.getCutscene() != null) {
            json.addProperty("cutscene", data.getCutscene());
        }
        if (data.getDefaultAnimation() != null) {
            json.addProperty("defaultAnimation", data.getDefaultAnimation());
        }
        if (data.getWaypoints() != null && !data.getWaypoints().isEmpty()) {
            JsonArray waypoints = new JsonArray();
            for (NpcData.Waypoint wp : data.getWaypoints()) {
                JsonObject wpJson = new JsonObject();
                wpJson.addProperty("x", wp.getPos().getX());
                wpJson.addProperty("y", wp.getPos().getY());
                wpJson.addProperty("z", wp.getPos().getZ());
                wpJson.addProperty("waitTicks", wp.getWaitTicks());
                wpJson.addProperty("speed", wp.getSpeed());
                waypoints.add(wpJson);
            }
            json.add("waypoints", waypoints);
            json.addProperty("waypointsLoop", data.isWaypointsLoop());
        }
        if (data.getMaxHealth() != null) {
            json.addProperty("maxHealth", data.getMaxHealth());
        }
        if (data.getHealth() != null) {
            json.addProperty("health", data.getHealth());
        }
        if (data.getInvulnerable() != null) {
            json.addProperty("invulnerable", data.getInvulnerable());
        }
        if (data.getProximityText() != null) {
            json.addProperty("proximityText", data.getProximityText());
        }
        if (data.getProximityRange() != null) {
            json.addProperty("proximityRange", data.getProximityRange());
        }
        if (data.getShowNameTag() != null) {
            json.addProperty("showNameTag", data.getShowNameTag());
        }
        if (data.getShowProximityTextInActionBar() != null) {
            json.addProperty("showProximityTextInActionBar", data.getShowProximityTextInActionBar());
        }
        if (data.getShowProximityTextInChat() != null) {
            json.addProperty("showProximityTextInChat", data.getShowProximityTextInChat());
        }
        if (data.getValk() != null) {
            json.addProperty("valk", data.getValk());
        }
        if (data.getValkRadius() != null) {
            json.addProperty("valkRadius", data.getValkRadius());
        }
        if (data.getValkPattern() != null) {
            json.addProperty("valkPattern", data.getValkPattern());
        }
        if (data.getLookAtPlayers() != null) {
            json.addProperty("lookAtPlayers", data.getLookAtPlayers());
        }
        if (data.getWildfireSettings() != null) {
            JsonObject wildfire = new JsonObject();
            NpcData.WildfireSettings wf = data.getWildfireSettings();
            if (wf.getEnabled() != null) wildfire.addProperty("enabled", wf.getEnabled());
            if (wf.getGender() != null) wildfire.addProperty("gender", wf.getGender());
            if (wf.getBustSize() != null) wildfire.addProperty("bustSize", wf.getBustSize());
            if (wf.getBreastPhysics() != null) wildfire.addProperty("breastPhysics", wf.getBreastPhysics());
            if (wf.getBounceMultiplier() != null) wildfire.addProperty("bounceMultiplier", wf.getBounceMultiplier());
            if (wf.getFloppiness() != null) wildfire.addProperty("floppiness", wf.getFloppiness());
            if (wf.getShowBreastsInArmor() != null) wildfire.addProperty("showBreastsInArmor", wf.getShowBreastsInArmor());
            if (wf.getCleavage() != null) wildfire.addProperty("cleavage", wf.getCleavage());
            if (wf.getUniboob() != null) wildfire.addProperty("uniboob", wf.getUniboob());
            if (wf.getXOffset() != null) wildfire.addProperty("xOffset", wf.getXOffset());
            if (wf.getYOffset() != null) wildfire.addProperty("yOffset", wf.getYOffset());
            if (wf.getZOffset() != null) wildfire.addProperty("zOffset", wf.getZOffset());
            json.add("wildfire", wildfire);
        }
        if (data.getAiSettings() != null) {
            JsonObject ai = new JsonObject();
            NpcData.AiSettings aiSettings = data.getAiSettings();
            ai.addProperty("enabled", aiSettings.isEnabled());
            if (aiSettings.getSystemPrompt() != null) ai.addProperty("systemPrompt", aiSettings.getSystemPrompt());
            if (aiSettings.getModel() != null) ai.addProperty("model", aiSettings.getModel());
            if (aiSettings.getTemperature() != null) ai.addProperty("temperature", aiSettings.getTemperature());
            if (aiSettings.getMaxTokens() != null) ai.addProperty("maxTokens", aiSettings.getMaxTokens());
            if (aiSettings.getOptionCount() != null) ai.addProperty("optionCount", aiSettings.getOptionCount());
            if (aiSettings.getMemoryTurns() != null) ai.addProperty("memoryTurns", aiSettings.getMemoryTurns());
            json.add("ai", ai);
        }
        if (data.getMetadata() != null && !data.getMetadata().isEmpty()) {
            JsonObject metadata = new JsonObject();
            for (var entry : data.getMetadata().entrySet()) {
                metadata.addProperty(entry.getKey(), entry.getValue());
            }
            json.add("metadata", metadata);
        }

        File file = npcsDir.resolve(data.getId() + ".json").toFile();
        try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
            new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(json, writer);
        } catch (IOException e) {
            Marallyzen.LOGGER.error("Failed to save NPC file: {}", file.getName(), e);
        }
    }

    private static String safeTrim(String value) {
        return value != null ? value.trim() : null;
    }
}


