package neutka.marallys.marallyzen.ai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import neutka.marallys.marallyzen.Marallyzen;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class NpcAiGlobalConfig {
    public boolean enabled = false;
    public String baseUrl = "https://api.openai.com/v1/chat/completions";
    public String apiKey = "";
    public String model = "gpt-4o-mini";
    public double temperature = 0.7;
    public int maxTokens = 200;
    public int timeoutSeconds = 20;
    public int defaultOptionCount = 3;
    public int defaultMemoryTurns = 8;
    public String systemPrompt = "Ты NPC в игре. Отвечай только на русском. "
            + "Возвращай только JSON без markdown и без лишнего текста: "
            + "{\"reply\":\"...\",\"options\":[\"...\",\"...\"]}. "
            + "reply — короткая реплика NPC. options — короткие варианты выбора игрока.";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static volatile NpcAiGlobalConfig cached;

    public static NpcAiGlobalConfig get() {
        NpcAiGlobalConfig local = cached;
        if (local != null) {
            return local;
        }
        synchronized (NpcAiGlobalConfig.class) {
            if (cached == null) {
                cached = loadOrCreate();
            }
            return cached;
        }
    }

    public static void reload() {
        synchronized (NpcAiGlobalConfig.class) {
            cached = loadOrCreate();
        }
    }

    private static NpcAiGlobalConfig loadOrCreate() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(Marallyzen.MODID);
        Path configFile = configDir.resolve("ai.json");
        try {
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
            if (!Files.exists(configFile)) {
                NpcAiGlobalConfig defaults = new NpcAiGlobalConfig();
                try (Writer writer = Files.newBufferedWriter(configFile)) {
                    GSON.toJson(defaults, writer);
                }
                Marallyzen.LOGGER.info("Created AI config at {}", configFile);
                return defaults;
            }
            try (Reader reader = Files.newBufferedReader(configFile)) {
                NpcAiGlobalConfig loaded = GSON.fromJson(reader, NpcAiGlobalConfig.class);
                if (loaded == null) {
                    loaded = new NpcAiGlobalConfig();
                }
                // Allow env override to avoid storing secrets in config files.
                String envKey = System.getenv("MARALLYZEN_AI_API_KEY");
                if (envKey != null && !envKey.isBlank()) {
                    loaded.apiKey = envKey;
                }
                return loaded;
            }
        } catch (IOException e) {
            Marallyzen.LOGGER.warn("Failed to load AI config, using defaults", e);
            return new NpcAiGlobalConfig();
        }
    }
}
