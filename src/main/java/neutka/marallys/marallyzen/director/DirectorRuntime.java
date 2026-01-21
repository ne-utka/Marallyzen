package neutka.marallys.marallyzen.director;

public final class DirectorRuntime {
    private static boolean active;
    private static boolean previewing;
    private static DirectorProject project;
    private static final CameraEvaluator EVALUATOR = new CameraEvaluator();

    private DirectorRuntime() {
    }

    public static void start(DirectorProject value) {
        project = value;
        active = true;
        DirectorEventRunner.reset();
    }

    public static void stop() {
        active = false;
        previewing = false;
        project = null;
        DirectorEventRunner.reset();
    }

    public static void tick(long timeMs) {
        if (!previewing || project == null) {
            return;
        }
        DirectorEventRunner.tick(timeMs, project);
    }

    public static CameraState evaluate(long timeMs) {
        if (!previewing || project == null) {
            return null;
        }
        for (DirectorTrack<?> track : project.tracks) {
            if (track instanceof CameraTrack camTrack) {
                return EVALUATOR.evaluate(camTrack.keyframes(), timeMs);
            }
        }
        return null;
    }

    public static void startPreview() {
        if (!active || project == null) {
            return;
        }
        previewing = true;
        DirectorEventRunner.reset();
    }

    public static void stopPreview() {
        previewing = false;
        DirectorEventRunner.reset();
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean isPreviewing() {
        return previewing;
    }
}
