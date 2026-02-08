package neutka.marallys.marallyzen.client.animation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import neutka.marallys.marallyzen.Marallyzen;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
public final class LeverShakeAnimationClient {
    private static final Identifier ANIMATION_ID =        Identifier.fromNamespaceAndPath(Marallyzen.MODID, "lever_hand_shake");
    private static final Identifier ANIMATION_RESOURCE =        Identifier.fromNamespaceAndPath(Marallyzen.MODID, "player_animations/lever_hand_shake.json");
    private static final int LAYER_PRIORITY = 1200;
    // Keep the layer per-player to avoid adding duplicates on repeated clicks.
    private static final Map<UUID, LayerState> LAYERS = new HashMap<>();
    private static Object cachedPlayable;
    private static boolean loggedMissingAnimLib;
    private static boolean loggedMissingAnimation;
    private static boolean loggedFirstPersonModes;
    private static boolean loggedEmotecraftMissing;
    private static boolean loggedEmotecraftError;
    private LeverShakeAnimationClient() {
    }
    public static void playForLocalPlayer() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) {
            return;
        }
        Marallyzen.LOGGER.info("[LeverShake] playForLocalPlayer player={}", mc.player.getGameProfile().name());
        Object playable = getAnimation();
        if (playable == null) {
            Marallyzen.LOGGER.warn("[LeverShake] animation playable missing");
            return;
        }
        if (tryPlayViaEmotecraft(playable)) {
            Marallyzen.LOGGER.info("[LeverShake] animation started via Emotecraft");
            return;
        }
        Object actual = playAnimation(playable);
        if (actual == null) {
            Marallyzen.LOGGER.warn("[LeverShake] failed to create animation instance");
            return;
        }
        applyFirstPersonMode(actual);
        boolean appliedAssociated = applyAssociatedAnimation(mc.player, actual);
        if (appliedAssociated) {
            Marallyzen.LOGGER.info("[LeverShake] animation applied via associated data");
        }
        LayerState layer = getOrCreateLayer(mc.player);
        if (layer == null) {
            Marallyzen.LOGGER.warn("[LeverShake] no animation layer available");
            return;
        }
        setLayerAnimation(layer.layer, actual);
        Marallyzen.LOGGER.info("[LeverShake] animation applied via layer");
    }
    public static void clear() {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.player != null) {
            clearAssociatedAnimation(mc.player);
        }
        for (LayerState state : LAYERS.values()) {
            removeLayer(state);
        }
        LAYERS.clear();
    }
    private static LayerState getOrCreateLayer(AbstractClientPlayer player) {
        UUID id = player.getUUID();
        LayerState existing = LAYERS.get(id);
        if (existing != null && existing.entityId == player.getId()) {
            return existing;
        }
        if (existing != null) {
            removeLayer(existing);
        }
        Object stack = getAnimationStack(player);
        if (stack == null) {
            return null;
        }
        Object layer = createModifierLayer();
        if (layer == null) {
            return null;
        }
        if (!addLayer(stack, layer)) {
            return null;
        }
        LayerState state = new LayerState(stack, layer, player.getId());
        LAYERS.put(id, state);
        return state;
    }
    private static Object getAnimationStack(AbstractClientPlayer player) {
        try {
            Class<?> accessClass = Class.forName("dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess");
            Method getLayer = accessClass.getMethod("getPlayerAnimLayer", AbstractClientPlayer.class);
            return getLayer.invoke(null, player);
        }
 catch (ClassNotFoundException e) {
            if (!loggedMissingAnimLib) {
                loggedMissingAnimLib = true;
                Marallyzen.LOGGER.warn("LeverShake: player-animation-lib (kosmx) not found.");
            }
            return null;
        }
 catch (Exception e) {
            Marallyzen.LOGGER.warn("LeverShake: failed to fetch animation stack.", e);
            return null;
        }
    }
    private static Object createModifierLayer() {
        try {
            Class<?> layerClass = Class.forName("dev.kosmx.playerAnim.api.layered.ModifierLayer");
            return layerClass.getConstructor().newInstance();
        }
 catch (Exception e) {
            Marallyzen.LOGGER.warn("LeverShake: failed to create modifier layer.", e);
            return null;
        }
    }
    private static boolean addLayer(Object stack, Object layer) {
        try {
            Method addLayer = findAddLayerMethod(stack, layer);
            if (addLayer == null) {
                Marallyzen.LOGGER.warn("LeverShake: addAnimLayer not found on AnimationStack.");
                return false;
            }
            addLayer.invoke(stack, LAYER_PRIORITY, layer);
            return true;
        }
 catch (Exception e) {
            Marallyzen.LOGGER.warn("LeverShake: failed to add animation layer.", e);
            return false;
        }
    }
    private static void removeLayer(LayerState state) {
        if (state == null || state.stack == null || state.layer == null) {
            return;
        }
        try {
            Method removeLayer = findRemoveLayerMethod(state.stack, state.layer);
            if (removeLayer != null) {
                removeLayer.invoke(state.stack, state.layer);
            }
        }
 catch (Exception e) {
            Marallyzen.LOGGER.warn("LeverShake: failed to remove animation layer.", e);
        }
    }
    private static Object getAnimation() {
        Object playable = getAnimationFromRegistry();
        if (playable != null) {
            return playable;
        }
        return getAnimationFromResource();
    }
    private static Object getAnimationFromRegistry() {
        try {
            Class<?> registryClass = Class.forName("dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry");
            Method getAnimation = registryClass.getMethod("getAnimation", Identifier.class);
            Object playable = getAnimation.invoke(null, ANIMATION_ID);
            if (playable == null && !loggedMissingAnimation) {
                loggedMissingAnimation = true;
                Marallyzen.LOGGER.warn("LeverShake: animation not found in registry {}", ANIMATION_ID);
            }
            return playable;
        }
 catch (Exception e) {
            if (!loggedMissingAnimation) {
                loggedMissingAnimation = true;
                Marallyzen.LOGGER.warn("LeverShake: failed to query animation registry {}", ANIMATION_ID, e);
            }
            return null;
        }
    }
    private static Object getAnimationFromResource() {
        if (cachedPlayable != null) {
            return cachedPlayable;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return null;
        }
        var resource = mc.getResourceManager().getResource(ANIMATION_RESOURCE);
        if (resource.isEmpty()) {
            Marallyzen.LOGGER.warn("LeverShake: animation resource missing {}", ANIMATION_RESOURCE);
            return null;
        }
        try (var stream = resource.get().open()) {
            Class<?> codecsClass = Class.forName("dev.kosmx.playerAnim.minecraftApi.codec.AnimationCodecs");
            Method deserialize = codecsClass.getMethod("deserialize", String.class, java.io.InputStream.class);
            Object result = deserialize.invoke(null, "emotecraft", stream);
            if (result instanceof Iterable<?> iterable) {
                Object match = pickAnimation(iterable);
                if (match != null) {
                    cachedPlayable = match;
                    Marallyzen.LOGGER.info("LeverShake: loaded animation from resource {}", ANIMATION_RESOURCE);
                    return match;
                }
            }
        }
 catch (Exception e) {
            Marallyzen.LOGGER.warn("LeverShake: failed to load animation from resource {}", ANIMATION_RESOURCE, e);
        }
        return null;
    }
    private static Object pickAnimation(Iterable<?> animations) {
        Object first = null;
        for (Object anim : animations) {
            if (first == null) {
                first = anim;
            }
            String name = getPlayableName(anim);
            if (ANIMATION_ID.getPath().equals(name)) {
                return anim;
            }
        }
        return first;
    }
    private static String getPlayableName(Object playable) {
        if (playable == null) {
            return null;
        }
        try {
            Method getName = playable.getClass().getMethod("getName");
            Object value = getName.invoke(playable);
            if (value instanceof String name) {
                return name;
            }
        }
 catch (Exception e) {
            Marallyzen.LOGGER.warn("LeverShake: failed to read playable name.", e);
        }
        return null;
    }
    private static Object playAnimation(Object playable) {
        if (playable == null) {
            return null;
        }
        Object keyframePlayer = tryCreateKeyframePlayer(playable);
        if (keyframePlayer != null) {
            return keyframePlayer;
        }
        try {
            Method playAnimation = playable.getClass().getMethod("playAnimation");
            return playAnimation.invoke(playable);
        }
 catch (Exception e) {
            Marallyzen.LOGGER.warn("LeverShake: failed to create animation instance.", e);
            return null;
        }
    }
    private static Object tryCreateKeyframePlayer(Object playable) {
        try {
            Class<?> keyframeClass = Class.forName("dev.kosmx.playerAnim.core.data.KeyframeAnimation");
            if (!keyframeClass.isInstance(playable)) {
                return null;
            }
            Class<?> playerClass = Class.forName("dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer");
            return playerClass.getConstructor(keyframeClass).newInstance(playable);
        }
 catch (ClassNotFoundException e) {
            return null;
        }
 catch (Exception e) {
            Marallyzen.LOGGER.warn("LeverShake: failed to create KeyframeAnimationPlayer.", e);
            return null;
        }
    }
    private static void setLayerAnimation(Object layer, Object animation) {
        if (layer == null || animation == null) {
            return;
        }
        try {
            Method setAnimation = findMethod(layer.getClass(), "setAnimation", 1);
            if (setAnimation == null) {
                Marallyzen.LOGGER.warn("LeverShake: setAnimation not found on ModifierLayer.");
                return;
            }
            setAnimation.invoke(layer, new Object[]{
null}
);
            setAnimation.invoke(layer, animation);
        }
 catch (Exception e) {
            Marallyzen.LOGGER.warn("LeverShake: failed to set animation on layer.", e);
        }
    }
    private static void applyFirstPersonMode(Object animation) {
        try {
            Class<?> modeClass = Class.forName("dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode");
            Object mode = pickFirstPersonMode(modeClass);
            Method setMode = findMethod(animation.getClass(), "setFirstPersonMode", modeClass);
            if (setMode != null && mode != null) {
                setMode.invoke(animation, mode);
                Marallyzen.LOGGER.info("[LeverShake] first-person mode={}", enumName(mode));
            }
            Class<?> configClass = Class.forName("dev.kosmx.playerAnim.api.firstPerson.FirstPersonConfiguration");
            Object config = configClass.getConstructor().newInstance();
            Method showRightArm = findMethod(configClass, "setShowRightArm", boolean.class);
            Method showLeftArm = findMethod(configClass, "setShowLeftArm", boolean.class);
            Method showRightItem = findMethod(configClass, "setShowRightItem", boolean.class);
            Method showLeftItem = findMethod(configClass, "setShowLeftItem", boolean.class);
            if (showRightArm != null) {
                showRightArm.invoke(config, true);
            }
            if (showLeftArm != null) {
                showLeftArm.invoke(config, true);
            }
            if (showRightItem != null) {
                showRightItem.invoke(config, true);
            }
            if (showLeftItem != null) {
                showLeftItem.invoke(config, true);
            }
            Method setConfig = findMethod(animation.getClass(), "setFirstPersonConfiguration", configClass);
            if (setConfig != null) {
                setConfig.invoke(animation, config);
            }
        }
 catch (Exception e) {
            Marallyzen.LOGGER.warn("LeverShake: failed to apply first-person mode.", e);
        }
    }
    private static Object pickFirstPersonMode(Class<?> modeClass) {
        try {
            Object[] constants = modeClass.getEnumConstants();
            if (constants == null || constants.length == 0) {
                return null;
            }
            if (!loggedFirstPersonModes) {
                loggedFirstPersonModes = true;
                StringBuilder names = new StringBuilder();
                for (Object constant : constants) {
                    String name = enumName(constant);
                    if (name == null) {
                        continue;
                    }
                    if (!names.isEmpty()) {
                        names.append(", ");
                    }
                    names.append(name);
                }
                Marallyzen.LOGGER.info("[LeverShake] available first-person modes: {}", names);
            }
            Object hands = null;
            Object firstPerson = null;
            Object vanilla = null;
            Object safe = null;
            Object thirdModel = null;
            Object fallback = constants[0];
            for (Object constant : constants) {
                String name = enumName(constant);
                if (name == null) {
                    continue;
                }
                if (name.equals("VANILLA")) {
                    vanilla = constant;
                    continue;
                }
                if (name.contains("THIRD") && name.contains("MODEL")) {
                    thirdModel = constant;
                    continue;
                }
                if (name.contains("HAND") || name.contains("ARM")) {
                    hands = constant;
                }
                if (name.contains("FIRST")) {
                    firstPerson = constant;
                }
                if (name.contains("NONE") || name.contains("DISABLED") || name.contains("OFF") || name.contains("DEFAULT")) {
                    safe = constant;
                }
            }
            if (hands != null) {
                return hands;
            }
            if (vanilla != null) {
                return vanilla;
            }
            if (firstPerson != null) {
                return firstPerson;
            }
            if (safe != null) {
                return safe;
            }
            if (fallback != null && fallback != thirdModel) {
                return fallback;
            }
            return thirdModel;
        }
 catch (Exception e) {
            Marallyzen.LOGGER.warn("LeverShake: failed to select first-person mode.", e);
            return null;
        }
    }
    private static boolean tryPlayViaEmotecraft(Object playable) {
        try {
            Class<?> keyframeClass = Class.forName("dev.kosmx.playerAnim.core.data.KeyframeAnimation");
            if (!keyframeClass.isInstance(playable)) {
                return false;
            }
            Class<?> clientPlayClass = Class.forName("io.github.kosmx.emotes.main.network.ClientEmotePlay");
            Method startLocal = clientPlayClass.getMethod("clientStartLocalEmote", keyframeClass);
            Object result = startLocal.invoke(null, playable);
            return Boolean.TRUE.equals(result);
        }
 catch (ClassNotFoundException e) {
            if (!loggedEmotecraftMissing) {
                loggedEmotecraftMissing = true;
                Marallyzen.LOGGER.info("[LeverShake] Emotecraft not present, using fallback animation path.");
            }
            return false;
        }
 catch (Exception e) {
            if (!loggedEmotecraftError) {
                loggedEmotecraftError = true;
                Marallyzen.LOGGER.warn("[LeverShake] Emotecraft local emote failed, using fallback.", e);
            }
            return false;
        }
    }
    private static String enumName(Object value) {
        if (value instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        return null;
    }
    private static boolean applyAssociatedAnimation(AbstractClientPlayer player, Object animation) {
        try {
            Class<?> accessClass = Class.forName("dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess");
            Method getData = accessClass.getMethod("getPlayerAssociatedData", AbstractClientPlayer.class);
            Object data = getData.invoke(null, player);
            if (data == null) {
                return false;
            }
            Method set = findMethod(data.getClass(), "set", 2);
            if (set == null) {
                return false;
            }
            set.invoke(data, ANIMATION_ID, animation);
            return true;
        }
 catch (Exception e) {
            Marallyzen.LOGGER.warn("LeverShake: failed to apply associated animation.", e);
            return false;
        }
    }
    private static void clearAssociatedAnimation(AbstractClientPlayer player) {
        try {
            Class<?> accessClass = Class.forName("dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess");
            Method getData = accessClass.getMethod("getPlayerAssociatedData", AbstractClientPlayer.class);
            Object data = getData.invoke(null, player);
            if (data == null) {
                return;
            }
            Method set = findMethod(data.getClass(), "set", 2);
            if (set != null) {
                set.invoke(data, ANIMATION_ID, null);
            }
        }
 catch (Exception e) {
            Marallyzen.LOGGER.warn("LeverShake: failed to clear associated animation.", e);
        }
    }
    private static Method findAddLayerMethod(Object stack, Object layer) {
        if (stack == null || layer == null) {
            return null;
        }
        for (Method method : stack.getClass().getMethods()) {
            if (!method.getName().equals("addAnimLayer") || method.getParameterCount() != 2) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            if (!isIntType(params[0])) {
                continue;
            }
            if (!params[1].isAssignableFrom(layer.getClass())) {
                continue;
            }
            return method;
        }
        return null;
    }
    private static Method findRemoveLayerMethod(Object stack, Object layer) {
        if (stack == null || layer == null) {
            return null;
        }
        for (Method method : stack.getClass().getMethods()) {
            if (!method.getName().equals("removeLayer") || method.getParameterCount() != 1) {
                continue;
            }
            Class<?> param = method.getParameterTypes()[0];
            if (param.isAssignableFrom(layer.getClass())) {
                return method;
            }
        }
        return null;
    }
    private static Method findMethod(Class<?> type, String name, int paramCount) {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == paramCount) {
                return method;
            }
        }
        return null;
    }
    private static Method findMethod(Class<?> type, String name, Class<?> paramType) {
        try {
            return type.getMethod(name, paramType);
        }
 catch (NoSuchMethodException e) {
            return null;
        }
    }
    private static boolean isIntType(Class<?> type) {
        return type == int.class || type == Integer.class;
    }
    private record LayerState(Object stack, Object layer, int entityId) {
    }
}





