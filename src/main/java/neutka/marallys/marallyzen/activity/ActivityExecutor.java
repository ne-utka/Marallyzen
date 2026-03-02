package neutka.marallys.marallyzen.activity;

import neutka.marallys.marallyzen.trigger.blueprint.TriggerBlueprint;

public interface ActivityExecutor {
    ActivityResult execute(ActivityContext context, ActivityScript script, TriggerBlueprint.Settings.ActionSettings action, Runnable onComplete);
}
