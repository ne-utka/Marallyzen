package neutka.marallys.marallyzen.client.fpv;

import net.minecraft.resources.Identifier;

public final class FpvPhaseController {
    public enum Phase {
        IDLE,
        LEVER_GRAB,
        LEVER_SHAKE,
        LEVER_DOWN,
        VALVE_GRAB,
        VALVE_SPIN,
        VALVE_DONE
    }

    public record Sample(Phase phase, float grab, float shake, float down, float valveSpin) {
    }

    private Phase current = Phase.IDLE;

    public void update(Identifier emoteId, boolean active) {
        FpvQteState.Snapshot qte = FpvQteState.snapshot();
        if (qte.mode() == FpvQteState.Mode.LEVER) {
            if (qte.finishing() && qte.outcome() == FpvQteState.Outcome.SUCCESS) {
                current = Phase.LEVER_DOWN;
                return;
            }
            if (qte.active()) {
                current = Phase.LEVER_SHAKE;
                return;
            }
        } else if (qte.mode() == FpvQteState.Mode.VALVE) {
            if (qte.finishing() && qte.outcome() == FpvQteState.Outcome.SUCCESS) {
                current = Phase.VALVE_DONE;
                return;
            }
            if (qte.active()) {
                current = qte.spinInputPulse() ? Phase.VALVE_SPIN : Phase.VALVE_GRAB;
                return;
            }
        }

        if (!active || emoteId == null) {
            current = Phase.IDLE;
            return;
        }
        String path = emoteId.getPath().toLowerCase();
        if (path.contains("lever_grab")) {
            current = Phase.LEVER_GRAB;
            return;
        }
        if (path.contains("lever_shake")) {
            current = Phase.LEVER_SHAKE;
            return;
        }
        if (path.contains("lever_down")) {
            current = Phase.LEVER_DOWN;
            return;
        }
        if (path.contains("valve_grab")) {
            current = Phase.VALVE_GRAB;
            return;
        }
        if (path.contains("valve_spin")) {
            current = Phase.VALVE_SPIN;
            return;
        }
        if (path.contains("valve_done")) {
            current = Phase.VALVE_DONE;
            return;
        }
        current = Phase.IDLE;
    }

    public Sample sample(float timeSeconds) {
        float shake = (float) Math.sin(timeSeconds * 22.0f);
        float spin = (float) Math.sin(timeSeconds * 14.0f);
        return switch (current) {
            case LEVER_GRAB -> new Sample(current, 1.0f, 0.0f, 0.0f, 0.0f);
            case LEVER_SHAKE -> new Sample(current, 0.8f, shake, 0.0f, 0.0f);
            case LEVER_DOWN -> new Sample(current, 0.6f, 0.0f, 1.0f, 0.0f);
            case VALVE_GRAB -> new Sample(current, 0.7f, 0.0f, 0.0f, 0.0f);
            case VALVE_SPIN -> new Sample(current, 0.7f, 0.0f, 0.0f, spin);
            case VALVE_DONE -> new Sample(current, 0.3f, 0.0f, 0.6f, 0.0f);
            default -> new Sample(current, 0.0f, 0.0f, 0.0f, 0.0f);
        };
    }
}
