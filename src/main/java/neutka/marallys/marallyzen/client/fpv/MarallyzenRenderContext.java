package neutka.marallys.marallyzen.client.fpv;

import neutka.marallys.marallyzen.Marallyzen;

/**
 * Single source of truth for enabling FPV emote rendering.
 * Only Marallyzen code may toggle this flag.
 */
public final class MarallyzenRenderContext {
    private static boolean fpvEmoteEnabled = false;
    private static net.minecraft.resources.ResourceLocation currentEmoteId = null;
    private static boolean headMovementEnabled = true; // Default: enabled

    private MarallyzenRenderContext() {}

    public static boolean isFpvEmoteEnabled() {
        return fpvEmoteEnabled;
    }

    public static void setFpvEmoteEnabled(boolean enabled) {
        boolean wasEnabled = fpvEmoteEnabled;
        fpvEmoteEnabled = enabled;
        // Marallyzen.LOGGER.info("[FPV] MarallyzenRenderContext.setFpvEmoteEnabled: {} -> {} (currentEmoteId={})", wasEnabled, enabled, currentEmoteId);
        if (!enabled) {
            currentEmoteId = null;
            headMovementEnabled = true; // Reset to default when disabling
        }
    }

    public static void setCurrentEmoteId(net.minecraft.resources.ResourceLocation id) {
        // Marallyzen.LOGGER.info("[FPV] MarallyzenRenderContext.setCurrentEmoteId: {} -> {}", currentEmoteId, id);
        currentEmoteId = id;
        
        // Auto-disable head movement for specific emotes
        if (id != null && id.getPath().equals("spe_poke")) {
            headMovementEnabled = false;
            // Marallyzen.LOGGER.info("[FPV] MarallyzenRenderContext: Head movement disabled for SPE_Poke");
        } else {
            headMovementEnabled = true; // Enable for other emotes
        }
    }

    public static net.minecraft.resources.ResourceLocation getCurrentEmoteId() {
        return currentEmoteId;
    }

    public static boolean isHeadMovementEnabled() {
        return headMovementEnabled;
    }

    public static void setHeadMovementEnabled(boolean enabled) {
        // Marallyzen.LOGGER.info("[FPV] MarallyzenRenderContext.setHeadMovementEnabled: {}", enabled);
        headMovementEnabled = enabled;
    }
}

