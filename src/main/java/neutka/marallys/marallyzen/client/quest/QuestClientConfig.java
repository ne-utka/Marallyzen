package neutka.marallys.marallyzen.client.quest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import neutka.marallys.marallyzen.Marallyzen;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class QuestClientConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE_NAME = "quest_client.json";
    
    private boolean questHudEnabled = true;
    
    private QuestClientConfig() {
    }
    
    public static QuestClientConfig load() {
        QuestClientConfig config = new QuestClientConfig();
        File configFile = getConfigFile();
        
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                if (json != null && json.has("questHudEnabled")) {
                    config.questHudEnabled = json.get("questHudEnabled").getAsBoolean();
                }
            } catch (Exception e) {
                Marallyzen.LOGGER.warn("Failed to load quest client config, using defaults", e);
            }
        }
        
        return config;
    }
    
    public void save() {
        File configFile = getConfigFile();
        
        try {
            // Ensure parent directory exists
            Path parentDir = configFile.toPath().getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }
            
            JsonObject json = new JsonObject();
            json.addProperty("questHudEnabled", questHudEnabled);
            
            try (FileWriter writer = new FileWriter(configFile)) {
                GSON.toJson(json, writer);
            }
        } catch (IOException e) {
            Marallyzen.LOGGER.warn("Failed to save quest client config", e);
        }
    }
    
    private static File getConfigFile() {
        // Get Minecraft config directory
        try {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc != null && mc.gameDirectory != null) {
                File configDir = new File(mc.gameDirectory, "config" + File.separator + "marallyzen");
                return new File(configDir, CONFIG_FILE_NAME);
            }
        } catch (Exception e) {
            // Minecraft not initialized yet, use fallback
        }
        
        // Fallback if Minecraft is not initialized yet
        return new File("config", "marallyzen" + File.separator + CONFIG_FILE_NAME);
    }
    
    public boolean isQuestHudEnabled() {
        return questHudEnabled;
    }
    
    public void setQuestHudEnabled(boolean questHudEnabled) {
        this.questHudEnabled = questHudEnabled;
    }
}

