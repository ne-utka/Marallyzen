package neutka.marallys.marallyzen.trigger.client;

import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.ViewportEvent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class TriggerScreenShakeManager {
    private static final TriggerScreenShakeManager INSTANCE = new TriggerScreenShakeManager();

    private final List<ActiveShake> shakes = new CopyOnWriteArrayList<>();
    private long clientTick;

    private TriggerScreenShakeManager() {
    }

    public static TriggerScreenShakeManager getInstance() {
        return INSTANCE;
    }

    public void push(float intensity, int durationTicks) {
        float clampedIntensity = Mth.clamp(intensity, 0.0F, 2.0F);
        int duration = Math.max(1, durationTicks);
        shakes.add(new ActiveShake(clampedIntensity, duration));
    }

    public void tick() {
        clientTick++;
        shakes.removeIf(shake -> ++shake.ageTicks >= shake.durationTicks);
    }

    public void apply(ViewportEvent.ComputeCameraAngles event) {
        if (event == null || shakes.isEmpty()) {
            return;
        }
        float partialTick = (float) event.getPartialTick();
        float strength = 0.0F;
        for (ActiveShake shake : shakes) {
            float life = 1.0F - ((shake.ageTicks + partialTick) / (float) shake.durationTicks);
            if (life <= 0.0F) {
                continue;
            }
            strength += shake.intensity * life;
        }
        strength = Mth.clamp(strength, 0.0F, 2.0F);
        if (strength <= 0.0001F) {
            return;
        }

        float t = clientTick + partialTick;
        float yawJitter = (Mth.sin(t * 0.85F + 0.3F) + Mth.sin(t * 2.35F + 1.9F) * 0.45F) * strength * 1.25F;
        float pitchJitter = (Mth.sin(t * 1.35F + 2.1F) + Mth.sin(t * 3.65F + 0.2F) * 0.35F) * strength * 0.95F;

        event.setYaw(event.getYaw() + yawJitter);
        event.setPitch(event.getPitch() + pitchJitter);
    }

    public void clear() {
        shakes.clear();
    }

    private static final class ActiveShake {
        private final float intensity;
        private final int durationTicks;
        private int ageTicks;

        private ActiveShake(float intensity, int durationTicks) {
            this.intensity = intensity;
            this.durationTicks = durationTicks;
        }
    }
}
