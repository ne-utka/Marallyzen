package neutka.marallys.marallyzen.ai;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.network.DialogStateChangedPacket;
import neutka.marallys.marallyzen.network.NetworkHelper;
import neutka.marallys.marallyzen.network.OpenDialogPacket;
import neutka.marallys.marallyzen.npc.NpcData;
import neutka.marallys.marallyzen.npc.NpcNarrateHandler;
import neutka.marallys.marallyzen.npc.NpcProximityHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NpcAiManager {

    private record ConversationKey(UUID playerId, String npcId) { }

    private static class Conversation {
        final List<NpcAiService.ChatMessage> history = new ArrayList<>();
        Map<String, String> lastOptions = Collections.emptyMap();
        boolean inFlight;
        long lastUpdatedMs;
    }

    private static final Map<ConversationKey, Conversation> conversations = new ConcurrentHashMap<>();

    public static boolean isAiEnabled(NpcData npcData) {
        return npcData != null && npcData.getAiSettings() != null && npcData.getAiSettings().isEnabled();
    }

    public static void openInitialDialog(ServerPlayer player, Entity npcEntity, NpcData npcData) {
        if (!isAiEnabled(npcData)) {
            return;
        }
        String userMessage = "Player starts a conversation.";
        requestAndOpen(player, npcEntity, npcData, userMessage, null);
    }

    public static void handleChoice(ServerPlayer player, Entity npcEntity, NpcData npcData, String buttonId) {
        if (!isAiEnabled(npcData)) {
            return;
        }
        Conversation convo = getConversation(player, npcData);
        String choiceText = convo.lastOptions.getOrDefault(buttonId, buttonId);
        String userMessage = "Player choice: " + choiceText;
        requestAndOpen(player, npcEntity, npcData, userMessage, choiceText);
    }

    public static void onDialogClosed(ServerPlayer player, UUID npcEntityUuid) {
        // Keep conversation memory, just clear in-flight flag for safety.
        if (player == null || npcEntityUuid == null) {
            return;
        }
        for (Map.Entry<ConversationKey, Conversation> entry : conversations.entrySet()) {
            if (entry.getKey().playerId().equals(player.getUUID())) {
                entry.getValue().inFlight = false;
            }
        }
    }

    public static void clearPlayer(UUID playerId) {
        conversations.entrySet().removeIf(entry -> entry.getKey().playerId().equals(playerId));
    }

    public static void reload() {
        NpcAiGlobalConfig.reload();
    }

    private static Conversation getConversation(ServerPlayer player, NpcData npcData) {
        ConversationKey key = new ConversationKey(player.getUUID(), npcData.getId());
        return conversations.computeIfAbsent(key, ignored -> new Conversation());
    }

    private static void requestAndOpen(ServerPlayer player, Entity npcEntity, NpcData npcData, String userMessage, String choiceTextForChat) {
        if (!(npcEntity.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Conversation convo = getConversation(player, npcData);
        synchronized (convo) {
            if (convo.inFlight) {
                Marallyzen.LOGGER.debug("AI dialog already in flight for player {} npc {}", player.getName().getString(), npcData.getId());
                return;
            }
            convo.inFlight = true;
        }

        NpcAiGlobalConfig global = NpcAiGlobalConfig.get();
        if (!global.enabled) {
            sendAiError(player, "AI is disabled in config.");
            setInFlight(convo, false);
            return;
        }

        List<NpcAiService.ChatMessage> messages = new ArrayList<>();
        String systemPrompt = buildSystemPrompt(player, npcData);
        messages.add(new NpcAiService.ChatMessage("system", systemPrompt));

        int memoryTurns = resolveMemoryTurns(npcData, global);
        addHistory(messages, convo.history, memoryTurns);
        messages.add(new NpcAiService.ChatMessage("user", userMessage));

        NetworkHelper.sendToPlayer(player, new DialogStateChangedPacket(neutka.marallys.marallyzen.client.gui.DialogState.EXECUTING));
        NpcProximityHandler.setPlayerDialogClosed(player.getUUID());

        NpcAiRequestOptions requestOptions = new NpcAiRequestOptions(
                npcData.getAiSettings().getModel(),
                npcData.getAiSettings().getTemperature(),
                npcData.getAiSettings().getMaxTokens()
        );

        NpcAiService.generateResponse(messages, requestOptions)
                .whenComplete((response, error) -> {
                    serverLevel.getServer().execute(() -> {
                        if (error != null) {
                            Marallyzen.LOGGER.warn("AI dialog failed for npc {}: {}", npcData.getId(), error.getMessage());
                            sendAiError(player, "AI dialog failed. Check server logs.");
                            setInFlight(convo, false);
                            return;
                        }
                        if (response == null) {
                            sendAiError(player, "AI dialog returned no response.");
                            setInFlight(convo, false);
                            return;
                        }

                        String reply = response.reply() != null ? response.reply().trim() : "";
                        List<String> options = response.options() != null ? response.options() : Collections.emptyList();
                        if (reply.isEmpty()) {
                            reply = "Мне нечего добавить.";
                        }

                        String npcName = npcData.getName() != null ? npcData.getName() : npcData.getId();
                        Map<String, String> optionsMap = buildOptionsMap(options, resolveOptionCount(npcData, global));
                        convo.lastOptions = optionsMap;
                        convo.lastUpdatedMs = System.currentTimeMillis();
                        appendHistory(convo, userMessage, reply, memoryTurns);

                        List<String> narrate = Collections.singletonList(reply);
                        int durationTicks = estimateDurationTicks(reply);

                        Runnable openDialog = () -> {
                            NetworkHelper.sendToPlayer(player, new OpenDialogPacket(
                                    npcData.getId(),
                                    npcName,
                                    optionsMap,
                                    npcEntity.getUUID()
                            ));
                            NpcProximityHandler.setPlayerDialogOpen(player.getUUID(), npcEntity.getUUID(), 1);
                            NpcProximityHandler.sendProximityForOpenDialog(player, npcEntity, npcData, serverLevel);
                            setInFlight(convo, false);
                        };

                        NpcNarrateHandler.scheduleNarrateMessages(
                                player,
                                narrate,
                                durationTicks,
                                npcName,
                                npcEntity.getUUID(),
                                serverLevel,
                                openDialog
                        );
                    });
                });
    }

    private static void addHistory(List<NpcAiService.ChatMessage> target, List<NpcAiService.ChatMessage> history, int memoryTurns) {
        if (history.isEmpty() || memoryTurns <= 0) {
            return;
        }
        int maxMessages = memoryTurns * 2;
        int start = Math.max(0, history.size() - maxMessages);
        for (int i = start; i < history.size(); i++) {
            target.add(history.get(i));
        }
    }

    private static void appendHistory(Conversation convo, String userMessage, String reply, int memoryTurns) {
        if (memoryTurns <= 0) {
            convo.history.clear();
            return;
        }
        convo.history.add(new NpcAiService.ChatMessage("user", userMessage));
        convo.history.add(new NpcAiService.ChatMessage("assistant", reply));
        int maxMessages = memoryTurns * 2;
        if (convo.history.size() > maxMessages) {
            int removeCount = convo.history.size() - maxMessages;
            convo.history.subList(0, removeCount).clear();
        }
    }

    private static String buildSystemPrompt(ServerPlayer player, NpcData npcData) {
        NpcAiGlobalConfig global = NpcAiGlobalConfig.get();
        String npcName = npcData.getName() != null ? npcData.getName() : npcData.getId();
        StringBuilder prompt = new StringBuilder();
        if (global.systemPrompt != null && !global.systemPrompt.isBlank()) {
            prompt.append(global.systemPrompt.trim());
        }
        String npcPrompt = npcData.getAiSettings().getSystemPrompt();
        if (npcPrompt != null && !npcPrompt.isBlank()) {
            if (!prompt.isEmpty()) {
                prompt.append(" ");
            }
            prompt.append(npcPrompt.trim());
        }
        prompt.append(" NPC name: ").append(npcName).append(".");
        prompt.append(" Player name: ").append(player.getName().getString()).append(".");
        prompt.append(" Отвечай строго в JSON.");
        return prompt.toString();
    }

    private static Map<String, String> buildOptionsMap(List<String> options, int optionCount) {
        Map<String, String> map = new LinkedHashMap<>();
        int count = Math.min(optionCount, options.size());
        for (int i = 0; i < count; i++) {
            map.put("opt_" + (i + 1), options.get(i));
        }
        while (map.size() < Math.max(1, optionCount)) {
            int idx = map.size() + 1;
            map.put("opt_" + idx, idx == 1 ? "Continue" : "Goodbye");
            if (map.size() >= optionCount) {
                break;
            }
        }
        return map;
    }

    private static int resolveOptionCount(NpcData npcData, NpcAiGlobalConfig global) {
        Integer count = npcData.getAiSettings().getOptionCount();
        return count != null && count > 0 ? count : Math.max(1, global.defaultOptionCount);
    }

    private static int resolveMemoryTurns(NpcData npcData, NpcAiGlobalConfig global) {
        Integer memory = npcData.getAiSettings().getMemoryTurns();
        return memory != null && memory >= 0 ? memory : Math.max(0, global.defaultMemoryTurns);
    }

    private static int estimateDurationTicks(String text) {
        int chars = text != null ? text.length() : 0;
        int base = 60;
        int extra = chars / 2;
        return Math.min(20 * 30, base + extra);
    }

    private static void sendAiError(ServerPlayer player, String message) {
        if (player != null) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(message));
            NetworkHelper.sendToPlayer(
                    player,
                    new DialogStateChangedPacket(neutka.marallys.marallyzen.client.gui.DialogState.CLOSED)
            );
        }
    }

    private static void setInFlight(Conversation convo, boolean value) {
        synchronized (convo) {
            convo.inFlight = value;
        }
    }
}


