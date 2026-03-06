package neutka.marallys.marallyzen.activity;

import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.trigger.blueprint.TriggerBlueprint;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ActivityRegistry {
    private final ActivityScriptLoader loader = new ActivityScriptLoader();
    private final ActivityTypeLoader typeLoader = new ActivityTypeLoader();
    private final Map<String, ActivityExecutor> executors = new ConcurrentHashMap<>();

    public ActivityScriptLoader loader() {
        return loader;
    }

    public void reload() {
        loader.reload();
        reloadExecutors();
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
            Marallyzen.LOGGER.error("ActivityRegistry: script not found type='{}' id='{}'", ref.type(), ref.script());
            return ActivityResult.failed("Activity script not found: " + ref.type() + "/" + ref.script());
        }
        return executor.execute(context, script, action, onComplete);
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
    }

    private void reloadExecutors() {
        executors.clear();
        typeLoader.reload();
        for (Map.Entry<String, String> entry : typeLoader.types().entrySet()) {
            String type = entry.getKey();
            String className = entry.getValue();
            ActivityExecutor executor = instantiateExecutor(className, type);
            if (executor != null) {
                executors.put(normalize(type), executor);
            }
        }
        if (executors.isEmpty()) {
            Marallyzen.LOGGER.error("ActivityRegistry: no executors registered. Check activity_types.json");
        }
    }

    private ActivityExecutor instantiateExecutor(String className, String type) {
        if (className == null || className.isBlank()) {
            Marallyzen.LOGGER.error("ActivityRegistry: empty executor class for type '{}'", type);
            return null;
        }
        try {
            Class<?> clazz = Class.forName(className);
            if (!ActivityExecutor.class.isAssignableFrom(clazz)) {
                Marallyzen.LOGGER.error("ActivityRegistry: {} does not implement ActivityExecutor for type '{}'", className, type);
                return null;
            }
            return (ActivityExecutor) clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            Marallyzen.LOGGER.error("ActivityRegistry: failed to load executor {} for type '{}'", className, type, e);
            return null;
        }
    }
}
