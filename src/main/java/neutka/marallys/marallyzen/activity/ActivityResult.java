package neutka.marallys.marallyzen.activity;

public record ActivityResult(boolean success, boolean waitForCompletion, String message) {
    public static ActivityResult ok(boolean waitForCompletion) {
        return new ActivityResult(true, waitForCompletion, "");
    }

    public static ActivityResult failed(String message) {
        return new ActivityResult(false, false, message == null ? "" : message);
    }
}
