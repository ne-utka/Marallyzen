package neutka.marallys.marallyzen.client.fpv;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.client.emote.ClientEmoteHandler;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.UUID;

/**
 * Minimal, reflection-based helper to play an Emotecraft emote by id/name without compile-time dependency.
 */
public final class FpvEmoteInvoker {
    private FpvEmoteInvoker() {}

    /**
        * Try to play emote for player. Returns true if invocation attempted.
        */
    public static boolean play(Player player, String emoteId) {
        if (player == null || emoteId == null || emoteId.isBlank()) {
            // Marallyzen.LOGGER.debug("[FPV] FpvEmoteInvoker.play: Invalid args - player={}, emoteId={}", player, emoteId);
            return false;
        }

        // Marallyzen.LOGGER.info("[FPV] FpvEmoteInvoker.play: Attempting to play emote '{}' for player {}", emoteId, player.getName().getString());

        Object anim = resolveAnimation(emoteId);

        // Track intent in render context so FPV gating can allow it even if EmoteHolder lookup fails
        // Force marallyzen namespace for SPE_* emotes
        net.minecraft.resources.ResourceLocation ctxId;
        if (emoteId.startsWith("SPE_") || emoteId.startsWith("spe_")) {
            ctxId = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("marallyzen",
                    emoteId.trim().toLowerCase().replace(' ', '_'));
        } else {
            ctxId = net.minecraft.resources.ResourceLocation.tryParse(emoteId);
            if (ctxId == null) {
                ctxId = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("marallyzen",
                        emoteId.trim().toLowerCase().replace(' ', '_'));
            } else if (ctxId.getNamespace().equals("minecraft")) {
                // If parsed as minecraft:*, convert to marallyzen:*
                ctxId = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("marallyzen", ctxId.getPath());
            }
        }
        // Marallyzen.LOGGER.info("[FPV] FpvEmoteInvoker.play: Setting context emoteId={}, resolved anim={}", ctxId, anim != null ? anim.getClass().getSimpleName() : "null");
        neutka.marallys.marallyzen.client.fpv.MarallyzenRenderContext.setCurrentEmoteId(ctxId);

        // If not found in EmoteHolder or incompatible, fall back to robust handler
        if (anim == null) {
            // Marallyzen.LOGGER.info("[FPV] FpvEmoteInvoker.play: Animation not found in EmoteHolder, using ClientEmoteHandler fallback");
            ClientEmoteHandler.handle(player.getUUID(), emoteId);
            return true;
        }

        Object emotePlayer = getEmotePlayer(player);
        if (emotePlayer == null) {
            // Marallyzen.LOGGER.info("[FPV] FpvEmoteInvoker.play: EmotePlayer not found, using ClientEmoteHandler fallback");
            ClientEmoteHandler.handle(player.getUUID(), emoteId);
            return true;
        }

        Method playMethod = findPlayMethod(emotePlayer.getClass(), anim.getClass());
        if (playMethod == null) {
            // Marallyzen.LOGGER.info("[FPV] FpvEmoteInvoker.play: Play method not found, using ClientEmoteHandler fallback");
            ClientEmoteHandler.handle(player.getUUID(), emoteId);
            return true;
        }

        try {
            Object tick = playMethod.getParameterTypes()[1] == int.class ? 0 : 0f;
            playMethod.setAccessible(true);
            playMethod.invoke(emotePlayer, anim, tick, true);
            // Marallyzen.LOGGER.info("[FPV] FpvEmoteInvoker.play: Successfully invoked playEmote via reflection");
            return true;
        } catch (Exception e) {
            // Fallback to the tolerant handler that copes with old Animation types
            // Marallyzen.LOGGER.warn("[FPV] FpvEmoteInvoker.play: Reflection invoke failed: {}, using ClientEmoteHandler fallback", e.getMessage());
            ClientEmoteHandler.handle(player.getUUID(), emoteId);
            return true;
        }
    }

    private static Object getEmotePlayer(Player player) {
        try {
            Method m = player.getClass().getMethod("emotecraft$getEmote");
            m.setAccessible(true);
            return m.invoke(player);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Method findPlayMethod(Class<?> emotePlayerClass, Class<?> animClass) {
        for (Method m : emotePlayerClass.getMethods()) {
            if (!m.getName().equals("emotecraft$playEmote")) continue;
            Class<?>[] p = m.getParameterTypes();
            if (p.length == 3 && (p[1] == float.class || p[1] == int.class) && p[2] == boolean.class) {
                // Accept if first param is assignable or Object
                if (p[0].isAssignableFrom(animClass) || animClass.isAssignableFrom(p[0]) || p[0] == Object.class) {
                    return m;
                }
            }
        }
        return null;
    }

    private static Object resolveAnimation(String emoteId) {
        ResourceLocation id = ResourceLocation.tryParse(emoteId);
        String path = id != null ? id.getPath() : emoteId;
        UUID uuid = null;
        try { uuid = UUID.fromString(emoteId); } catch (Exception ignored) {}

        try {
            Class<?> holderClass = Class.forName("io.github.kosmx.emotes.main.EmoteHolder");
            Field listField = holderClass.getField("list");
            Object listObj = listField.get(null);
            Method values = listObj.getClass().getMethod("values");
            @SuppressWarnings("unchecked")
            Collection<Object> holders = (Collection<Object>) values.invoke(listObj);

            for (Object holder : holders) {
                try {
                    // Match UUID
                    if (uuid != null) {
                        Method getUuid = holderClass.getMethod("getUuid");
                        Object holderUuid = getUuid.invoke(holder);
                        if (uuid.equals(holderUuid)) {
                            Field emoteField = holderClass.getField("emote");
                            return emoteField.get(holder);
                        }
                    }

                    // Match name
                    Field nameField = holderClass.getField("name");
                    Object nameComp = nameField.get(holder);
                    Method getString = nameComp.getClass().getMethod("getString");
                    String name = ((String) getString.invoke(nameComp)).toLowerCase();
                    if (name.equalsIgnoreCase(path) || name.contains(path.toLowerCase())) {
                        Field emoteField = holderClass.getField("emote");
                        return emoteField.get(holder);
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }
}

