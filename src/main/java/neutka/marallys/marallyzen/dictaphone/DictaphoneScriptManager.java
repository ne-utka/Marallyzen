package neutka.marallys.marallyzen.dictaphone;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLPaths;
import neutka.marallys.marallyzen.DenizenService;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.audio.AudioMetadata;
import neutka.marallys.marallyzen.audio.MarallyzenAudioService;
import neutka.marallys.marallyzen.audio.MarallyzenSounds;
import neutka.marallys.marallyzen.entity.DictaphoneEntity;
import neutka.marallys.marallyzen.network.NarratePacket;
import neutka.marallys.marallyzen.network.NetworkHelper;
import neutka.marallys.marallyzen.npc.DialogScriptLoader;
import neutka.marallys.marallyzen.npc.NpcNarrateHandler;

import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public final class DictaphoneScriptManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path STATE_PATH = FMLPaths.CONFIGDIR.get()
        .resolve(Marallyzen.MODID)
        .resolve("dictaphone_bindings.json");
    private static final String DEFAULT_NARRATOR_NAME = "*****";
    private static final String SYSTEM_SOUND_START = "dictophone_start";
    private static final String SYSTEM_SOUND_STOP = "dictophone_stop";

    private static final Map<String, String> bindings = new HashMap<>();
    private static final Map<String, Boolean> protectedByOp = new HashMap<>();
    private static boolean loaded;
    private static final Map<UUID, Long> narrationLockUntil = new HashMap<>();
    private static final Map<String, Long> playLockUntil = new HashMap<>();
    private static final Map<String, Boolean> isPlayingByPos = new HashMap<>();

    private DictaphoneScriptManager() {
    }

    public static boolean bindScript(BlockPos pos, ResourceKey<Level> dimension, String scriptName) {
        return bindScript(pos, dimension, scriptName, false);
    }

    public static boolean bindScript(BlockPos pos, ResourceKey<Level> dimension, String scriptName, boolean protectByOp) {
        if (scriptName == null || scriptName.isBlank()) {
            return false;
        }
        ensureLoaded();
        String key = buildKey(dimension, pos);
        bindings.put(key, normalizeScriptName(scriptName));
        if (protectByOp) {
            protectedByOp.put(key, true);
        }
        saveState();
        return true;
    }

    public static String getBoundScript(BlockPos pos, ResourceKey<Level> dimension) {
        ensureLoaded();
        return bindings.get(buildKey(dimension, pos));
    }

    public static boolean isProtectedByOp(BlockPos pos, ResourceKey<Level> dimension) {
        ensureLoaded();
        return Boolean.TRUE.equals(protectedByOp.get(buildKey(dimension, pos)));
    }

    public static boolean scriptExists(String scriptName) {
        String normalized = normalizeScriptName(scriptName);
        if (normalized == null) {
            return false;
        }
        File scriptsFolder = DenizenService.getScriptsFolder();
        File scriptFile = new File(scriptsFolder, normalized + ".dsc");
        return scriptFile.exists();
    }

    public static void playBoundScript(ServerPlayer player, ServerLevel level, BlockPos pos, String scriptName) {
        if (scriptName == null || scriptName.isBlank()) {
            return;
        }
        String posKey = buildKey(level.dimension(), pos);
        long currentTick = level.getGameTime();
        if (Boolean.TRUE.equals(isPlayingByPos.get(posKey))) {
            Marallyzen.LOGGER.info("DictaphoneScriptManager: already playing at pos={} dim={}",
                pos, level.dimension().location());
            return;
        }
        Long playUntil = playLockUntil.get(posKey);
        if (playUntil != null && currentTick <= playUntil) {
            return;
        }
        isPlayingByPos.put(posKey, true);
        Marallyzen.LOGGER.info("DictaphoneScriptManager: playBoundScript script='{}' player='{}' pos={} dim={}",
            scriptName, player.getName().getString(), pos, level.dimension().location());
        playLocalSystemSound(level, player, SYSTEM_SOUND_START);
        java.util.Map.Entry<List<String>, Integer> narrateData =
            DialogScriptLoader.parseInitialNarrateMessages(scriptName, 1);
        List<String> messages = narrateData.getKey();
        int duration = narrateData.getValue();

        List<DialogScriptLoader.AudioData> audioList = DialogScriptLoader.parseInitialAudioCommands(scriptName, 1);
        long totalAudioDurationMs = calculateTotalAudioDurationMs(audioList);
        if (totalAudioDurationMs > 0) {
            int audioTicks = (int) (totalAudioDurationMs / 50);
            if (audioTicks > 0) {
                duration = audioTicks;
            }
        }
        int narrationDurationTicks = duration > 0 ? duration : 100;
        int lockDurationTicks = narrationDurationTicks + (int) DictaphoneEntity.RETURN_DURATION_TICKS;
        lockNarration(player.getUUID(), currentTick, lockDurationTicks);
        playLockUntil.put(posKey, currentTick + lockDurationTicks + 5L);

        if (!audioList.isEmpty()) {
            Marallyzen.LOGGER.info("DictaphoneScriptManager: audio files={} script='{}'",
                audioList.stream().map(DialogScriptLoader.AudioData::filePath).toList(), scriptName);
            playAudioSequentially(level, player, pos, audioList, () ->
                Marallyzen.LOGGER.info("DictaphoneScriptManager: audio playback complete script='{}'", scriptName));
        } else {
            Marallyzen.LOGGER.info("DictaphoneScriptManager: no audio commands script='{}'", scriptName);
        }

        String narratorName = DialogScriptLoader.parseInitialNarratorName(scriptName, 1);
        if (narratorName == null || narratorName.isBlank()) {
            narratorName = DEFAULT_NARRATOR_NAME;
        }
        if (messages != null && !messages.isEmpty()) {
            Marallyzen.LOGGER.info("DictaphoneScriptManager: narrate messages={} durationTicks={} narrator='{}' script='{}'",
                messages.size(), narrationDurationTicks, narratorName, scriptName);
            NpcNarrateHandler.scheduleNarrateMessages(
                player,
                messages,
                narrationDurationTicks,
                narratorName,
                (java.util.UUID) null,
                level,
                null
            );
        } else {
            Marallyzen.LOGGER.info("DictaphoneScriptManager: no narrate messages script='{}'", scriptName);
        }

        long narrationDurationMs = narrationDurationTicks * 50L;
        long totalDurationMs = Math.max(narrationDurationMs, totalAudioDurationMs);
        scheduleStopSound(level, player, posKey, totalDurationMs);
    }

    public static void sendBindNarration(ServerPlayer player, String scriptName) {
        Component colored = Component.literal(scriptName)
            .withStyle(style -> style.withColor(TextColor.fromRgb(0xD48E03)));
        Component message = Component.translatable("narration.marallyzen.dictaphone_bind", colored);
        sendNarration(player, message);
    }

    public static void sendMissingNarration(ServerPlayer player, String scriptName) {
        Component colored = Component.literal(scriptName)
            .withStyle(style -> style.withColor(TextColor.fromRgb(0xD48E03)));
        Component message = Component.translatable("narration.marallyzen.dictaphone_missing", colored);
        sendNarration(player, message);
    }

    private static void sendNarration(ServerPlayer player, Component message) {
        NetworkHelper.sendToPlayer(player, new NarratePacket(message, null, 5, 100, 3));
    }

    private static void playAudioSequentially(ServerLevel level, ServerPlayer player, BlockPos pos,
                                              List<DialogScriptLoader.AudioData> audioList,
                                              Runnable onComplete) {
        if (audioList.isEmpty()) {
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }
        Vec3 dictaphonePos = Vec3.atCenterOf(pos);
        final List<DialogScriptLoader.AudioData> finalAudioList = List.copyOf(audioList);

        Consumer<Integer> playNext = new Consumer<>() {
            @Override
            public void accept(Integer index) {
                if (index >= finalAudioList.size()) {
                    return;
                }
                DialogScriptLoader.AudioData audio = finalAudioList.get(index);
                try {
                    Vec3 audioPosition = resolveAudioPosition(audio, dictaphonePos, player);
                    String filePath = normalizeAudioPath(audio.filePath());
                    long durationMs;
                    if (audio.positional() && audioPosition != null) {
                        Marallyzen.LOGGER.info("DictaphoneScriptManager: play positional audio '{}' at {} radius={}",
                            audio.filePath(), audioPosition, audio.radius());
                        durationMs = MarallyzenAudioService.playDictaphoneAudio(
                            level,
                            audioPosition,
                            filePath,
                            audio.radius(),
                            true,
                            java.util.Collections.singletonList(player)
                        );
                    } else {
                        Marallyzen.LOGGER.info("DictaphoneScriptManager: play global audio '{}' at {}",
                            audio.filePath(), dictaphonePos);
                        durationMs = MarallyzenAudioService.playDictaphoneAudio(
                            level,
                            dictaphonePos,
                            filePath,
                            audio.radius(),
                            false,
                            java.util.Collections.singletonList(player)
                        );
                    }
                    if (index + 1 < finalAudioList.size()) {
                        long delayMs = durationMs > 0
                            ? durationMs
                            : Math.max(3000L, AudioMetadata.getDurationMs(filePath));
                        java.util.Timer timer = new java.util.Timer();
                        timer.schedule(new java.util.TimerTask() {
                            @Override
                            public void run() {
                                level.getServer().execute(() -> accept(index + 1));
                            }
                        }, delayMs + 50);
                    } else if (onComplete != null) {
                        long delayMs = durationMs > 0
                            ? durationMs
                            : Math.max(3000L, AudioMetadata.getDurationMs(filePath));
                        java.util.Timer timer = new java.util.Timer();
                        timer.schedule(new java.util.TimerTask() {
                            @Override
                            public void run() {
                                level.getServer().execute(onComplete);
                            }
                        }, delayMs + 50);
                    }
                } catch (Exception e) {
                    Marallyzen.LOGGER.error("DictaphoneScriptManager: Failed to play audio '{}'", audio.filePath(), e);
                    if (index + 1 < finalAudioList.size()) {
                        level.getServer().execute(() -> accept(index + 1));
                    } else if (onComplete != null) {
                        level.getServer().execute(onComplete);
                    }
                }
            }
        };

        playNext.accept(0);
    }

    private static void playLocalSystemSound(ServerLevel level, ServerPlayer player, String soundId) {
        Marallyzen.LOGGER.info("DictaphoneScriptManager: play system sound '{}' for player='{}'",
            soundId, player.getName().getString());
        if (SYSTEM_SOUND_START.equals(soundId)) {
            player.playNotifySound(MarallyzenSounds.DICTAPHONE_START.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
            return;
        }
        if (SYSTEM_SOUND_STOP.equals(soundId)) {
            player.playNotifySound(MarallyzenSounds.DICTAPHONE_STOP.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
            return;
        }
        Marallyzen.LOGGER.warn("DictaphoneScriptManager: unknown system sound id '{}'", soundId);
    }

    private static void scheduleStopSound(ServerLevel level, ServerPlayer player, String posKey, long totalDurationMs) {
        long delayMs = Math.max(0L, totalDurationMs);
        java.util.Timer timer = new java.util.Timer();
        timer.schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                level.getServer().execute(() -> {
                    playLocalSystemSound(level, player, SYSTEM_SOUND_STOP);
                    isPlayingByPos.remove(posKey);
                    Marallyzen.LOGGER.info("DictaphoneScriptManager: stop sound played for posKey={}", posKey);
                });
            }
        }, delayMs + 50);
    }

    private static Vec3 resolveAudioPosition(DialogScriptLoader.AudioData audio, Vec3 dictaphonePos, ServerPlayer player) {
        if (audio.source().equals("player")) {
            return player.position();
        }
        if (audio.source().startsWith("position:")) {
            String posStr = audio.source().substring("position:".length());
            String[] parts = posStr.split(",");
            if (parts.length == 3) {
                try {
                    double x = Double.parseDouble(parts[0].trim());
                    double y = Double.parseDouble(parts[1].trim());
                    double z = Double.parseDouble(parts[2].trim());
                    return new Vec3(x, y, z);
                } catch (NumberFormatException ignored) {
                    return dictaphonePos;
                }
            }
            return dictaphonePos;
        }
        return dictaphonePos;
    }

    private static long calculateTotalAudioDurationMs(List<DialogScriptLoader.AudioData> audioList) {
        long total = 0;
        for (DialogScriptLoader.AudioData audio : audioList) {
            String path = normalizeAudioPath(audio.filePath());
            long durationMs = AudioMetadata.getDurationMs(path);
            if (durationMs > 0) {
                total += durationMs;
            }
        }
        return total;
    }

    private static String normalizeAudioPath(String filePath) {
        if (filePath == null) {
            return "";
        }
        String trimmed = filePath.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        if (trimmed.contains("/")) {
            return trimmed;
        }
        return "dictophone/" + trimmed;
    }

    public static boolean isNarrationLocked(UUID playerId, long currentTick) {
        Long until = narrationLockUntil.get(playerId);
        return until != null && currentTick <= until;
    }

    public static boolean hasNarrationLock(UUID playerId) {
        return narrationLockUntil.containsKey(playerId);
    }

    public static void clearNarrationLock(UUID playerId) {
        narrationLockUntil.remove(playerId);
    }

    private static void lockNarration(UUID playerId, long currentTick, int durationTicks) {
        if (durationTicks <= 0) {
            return;
        }
        long until = currentTick + durationTicks + 5L;
        narrationLockUntil.put(playerId, until);
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        loadState();
    }

    private static void loadState() {
        if (!Files.exists(STATE_PATH)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(STATE_PATH)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null) {
                return;
            }
            JsonArray array = root.getAsJsonArray("bindings");
            if (array != null) {
                for (int i = 0; i < array.size(); i++) {
                    JsonObject obj = array.get(i).getAsJsonObject();
                    String key = obj.get("key").getAsString();
                    String script = obj.get("script").getAsString();
                    bindings.put(key, script);
                    if (obj.has("protected") && obj.get("protected").getAsBoolean()) {
                        protectedByOp.put(key, true);
                    }
                }
            }
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("DictaphoneScriptManager: failed to read {}", STATE_PATH, e);
        }
    }

    private static void saveState() {
        JsonObject root = new JsonObject();
        JsonArray array = new JsonArray();
        for (Map.Entry<String, String> entry : bindings.entrySet()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("key", entry.getKey());
            obj.addProperty("script", entry.getValue());
            if (Boolean.TRUE.equals(protectedByOp.get(entry.getKey()))) {
                obj.addProperty("protected", true);
            }
            array.add(obj);
        }
        root.add("bindings", array);
        try {
            Files.createDirectories(STATE_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(STATE_PATH)) {
                GSON.toJson(root, writer);
            }
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("DictaphoneScriptManager: failed to write {}", STATE_PATH, e);
        }
    }

    private static String buildKey(ResourceKey<Level> dimension, BlockPos pos) {
        return dimension.location() + "|" + pos.getX() + "|" + pos.getY() + "|" + pos.getZ();
    }

    private static String normalizeScriptName(String name) {
        if (name == null) {
            return null;
        }
        String trimmed = name.trim();
        if (trimmed.endsWith(".dsc")) {
            trimmed = trimmed.substring(0, trimmed.length() - 4);
        }
        return trimmed.isEmpty() ? null : trimmed;
    }
}

