package neutka.marallys.marallyzen.client.fpv;

public final class FpvQteState {
    public enum Mode {
        NONE,
        LEVER,
        VALVE
    }

    public enum Outcome {
        NONE,
        SUCCESS,
        FAIL
    }

    public record Snapshot(Mode mode, boolean active, boolean finishing, boolean spinInputPulse, Outcome outcome) {
    }

    private static Mode mode = Mode.NONE;
    private static boolean active;
    private static boolean finishing;
    private static boolean spinInputPulse;
    private static Outcome outcome = Outcome.NONE;

    private FpvQteState() {
    }

    public static void onLeverStart() {
        mode = Mode.LEVER;
        active = true;
        finishing = false;
        spinInputPulse = false;
        outcome = Outcome.NONE;
    }

    public static void onLeverSuccess() {
        mode = Mode.LEVER;
        active = true;
        finishing = true;
        outcome = Outcome.SUCCESS;
    }

    public static void onLeverFail() {
        mode = Mode.LEVER;
        active = false;
        finishing = false;
        spinInputPulse = false;
        outcome = Outcome.FAIL;
    }

    public static void onValveStart() {
        mode = Mode.VALVE;
        active = true;
        finishing = false;
        spinInputPulse = false;
        outcome = Outcome.NONE;
    }

    public static void onValveInputPulse() {
        mode = Mode.VALVE;
        spinInputPulse = true;
    }

    public static void onValveSuccess() {
        mode = Mode.VALVE;
        active = true;
        finishing = true;
        spinInputPulse = false;
        outcome = Outcome.SUCCESS;
    }

    public static void onValveFail() {
        mode = Mode.VALVE;
        active = false;
        finishing = false;
        spinInputPulse = false;
        outcome = Outcome.FAIL;
    }

    public static void onQteClosed() {
        active = false;
        finishing = false;
        spinInputPulse = false;
        outcome = Outcome.NONE;
        mode = Mode.NONE;
    }

    public static Snapshot snapshot() {
        return new Snapshot(mode, active, finishing, spinInputPulse, outcome);
    }

    public static void endFrame() {
        spinInputPulse = false;
    }
}
