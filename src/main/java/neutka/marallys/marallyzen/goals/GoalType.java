package neutka.marallys.marallyzen.goals;

import java.util.Map;

/**
 * Server-side goal behavior contract.
 */
public interface GoalType {
    void tick(GoalRuntimeContext context, GoalInstance instance);

    default void collectDisplayVariables(GoalRuntimeContext context, GoalInstance instance, Map<String, String> output) {
    }
}
