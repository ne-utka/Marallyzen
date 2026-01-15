package neutka.marallys.marallyzen.quest;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import neutka.marallys.marallyzen.Marallyzen;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class QuestRewardHandler {

    private static final AtomicBoolean RELOAD_IN_PROGRESS = new AtomicBoolean(false);

    public void applyRewards(ServerPlayer player, QuestPlayerData data, QuestDefinition definition) {
        if (definition == null || definition.rewards() == null) {
            return;
        }
        for (QuestRewardDef reward : definition.rewards()) {
            applyReward(player, data, reward);
        }
    }

    private void applyReward(ServerPlayer player, QuestPlayerData data, QuestRewardDef reward) {
        if (reward == null || reward.type() == null) {
            return;
        }
        JsonObject obj = reward.data();
        String type = reward.type();
        switch (type) {
            case "item" -> giveItem(player, obj);
            case "xp" -> giveXp(player, obj);
            case "command" -> runCommand(player, obj);
            case "message" -> sendMessage(player, obj);
            case "unlock_trade" -> unlockTrade(data, obj);
            case "advancement" -> grantAdvancement(player, obj, false);
            default -> {
            }
        }
    }

    private void giveItem(ServerPlayer player, JsonObject obj) {
        String itemId = QuestJsonUtils.getString(obj, "item", "");
        int count = QuestJsonUtils.getInt(obj, "count", 1);
        if (itemId.isBlank() || count <= 0) {
            return;
        }
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
        if (item == null) {
            Marallyzen.LOGGER.warn("QuestRewardHandler: item not found {}", itemId);
            return;
        }
        ItemStack stack = new ItemStack(item, count);
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
    }

    private void giveXp(ServerPlayer player, JsonObject obj) {
        int amount = QuestJsonUtils.getInt(obj, "amount", 0);
        if (amount > 0) {
            player.giveExperiencePoints(amount);
        }
    }

    private void runCommand(ServerPlayer player, JsonObject obj) {
        String command = QuestJsonUtils.getString(obj, "command", "");
        if (command.isBlank() || player.getServer() == null) {
            return;
        }
        String finalCommand = command.replace("{player}", player.getName().getString());
        player.getServer().getCommands().performPrefixedCommand(player.getServer().createCommandSourceStack(), finalCommand);
    }

    private void sendMessage(ServerPlayer player, JsonObject obj) {
        String text = QuestJsonUtils.getString(obj, "text", "");
        if (!text.isBlank()) {
            player.sendSystemMessage(Component.literal(text));
        }
    }

    private void unlockTrade(QuestPlayerData data, JsonObject obj) {
        String npcId = QuestJsonUtils.getString(obj, "npcId", "");
        if (!npcId.isBlank()) {
            data.setNpcTradeUnlocked(npcId, true);
        }
    }

    private void grantAdvancement(ServerPlayer player, JsonObject obj, boolean triedReload) {
        String id = QuestJsonUtils.getString(obj, "id", "");
        if (id.isBlank() || player.getServer() == null) {
            return;
        }
        String resolvedId = id.contains(":") ? id : Marallyzen.MODID + ":" + id;
        grantRootIfNeeded(player, resolvedId);
        var advancement = findAdvancement(player, resolvedId);
        if (advancement == null) {
            if (!triedReload && RELOAD_IN_PROGRESS.compareAndSet(false, true)) {
                var server = player.getServer();
                Marallyzen.LOGGER.warn("QuestRewardHandler: advancement not found {}, reloading resources", resolvedId);
                server.reloadResources(server.getPackRepository().getSelectedIds())
                        .whenComplete((ignored, error) -> {
                            RELOAD_IN_PROGRESS.set(false);
                            if (error != null) {
                                Marallyzen.LOGGER.warn("QuestRewardHandler: resource reload failed", error);
                                return;
                            }
                            server.execute(() -> grantAdvancement(player, obj, true));
                        });
                return;
            }
            Marallyzen.LOGGER.warn("QuestRewardHandler: advancement not found {}", resolvedId);
            return;
        }
        var progress = player.getAdvancements().getOrStartProgress(advancement);
        if (progress.isDone()) {
            Marallyzen.LOGGER.info("QuestRewardHandler: advancement already completed {}", resolvedId);
            return;
        }
        int remainingCount = 0;
        for (String ignored : progress.getRemainingCriteria()) {
            remainingCount++;
        }
        Marallyzen.LOGGER.info(
                "QuestRewardHandler: awarding advancement {} remainingCriteria={}",
                resolvedId,
                remainingCount
        );
        int granted = 0;
        for (String criterion : progress.getRemainingCriteria()) {
            if (player.getAdvancements().award(advancement, criterion)) {
                granted++;
            }
        }
        Marallyzen.LOGGER.info(
                "QuestRewardHandler: advancement {} grantedCriteria={} done={}",
                resolvedId,
                granted,
                player.getAdvancements().getOrStartProgress(advancement).isDone()
        );
    }

    private void grantRootIfNeeded(ServerPlayer player, String resolvedId) {
        if (!resolvedId.startsWith(Marallyzen.MODID + ":")) {
            return;
        }
        if (resolvedId.equals(Marallyzen.MODID + ":root")) {
            return;
        }
        var root = findAdvancement(player, Marallyzen.MODID + ":root");
        if (root == null) {
            return;
        }
        var progress = player.getAdvancements().getOrStartProgress(root);
        if (progress.isDone()) {
            return;
        }
        for (String criterion : progress.getRemainingCriteria()) {
            player.getAdvancements().award(root, criterion);
        }
        Marallyzen.LOGGER.info("QuestRewardHandler: root advancement granted for {}", player.getGameProfile().getName());
    }

    private net.minecraft.advancements.AdvancementHolder findAdvancement(ServerPlayer player, String resolvedId) {
        Set<String> candidates = new LinkedHashSet<>();
        candidates.add(resolvedId);

        if (resolvedId.startsWith(Marallyzen.MODID + ":")) {
            String path = resolvedId.substring(Marallyzen.MODID.length() + 1);
            if (!path.startsWith("quest/")) {
                candidates.add(Marallyzen.MODID + ":quest/" + path);
            }
        } else if (!resolvedId.contains(":")) {
            candidates.add(Marallyzen.MODID + ":" + resolvedId);
            candidates.add(Marallyzen.MODID + ":quest/" + resolvedId);
        }

        List<ResourceLocation> locations = new ArrayList<>();
        for (String candidate : candidates) {
            ResourceLocation location = safeParseLocation(candidate);
            if (location != null) {
                locations.add(location);
            }
        }

        var server = player.getServer();
        for (ResourceLocation location : locations) {
            var advancement = server.getAdvancements().get(location);
            if (advancement != null) {
                return advancement;
            }
        }
        return null;
    }

    private ResourceLocation safeParseLocation(String id) {
        try {
            return ResourceLocation.parse(id);
        } catch (Exception ignored) {
            return null;
        }
    }
}
