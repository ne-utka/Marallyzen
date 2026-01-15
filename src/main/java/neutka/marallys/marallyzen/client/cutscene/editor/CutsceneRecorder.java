package neutka.marallys.marallyzen.client.cutscene.editor;

import net.minecraft.client.Minecraft;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;
import neutka.marallys.marallyzen.Marallyzen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Records camera movement in real-time for cutscene creation.
 * Captures camera position, rotation, and FOV at regular intervals.
 */
public class CutsceneRecorder {
    private static final Minecraft mc = Minecraft.getInstance();
    private static final CutsceneRecorder INSTANCE = new CutsceneRecorder();
    
    private boolean isRecording = false;
    private boolean isPaused = false;
    private long startTime = 0;
    private long pauseStartTime = 0;
    private long totalPauseDuration = 0;
    
    private final List<RecordedFrame> recordedFrames = new ArrayList<>();
    private final Map<UUID, RecordedActorInfo> recordedActors = new HashMap<>();
    private final Map<UUID, List<RecordedActorFrame>> recordedActorFrames = new HashMap<>();
    private final Map<UUID, List<RecordedActorEmoteFrame>> recordedActorEmotes = new HashMap<>();
    private final List<RecordedItemDropFrame> recordedItemDrops = new ArrayList<>();
    private final Map<UUID, String> lastActorEmotes = new HashMap<>();
    private final Map<UUID, Long> lastEmoteResolveLogTick = new HashMap<>();
    private final Set<UUID> trackedActors = new HashSet<>();
    private final Set<UUID> trackedItemEntities = new HashSet<>();
    private int recordingFrequency = 1; // Record every N ticks (1 = every tick)
    private boolean hasYaw = false;
    private float lastYaw = 0.0f;
    private Pose lastLocalPose;
    private boolean lastLocalCrouching;
    private boolean lastLocalSprinting;
    private boolean lastLocalSwimming;
    private boolean lastLocalFallFlying;
    private long lastLocalLogTick = Long.MIN_VALUE;
    private boolean lastLocalStateInit = false;

    private CutsceneRecorder() {}

    public static CutsceneRecorder getInstance() {
        return INSTANCE;
    }
    
    /**
     * Represents a single recorded frame.
     */
    public static class RecordedFrame {
        private final long time; // Relative time from recording start, in ticks
        private final Vec3 position;
        private final float yaw;
        private final float pitch;
        private final float fov;
        private final CutsceneEditorData.CameraMode cameraMode;

        public RecordedFrame(long time, Vec3 position, float yaw, float pitch, float fov, CutsceneEditorData.CameraMode cameraMode) {
            this.time = time;
            this.position = position;
            this.yaw = yaw;
            this.pitch = pitch;
            this.fov = fov;
            this.cameraMode = cameraMode;
        }

        public long getTime() {
            return time;
        }

        public Vec3 getPosition() {
            return position;
        }

        public float getYaw() {
            return yaw;
        }

        public float getPitch() {
            return pitch;
        }

        public float getFov() {
            return fov;
        }

        public CutsceneEditorData.CameraMode getCameraMode() {
            return cameraMode;
        }
    }

    public static class RecordedActorInfo {
        private final UUID id;
        private final String entityTypeId;
        private final boolean player;
        private final String name;
        private final String skinValue;
        private final String skinSignature;

        public RecordedActorInfo(UUID id, String entityTypeId, boolean player, String name,
                                 String skinValue, String skinSignature) {
            this.id = id;
            this.entityTypeId = entityTypeId;
            this.player = player;
            this.name = name;
            this.skinValue = skinValue;
            this.skinSignature = skinSignature;
        }

        public UUID getId() {
            return id;
        }

        public String getEntityTypeId() {
            return entityTypeId;
        }

        public boolean isPlayer() {
            return player;
        }

        public String getName() {
            return name;
        }

        public String getSkinValue() {
            return skinValue;
        }

        public String getSkinSignature() {
            return skinSignature;
        }
    }

    public static class RecordedActorFrame {
        private final long time;
        private final Vec3 position;
        private final float yaw;
        private final float pitch;
        private final float headYaw;
        private final float bodyYaw;
        private final boolean crouching;
        private final Pose pose;
        private final boolean sprinting;
        private final boolean swimming;
        private final boolean fallFlying;
        private final ItemStack mainHand;
        private final ItemStack offHand;
        private final ItemStack head;
        private final ItemStack chest;
        private final ItemStack legs;
        private final ItemStack feet;

        public RecordedActorFrame(long time, Vec3 position, float yaw, float pitch, float headYaw, float bodyYaw,
                                  boolean crouching, Pose pose, boolean sprinting, boolean swimming,
                                  boolean fallFlying, ItemStack mainHand, ItemStack offHand, ItemStack head,
                                  ItemStack chest, ItemStack legs, ItemStack feet) {
            this.time = time;
            this.position = position;
            this.yaw = yaw;
            this.pitch = pitch;
            this.headYaw = headYaw;
            this.bodyYaw = bodyYaw;
            this.crouching = crouching;
            this.pose = pose;
            this.sprinting = sprinting;
            this.swimming = swimming;
            this.fallFlying = fallFlying;
            this.mainHand = mainHand == null ? ItemStack.EMPTY : mainHand;
            this.offHand = offHand == null ? ItemStack.EMPTY : offHand;
            this.head = head == null ? ItemStack.EMPTY : head;
            this.chest = chest == null ? ItemStack.EMPTY : chest;
            this.legs = legs == null ? ItemStack.EMPTY : legs;
            this.feet = feet == null ? ItemStack.EMPTY : feet;
        }

        public long getTime() {
            return time;
        }

        public Vec3 getPosition() {
            return position;
        }

        public float getYaw() {
            return yaw;
        }

        public float getPitch() {
            return pitch;
        }

        public float getHeadYaw() {
            return headYaw;
        }

        public float getBodyYaw() {
            return bodyYaw;
        }

        public boolean isCrouching() {
            return crouching;
        }

        public Pose getPose() {
            return pose;
        }

        public boolean isSprinting() {
            return sprinting;
        }

        public boolean isSwimming() {
            return swimming;
        }

        public boolean isFallFlying() {
            return fallFlying;
        }

        public ItemStack getMainHand() {
            return mainHand;
        }

        public ItemStack getOffHand() {
            return offHand;
        }

        public ItemStack getHead() {
            return head;
        }

        public ItemStack getChest() {
            return chest;
        }

        public ItemStack getLegs() {
            return legs;
        }

        public ItemStack getFeet() {
            return feet;
        }
    }

    public static class RecordedActorEmoteFrame {
        private final long time;
        private final String emoteId;
        private final boolean stop;

        public RecordedActorEmoteFrame(long time, String emoteId) {
            this(time, emoteId, false);
        }

        public RecordedActorEmoteFrame(long time, String emoteId, boolean stop) {
            this.time = time;
            this.emoteId = emoteId;
            this.stop = stop;
        }

        public long getTime() {
            return time;
        }

        public String getEmoteId() {
            return emoteId;
        }

        public boolean isStop() {
            return stop;
        }
    }

    public static class RecordedItemDropFrame {
        private final long time;
        private final Vec3 position;
        private final Vec3 velocity;
        private final ItemStack stack;

        public RecordedItemDropFrame(long time, Vec3 position, Vec3 velocity, ItemStack stack) {
            this.time = time;
            this.position = position;
            this.velocity = velocity;
            this.stack = stack == null ? ItemStack.EMPTY : stack;
        }

        public long getTime() {
            return time;
        }

        public Vec3 getPosition() {
            return position;
        }

        public Vec3 getVelocity() {
            return velocity;
        }

        public ItemStack getStack() {
            return stack;
        }
    }

    public static class RecordedActorTracks {
        private final Map<UUID, RecordedActorInfo> actors;
        private final Map<UUID, List<RecordedActorFrame>> frames;
        private final Map<UUID, List<RecordedActorEmoteFrame>> emotes;
        private final List<RecordedItemDropFrame> itemDrops;

        public RecordedActorTracks(Map<UUID, RecordedActorInfo> actors,
                                   Map<UUID, List<RecordedActorFrame>> frames,
                                   Map<UUID, List<RecordedActorEmoteFrame>> emotes,
                                   List<RecordedItemDropFrame> itemDrops) {
            this.actors = actors;
            this.frames = frames;
            this.emotes = emotes;
            this.itemDrops = itemDrops;
        }

        public Map<UUID, RecordedActorInfo> getActors() {
            return actors;
        }

        public Map<UUID, List<RecordedActorFrame>> getFrames() {
            return frames;
        }

        public Map<UUID, List<RecordedActorEmoteFrame>> getEmotes() {
            return emotes;
        }

        public List<RecordedItemDropFrame> getItemDrops() {
            return itemDrops;
        }
    }

    /**
     * Starts recording camera movement.
     */
    public void startRecording() {
        if (isRecording) {
            Marallyzen.LOGGER.warn("Recording already in progress");
            return;
        }
        
        isRecording = true;
        isPaused = false;
        startTime = mc.level != null ? mc.level.getGameTime() : System.currentTimeMillis() / 50;
        totalPauseDuration = 0;
          recordedFrames.clear();
          recordedActors.clear();
          recordedActorFrames.clear();
          recordedActorEmotes.clear();
          recordedItemDrops.clear();
          lastActorEmotes.clear();
          trackedActors.clear();
          trackedItemEntities.clear();
        primeTrackedItems();
        
        Marallyzen.LOGGER.info("Started cutscene recording");
    }

    /**
     * Stops recording and returns recorded frames.
     */
    public void stopRecording() {
        if (!isRecording) {
            return;
        }
        
        isRecording = false;
        isPaused = false;
        
        Marallyzen.LOGGER.info("Stopped cutscene recording. Recorded {} frames", recordedFrames.size());
    }

    /**
     * Pauses recording.
     */
    public void pauseRecording() {
        if (!isRecording || isPaused) {
            return;
        }
        
        isPaused = true;
        pauseStartTime = mc.level != null ? mc.level.getGameTime() : System.currentTimeMillis() / 50;
    }

    /**
     * Resumes recording.
     */
    public void resumeRecording() {
        if (!isRecording || !isPaused) {
            return;
        }
        
        long pauseDuration = (mc.level != null ? mc.level.getGameTime() : System.currentTimeMillis() / 50) - pauseStartTime;
        totalPauseDuration += pauseDuration;
        isPaused = false;
    }

    /**
     * Records current camera state (called every tick during recording).
     */
    public void tick() {
        if (!isRecording || isPaused || mc.player == null || mc.level == null) {
            return;
        }

        long currentTick = mc.level.getGameTime();
        long relativeTime = currentTick - startTime - totalPauseDuration;
        
        // Record based on frequency
        if (relativeTime % recordingFrequency != 0) {
            return;
        }

        // Get current camera state
        Vec3 position = mc.player.getEyePosition();
        float yaw = mc.player.getYRot();
        float pitch = mc.player.getXRot();
        float fov = mc.options.fov().get().floatValue();

        if (!hasYaw) {
            lastYaw = yaw;
            hasYaw = true;
        } else {
            float delta = yaw - lastYaw;
            if (delta > 180.0f) {
                delta -= 360.0f;
            } else if (delta < -180.0f) {
                delta += 360.0f;
            }
            lastYaw = lastYaw + delta;
            yaw = lastYaw;
        }
        
        // Determine camera mode (first person = 0, third person = 1)
        int cameraType = mc.options.getCameraType().ordinal();
        CutsceneEditorData.CameraMode mode = cameraType == 0 
            ? CutsceneEditorData.CameraMode.FIRST_PERSON 
            : CutsceneEditorData.CameraMode.THIRD_PERSON;

        recordedFrames.add(new RecordedFrame(relativeTime, position, yaw, pitch, fov, mode));
    }

    public void recordActors(Vec3 cameraPosition, float cameraYaw, float cameraPitch, float cameraFov) {
        if (!isRecording || isPaused || mc.level == null) {
            return;
        }
        long currentTick = mc.level.getGameTime();
        long relativeTime = currentTick - startTime - totalPauseDuration;
        if (relativeTime % recordingFrequency != 0) {
            return;
        }
        if (cameraPosition == null) {
            return;
        }

        recordItemDrops(relativeTime);

        Vec3 forward = Vec3.directionFromRotation(cameraPitch, cameraYaw);
        double fovHalf = Math.max(1.0f, cameraFov * 0.5f) + 5.0f;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == null || entity.isRemoved()) {
                continue;
            }
            UUID uuid = entity.getUUID();
            Vec3 target = entity instanceof LivingEntity living
                ? living.getEyePosition()
                : entity.position();
            Vec3 toTarget = target.subtract(cameraPosition);
            double length = toTarget.length();
            if (length <= 0.0001) {
                if (entity == mc.player) {
                    trackedActors.add(uuid);
                    if (entity instanceof net.minecraft.world.entity.player.Player player) {
                        String lastEmote = lastActorEmotes.get(uuid);
                        Boolean emoteActive = isEmoteActive(player);
                        if (lastEmote != null && Boolean.FALSE.equals(emoteActive)) {
                            recordedActorEmotes
                                .computeIfAbsent(uuid, k -> new ArrayList<>())
                                .add(new RecordedActorEmoteFrame(relativeTime, lastEmote, true));
                            Marallyzen.LOGGER.info(
                                "[Recorder] emoteStop actor={} emote={} tick={}",
                                uuid,
                                lastEmote,
                                relativeTime
                            );
                            lastActorEmotes.remove(uuid);
                        }
                    }
                }
                continue;
            }
            double dot = forward.dot(toTarget.normalize());
            if (dot <= 0.0) {
                continue;
            }
            double angle = Math.toDegrees(Math.acos(Math.min(1.0, Math.max(-1.0, dot))));
            if (angle > fovHalf) {
                continue;
            }
            trackedActors.add(uuid);
        }

        if (trackedActors.isEmpty()) {
            return;
        }

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == null || entity.isRemoved()) {
                continue;
            }
            UUID uuid = entity.getUUID();
            if (!trackedActors.contains(uuid)) {
                continue;
            }
            RecordedActorInfo info = recordedActors.get(uuid);
            if (info == null) {
                ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
                String entityTypeId = typeId != null ? typeId.toString() : "minecraft:pig";
                boolean isPlayer = entity instanceof net.minecraft.world.entity.player.Player;
                String name = entity.getName().getString();
                String skinValue = null;
                String skinSignature = null;
                if (isPlayer && entity instanceof AbstractClientPlayer clientPlayer) {
                    GameProfile profile = clientPlayer.getGameProfile();
                    if (profile != null) {
                        Property textures = profile.getProperties().get("textures").stream().findFirst().orElse(null);
                        if (textures != null) {
                            skinValue = textures.value();
                            skinSignature = textures.signature();
                        }
                    }
                }
                info = new RecordedActorInfo(uuid, entityTypeId, isPlayer, name, skinValue, skinSignature);
                recordedActors.put(uuid, info);
            }

            Vec3 pos = entity.position();
            float yaw = entity.getYRot();
            float pitch = entity.getXRot();
            float headYaw = yaw;
            float bodyYaw = yaw;
            if (entity instanceof LivingEntity living) {
                headYaw = living.getYHeadRot();
                bodyYaw = living.yBodyRot;
            }

            boolean crouching = entity.isShiftKeyDown();
            boolean fallFlying = false;
            if (entity instanceof LivingEntity living) {
                crouching = living.isCrouching() || living.isShiftKeyDown();
                fallFlying = living.isFallFlying();
            }
            Pose pose = entity.getPose();
            boolean sprinting = entity.isSprinting();
            boolean swimming = entity.isSwimming();
            ItemStack mainHand = ItemStack.EMPTY;
            ItemStack offHand = ItemStack.EMPTY;
            ItemStack head = ItemStack.EMPTY;
            ItemStack chest = ItemStack.EMPTY;
            ItemStack legs = ItemStack.EMPTY;
            ItemStack feet = ItemStack.EMPTY;
            if (entity instanceof LivingEntity living) {
                mainHand = living.getItemBySlot(EquipmentSlot.MAINHAND).copy();
                offHand = living.getItemBySlot(EquipmentSlot.OFFHAND).copy();
                head = living.getItemBySlot(EquipmentSlot.HEAD).copy();
                chest = living.getItemBySlot(EquipmentSlot.CHEST).copy();
                legs = living.getItemBySlot(EquipmentSlot.LEGS).copy();
                feet = living.getItemBySlot(EquipmentSlot.FEET).copy();
            }
            if (entity == mc.player) {
                boolean changed = !lastLocalStateInit
                    || pose != lastLocalPose
                    || crouching != lastLocalCrouching
                    || sprinting != lastLocalSprinting
                    || swimming != lastLocalSwimming
                    || fallFlying != lastLocalFallFlying;
                if (changed || relativeTime - lastLocalLogTick >= 20) {
                    Marallyzen.LOGGER.info(
                        "Recorder local actor: t={} pose={} crouch={} sprint={} swim={} fallFly={}",
                        relativeTime,
                        pose,
                        crouching,
                        sprinting,
                        swimming,
                        fallFlying
                    );
                    lastLocalLogTick = relativeTime;
                }
                lastLocalPose = pose;
                lastLocalCrouching = crouching;
                lastLocalSprinting = sprinting;
                lastLocalSwimming = swimming;
                lastLocalFallFlying = fallFlying;
                lastLocalStateInit = true;
            }
            recordedActorFrames
                .computeIfAbsent(uuid, k -> new ArrayList<>())
                .add(new RecordedActorFrame(relativeTime, pos, yaw, pitch, headYaw, bodyYaw,
                    crouching, pose, sprinting, swimming, fallFlying, mainHand, offHand, head, chest, legs, feet));

            if (entity instanceof net.minecraft.world.entity.player.Player player) {
                String lastEmote = lastActorEmotes.get(uuid);
                Boolean emoteActive = isEmoteActive(player);
                if (lastEmote == null && Boolean.TRUE.equals(emoteActive)) {
                    String resolved = resolveActiveEmoteId(player);
                    if (resolved != null && !resolved.isBlank()) {
                        recordEmoteEvent(entity, resolved);
                        lastEmote = resolved;
                    }
                }
                if (lastEmote != null && Boolean.FALSE.equals(emoteActive)) {
                    recordedActorEmotes
                        .computeIfAbsent(uuid, k -> new ArrayList<>())
                        .add(new RecordedActorEmoteFrame(relativeTime, lastEmote, true));
                    Marallyzen.LOGGER.info(
                        "[Recorder] emoteStop actor={} emote={} tick={}",
                        uuid,
                        lastEmote,
                        relativeTime
                    );
                    lastActorEmotes.remove(uuid);
                }
            }
          }
      }

    public void recordEmoteEvent(Entity entity, String emoteId) {
        if (!isRecording || isPaused || mc.level == null || entity == null) {
            return;
        }
        if (emoteId == null || emoteId.isBlank()) {
            return;
        }
        long currentTick = mc.level.getGameTime();
        long relativeTime = currentTick - startTime - totalPauseDuration;
        UUID uuid = entity.getUUID();
        String lastEmote = lastActorEmotes.get(uuid);
        if (emoteId.equals(lastEmote)) {
            return;
        }
        Marallyzen.LOGGER.info(
            "[Recorder] emoteStart actor={} emote={} tick={}",
            uuid,
            emoteId,
            relativeTime
        );
        RecordedActorInfo info = recordedActors.get(uuid);
        if (info == null) {
            ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            String entityTypeId = typeId != null ? typeId.toString() : "minecraft:pig";
            boolean isPlayer = entity instanceof net.minecraft.world.entity.player.Player;
            String name = entity.getName().getString();
            String skinValue = null;
            String skinSignature = null;
            if (isPlayer && entity instanceof AbstractClientPlayer clientPlayer) {
                GameProfile profile = clientPlayer.getGameProfile();
                if (profile != null) {
                    Property textures = profile.getProperties().get("textures").stream().findFirst().orElse(null);
                    if (textures != null) {
                        skinValue = textures.value();
                        skinSignature = textures.signature();
                    }
                }
            }
            info = new RecordedActorInfo(uuid, entityTypeId, isPlayer, name, skinValue, skinSignature);
            recordedActors.put(uuid, info);
        }
        trackedActors.add(uuid);
        recordedActorEmotes
            .computeIfAbsent(uuid, k -> new ArrayList<>())
            .add(new RecordedActorEmoteFrame(relativeTime, emoteId, false));
        lastActorEmotes.put(uuid, emoteId);
    }

    private String resolveActiveEmoteId(net.minecraft.world.entity.player.Player player) {
        if (player == null) {
            return null;
        }
        Object emotePlayer;
        try {
            java.lang.reflect.Method getEmote = player.getClass().getMethod("emotecraft$getEmote");
            getEmote.setAccessible(true);
            emotePlayer = getEmote.invoke(player);
        } catch (Exception ignored) {
            return null;
        }
        if (emotePlayer == null) {
            return null;
        }
        Boolean activeFlag = readEmoteActive(emotePlayer);
        Object anim = resolveCurrentAnimationInstance(emotePlayer);
        if (anim == null) {
            String fallback = resolveEmoteFromPlayer(emotePlayer);
            if (fallback != null && !fallback.isBlank()) {
                return fallback;
            }
            String fieldFallback = resolveEmoteFromFields(emotePlayer);
            if (fieldFallback != null && !fieldFallback.isBlank()) {
                return fieldFallback;
            }
            if (Boolean.TRUE.equals(activeFlag)) {
                logEmoteResolveFailure(player, emotePlayer, null);
            }
            return null;
        }
        UUID uuid = resolveEmoteUuid(anim);
        String resolvedFromHolder = resolveEmoteFromHolder(anim, uuid);
        if (resolvedFromHolder != null && !resolvedFromHolder.isBlank()) {
            return resolvedFromHolder;
        }
        String name = resolveEmoteName(anim);
        if (name != null && !name.isBlank()) {
            return name;
        }
        String resolvedFromObject = resolveEmoteFromObject(anim);
        if (resolvedFromObject != null && !resolvedFromObject.isBlank()) {
            return resolvedFromObject;
        }
        String fieldFallback = resolveEmoteFromFields(anim);
        if (fieldFallback != null && !fieldFallback.isBlank()) {
            return fieldFallback;
        }
        if (uuid != null) {
            return uuid.toString();
        }
        String fallback = resolveEmoteFromPlayer(emotePlayer);
        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }
        if (Boolean.TRUE.equals(activeFlag)) {
            logEmoteResolveFailure(player, emotePlayer, anim);
        }
        return null;
    }

    private Boolean isEmoteActive(net.minecraft.world.entity.player.Player player) {
        if (player == null) {
            return null;
        }
        Object emotePlayer;
        try {
            java.lang.reflect.Method getEmote = player.getClass().getMethod("emotecraft$getEmote");
            getEmote.setAccessible(true);
            emotePlayer = getEmote.invoke(player);
        } catch (Exception ignored) {
            return null;
        }
        return readEmoteActive(emotePlayer);
    }

    private Boolean readEmoteActive(Object emotePlayer) {
        if (emotePlayer == null) {
            return null;
        }
        try {
            java.lang.reflect.Method isActive = findMethod(emotePlayer.getClass(), "isActive");
            if (isActive != null) {
                isActive.setAccessible(true);
                Object active = isActive.invoke(emotePlayer);
                if (active instanceof Boolean b) {
                    return b;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String resolveEmoteFromPlayer(Object emotePlayer) {
        String[] methodNames = new String[] {
            "getEmoteId",
            "getEmoteName",
            "getCurrentEmoteId",
            "getCurrentEmoteName",
            "getEmote"
        };
        for (String name : methodNames) {
            try {
                java.lang.reflect.Method m = findMethod(emotePlayer.getClass(), name);
                if (m == null) {
                    continue;
                }
                m.setAccessible(true);
                Object val = m.invoke(emotePlayer);
                if (val == null) {
                    continue;
                }
                if (val instanceof net.minecraft.resources.ResourceLocation id) {
                    return id.toString();
                }
                if (val instanceof String s) {
                    return s;
                }
                String resolved = resolveEmoteFromObject(val);
                if (resolved != null && !resolved.isBlank()) {
                    return resolved;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private String resolveEmoteFromObject(Object obj) {
        if (obj == null) {
            return null;
        }
        Object value = extractValueFromObject(obj, new String[] {
            "getEmoteId",
            "getEmoteName",
            "getCurrentEmoteId",
            "getCurrentEmoteName",
            "getId",
            "getName",
            "getKey",
            "getEmote",
            "getAnimation",
            "getCurrentAnimation",
            "getAnimationId",
            "getAnimationKey"
        });
        String resolved = resolveEmoteValue(value);
        if (resolved != null && !resolved.isBlank()) {
            return resolved;
        }
        String resolvedFromMethods = resolveEmoteFromMethods(obj);
        if (resolvedFromMethods != null && !resolvedFromMethods.isBlank()) {
            return resolvedFromMethods;
        }
        String fieldResolved = resolveEmoteFromFields(obj);
        if (fieldResolved != null && !fieldResolved.isBlank()) {
            return fieldResolved;
        }
        String holderName = extractHolderName(obj.getClass(), obj);
        if (holderName != null && !holderName.isBlank()) {
            return holderName;
        }
        return null;
    }

    private String resolveEmoteFromMethods(Object obj) {
        if (obj == null) {
            return null;
        }
        String className = obj.getClass().getName();
        if (!className.contains("EmotePlayImpl")) {
            return null;
        }
        java.util.List<java.lang.reflect.Method> methods = new java.util.ArrayList<>();
        Class<?> current = obj.getClass();
        while (current != null && current != Object.class) {
            for (java.lang.reflect.Method method : current.getDeclaredMethods()) {
                methods.add(method);
            }
            current = current.getSuperclass();
        }
        for (java.lang.reflect.Method method : obj.getClass().getMethods()) {
            methods.add(method);
        }
        for (java.lang.reflect.Method method : methods) {
            if (method.getParameterCount() != 0) {
                continue;
            }
            Class<?> returnType = method.getReturnType();
            if (returnType == Void.TYPE) {
                continue;
            }
            try {
                method.setAccessible(true);
                Object val = method.invoke(obj);
                String resolved = resolveEmoteValue(val);
                if (resolved != null && !resolved.isBlank()) {
                    return resolved;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private Object extractValueFromObject(Object obj, String[] methodNames) {
        if (obj == null) {
            return null;
        }
        for (String name : methodNames) {
            try {
                java.lang.reflect.Method m = findMethod(obj.getClass(), name);
                if (m == null) {
                    continue;
                }
                m.setAccessible(true);
                Object val = m.invoke(obj);
                if (val != null) {
                    return val;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private String resolveEmoteFromFields(Object obj) {
        if (obj == null) {
            return null;
        }
        Class<?> current = obj.getClass();
        while (current != null && current != Object.class) {
            java.lang.reflect.Field[] fields = current.getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                String name = field.getName();
                String lower = name == null ? "" : name.toLowerCase();
                if (!lower.contains("emote")
                    && !lower.contains("anim")
                    && !lower.contains("animation")
                    && !lower.contains("id")
                    && !lower.contains("name")) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object val = field.get(obj);
                    String resolved = resolveEmoteValue(val);
                    if (resolved != null && !resolved.isBlank()) {
                        return resolved;
                    }
                } catch (Exception ignored) {
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private String resolveEmoteValue(Object val) {
        if (val == null) {
            return null;
        }
        if (val instanceof java.util.Optional<?> opt) {
            val = opt.orElse(null);
            if (val == null) {
                return null;
            }
        }
        if (val instanceof net.minecraft.resources.ResourceLocation id) {
            return id.toString();
        }
        if (val instanceof UUID uuid) {
            return uuid.toString();
        }
        if (val instanceof String s) {
            return s;
        }
        String holderName = extractHolderName(val.getClass(), val);
        if (holderName != null && !holderName.isBlank()) {
            return holderName;
        }
        return null;
    }

    private void logEmoteResolveFailure(net.minecraft.world.entity.player.Player player,
                                        Object emotePlayer,
                                        Object anim) {
        if (player == null || mc.level == null) {
            return;
        }
        long tick = mc.level.getGameTime();
        UUID uuid = player.getUUID();
        Long lastTick = lastEmoteResolveLogTick.get(uuid);
        if (lastTick != null && tick - lastTick < 20) {
            return;
        }
        lastEmoteResolveLogTick.put(uuid, tick);
        String emotePlayerClass = emotePlayer != null ? emotePlayer.getClass().getName() : "null";
        String animClass = anim != null ? anim.getClass().getName() : "null";
        Marallyzen.LOGGER.info(
            "[Recorder] emote active but could not resolve id for {} (emotePlayer={}, anim={})",
            player.getName().getString(),
            emotePlayerClass,
            animClass
        );
        String emotePlayerFields = emotePlayer != null ? resolveEmoteFromFields(emotePlayer) : null;
        String animFields = anim != null ? resolveEmoteFromFields(anim) : null;
        if ((emotePlayerFields != null && !emotePlayerFields.isBlank())
            || (animFields != null && !animFields.isBlank())) {
            Marallyzen.LOGGER.info(
                "[Recorder] emote unresolved details: emotePlayerField={} animField={}",
                emotePlayerFields,
                animFields
            );
        }
        logEmoteReflectionSnapshot(emotePlayer);
    }

    private void logEmoteReflectionSnapshot(Object emotePlayer) {
        if (emotePlayer == null) {
            return;
        }
        String className = emotePlayer.getClass().getName();
        if (!className.contains("EmotePlayImpl")) {
            return;
        }
        try {
            StringBuilder fields = new StringBuilder();
            Class<?> current = emotePlayer.getClass();
            while (current != null && current != Object.class) {
                for (java.lang.reflect.Field field : current.getDeclaredFields()) {
                    field.setAccessible(true);
                    Object val = null;
                    try {
                        val = field.get(emotePlayer);
                    } catch (Exception ignored) {
                    }
                    String valueString = null;
                    if (val instanceof java.util.Optional<?> opt) {
                        val = opt.orElse(null);
                    }
                    if (val instanceof net.minecraft.resources.ResourceLocation id) {
                        valueString = id.toString();
                    } else if (val instanceof UUID uuid) {
                        valueString = uuid.toString();
                    } else if (val instanceof String s) {
                        valueString = s;
                    }
                    fields.append(field.getName())
                        .append(":")
                        .append(field.getType().getSimpleName());
                    if (valueString != null && !valueString.isBlank()) {
                        fields.append("=").append(valueString);
                    }
                    fields.append(", ");
                }
                current = current.getSuperclass();
            }
            String fieldDump = fields.length() > 0
                ? fields.substring(0, fields.length() - 2)
                : "";
            Marallyzen.LOGGER.info("[Recorder] EmotePlayImpl fields: {}", fieldDump);
            StringBuilder methods = new StringBuilder();
            int count = 0;
            java.util.List<java.lang.reflect.Method> allMethods = new java.util.ArrayList<>();
            Class<?> methodClass = emotePlayer.getClass();
            while (methodClass != null && methodClass != Object.class) {
                for (java.lang.reflect.Method method : methodClass.getDeclaredMethods()) {
                    allMethods.add(method);
                }
                methodClass = methodClass.getSuperclass();
            }
            for (java.lang.reflect.Method method : emotePlayer.getClass().getMethods()) {
                allMethods.add(method);
            }
            for (java.lang.reflect.Method method : allMethods) {
                if (method.getParameterCount() != 0) {
                    continue;
                }
                Class<?> returnType = method.getReturnType();
                if (returnType == Void.TYPE) {
                    continue;
                }
                methods.append(method.getName())
                    .append(":")
                    .append(returnType.getSimpleName())
                    .append(", ");
                count++;
                if (count >= 30) {
                    break;
                }
            }
            String methodDump = methods.length() > 0
                ? methods.substring(0, methods.length() - 2)
                : "";
            Marallyzen.LOGGER.info("[Recorder] EmotePlayImpl methods: {}", methodDump);
        } catch (Exception ignored) {
        }
    }

    private Object resolveCurrentAnimationInstance(Object emotePlayer) {
        if (emotePlayer == null) {
            return null;
        }
        String[] methodNames = new String[] {
            "getCurrentAnimationInstance",
            "getCurrentAnimation",
            "getCurrentEmote",
            "getEmoteInstance"
        };
        for (String name : methodNames) {
            try {
                java.lang.reflect.Method current = findMethod(emotePlayer.getClass(), name);
                if (current != null) {
                    current.setAccessible(true);
                    Object anim = current.invoke(emotePlayer);
                    if (anim != null) {
                        return anim;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private java.lang.reflect.Method findMethod(Class<?> target, String name, Class<?>... params) {
        if (target == null || name == null) {
            return null;
        }
        Class<?> current = target;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredMethod(name, params);
            } catch (NoSuchMethodException ignored) {
            }
            current = current.getSuperclass();
        }
        try {
            return target.getMethod(name, params);
        } catch (NoSuchMethodException ignored) {
        }
        return null;
    }

    private UUID resolveEmoteUuid(Object anim) {
        try {
            java.lang.reflect.Method data = anim.getClass().getMethod("data");
            Object extra = data.invoke(anim);
            if (extra == null) {
                return null;
            }
            try {
                java.lang.reflect.Field f = extra.getClass().getField("UUID_KEY");
                Object key = f.get(null);
                if (key instanceof String s) {
                    java.lang.reflect.Method get = extra.getClass().getMethod("get", String.class);
                    Object val = get.invoke(extra, s);
                    if (val instanceof UUID u) {
                        return u;
                    }
                }
            } catch (Exception ignored) {
            }
            try {
                java.lang.reflect.Method get = extra.getClass().getMethod("get", String.class);
                Object val = get.invoke(extra, "uuid");
                if (val instanceof UUID u) {
                    return u;
                }
            } catch (Exception ignored) {
            }
        } catch (Exception ignored) {
        }
        try {
            java.lang.reflect.Method getUuid = anim.getClass().getMethod("getUuid");
            Object val = getUuid.invoke(anim);
            if (val instanceof UUID u) {
                return u;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String resolveEmoteName(Object anim) {
        try {
            java.lang.reflect.Method data = anim.getClass().getMethod("data");
            Object extra = data.invoke(anim);
            if (extra == null) {
                return null;
            }
            try {
                java.lang.reflect.Field f = extra.getClass().getField("NAME_KEY");
                Object key = f.get(null);
                if (key instanceof String s) {
                    java.lang.reflect.Method get = extra.getClass().getMethod("get", String.class);
                    Object val = get.invoke(extra, s);
                    if (val != null) {
                        return val.toString();
                    }
                }
            } catch (Exception ignored) {
            }
            try {
                java.lang.reflect.Method get = extra.getClass().getMethod("get", String.class);
                Object val = get.invoke(extra, "name");
                if (val != null) {
                    return val.toString();
                }
            } catch (Exception ignored) {
            }
        } catch (Exception ignored) {
        }
        try {
            java.lang.reflect.Method getName = anim.getClass().getMethod("getName");
            Object val = getName.invoke(anim);
            if (val != null) {
                return val.toString();
            }
        } catch (Exception ignored) {
        }
        try {
            java.lang.reflect.Method getId = anim.getClass().getMethod("getId");
            Object val = getId.invoke(anim);
            if (val != null) {
                return val.toString();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String resolveEmoteFromHolder(Object anim, UUID uuid) {
        try {
            Class<?> holderClass = Class.forName("io.github.kosmx.emotes.main.EmoteHolder");
            java.lang.reflect.Field listField = holderClass.getField("list");
            Object listObj = listField.get(null);
            java.lang.reflect.Method values = listObj.getClass().getMethod("values");
            java.util.Collection<?> holders = (java.util.Collection<?>) values.invoke(listObj);
            for (Object holder : holders) {
                try {
                    java.lang.reflect.Field emoteField = holderClass.getField("emote");
                    Object emoteAnim = emoteField.get(holder);
                    if (emoteAnim == anim) {
                        return extractHolderName(holderClass, holder);
                    }
                } catch (Exception ignored) {
                }
                if (uuid != null) {
                    try {
                        java.lang.reflect.Method getUuid = holderClass.getMethod("getUuid");
                        Object holderUuid = getUuid.invoke(holder);
                        if (uuid.equals(holderUuid)) {
                            return extractHolderName(holderClass, holder);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String extractHolderName(Class<?> holderClass, Object holder) {
        try {
            java.lang.reflect.Field nameField = holderClass.getField("name");
            Object nameComponent = nameField.get(holder);
            java.lang.reflect.Method getString = nameComponent.getClass().getMethod("getString");
            return (String) getString.invoke(nameComponent);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Converts recorded frames to camera keyframes.
     * Groups consecutive frames with similar camera mode and creates keyframes with duration.
     */
    public List<CutsceneEditorData.CameraKeyframe> convertToKeyframes(long baseTime) {
        List<CutsceneEditorData.CameraKeyframe> keyframes = new ArrayList<>();
        
        if (recordedFrames.isEmpty()) {
            return keyframes;
        }

        for (int i = 0; i < recordedFrames.size(); i++) {
            RecordedFrame frame = recordedFrames.get(i);
            long duration;
            if (i < recordedFrames.size() - 1) {
                duration = Math.max(1, recordedFrames.get(i + 1).getTime() - frame.getTime());
            } else {
                duration = Math.max(1, recordingFrequency);
            }
            keyframes.add(new CutsceneEditorData.CameraKeyframe(
                baseTime + frame.getTime(),
                frame.getPosition(),
                frame.getYaw(),
                frame.getPitch(),
                frame.getFov(),
                1,
                false
            ));
        }

        return keyframes;
    }

    public List<CutsceneEditorData.ActorKeyframe> convertToActorKeyframes(long baseTime) {
        List<CutsceneEditorData.ActorKeyframe> keyframes = new ArrayList<>();
        if (recordedFrames.isEmpty()) {
            return keyframes;
        }
        for (int i = 0; i < recordedFrames.size(); i++) {
            RecordedFrame frame = recordedFrames.get(i);
            long duration;
            if (i < recordedFrames.size() - 1) {
                duration = Math.max(1, recordedFrames.get(i + 1).getTime() - frame.getTime());
            } else {
                duration = Math.max(1, recordingFrequency);
            }
            keyframes.add(new CutsceneEditorData.ActorKeyframe(
                baseTime + frame.getTime(),
                frame.getPosition(),
                frame.getYaw(),
                frame.getPitch(),
                duration
            ));
        }
        return keyframes;
    }

    public List<CutsceneEditorData.EmotionKeyframe> convertToActorEmoteKeyframes(long baseTime) {
        List<CutsceneEditorData.EmotionKeyframe> keyframes = new ArrayList<>();
        if (recordedActorEmotes.isEmpty()) {
            return keyframes;
        }
        for (Map.Entry<UUID, List<RecordedActorEmoteFrame>> entry : recordedActorEmotes.entrySet()) {
            UUID uuid = entry.getKey();
            List<RecordedActorEmoteFrame> frames = entry.getValue();
            if (frames == null || frames.isEmpty()) {
                continue;
            }
            for (RecordedActorEmoteFrame frame : frames) {
                if (frame.isStop()) {
                    continue;
                }
                if (frame.getEmoteId() == null || frame.getEmoteId().isBlank()) {
                    continue;
                }
                keyframes.add(new CutsceneEditorData.EmotionKeyframe(
                    baseTime + frame.getTime(),
                    uuid.toString(),
                    frame.getEmoteId()
                ));
            }
        }
        return keyframes;
    }

    /**
     * Clears all recorded frames.
     */
    public void clear() {
        recordedFrames.clear();
        recordedActors.clear();
        recordedActorFrames.clear();
        recordedActorEmotes.clear();
        recordedItemDrops.clear();
        trackedActors.clear();
        trackedItemEntities.clear();
        isRecording = false;
        isPaused = false;
        totalPauseDuration = 0;
        hasYaw = false;
        lastYaw = 0.0f;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public List<RecordedFrame> getRecordedFrames() {
        return new ArrayList<>(recordedFrames);
    }

    public RecordedActorTracks snapshotRecordedActors() {
        Map<UUID, RecordedActorInfo> actorCopy = new HashMap<>(recordedActors);
        Map<UUID, List<RecordedActorFrame>> framesCopy = new HashMap<>();
        for (Map.Entry<UUID, List<RecordedActorFrame>> entry : recordedActorFrames.entrySet()) {
            framesCopy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        Map<UUID, List<RecordedActorEmoteFrame>> emotesCopy = new HashMap<>();
        for (Map.Entry<UUID, List<RecordedActorEmoteFrame>> entry : recordedActorEmotes.entrySet()) {
            emotesCopy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        List<RecordedItemDropFrame> dropsCopy = new ArrayList<>(recordedItemDrops);
        return new RecordedActorTracks(actorCopy, framesCopy, emotesCopy, dropsCopy);
    }

    public boolean hasRecordedActors() {
        return !recordedActorFrames.isEmpty()
            || !recordedActorEmotes.isEmpty()
            || !recordedItemDrops.isEmpty();
    }

    public int getRecordingFrequency() {
        return recordingFrequency;
    }

    public void setRecordingFrequency(int frequency) {
        this.recordingFrequency = Math.max(1, frequency);
    }

    private void primeTrackedItems() {
        if (mc.level == null) {
            return;
        }
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof ItemEntity) {
                trackedItemEntities.add(entity.getUUID());
            }
        }
    }

    private void recordItemDrops(long relativeTime) {
        if (mc.level == null) {
            return;
        }
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof ItemEntity itemEntity)) {
                continue;
            }
            if (itemEntity.isRemoved()) {
                continue;
            }
            UUID uuid = itemEntity.getUUID();
            if (trackedItemEntities.contains(uuid)) {
                continue;
            }
            trackedItemEntities.add(uuid);
            ItemStack stack = itemEntity.getItem();
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            recordedItemDrops.add(new RecordedItemDropFrame(
                relativeTime,
                itemEntity.position(),
                itemEntity.getDeltaMovement(),
                stack.copy()
            ));
        }
    }

}


