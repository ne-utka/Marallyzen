package neutka.marallys.marallyzen.goals;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/**
 * Per-tick execution context for goal types.
 */
public record GoalRuntimeContext(
        MinecraftServer server,
        ServerLevel level,
        GoalSavedData savedData,
        GoalZoneManager zoneManager,
        long gameTime
) {
}
