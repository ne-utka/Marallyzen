package neutka.marallys.marallyzen.replay.camera;

public enum CameraEase {
    LINEAR,
    SMOOTH,
    EASE_IN,
    EASE_OUT,
    EASE_IN_OUT;

    public static CameraEase fromString(String value) {
        if (value == null) {
            return LINEAR;
        }
        return switch (value.trim().toLowerCase()) {
            case "smooth", "smoothstep" -> SMOOTH;
            case "easein", "ease_in", "in" -> EASE_IN;
            case "easeout", "ease_out", "out" -> EASE_OUT;
            case "easeinout", "ease_in_out", "inout" -> EASE_IN_OUT;
            default -> LINEAR;
        };
    }

    public double apply(double t) {
        t = clamp01(t);
        return switch (this) {
            case SMOOTH -> t * t * (3.0 - 2.0 * t);
            case EASE_IN -> t * t;
            case EASE_OUT -> 1.0 - Math.pow(1.0 - t, 2.0);
            case EASE_IN_OUT -> t < 0.5
                ? 2.0 * t * t
                : 1.0 - Math.pow(-2.0 * t + 2.0, 2.0) / 2.0;
            case LINEAR -> t;
        };
    }

    private static double clamp01(double value) {
        if (value < 0.0) {
            return 0.0;
        }
        if (value > 1.0) {
            return 1.0;
        }
        return value;
    }
}
