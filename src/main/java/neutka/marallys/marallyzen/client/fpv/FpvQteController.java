package neutka.marallys.marallyzen.client.fpv;

import net.minecraft.resources.Identifier;

public final class FpvQteController {
    private static final float LEVER_SHAKE_LOOP_SECONDS = 0.35f;
    private static final float VALVE_SPIN_LOOP_SECONDS = 0.45f;

    private final FpvPhaseController phases = new FpvPhaseController();
    private FpvPhaseController.Phase currentPhase = FpvPhaseController.Phase.IDLE;
    private float phaseElapsedSeconds;
    private float qteElapsedSeconds;
    private long lastTickNanos = -1L;

    public void tick(Identifier emoteId, boolean fpvActive) {
        long now = System.nanoTime();
        float deltaSeconds = 0.0f;
        if (lastTickNanos > 0L) {
            deltaSeconds = (float) ((now - lastTickNanos) / 1_000_000_000.0);
            if (deltaSeconds < 0.0f) {
                deltaSeconds = 0.0f;
            } else if (deltaSeconds > 0.25f) {
                deltaSeconds = 0.25f;
            }
        }
        lastTickNanos = now;

        FpvQteState.Snapshot qte = FpvQteState.snapshot();
        if (qte.active() || qte.finishing()) {
            qteElapsedSeconds += deltaSeconds;
        } else {
            qteElapsedSeconds = 0.0f;
        }

        phases.update(emoteId, fpvActive);
        FpvPhaseController.Phase nextPhase = phases.sample(0.0f).phase();
        if (nextPhase != currentPhase) {
            currentPhase = nextPhase;
            phaseElapsedSeconds = 0.0f;
        } else {
            phaseElapsedSeconds += deltaSeconds;
        }
        FpvQteState.endFrame();
    }

    public FpvPhaseController.Sample sample(float timeSeconds) {
        float phaseTime = switch (currentPhase) {
            case LEVER_SHAKE -> wrapTime(qteElapsedSeconds, LEVER_SHAKE_LOOP_SECONDS);
            case VALVE_SPIN -> wrapTime(qteElapsedSeconds, VALVE_SPIN_LOOP_SECONDS);
            default -> phaseElapsedSeconds;
        };
        return phases.sample(phaseTime);
    }

    private static float wrapTime(float timeSeconds, float periodSeconds) {
        if (periodSeconds <= 0.0f) {
            return 0.0f;
        }
        return timeSeconds % periodSeconds;
    }
}
