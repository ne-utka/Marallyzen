package neutka.marallys.marallyzen.client.fpv;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import neutka.marallys.marallyzen.Marallyzen;

/**
 * Interprets Emotecraft state for First-Person View.
 * Reads the current emote state and calculates target transforms for camera and body parts.
 */
public class EmoteFpvInterpreter {
    private static final EmoteFpvInterpreter INSTANCE = new EmoteFpvInterpreter();

    public static EmoteFpvInterpreter getInstance() {
        return INSTANCE;
    }

    // Current and previous state (tick-based, no extra smoothing)
    private float headYaw, headPitch, headRoll;
    private float headYawPrev, headPitchPrev, headRollPrev;
    private float torsoPitch, torsoYaw, torsoRoll;
    private float torsoPitchPrev, torsoYawPrev, torsoRollPrev;

    private boolean isEmoteActive = false;
    private boolean isRenderingFpvBody = false;
    
    // Fade in/out for smooth transitions
    private float fadeProgress = 0.0f; // 0.0 = no emote, 1.0 = full emote
    private static final float FADE_IN_SPEED = 0.15f; // per tick
    private static final float FADE_OUT_SPEED = 0.2f; // per tick
    
    // Smoothing for sudden changes (rate limiting)
    private static final float MAX_CHANGE_PER_TICK = 0.15f; // radians per tick (reduced for head stability)
    private static final float MAX_HEAD_CHANGE_PER_TICK = 0.08f; // even more restrictive for head
    // Exponential smoothing factor for head (0.0 = no smoothing, 1.0 = no change)
    private static final float HEAD_SMOOTHING_FACTOR = 0.7f; // higher = more smoothing
    
    private boolean lastLoggedActive = false;

    public void update(AbstractClientPlayer player) {
        if (player == null) {
            return;
        }
        
        // 1. Get EmotePlayer (via reflection to avoid hard dependency)
        Object emotePlayer = getEmotePlayer(player);
        boolean active = isEmoteActive(emotePlayer);
        
        if (active != lastLoggedActive) {
            // Marallyzen.LOGGER.info("[FPV] EmoteFpvInterpreter.update: player={}, emotePlayer={}, isActive={}", 
            //         player.getName().getString(), emotePlayer != null, active);
            lastLoggedActive = active;
        }
        
        // Update fade progress
        if (active) {
            fadeProgress = Math.min(1.0f, fadeProgress + FADE_IN_SPEED);
            isEmoteActive = fadeProgress > 0.01f; // Consider active if any fade
            if (isEmoteActive) {
                updateTargets(player);
            }
        } else {
            fadeProgress = Math.max(0.0f, fadeProgress - FADE_OUT_SPEED);
            isEmoteActive = fadeProgress > 0.01f;
            if (!isEmoteActive && fadeProgress <= 0.0f) {
                resetState();
            }
        }
    }

    public boolean isActive() {
        return isEmoteActive;
    }

    public void forceInactive() {
        fadeProgress = 0.0f;
        isEmoteActive = false;
        resetState();
    }

    public boolean isRenderingFpvBody() {
        return isRenderingFpvBody;
    }

    public void setRenderingFpvBody(boolean renderingFpvBody) {
        isRenderingFpvBody = renderingFpvBody;
    }

    public float getHeadPitch() { return headPitch; }
    public float getHeadYaw() { return headYaw; }
    public float getHeadRoll() { return headRoll; }

    private Object getEmotePlayer(AbstractClientPlayer player) {
        try {
            var method = player.getClass().getMethod("emotecraft$getEmote");
            method.setAccessible(true);
            return method.invoke(player);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isEmoteActive(Object emotePlayer) {
        if (emotePlayer == null) return false;
        try {
            var method = emotePlayer.getClass().getMethod("isActive");
            method.setAccessible(true);
            Object result = method.invoke(emotePlayer);
            if (result instanceof Boolean bool) {
                return bool;
            }
        } catch (Exception ignored) {
            // Fallback: consider inactive on failure
        }
        return false;
    }

    private void updateTargets(AbstractClientPlayer player) {
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        PlayerRenderer renderer = (PlayerRenderer) dispatcher.getRenderer(player);
        PlayerModel<AbstractClientPlayer> model = renderer.getModel();

        // Read head rotation (radians)
        ModelPart head = model.getHead();
        ModelPart body = model.body;
        
        // Shift previous
        this.headPitchPrev = this.headPitch;
        this.headYawPrev = this.headYaw;
        this.headRollPrev = this.headRoll;
        this.torsoPitchPrev = this.torsoPitch;
        this.torsoYawPrev = this.torsoYaw;
        this.torsoRollPrev = this.torsoRoll;

        // Store current with fade multiplier and rate limiting for smooth transitions
        float fade = fadeProgress;
        float targetPitch = head.xRot * fade;
        float targetYaw = head.yRot * fade;
        float targetRoll = head.zRot * fade;
        
        // Apply exponential smoothing + rate limiting for head to prevent shaking
        float smoothedPitch = exponentialSmooth(this.headPitch, targetPitch, HEAD_SMOOTHING_FACTOR);
        float smoothedYaw = exponentialSmooth(this.headYaw, targetYaw, HEAD_SMOOTHING_FACTOR);
        float smoothedRoll = exponentialSmooth(this.headRoll, targetRoll, HEAD_SMOOTHING_FACTOR);
        
        // Then rate limit to prevent sudden jumps
        this.headPitch = rateLimit(this.headPitch, smoothedPitch, MAX_HEAD_CHANGE_PER_TICK);
        this.headYaw = rateLimit(this.headYaw, smoothedYaw, MAX_HEAD_CHANGE_PER_TICK);
        this.headRoll = rateLimit(this.headRoll, smoothedRoll, MAX_HEAD_CHANGE_PER_TICK);
        
        // Torso can change faster
        this.torsoPitch = rateLimit(this.torsoPitch, body.xRot * fade, MAX_CHANGE_PER_TICK);
        this.torsoYaw = rateLimit(this.torsoYaw, body.yRot * fade, MAX_CHANGE_PER_TICK);
        this.torsoRoll = rateLimit(this.torsoRoll, body.zRot * fade, MAX_CHANGE_PER_TICK);
    }

    private void resetState() {
        // Don't reset immediately - let fade-out handle it smoothly
        // Only reset when fade is complete
        if (fadeProgress <= 0.0f) {
            headPitchPrev = headPitch;
            headYawPrev = headYaw;
            headRollPrev = headRoll;
            torsoPitchPrev = torsoPitch;
            torsoYawPrev = torsoYaw;
            torsoRollPrev = torsoRoll;

            headPitch = 0;
            headYaw = 0;
            headRoll = 0;
            torsoPitch = 0;
            torsoYaw = 0;
            torsoRoll = 0;
        } else {
            // During fade-out, smoothly return to zero
            float fade = fadeProgress;
            headPitchPrev = headPitch;
            headYawPrev = headYaw;
            headRollPrev = headRoll;
            torsoPitchPrev = torsoPitch;
            torsoYawPrev = torsoYaw;
            torsoRollPrev = torsoRoll;
            
            // Smoothly decay to zero
            this.headPitch = rateLimit(this.headPitch, 0, MAX_HEAD_CHANGE_PER_TICK * 1.5f);
            this.headYaw = rateLimit(this.headYaw, 0, MAX_HEAD_CHANGE_PER_TICK * 1.5f);
            this.headRoll = rateLimit(this.headRoll, 0, MAX_HEAD_CHANGE_PER_TICK * 1.5f);
            this.torsoPitch = rateLimit(this.torsoPitch, 0, MAX_CHANGE_PER_TICK * 1.5f);
            this.torsoYaw = rateLimit(this.torsoYaw, 0, MAX_CHANGE_PER_TICK * 1.5f);
            this.torsoRoll = rateLimit(this.torsoRoll, 0, MAX_CHANGE_PER_TICK * 1.5f);
        }
    }

    public float getHeadPitch(float alpha) { return lerp(headPitchPrev, headPitch, alpha); }
    public float getHeadYaw(float alpha) { return lerp(headYawPrev, headYaw, alpha); }
    public float getHeadRoll(float alpha) { return lerp(headRollPrev, headRoll, alpha); }

    private float lerp(float a, float b, float t) { return a + (b - a) * t; }
    
    private float rateLimit(float current, float target, float maxChange) {
        float diff = target - current;
        if (Math.abs(diff) <= maxChange) {
            return target;
        }
        return current + Math.signum(diff) * maxChange;
    }
    
    private float exponentialSmooth(float current, float target, float factor) {
        return current + (target - current) * (1.0f - factor);
    }
}
