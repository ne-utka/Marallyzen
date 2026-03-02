package neutka.marallys.marallyzen.activity;

import net.minecraft.server.MinecraftServer;
import neutka.marallys.marallyzen.door.DoorEngine;
import neutka.marallys.marallyzen.trigger.blueprint.TriggerBlueprint;

public final class DoorActivityExecutor implements ActivityExecutor {
    @Override
    public ActivityResult execute(ActivityContext context, ActivityScript script, TriggerBlueprint.Settings.ActionSettings action, Runnable onComplete) {
        if (context == null || script == null || action == null) {
            return ActivityResult.failed("Invalid activity context.");
        }
        MinecraftServer server = context.server();
        if (server == null) {
            return ActivityResult.failed("Server unavailable.");
        }
        String doorId = action.param("door_id");
        if (doorId == null || doorId.isBlank()) {
            doorId = action.param("door");
        }
        if (doorId == null || doorId.isBlank()) {
            doorId = action.resolveActivityScript();
        }
        if (doorId == null || doorId.isBlank()) {
            return ActivityResult.failed("door_id is required.");
        }
        boolean ok = DoorEngine.getInstance().activate(server, doorId, script);
        if (!ok) {
            return ActivityResult.failed("Failed to activate door: " + doorId);
        }
        return ActivityResult.ok(false);
    }
}
