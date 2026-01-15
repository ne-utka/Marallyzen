package neutka.marallys.marallyzen.client.fpv;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import neutka.marallys.marallyzen.Marallyzen;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.UUID;

/**
 * Decides whether FPV should apply and provides camera application entrypoint.
 * This is intentionally minimal: decisions are made only by Marallyzen context + allowed emotes.
 */
public final class MarallyzenFpvController {

    private MarallyzenFpvController() {}

    /**
     * Should FPV be applied for this player right now.
     */
    private static boolean lastShouldApplyResult = false;
    private static ResourceLocation lastCtxId = null;
    private static ResourceLocation lastResolvedId = null;

    public static boolean shouldApply(Player player) {
        if (player == null) {
            return false;
        }
        
        boolean contextEnabled = MarallyzenRenderContext.isFpvEmoteEnabled();
        if (!contextEnabled) {
            if (lastShouldApplyResult) {
                // Marallyzen.LOGGER.info("[FPV] MarallyzenFpvController.shouldApply: Context disabled");
                lastShouldApplyResult = false;
            }
            return false;
        }

        // Prefer context-emote if set (explicit intent from Marallyzen)
        ResourceLocation ctxId = MarallyzenRenderContext.getCurrentEmoteId();
        if (ctxId != null) {
            boolean allowed = MarallyzenFpvEmotes.isAllowed(ctxId);
            if (ctxId != lastCtxId || allowed != lastShouldApplyResult) {
                // Marallyzen.LOGGER.info("[FPV] MarallyzenFpvController.shouldApply: ctxId={}, allowed={}", ctxId, allowed);
                lastCtxId = ctxId;
            }
            if (allowed) {
                if (!lastShouldApplyResult) {
                    lastShouldApplyResult = true;
                }
                return true;
            }
        }

        // Fallback to inspecting Emotecraft state
        Object emotePlayer = getEmotePlayer(player);
        ResourceLocation emoteId = resolveEmoteId(emotePlayer);
        boolean allowed = emoteId != null && MarallyzenFpvEmotes.isAllowed(emoteId);
        if (emoteId != lastResolvedId || allowed != lastShouldApplyResult) {
            // Marallyzen.LOGGER.info("[FPV] MarallyzenFpvController.shouldApply: resolved emoteId={}, allowed={}, final result={}", emoteId, allowed, allowed);
            lastResolvedId = emoteId;
        }
        if (allowed != lastShouldApplyResult) {
            lastShouldApplyResult = allowed;
        }
        return allowed;
    }

    /**
     * Apply camera using FPV interpreter (head pose already tracked there).
     */
    public static void applyCamera(Player player, float tickDelta, net.minecraft.client.Camera camera) {
        // Camera application is handled in CameraMixin via EmoteFpvInterpreter & existing logic.
        // This method is kept for parity with the requested architecture.
    }

    public static ResourceLocation getResolvedEmoteId(Player player) {
        if (player == null) {
            return null;
        }
        Object emotePlayer = getEmotePlayer(player);
        return resolveEmoteId(emotePlayer);
    }

    // --- Helpers ---

    private static Object getEmotePlayer(Player player) {
        try {
            Method m = player.getClass().getMethod("emotecraft$getEmote");
            m.setAccessible(true);
            return m.invoke(player);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean isEmoteActive(Object emotePlayer) {
        if (emotePlayer == null) return false;
        try {
            Method m = emotePlayer.getClass().getMethod("isActive");
            m.setAccessible(true);
            Object r = m.invoke(emotePlayer);
            return r instanceof Boolean b && b;
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Resolve the emote ID to a ResourceLocation by inspecting EmoteHolder list and current animation.
     * Falls back to null if cannot resolve.
     */
    private static ResourceLocation resolveEmoteId(Object emotePlayer) {
        Object anim = getCurrentAnimation(emotePlayer);
        if (anim == null) return null;

        // Try: ExtraAnimationData -> UUID key
        UUID uuid = resolveUuidFromAnimation(anim);
        String name = resolveNameFromAnimation(anim);

        // Try match against EmoteHolder.list to get UUID/name
        ResourceLocation fromHolder = resolveFromEmoteHolder(anim, uuid, name);
        if (fromHolder != null) return fromHolder;

        // If name looks like namespace:path, parse; else if present, assume marallyzen:name
        if (name != null && !name.isBlank()) {
            if (name.contains(":")) {
                try {
                    return ResourceLocation.parse(name);
                } catch (Exception ignored) { }
            } else {
                return ResourceLocation.fromNamespaceAndPath("marallyzen", sanitizeName(name));
            }
        }

        return null;
    }

    private static Object getCurrentAnimation(Object emotePlayer) {
        try {
            Method m = emotePlayer.getClass().getMethod("getCurrentAnimationInstance");
            m.setAccessible(true);
            return m.invoke(emotePlayer);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static UUID resolveUuidFromAnimation(Object anim) {
        try {
            Method data = anim.getClass().getMethod("data");
            Object extra = data.invoke(anim);
            if (extra != null) {
                // Try UUID_KEY field
                try {
                    Field f = extra.getClass().getField("UUID_KEY");
                    Object key = f.get(null);
                    if (key instanceof String s) {
                        Method get = extra.getClass().getMethod("get", String.class);
                        Object val = get.invoke(extra, s);
                        if (val instanceof UUID u) return u;
                    }
                } catch (Exception ignored) {}
                // Try generic get("uuid")
                try {
                    Method get = extra.getClass().getMethod("get", String.class);
                    Object val = get.invoke(extra, "uuid");
                    if (val instanceof UUID u) return u;
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static String resolveNameFromAnimation(Object anim) {
        try {
            Method data = anim.getClass().getMethod("data");
            Object extra = data.invoke(anim);
            if (extra != null) {
                // Try NAME_KEY
                try {
                    Field f = extra.getClass().getField("NAME_KEY");
                    Object key = f.get(null);
                    if (key instanceof String s) {
                        Method get = extra.getClass().getMethod("get", String.class);
                        Object val = get.invoke(extra, s);
                        if (val != null) return val.toString();
                    }
                } catch (Exception ignored) {}
                // Try generic "name"
                try {
                    Method get = extra.getClass().getMethod("get", String.class);
                    Object val = get.invoke(extra, "name");
                    if (val != null) return val.toString();
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return null;
    }

    @SuppressWarnings("unchecked")
    private static ResourceLocation resolveFromEmoteHolder(Object anim, UUID uuid, String name) {
        try {
            Class<?> holderClass = Class.forName("io.github.kosmx.emotes.main.EmoteHolder");
            Field listField = holderClass.getField("list");
            Object listObj = listField.get(null);
            Method values = listObj.getClass().getMethod("values");
            Collection<?> holders = (Collection<?>) values.invoke(listObj);

            for (Object holder : holders) {
                // Match by instance
                try {
                    Field emoteField = holderClass.getField("emote");
                    Object emoteAnim = emoteField.get(holder);
                    if (emoteAnim == anim) {
                        String resolved = extractNameFromHolder(holderClass, holder);
                        if (resolved != null) return ResourceLocation.fromNamespaceAndPath("marallyzen", sanitizeName(resolved));
                    }
                } catch (Exception ignored) {}

                // Match by UUID
                if (uuid != null) {
                    try {
                        Method getUuid = holderClass.getMethod("getUuid");
                        Object holderUuid = getUuid.invoke(holder);
                        if (uuid.equals(holderUuid)) {
                            String resolved = extractNameFromHolder(holderClass, holder);
                            if (resolved != null) return ResourceLocation.fromNamespaceAndPath("marallyzen", sanitizeName(resolved));
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}
        // Fallback: if name present, allow
        if (name != null && !name.isBlank()) {
            return ResourceLocation.fromNamespaceAndPath("marallyzen", sanitizeName(name));
        }
        return null;
    }

    private static String extractNameFromHolder(Class<?> holderClass, Object holder) {
        try {
            Field nameField = holderClass.getField("name");
            Object nameComp = nameField.get(holder);
            Method getString = nameComp.getClass().getMethod("getString");
            return (String) getString.invoke(nameComp);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String sanitizeName(String raw) {
        return raw.trim().toLowerCase().replace(' ', '_');
    }
}
