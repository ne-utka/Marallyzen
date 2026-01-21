package neutka.marallys.marallyzen.quest;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import neutka.marallys.marallyzen.DenizenService;
import neutka.marallys.marallyzen.audio.AudioMetadata;
import neutka.marallys.marallyzen.npc.DialogScriptLoader;
import neutka.marallys.marallyzen.npc.NpcNarrateHandler;
import neutka.marallys.marallyzen.network.NetworkHelper;
import neutka.marallys.marallyzen.network.QuestAudioPacket;
import neutka.marallys.marallyzen.network.QuestNarratePacket;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.AbstractMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class QuestNarrationScriptManager {
    private static final int DEFAULT_DURATION_TICKS = 100;
    private static final long FALLBACK_AUDIO_MS = 3000L;
    private static final String DEFAULT_NARRATOR = "*****";

    private QuestNarrationScriptManager() {
    }

    public static void playStepNarration(ServerPlayer player, String questId, int stepNumber) {
        if (player == null || questId == null || questId.isBlank() || stepNumber <= 0) {
            return;
        }
        String scriptName = "quest_" + questId + "_narration";
        File scriptFile = new File(DenizenService.getScriptsFolder(), scriptName + ".dsc");
        if (!scriptFile.exists()) {
            return;
        }
        AbstractMap.Entry<List<String>, Integer> narrateData =
            DialogScriptLoader.parseInitialNarrateMessages(scriptName, stepNumber);
        List<String> messages = narrateData.getKey();
        int durationTicks = narrateData.getValue();
        if (durationTicks <= 0) {
            durationTicks = DEFAULT_DURATION_TICKS;
        }

        long startDelayMs = parseStartDelayMs(scriptName, stepNumber);
        long gapMs = parseGapMs(scriptName, stepNumber);
        List<DialogScriptLoader.AudioData> audioList =
            DialogScriptLoader.parseInitialAudioCommands(scriptName, stepNumber);
        long totalAudioMs = calculateTotalAudioDurationMs(audioList, gapMs);
        if (totalAudioMs > 0) {
            durationTicks = (int) (totalAudioMs / 50L);
        }
        if (gapMs > 0 && messages != null && messages.size() > 1) {
            long gapTicks = gapMs / 50L;
            durationTicks += (int) (gapTicks * (messages.size() - 1));
        }

        String narrator = DialogScriptLoader.parseInitialNarratorName(scriptName, stepNumber);
        if (narrator == null || narrator.isBlank()) {
            narrator = DEFAULT_NARRATOR;
        }

        ServerLevel level = player.serverLevel();
        if (!audioList.isEmpty() && messages != null && messages.size() == audioList.size()) {
            scheduleNarrationWithAudio(level, player, narrator, messages, audioList, startDelayMs, gapMs);
            return;
        }
        List<DialogScriptLoader.AudioData> finalAudioList = audioList != null ? List.copyOf(audioList) : List.of();
        List<String> finalMessages = messages != null ? List.copyOf(messages) : List.of();
        int finalDurationTicks = durationTicks;
        String finalNarrator = narrator;
        Runnable start = () -> {
            if (!finalAudioList.isEmpty()) {
                playAudioSequentially(level, player, finalAudioList, gapMs);
            }
            if (!finalMessages.isEmpty() && finalDurationTicks > 0) {
                NpcNarrateHandler.scheduleNarrateMessages(player, finalMessages, finalDurationTicks, finalNarrator, level, null);
            }
        };
        if (startDelayMs > 0) {
            java.util.Timer timer = new java.util.Timer();
            timer.schedule(new java.util.TimerTask() {
                @Override
                public void run() {
                    level.getServer().execute(start);
                }
            }, startDelayMs);
        } else {
            start.run();
        }
    }

    private static void playAudioSequentially(ServerLevel level, ServerPlayer player, List<DialogScriptLoader.AudioData> audioList, long gapMs) {
        if (audioList == null || audioList.isEmpty()) {
            return;
        }
        List<DialogScriptLoader.AudioData> finalAudioList = List.copyOf(audioList);
        Consumer<Integer> playNext = new Consumer<>() {
            @Override
            public void accept(Integer index) {
                if (index >= finalAudioList.size()) {
                    return;
                }
                DialogScriptLoader.AudioData audio = finalAudioList.get(index);
                NetworkHelper.sendToPlayer(player, new QuestAudioPacket(audio.filePath(), 1.0f));
                long metadataMs = AudioMetadata.getDurationMs(audio.filePath());
                long durationMs = metadataMs > 0 ? metadataMs : FALLBACK_AUDIO_MS;
                if (index + 1 < finalAudioList.size()) {
                    long delayMs = durationMs + Math.max(0L, gapMs);
                    java.util.Timer timer = new java.util.Timer();
                    timer.schedule(new java.util.TimerTask() {
                        @Override
                        public void run() {
                            level.getServer().execute(() -> accept(index + 1));
                        }
                    }, delayMs + 50);
                }
            }
        };
        playNext.accept(0);
    }

    private static long calculateTotalAudioDurationMs(List<DialogScriptLoader.AudioData> audioList, long gapMs) {
        if (audioList == null || audioList.isEmpty()) {
            return 0L;
        }
        long total = 0L;
        for (int i = 0; i < audioList.size(); i++) {
            DialogScriptLoader.AudioData audio = audioList.get(i);
            long durationMs = AudioMetadata.getDurationMs(audio.filePath());
            if (durationMs > 0) {
                total += durationMs;
            } else {
                total += FALLBACK_AUDIO_MS;
            }
            if (i < audioList.size() - 1) {
                total += Math.max(0L, gapMs);
            }
        }
        return total;
    }

    private static long parseGapMs(String scriptName, int stepNumber) {
        try {
            File scriptsFolder = DenizenService.getScriptsFolder();
            File scriptFile = new File(scriptsFolder, scriptName + ".dsc");
            if (!scriptFile.exists()) {
                return 0L;
            }
            String content = readFile(scriptFile);
            Pattern stepPattern = Pattern.compile("\\s+" + stepNumber + "\\s*:\\s*\\n(.*?)(?=\\s+\\d+\\s*:|\\s+requirements:|\\Z)", Pattern.DOTALL);
            Matcher stepMatcher = stepPattern.matcher(content);
            if (!stepMatcher.find()) {
                return 0L;
            }
            String stepContent = stepMatcher.group(1);
            Pattern gapPattern = Pattern.compile("(?:-\\s+)?define\\s+gap(?:_ms)?:\\s*<\\[([0-9]+)(ms|s)?\\]>");
            Matcher gapMatcher = gapPattern.matcher(stepContent);
            if (!gapMatcher.find()) {
                return 0L;
            }
            long value = Long.parseLong(gapMatcher.group(1));
            String unit = gapMatcher.group(2);
            if ("s".equals(unit)) {
                return value * 1000L;
            }
            if ("ms".equals(unit)) {
                return value;
            }
            return value * 50L;
        } catch (Exception e) {
            return 0L;
        }
    }

    private static void scheduleNarrationWithAudio(ServerLevel level,
                                                   ServerPlayer player,
                                                   String narrator,
                                                   List<String> messages,
                                                   List<DialogScriptLoader.AudioData> audioList,
                                                   long startDelayMs,
                                                   long gapMs) {
        int fadeInTicks = 5;
        int fadeOutTicks = 3;
        long gapTicks = Math.max(0L, gapMs / 50L);
        long cumulativeDelayTicks = 0L;
        for (int i = 0; i < messages.size(); i++) {
            DialogScriptLoader.AudioData audio = audioList.get(i);
            long durationMs = AudioMetadata.getDurationMs(audio.filePath());
            if (durationMs <= 0) {
                durationMs = FALLBACK_AUDIO_MS;
            }
            int durationTicks = (int) Math.max(1L, durationMs / 50L);
            int minTicks = fadeInTicks + fadeOutTicks + 1;
            if (durationTicks < minTicks) {
                durationTicks = minTicks;
            }
            int stayTicks = Math.max(0, durationTicks - fadeInTicks - fadeOutTicks);
            Component formatted = buildNarrationComponent(narrator, messages.get(i));
            long delayMs = startDelayMs + cumulativeDelayTicks * 50L;
            java.util.Timer timer = new java.util.Timer();
            int finalDurationTicks = durationTicks;
            int finalStayTicks = stayTicks;
            timer.schedule(new java.util.TimerTask() {
                @Override
                public void run() {
                    level.getServer().execute(() -> {
                        NetworkHelper.sendToPlayer(player, new QuestAudioPacket(audio.filePath(), 1.0f));
                        NetworkHelper.sendToPlayer(player, new QuestNarratePacket(formatted, fadeInTicks, finalStayTicks, fadeOutTicks));
                    });
                }
            }, delayMs);
            cumulativeDelayTicks += finalDurationTicks + gapTicks;
        }
    }

    private static Component buildNarrationComponent(String narrator, String message) {
        String name = narrator != null ? narrator : DEFAULT_NARRATOR;
        Component nameComponent = Component.literal(name).withStyle(style ->
            style.withColor(TextColor.fromRgb(0xD48E03)));
        Component separator = Component.literal(" » ").withStyle(style ->
            style.withColor(TextColor.fromRgb(0x555555)));
        Component text = Component.literal(message != null ? message : "")
            .withStyle(style -> style.withColor(TextColor.fromRgb(0xFFFFFF)));
        return nameComponent.copy().append(separator).append(text);
    }

    private static long parseStartDelayMs(String scriptName, int stepNumber) {
        try {
            File scriptsFolder = DenizenService.getScriptsFolder();
            File scriptFile = new File(scriptsFolder, scriptName + ".dsc");
            if (!scriptFile.exists()) {
                return 0L;
            }
            String content = readFile(scriptFile);
            Pattern stepPattern = Pattern.compile("\\s+" + stepNumber + "\\s*:\\s*\\n(.*?)(?=\\s+\\d+\\s*:|\\s+requirements:|\\Z)", Pattern.DOTALL);
            Matcher stepMatcher = stepPattern.matcher(content);
            if (!stepMatcher.find()) {
                return 0L;
            }
            String stepContent = stepMatcher.group(1);
            Pattern delayPattern = Pattern.compile("(?:-\\s+)?define\\s+(?:start_delay|first_delay):\\s*<\\[([0-9]+)(ms|s)?\\]>");
            Matcher delayMatcher = delayPattern.matcher(stepContent);
            if (!delayMatcher.find()) {
                return 0L;
            }
            long value = Long.parseLong(delayMatcher.group(1));
            String unit = delayMatcher.group(2);
            if ("s".equals(unit)) {
                return value * 1000L;
            }
            if ("ms".equals(unit)) {
                return value;
            }
            return value * 50L;
        } catch (Exception e) {
            return 0L;
        }
    }

    private static String readFile(File file) throws Exception {
        try (FileInputStream fis = new FileInputStream(file);
             Scanner scanner = new Scanner(fis, StandardCharsets.UTF_8.name())) {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }
    }
}
