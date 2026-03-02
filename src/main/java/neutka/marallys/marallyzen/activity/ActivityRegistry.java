package neutka.marallys.marallyzen.activity;

import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.trigger.blueprint.TriggerBlueprint;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ActivityRegistry {
    private final ActivityScriptLoader loader = new ActivityScriptLoader();
    private final Map<String, ActivityExecutor> executors = new ConcurrentHashMap<>();

    public ActivityRegistry() {
        registerDefaults();
    }

    public ActivityScriptLoader loader() {
        return loader;
    }

    public void reload() {
        loader.reload();
    }

    public void register(String type, ActivityExecutor executor) {
        if (type == null || type.isBlank() || executor == null) {
            return;
        }
        executors.put(normalize(type), executor);
    }

    public ActivityExecutor getExecutor(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        return executors.get(normalize(type));
    }

    public Set<String> executorTypes() {
        return Set.copyOf(executors.keySet());
    }

    public ActivityResult dispatch(
            ActivityContext context,
            TriggerBlueprint.Settings.ActionSettings action,
            Runnable onComplete
    ) {
        if (action == null) {
            return ActivityResult.failed("Action is missing.");
        }
        ActivityReference ref = ActivityReference.from(action);
        if (ref == null || ref.type().isBlank() || ref.script().isBlank()) {
            return ActivityResult.failed("Activity type/script is missing.");
        }

        ActivityExecutor executor = getExecutor(ref.type());
        if (executor == null) {
            return ActivityResult.failed("Activity executor not found: " + ref.type());
        }

        ActivityScript script = loader.getScript(ref.type(), ref.script());
        if (script == null) {
            script = ActivityReference.buildFallback(ref, action);
            if (script == null) {
                Marallyzen.LOGGER.warn("ActivityRegistry: script not found type='{}' id='{}'", ref.type(), ref.script());
                return ActivityResult.failed("Activity script not found: " + ref.type() + "/" + ref.script());
            }
        }
        return executor.execute(context, script, action, onComplete);
    }

    private void registerDefaults() {
        register("door", new DoorActivityExecutor());
        register("npc", new NpcActivityExecutor());
    }

    private String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    public record ActivityReference(String type, String script) {
        public static ActivityReference from(TriggerBlueprint.Settings.ActionSettings action) {
            String type = action.resolveActivityType();
            String script = action.resolveActivityScript();
            if (type == null || type.isBlank()) {
                return null;
            }
            if (script == null || script.isBlank()) {
                return null;
            }
            return new ActivityReference(type, script);
        }

        public static ActivityScript buildFallback(ActivityReference ref, TriggerBlueprint.Settings.ActionSettings action) {
            if (ref == null || action == null) {
                return null;
            }
            if ("npc".equalsIgnoreCase(ref.type())) {
                var data = new com.google.gson.JsonObject();
                String replayParam = action.param("replay_id");
                if (replayParam != null && !replayParam.isBlank()) {
                    data.addProperty("replay_id", replayParam.trim().toLowerCase(Locale.ROOT));
                } else if (action.replay() != null && !action.replay().isBlank()) {
                    data.addProperty("replay_id", action.replay().trim().toLowerCase(Locale.ROOT));
                }
                String sceneParam = action.param("scene_id");
                if (sceneParam != null && !sceneParam.isBlank()) {
                    data.addProperty("scene_id", sceneParam.trim().toLowerCase(Locale.ROOT));
                } else if (action.scene() != null && !action.scene().isBlank()) {
                    data.addProperty("scene_id", action.scene().trim().toLowerCase(Locale.ROOT));
                }
                if (data.size() == 0) {
                    return null;
                }
                return new ActivityScript(ref.script(), ref.type(), ActivityScript.CURRENT_FORMAT,
                        ActivityScript.Animation.defaults(),
                        ActivityScript.Physics.defaults(),
                        ActivityScript.Effects.defaults(),
                        data);
            }
            if ("door".equalsIgnoreCase(ref.type())) {
                return new ActivityScript(ref.script(), ref.type(), ActivityScript.CURRENT_FORMAT,
                        ActivityScript.Animation.defaults(),
                        ActivityScript.Physics.defaults(),
                        ActivityScript.Effects.defaults(),
                        new com.google.gson.JsonObject());
            }
            return null;
        }
    }
}
