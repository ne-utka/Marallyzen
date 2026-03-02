package neutka.marallys.marallyzen.activity;

public final class EasingRegistry {
    private EasingRegistry() {
    }

    public static float apply(String easing, float t) {
        String raw = easing == null ? "" : easing.trim().toLowerCase();
        float clamped = clamp01(t);
        switch (raw) {
            case "linear":
                return clamped;
            case "ease_in":
                return clamped * clamped;
            case "ease_in_out":
                return clamped < 0.5F
                        ? 4.0F * clamped * clamped * clamped
                        : 1.0F - (float) Math.pow(-2.0F * clamped + 2.0F, 3) / 2.0F;
            case "bounce":
                return bounce(clamped);
            case "elastic":
                return elastic(clamped);
            case "ease_out":
            default:
                float inv = 1.0F - clamped;
                return 1.0F - inv * inv * inv;
        }
    }

    private static float clamp01(float value) {
        if (value < 0.0F) {
            return 0.0F;
        }
        if (value > 1.0F) {
            return 1.0F;
        }
        return value;
    }

    private static float bounce(float t) {
        float n1 = 7.5625F;
        float d1 = 2.75F;
        if (t < 1.0F / d1) {
            return n1 * t * t;
        } else if (t < 2.0F / d1) {
            float x = t - 1.5F / d1;
            return n1 * x * x + 0.75F;
        } else if (t < 2.5F / d1) {
            float x = t - 2.25F / d1;
            return n1 * x * x + 0.9375F;
        } else {
            float x = t - 2.625F / d1;
            return n1 * x * x + 0.984375F;
        }
    }

    private static float elastic(float t) {
        if (t == 0.0F || t == 1.0F) {
            return t;
        }
        float c4 = (float) (2.0F * Math.PI) / 3.0F;
        return (float) (Math.pow(2.0F, -10.0F * t) * Math.sin((t * 10.0F - 0.75F) * c4) + 1.0F);
    }
}
