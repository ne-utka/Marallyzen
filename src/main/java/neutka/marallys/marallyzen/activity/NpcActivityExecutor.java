package neutka.marallys.marallyzen.activity;

import net.minecraft.server.level.ServerPlayer;
import neutka.marallys.marallyzen.denizen.commands.CommandScriptRegistry;
import neutka.marallys.marallyzen.npc.replay.NpcReplayEngine;
import neutka.marallys.marallyzen.trigger.blueprint.TriggerBlueprint;

public final class NpcActivityExecutor implements ActivityExecutor {
    @Override
    public ActivityResult execute(ActivityContext context, ActivityScript script, TriggerBlueprint.Settings.ActionSettings action, Runnable onComplete) {
        if (context == null || script == null || action == null) {
            return ActivityResult.failed("Invalid activity context.");
        }
        String npcId = resolveNpcId(script, action);
        if (npcId == null || npcId.isBlank()) {
            return ActivityResult.failed("npc_id is required.");
        }
        String replayId = firstNonBlank(
                script.getString("replay_id", ""),
                script.getString("replay", "")
        );
        String sceneId = firstNonBlank(
                script.getString("scene_id", ""),
                script.getString("scene", ""),
                script.getString("script", "")
        );
        if (!replayId.isBlank()) {
            NpcReplayEngine.Result result = NpcReplayEngine.play(npcId, replayId, onComplete);
            if (!result.success()) {
                return ActivityResult.failed(result.message());
            }
            return ActivityResult.ok(true);
        }

        if (!sceneId.isBlank()) {
            ServerPlayer player = context.player();
            if (player == null) {
                return ActivityResult.failed("Scene requires player context.");
            }
            String args = resolveArgs(script, action);
            boolean ok = CommandScriptRegistry.executeCommandScript(sceneId, player, args);
            if (!ok) {
                return ActivityResult.failed("NPC scene script not found: " + sceneId);
            }
            return ActivityResult.ok(false);
        }

        return ActivityResult.failed("NPC activity requires replay_id or scene_id.");
    }

    private String resolveNpcId(ActivityScript script, TriggerBlueprint.Settings.ActionSettings action) {
        String npc = action.param("npc_id");
        if (npc == null || npc.isBlank()) {
            npc = action.param("npc");
        }
        if (npc == null || npc.isBlank()) {
            npc = script.getString("npc_id", "");
        }
        if (npc == null || npc.isBlank()) {
            npc = script.getString("npc", "");
        }
        if (npc == null || npc.isBlank()) {
            npc = action.npc();
        }
        return npc == null ? "" : npc.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private String resolveArgs(ActivityScript script, TriggerBlueprint.Settings.ActionSettings action) {
        String args = action.param("scene_args");
        if (args == null || args.isBlank()) {
            args = action.param("args");
        }
        if (args == null || args.isBlank()) {
            args = action.activityArgs();
        }
        if (args == null || args.isBlank()) {
            args = script.getString("args", "");
        }
        return args == null ? "" : args;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
