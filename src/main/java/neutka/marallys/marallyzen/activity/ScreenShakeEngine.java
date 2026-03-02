package neutka.marallys.marallyzen.activity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import neutka.marallys.marallyzen.network.NetworkHelper;
import neutka.marallys.marallyzen.network.TriggerScreenShakePacket;

public final class ScreenShakeEngine {
    private ScreenShakeEngine() {
    }

    public static void broadcast(ServerLevel level, Vec3 center, ActivityScript.ShakeSettings shake) {
        if (level == null || center == null || shake == null) {
            return;
        }
        int radius = Math.max(0, shake.radius());
        float maxIntensity = Math.max(0.0F, shake.maxIntensity());
        if (radius <= 0 || maxIntensity <= 0.0F) {
            return;
        }
        String falloff = shake.falloff() == null ? "linear" : shake.falloff().trim().toLowerCase();
        int duration = Math.max(1, shake.durationTicks());

        double radiusSq = radius * radius;
        for (ServerPlayer player : level.players()) {
            double distSq = player.distanceToSqr(center);
            if (distSq > radiusSq) {
                continue;
            }
            double dist = Math.sqrt(distSq);
            float normalized = 1.0F - (float) (dist / radius);
            float factor = switch (falloff) {
                case "smooth", "quadratic" -> normalized * normalized;
                default -> normalized;
            };
            float intensity = maxIntensity * factor;
            if (intensity <= 0.0F) {
                continue;
            }
            NetworkHelper.sendToPlayer(player, new TriggerScreenShakePacket(intensity, duration));
        }
    }
}
