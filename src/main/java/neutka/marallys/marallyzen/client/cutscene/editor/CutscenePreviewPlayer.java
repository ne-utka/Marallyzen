package neutka.marallys.marallyzen.client.cutscene.editor;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.client.camera.CameraController;
import neutka.marallys.marallyzen.client.camera.CameraManager;
import neutka.marallys.marallyzen.client.cutscene.world.CutsceneWorldPlayback;
import neutka.marallys.marallyzen.cutscene.world.CutsceneWorldTrack;
import neutka.marallys.marallyzen.npc.GeckoNpcEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Preview player for cutscenes in the editor.
 * Plays back cutscene data in real-time for preview.
 */
public class CutscenePreviewPlayer {
    private static final Minecraft mc = Minecraft.getInstance();
    private static final int SHARED_FLAG_SWIMMING = 4;
    private static final int SHARED_FLAG_FALL_FLYING = 7;
    private static final String PREVIEW_HIDE_TEAM = "marallyzen_preview_hide";
    private static CutsceneEditorData activePreviewData;
    private static long activePreviewTime;
    private static boolean activePreviewPlaying;
    private static int activePreviewKeyframeIndex = -1;
    private static CutscenePreviewPlayer activePreviewInstance;
    private static boolean hideHandInPreview = false;
    private static CutsceneEditorData.CameraKeyframe activePreviewCameraOverride;
    private static List<CutsceneEditorData.CameraKeyframe> activePreviewPath;
    private static boolean fixedCameraMode = true;
    private static List<CutsceneEditorData.ActorKeyframe> activePreviewActorPath;
    
    private boolean isPlaying = false;
    private CutsceneEditorData data;
    private long startTime = 0;
    private long currentTime = 0;
    private int currentKeyframeIndex = 0;
    private Boolean prevSmoothCamera = null;
    private CutsceneRecorder.RecordedActorTracks recordedActorTracks;
    private Map<UUID, List<CutsceneRecorder.RecordedActorEmoteFrame>> recordedActorEmotes = new HashMap<>();
    private List<CutsceneRecorder.RecordedItemDropFrame> recordedItemDrops = new ArrayList<>();
    private final Map<UUID, Integer> actorEmoteIndices = new HashMap<>();
    private int itemDropIndex = 0;
    private final Map<UUID, GhostActor> ghostActors = new HashMap<>();
    private final Map<UUID, Long> ghostAnimTicks = new HashMap<>();
    private final List<Integer> previewDropEntityIds = new ArrayList<>();
    private boolean useActorGhosts = false;
    private int nextGhostEntityId = -1000;
    private UUID previewLogActorId;
    private Pose previewLogPose;
    private boolean previewLogCrouching;
    private boolean previewLogSprinting;
    private boolean previewLogSwimming;
    private boolean previewLogFallFlying;
    private long previewLogTick = Long.MIN_VALUE;
    private boolean previewLogInit = false;
    private java.lang.reflect.Field cachedWalkAnimationField;
    private java.lang.reflect.Method cachedWalkAnimationUpdate;
    private java.lang.reflect.Method cachedWalkAnimationSetSpeed;
    private boolean cachedWalkAnimationResolved = false;
    private java.lang.reflect.Field cachedLimbSwingField;
    private java.lang.reflect.Field cachedLimbSwingAmountField;
    private boolean cachedLimbFieldsResolved = false;
    private java.lang.reflect.Method cachedSetSharedFlag;
    private boolean cachedSetSharedFlagResolved = false;
    private CutsceneWorldPlayback worldPlayback;
    private boolean useRealtimeClock = false;
    
    private CameraController cameraController;
    private CutsceneCameraController editorCameraController;

    public CutscenePreviewPlayer() {
        this.cameraController = CameraManager.getInstance().getCameraController();
        this.editorCameraController = new CutsceneCameraController();
    }

    /**
     * Starts preview playback.
     */
    public void start(CutsceneEditorData data) {
        start(data, data != null ? data.getRecordedActorTracks() : null);
    }

    public void start(CutsceneEditorData data, CutsceneRecorder.RecordedActorTracks actorTracks) {
        if (isPlaying) {
            stop();
        }
        
        this.data = data;
        this.isPlaying = true;
        this.startTime = 0;
        this.currentTime = 0;
        this.currentKeyframeIndex = 0;
        this.recordedActorTracks = actorTracks;
        this.useActorGhosts = false;
        this.ghostActors.clear();
        this.nextGhostEntityId = -1000;
        this.recordedActorEmotes.clear();
        this.recordedItemDrops.clear();
        this.actorEmoteIndices.clear();
        this.ghostAnimTicks.clear();
        this.previewDropEntityIds.clear();
        this.itemDropIndex = 0;
        this.previewLogActorId = null;
        this.previewLogInit = false;
        this.previewLogTick = Long.MIN_VALUE;

        activePreviewData = data;
        activePreviewTime = 0;
        activePreviewPlaying = true;
        activePreviewKeyframeIndex = -1;
        activePreviewInstance = this;
        activePreviewCameraOverride = null;
        activePreviewPath = null;
        activePreviewActorPath = null;
        removeActorGhosts();
        removePreviewDrops();
        buildPreviewOverrides(data);
        logPreviewCameraSetup();

        CutsceneWorldTrack worldTrack = data != null ? data.getWorldTrack() : null;
        if (worldTrack != null && !worldTrack.getChunks().isEmpty()) {
            worldPlayback = new CutsceneWorldPlayback();
            worldPlayback.start(worldTrack);
        } else {
            worldPlayback = null;
            if (worldTrack != null) {
                Marallyzen.LOGGER.warn("Cutscene world track has no chunks; preview will use live world.");
            }
        }
        useRealtimeClock = worldPlayback != null;
        applyInitialCameraState();
        this.startTime = useRealtimeClock
            ? System.currentTimeMillis() / 50
            : (mc.level != null ? mc.level.getGameTime() : System.currentTimeMillis() / 50);

        cameraController.activate(true);
        
        editorCameraController.setBlockInput(true);
        editorCameraController.saveCameraType();

        if (mc.options != null) {
            prevSmoothCamera = mc.options.smoothCamera;
            mc.options.smoothCamera = false;
        }

        ActorTrackStatus trackStatus = evaluateActorTracks(recordedActorTracks);
        if (recordedActorTracks != null && recordedActorTracks.getItemDrops() != null) {
            recordedItemDrops = new ArrayList<>(recordedActorTracks.getItemDrops());
        }
        boolean usedRecordedTracks = false;
        if ("usable".equals(trackStatus.state())) {
            spawnActorGhosts(recordedActorTracks);
            usedRecordedTracks = useActorGhosts;
            if (usedRecordedTracks && recordedActorTracks.getEmotes() != null) {
                recordedActorEmotes = new HashMap<>(recordedActorTracks.getEmotes());
            }
            if (usedRecordedTracks) {
                Marallyzen.LOGGER.info("Preview uses RecordedActorTracks: {} actor(s)", trackStatus.actorCount());
            } else {
                trackStatus = new ActorTrackStatus("invalid", trackStatus.actorCount(), "ghosts not created");
            }
        }
        if (!usedRecordedTracks) {
            if (recordedActorTracks != null) {
                if (trackStatus.detail() != null) {
                    Marallyzen.LOGGER.warn(
                        "Preview skips actor keyframe fallback; RecordedActorTracks is {} ({})",
                        trackStatus.state(),
                        trackStatus.detail()
                    );
                } else {
                    Marallyzen.LOGGER.warn(
                        "Preview skips actor keyframe fallback; RecordedActorTracks is {}",
                        trackStatus.state()
                    );
                }
            } else if (activePreviewActorPath != null && !activePreviewActorPath.isEmpty()) {
                spawnActorGhostsFromKeyframes(activePreviewActorPath);
                Marallyzen.LOGGER.info(
                    "Preview uses actor keyframe fallback: {} keyframe(s) (RecordedActorTracks {})",
                    activePreviewActorPath.size(),
                    trackStatus.state()
                );
            } else {
                Marallyzen.LOGGER.info(
                    "Preview has no actor tracks (RecordedActorTracks {} and actor keyframes empty)",
                    trackStatus.state()
                );
            }
        }
        selectPreviewLogActor();
        
        Marallyzen.LOGGER.info("Started cutscene preview");
    }

    /**
     * Stops preview playback.
     */
    public void stop() {
        if (!isPlaying) {
            return;
        }
        
        isPlaying = false;
        activePreviewPlaying = false;
        activePreviewData = null;
        activePreviewTime = 0;
        activePreviewKeyframeIndex = -1;
        activePreviewInstance = null;
        activePreviewCameraOverride = null;
        activePreviewPath = null;
        activePreviewActorPath = null;

        if (worldPlayback != null) {
            worldPlayback.stop();
            worldPlayback = null;
        }

        cameraController.deactivate();
        editorCameraController.setBlockInput(false);
        editorCameraController.restoreCameraType();
        if (mc.options != null && prevSmoothCamera != null) {
            mc.options.smoothCamera = prevSmoothCamera;
            prevSmoothCamera = null;
        }
        removeActorGhosts();
        removePreviewDrops();
        recordedItemDrops.clear();
        itemDropIndex = 0;
        previewLogActorId = null;
        previewLogInit = false;
        previewLogTick = Long.MIN_VALUE;
        useRealtimeClock = false;
        
        Marallyzen.LOGGER.info("Stopped cutscene preview");
    }

    /**
     * Updates preview playback (called every tick).
     */
    public void tick() {
        if (!isPlaying || data == null || mc.level == null) {
            return;
        }

        long levelTime = useRealtimeClock
            ? System.currentTimeMillis() / 50
            : mc.level.getGameTime();
        currentTime = levelTime - startTime;
        activePreviewTime = currentTime;

        if (worldPlayback != null) {
            worldPlayback.tick(currentTime);
        }

        List<CutsceneEditorData.EditorKeyframe> keyframes = data.getKeyframes();
        
        // Process keyframes up to current time
        while (currentKeyframeIndex < keyframes.size()) {
            CutsceneEditorData.EditorKeyframe kf = keyframes.get(currentKeyframeIndex);
            
            if (currentTime >= kf.getTime()) {
                processKeyframe(kf);
                currentKeyframeIndex++;
            } else {
                break;
            }
        }
        activePreviewKeyframeIndex = Math.min(currentKeyframeIndex - 1, keyframes.size() - 1);

        // Check if preview is finished
        if (currentKeyframeIndex >= keyframes.size() && currentTime >= data.getTotalDuration()) {
            stop();
        }

        if (useActorGhosts) {
            tickActorEmotes();
            tickActorGhosts();
        }
        tickItemDrops();
        boolean previewCameraApplied = applyPreviewCamera(currentTime, cameraController);
        if ((fixedCameraMode || activePreviewCameraOverride != null) && previewCameraApplied) {
            if (worldPlayback != null) {
                worldPlayback.useCameraEntity(cameraController.getPosition(), cameraController.getYaw(), cameraController.getPitch());
            } else {
                syncCameraEntity(cameraController.getPosition(), cameraController.getYaw(), cameraController.getPitch());
            }
        }

        if (mc.player != null && !useActorGhosts && !fixedCameraMode && activePreviewCameraOverride == null) {
            PreviewState prevState;
            PreviewState curState;
            List<CutsceneEditorData.CameraKeyframe> path = activePreviewPath;
            prevState = getPreviewState(path, currentTime - 1.0f);
            curState = getPreviewState(path, currentTime);
            if (prevState != null && curState != null && curState.position != null) {
                mc.player.xo = prevState.position.x;
                mc.player.yo = prevState.position.y;
                mc.player.zo = prevState.position.z;
                mc.player.setPos(curState.position.x, curState.position.y, curState.position.z);
                mc.player.setDeltaMovement(Vec3.ZERO);
                mc.player.setYRot(curState.yaw);
                mc.player.setXRot(curState.pitch);
            }
        }
    }

    /**
     * Processes a keyframe during preview.
     */
    private void processKeyframe(CutsceneEditorData.EditorKeyframe keyframe) {
        switch (keyframe.getType()) {
            case CAMERA -> {
                // Camera keyframes are handled by applyInterpolatedCamera for smooth playback.
            }
            case PAUSE -> {
                // Pause is handled by time progression - just wait
                CutsceneEditorData.PauseKeyframe pauseKf = (CutsceneEditorData.PauseKeyframe) keyframe;
                // Time progression will naturally handle the pause
            }
            case EMOTION -> {
                CutsceneEditorData.EmotionKeyframe emotionKf = (CutsceneEditorData.EmotionKeyframe) keyframe;
                String targetId = emotionKf.getNpcId();
                if (targetId == null || targetId.isBlank()) {
                    return;
                }
                try {
                    UUID uuid = java.util.UUID.fromString(targetId);
                    if (useActorGhosts && recordedActorEmotes.containsKey(uuid)) {
                        return;
                    }
                    if (useActorGhosts) {
                        GhostActor ghost = ghostActors.get(uuid);
                        if (ghost != null && ghost.entity != null) {
                            uuid = ghost.entity.getUUID();
                        }
                    }
                    Marallyzen.LOGGER.info(
                        "PreviewEmote: via EmotionKeyframe -> ClientEmoteHandler.handle(target={}, emote={})",
                        uuid,
                        emotionKf.getEmoteId()
                    );
                    neutka.marallys.marallyzen.client.emote.ClientEmoteHandler.handle(uuid, emotionKf.getEmoteId(), false);
                } catch (IllegalArgumentException e) {
                    Marallyzen.LOGGER.warn("Failed to parse emote target UUID '{}'", targetId);
                } catch (Exception e) {
                    Marallyzen.LOGGER.warn("Failed to play emote in preview", e);
                }
            }
            case CAMERA_MODE -> {
                CutsceneEditorData.CameraModeKeyframe modeKf = (CutsceneEditorData.CameraModeKeyframe) keyframe;
                editorCameraController.setCameraMode(modeKf.getMode());
            }
            case ACTOR -> {
                // Actor movement is handled by per-tick interpolation.
            }
        }
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public long getCurrentTime() {
        return currentTime;
    }

    public static boolean applyPreviewCamera(float time, CameraController controller) {
        CutsceneEditorData data = activePreviewData;
        if (!activePreviewPlaying || data == null || controller == null) {
            return false;
        }
        if (activePreviewCameraOverride != null) {
            CutsceneEditorData.CameraKeyframe cam = activePreviewCameraOverride;
            controller.setRawState(cam.getPosition(), cam.getYaw(), cam.getPitch(), cam.getFov());
            return true;
        }
        if (fixedCameraMode && activePreviewCameraOverride == null
            && activePreviewPath != null && !activePreviewPath.isEmpty()) {
            CutsceneEditorData.CameraKeyframe cam = activePreviewPath.get(0);
            controller.setRawState(cam.getPosition(), cam.getYaw(), cam.getPitch(), cam.getFov());
            return true;
        }
        PreviewState state = getPreviewState(activePreviewPath, time);
        if (state == null) {
            return false;
        }
        controller.setRawState(state.position, state.yaw, state.pitch, state.fov);
        return true;
    }

    public static void renderActiveGhostActors(float partialTick) {
        if (activePreviewInstance == null || !activePreviewPlaying) {
            return;
        }
        activePreviewInstance.renderGhostActors(partialTick);
    }

    private static PreviewState getPreviewState(List<CutsceneEditorData.CameraKeyframe> keyframes, float time) {
        if (keyframes == null || keyframes.isEmpty()) {
            return null;
        }

        CutsceneEditorData.CameraKeyframe prev = null;
        CutsceneEditorData.CameraKeyframe next = null;
        for (CutsceneEditorData.CameraKeyframe cameraKf : keyframes) {
            if (cameraKf.getTime() <= time) {
                prev = cameraKf;
            }
            if (cameraKf.getTime() > time) {
                next = cameraKf;
                break;
            }
        }

        if (prev == null && next == null) {
            return null;
        }
        if (prev == null) {
            prev = next;
        }
        if (next == null) {
            next = prev;
        }

        long span = Math.max(1, next.getTime() - prev.getTime());
        float t = (time - prev.getTime()) / (float) span;
        t = Math.min(1.0f, Math.max(0.0f, t));

        Vec3 pos = lerp(prev.getPosition(), next.getPosition(), t);
        float yaw = lerpAngle(prev.getYaw(), next.getYaw(), t);
        float pitch = lerpAngle(prev.getPitch(), next.getPitch(), t);
        float fov = lerp(prev.getFov(), next.getFov(), t);
        return new PreviewState(pos, yaw, pitch, fov);
    }

    private void applyInitialCameraState() {
        CutsceneEditorData.CameraKeyframe start = resolveInitialCameraKeyframe();
        if (start == null) {
            return;
        }
        cameraController.setRawState(start.getPosition(), start.getYaw(), start.getPitch(), start.getFov());
        if (worldPlayback != null) {
            worldPlayback.useCameraEntity(start.getPosition(), start.getYaw(), start.getPitch());
        } else {
            syncCameraEntity(start.getPosition(), start.getYaw(), start.getPitch());
        }
    }

    private CutsceneEditorData.CameraKeyframe resolveInitialCameraKeyframe() {
        if (activePreviewCameraOverride != null) {
            return activePreviewCameraOverride;
        }
        if (activePreviewPath != null && !activePreviewPath.isEmpty()) {
            return activePreviewPath.get(0);
        }
        return null;
    }

    private void syncCameraEntity(Vec3 position, float yaw, float pitch) {
        if (position == null) {
            return;
        }
        Entity cameraEntity = mc.getCameraEntity();
        if (cameraEntity == null) {
            return;
        }
        cameraEntity.setPos(position.x, position.y, position.z);
        cameraEntity.setYRot(yaw);
        cameraEntity.setXRot(pitch);
    }

    private void spawnActorGhosts(CutsceneRecorder.RecordedActorTracks tracks) {
        if (!(mc.level instanceof ClientLevel clientLevel)) {
            return;
        }
        for (Map.Entry<UUID, List<CutsceneRecorder.RecordedActorFrame>> entry : tracks.getFrames().entrySet()) {
            List<CutsceneRecorder.RecordedActorFrame> frames = entry.getValue();
            if (frames == null || frames.isEmpty()) {
                continue;
            }
            CutsceneRecorder.RecordedActorInfo info = tracks.getActors().get(entry.getKey());
            Entity entity = createGhostEntity(clientLevel, info);
            if (entity == null) {
                continue;
            }
            CutsceneRecorder.RecordedActorFrame first = frames.get(0);
            GhostActor ghost = new GhostActor(entity, frames);
            if (entity instanceof GeckoNpcEntity && (info == null || info.getExpression() == null || info.getExpression().isBlank())) {
                ghost.geckoAutoExpression = true;
            }
            if (entity instanceof LivingEntity living) {
                applyRecordedState(entity, living, first.getPose(), first.isCrouching(),
                    first.isSprinting(), first.isSwimming(), first.isFallFlying(),
                    first.getMainHand(), first.getOffHand(), first.getHead(), first.getChest(),
                    first.getLegs(), first.getFeet(), ghost, true);
                living.yBodyRot = first.getBodyYaw();
                living.yHeadRot = first.getHeadYaw();
            }
            entity.setPos(first.getPosition().x, first.getPosition().y, first.getPosition().z);
            entity.setYRot(first.getYaw());
            entity.setXRot(first.getPitch());
            entity.setDeltaMovement(Vec3.ZERO);
            clientLevel.addEntity(entity);
            ghostActors.put(entry.getKey(), ghost);
        }
        useActorGhosts = !ghostActors.isEmpty();
    }

    private void spawnActorGhostsFromKeyframes(List<CutsceneEditorData.ActorKeyframe> keyframes) {
        if (!(mc.level instanceof ClientLevel clientLevel)) {
            return;
        }
        if (keyframes == null || keyframes.isEmpty()) {
            return;
        }
        List<CutsceneRecorder.RecordedActorFrame> frames = new ArrayList<>();
        for (CutsceneEditorData.ActorKeyframe kf : keyframes) {
            float yaw = kf.getYaw();
            float pitch = kf.getPitch();
            frames.add(new CutsceneRecorder.RecordedActorFrame(
                kf.getTime(),
                kf.getPosition(),
                yaw,
                pitch,
                yaw,
                yaw,
                false,
                Pose.STANDING,
                false,
                false,
                false,
                ItemStack.EMPTY,
                ItemStack.EMPTY,
                ItemStack.EMPTY,
                ItemStack.EMPTY,
                ItemStack.EMPTY,
                ItemStack.EMPTY
            ));
        }
        CutsceneRecorder.RecordedActorInfo info = buildLocalPreviewActorInfo();
        Entity entity = createGhostEntity(clientLevel, info);
        if (entity == null) {
            return;
        }
        CutsceneRecorder.RecordedActorFrame first = frames.get(0);
        GhostActor ghost = new GhostActor(entity, frames);
        if (entity instanceof GeckoNpcEntity) {
            ghost.geckoAutoExpression = true;
        }
        entity.setPos(first.getPosition().x, first.getPosition().y, first.getPosition().z);
        entity.setYRot(first.getYaw());
        entity.setXRot(first.getPitch());
        if (entity instanceof LivingEntity living) {
            applyRecordedState(entity, living, Pose.STANDING, false, false, false, false,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                ghost, true);
            living.yBodyRot = first.getBodyYaw();
            living.yHeadRot = first.getHeadYaw();
        }
        entity.setDeltaMovement(Vec3.ZERO);
        clientLevel.addEntity(entity);
        ghostActors.put(info.getId(), ghost);
        useActorGhosts = true;
    }

    private CutsceneRecorder.RecordedActorInfo buildLocalPreviewActorInfo() {
        UUID id = mc.player != null ? mc.player.getUUID() : UUID.randomUUID();
        boolean isPlayer = true;
        String name = "Actor";
        String skinValue = null;
        String skinSignature = null;
        if (mc.player != null) {
            GameProfile profile = mc.player.getGameProfile();
            if (profile != null) {
                name = "Actor";
                var textures = profile.getProperties().get("textures");
                if (textures != null && !textures.isEmpty()) {
                    Property property = textures.iterator().next();
                    skinValue = property.value();
                    skinSignature = property.signature();
                }
            }
        }
        return new CutsceneRecorder.RecordedActorInfo(id, "minecraft:player", isPlayer, name, skinValue, skinSignature);
    }

    private Entity createGhostEntity(ClientLevel clientLevel, CutsceneRecorder.RecordedActorInfo info) {
        Entity entity;
        if (info != null && info.isPlayer()) {
            String name = "Actor";
            UUID profileId = info.getId() != null ? info.getId() : UUID.randomUUID();
            GameProfile profile = new GameProfile(profileId, name);
            String skinValue = info.getSkinValue();
            if (skinValue != null) {
                String skinSignature = info.getSkinSignature();
                if (skinSignature != null && !skinSignature.isBlank()) {
                    profile.getProperties().put("textures", new Property("textures", skinValue, skinSignature));
                } else {
                    profile.getProperties().put("textures", new Property("textures", skinValue));
                }
            }
            entity = new RemotePlayer(clientLevel, profile);
        } else {
            String typeId = info != null ? info.getEntityTypeId() : "minecraft:pig";
            ResourceLocation key = ResourceLocation.tryParse(typeId);
            EntityType<?> type = key != null ? BuiltInRegistries.ENTITY_TYPE.get(key) : null;
            entity = type != null ? type.create(clientLevel) : null;
        }
        if (entity == null) {
            return null;
        }
        if (info != null && entity instanceof GeckoNpcEntity geckoNpc) {
            geckoNpc.setNpcId(info.getNpcId());
            String expression = info.getExpression();
            if (expression == null || expression.isBlank()) {
                expression = "idle";
            }
            geckoNpc.setExpression(expression);
            geckoNpc.setGeckolibModel(safeParseResource(info.getGeckoModel()));
            geckoNpc.setGeckolibAnimation(safeParseResource(info.getGeckoAnimation()));
            geckoNpc.setGeckolibTexture(safeParseResource(info.getGeckoTexture()));
        }
        entity.setId(nextGhostEntityId--);
        entity.setNoGravity(true);
        entity.noPhysics = true;
        entity.setSilent(true);
        entity.setInvulnerable(true);
        entity.setInvisible(false);
        entity.setCustomNameVisible(false);
        hideNameTag(entity);
        return entity;
    }

    private void hideNameTag(Entity entity) {
        if (entity == null || mc.level == null) {
            return;
        }
        entity.setCustomNameVisible(false);
        Scoreboard scoreboard = mc.level.getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam(PREVIEW_HIDE_TEAM);
        if (team == null) {
            team = scoreboard.addPlayerTeam(PREVIEW_HIDE_TEAM);
        }
        team.setNameTagVisibility(Team.Visibility.NEVER);
        String entry = entity.getScoreboardName();
        if (entry != null && !entry.isBlank()) {
            scoreboard.addPlayerToTeam(entry, team);
        }
    }

    private void removeActorGhosts() {
        if (!(mc.level instanceof ClientLevel clientLevel)) {
            ghostActors.clear();
            return;
        }
        for (GhostActor actor : ghostActors.values()) {
            if (actor.entity != null) {
                clientLevel.removeEntity(actor.entity.getId(), Entity.RemovalReason.DISCARDED);
            }
        }
        ghostActors.clear();
        useActorGhosts = false;
        ghostAnimTicks.clear();
    }

    private void removePreviewDrops() {
        if (!(mc.level instanceof ClientLevel clientLevel)) {
            previewDropEntityIds.clear();
            return;
        }
        for (int id : previewDropEntityIds) {
            clientLevel.removeEntity(id, Entity.RemovalReason.DISCARDED);
        }
        previewDropEntityIds.clear();
    }

    private void tickActorGhosts() {
        for (Map.Entry<UUID, GhostActor> entry : ghostActors.entrySet()) {
            UUID actorId = entry.getKey();
            GhostActor actor = entry.getValue();
            if (actor == null || actor.entity == null) {
                continue;
            }
            hideNameTag(actor.entity);
            ActorPreviewState prevState = getRecordedActorState(actor.frames, currentTime - 1.0f);
            ActorPreviewState curState = getRecordedActorState(actor.frames, currentTime);
            if (curState == null || curState.position == null) {
                continue;
            }
            logPreviewActorState(actorId, curState, currentTime);
            LivingEntity living = null;
            if (actor.entity instanceof LivingEntity castLiving) {
                living = castLiving;
                applyRecordedState(actor.entity, living, curState.pose, curState.crouching,
                    curState.sprinting, curState.swimming, curState.fallFlying,
                    curState.mainHand, curState.offHand, curState.head, curState.chest,
                    curState.legs, curState.feet, actor, true);
                living.yBodyRot = curState.bodyYaw;
                living.yHeadRot = curState.headYaw;
            }
            if (prevState != null && prevState.position != null) {
                actor.entity.xo = prevState.position.x;
                actor.entity.yo = prevState.position.y;
                actor.entity.zo = prevState.position.z;
            } else {
                actor.entity.xo = curState.position.x;
                actor.entity.yo = curState.position.y;
                actor.entity.zo = curState.position.z;
            }
            actor.entity.setPos(curState.position.x, curState.position.y, curState.position.z);
            actor.entity.setYRot(curState.yaw);
            actor.entity.setXRot(curState.pitch);
            float prevYaw = prevState != null ? prevState.yaw : curState.yaw;
            float prevPitch = prevState != null ? prevState.pitch : curState.pitch;
            actor.entity.yRotO = prevYaw;
            actor.entity.xRotO = prevPitch;
            if (living != null) {
                float prevHeadYaw = prevState != null ? prevState.headYaw : curState.headYaw;
                float prevBodyYaw = prevState != null ? prevState.bodyYaw : curState.bodyYaw;
                long tick = currentTime;
                long lastTick = ghostAnimTicks.getOrDefault(actor.entity.getUUID(), Long.MIN_VALUE);
                if (tick != lastTick) {
                    Vec3 prevLogic = actor.prevLogicPos;
                    Vec3 curLogic = curState.position;
                    if (prevLogic == null) {
                        prevLogic = curLogic;
                    } else {
                        applyGhostWalkAnimation(actor, living, prevLogic, curLogic);
                    }
                    actor.prevLogicPos = curLogic;
                    ghostAnimTicks.put(actor.entity.getUUID(), tick);
                }
                living.yHeadRotO = prevHeadYaw;
                living.yBodyRotO = prevBodyYaw;
            }
            if (actor.entity instanceof GeckoNpcEntity geckoNpc) {
                Vec3 delta = Vec3.ZERO;
                if (prevState != null && prevState.position != null) {
                    delta = curState.position.subtract(prevState.position);
                }
                float speed = (float) delta.horizontalDistance();
                if (actor.geckoAutoExpression) {
                    if (actor.geckoMoving) {
                        if (speed < 0.005f) {
                            actor.geckoMoving = false;
                            geckoNpc.setExpression("idle");
                        }
                    } else if (speed > 0.02f) {
                        actor.geckoMoving = true;
                        geckoNpc.setExpression("walk");
                    }
                }
                geckoNpc.setDeltaMovement(delta);
            } else {
                actor.entity.setDeltaMovement(Vec3.ZERO);
            }
        }
    }

    private void renderGhostActors(float partialTick) {
        if (!useActorGhosts || ghostActors.isEmpty()) {
            return;
        }
        float time = activePreviewTime + partialTick;
        for (Map.Entry<UUID, GhostActor> entry : ghostActors.entrySet()) {
            GhostActor actor = entry.getValue();
            if (actor == null || actor.entity == null) {
                continue;
            }
            ActorPreviewState state = getRecordedActorState(actor.frames, time);
            if (state == null || state.position == null) {
                continue;
            }
            actor.entity.setPos(state.position.x, state.position.y, state.position.z);
            actor.entity.xo = state.position.x;
            actor.entity.yo = state.position.y;
            actor.entity.zo = state.position.z;
            actor.entity.setYRot(state.yaw);
            actor.entity.setXRot(state.pitch);
            actor.entity.yRotO = state.yaw;
            actor.entity.xRotO = state.pitch;
            if (actor.entity instanceof LivingEntity living) {
                living.yHeadRot = state.headYaw;
                living.yBodyRot = state.bodyYaw;
                living.yHeadRotO = state.headYaw;
                living.yBodyRotO = state.bodyYaw;
            }
        }
    }

    private void tickActorEmotes() {
        if (recordedActorEmotes.isEmpty()) {
            return;
        }
        for (Map.Entry<UUID, List<CutsceneRecorder.RecordedActorEmoteFrame>> entry : recordedActorEmotes.entrySet()) {
            UUID uuid = entry.getKey();
            List<CutsceneRecorder.RecordedActorEmoteFrame> frames = entry.getValue();
            if (frames == null || frames.isEmpty()) {
                continue;
            }
            int index = actorEmoteIndices.getOrDefault(uuid, 0);
            GhostActor ghost = ghostActors.get(uuid);
            while (index < frames.size() && frames.get(index).getTime() <= currentTime) {
                CutsceneRecorder.RecordedActorEmoteFrame frame = frames.get(index);
                UUID target = ghost != null && ghost.entity != null ? ghost.entity.getUUID() : uuid;
                if (frame.isStop()) {
                    Marallyzen.LOGGER.info(
                        "[Preview] emoteStop actor={} tick={} packetSent=true",
                        target,
                        frame.getTime()
                    );
                    Marallyzen.LOGGER.info(
                        "PreviewEmote: via RecordedActorEmoteFrame.stop -> ClientEmoteHandler.stop(target={})",
                        target
                    );
                    neutka.marallys.marallyzen.client.emote.ClientEmoteHandler.stop(target);
                } else {
                    String emoteId = frame.getEmoteId();
                    if (emoteId != null && !emoteId.isBlank()) {
                        Marallyzen.LOGGER.info(
                            "[Preview] emoteApply actor={} emote={} tick={} packetSent=true",
                            target,
                            emoteId,
                            frame.getTime()
                        );
                        Marallyzen.LOGGER.info(
                            "PreviewEmote: via RecordedActorEmoteFrame.apply -> ClientEmoteHandler.handle(target={}, emote={})",
                            target,
                            emoteId
                        );
                        neutka.marallys.marallyzen.client.emote.ClientEmoteHandler.handle(target, emoteId, false);
                    }
                }
                index++;
            }
            actorEmoteIndices.put(uuid, index);
        }
    }

    private void tickItemDrops() {
        if (recordedItemDrops.isEmpty() || !(mc.level instanceof ClientLevel clientLevel)) {
            return;
        }
        while (itemDropIndex < recordedItemDrops.size()
            && recordedItemDrops.get(itemDropIndex).getTime() <= currentTime) {
            CutsceneRecorder.RecordedItemDropFrame drop = recordedItemDrops.get(itemDropIndex);
            itemDropIndex++;
            ItemStack stack = drop.getStack();
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            Vec3 pos = drop.getPosition();
            if (pos == null) {
                continue;
            }
            ItemEntity itemEntity = new ItemEntity(clientLevel, pos.x, pos.y, pos.z, stack.copy());
            Vec3 velocity = drop.getVelocity();
            if (velocity != null) {
                itemEntity.setDeltaMovement(velocity);
            }
            itemEntity.setId(nextGhostEntityId--);
            clientLevel.addEntity(itemEntity);
            previewDropEntityIds.add(itemEntity.getId());
        }
    }

    private void applyRecordedState(Entity entity, LivingEntity living, Pose pose, boolean crouching,
        boolean sprinting, boolean swimming, boolean fallFlying,
        ItemStack mainHand, ItemStack offHand, ItemStack head, ItemStack chest, ItemStack legs, ItemStack feet,
        GhostActor actor, boolean applyPoseFlags) {
        if (applyPoseFlags) {
            Pose targetPose;
            if (fallFlying) {
                targetPose = Pose.FALL_FLYING;
            } else if (swimming) {
                targetPose = Pose.SWIMMING;
            } else if (crouching) {
                targetPose = Pose.CROUCHING;
            } else if (pose != null) {
                targetPose = pose;
            } else {
                targetPose = Pose.STANDING;
            }

            if (actor.lastPose != targetPose) {
                living.setPose(targetPose);
                living.refreshDimensions();
                actor.lastPose = targetPose;
            }

            entity.setShiftKeyDown(crouching);
            entity.setSprinting(sprinting);
            setSharedFlag(entity, SHARED_FLAG_SWIMMING, swimming);
            setSharedFlag(entity, SHARED_FLAG_FALL_FLYING, fallFlying);

            actor.lastCrouching = crouching;
            actor.lastSprinting = sprinting;
            actor.lastSwimming = swimming;
            actor.lastFallFlying = fallFlying;
        }
        applyRecordedEquipment(living, mainHand, offHand, head, chest, legs, feet, actor);
    }

    private void applyRecordedEquipment(LivingEntity living, ItemStack mainHand, ItemStack offHand,
        ItemStack head, ItemStack chest, ItemStack legs, ItemStack feet, GhostActor actor) {
        updateEquipmentSlot(living, EquipmentSlot.MAINHAND, mainHand, actor);
        updateEquipmentSlot(living, EquipmentSlot.OFFHAND, offHand, actor);
        updateEquipmentSlot(living, EquipmentSlot.HEAD, head, actor);
        updateEquipmentSlot(living, EquipmentSlot.CHEST, chest, actor);
        updateEquipmentSlot(living, EquipmentSlot.LEGS, legs, actor);
        updateEquipmentSlot(living, EquipmentSlot.FEET, feet, actor);
    }

    private void updateEquipmentSlot(LivingEntity living, EquipmentSlot slot, ItemStack stack, GhostActor actor) {
        ItemStack current = switch (slot) {
            case MAINHAND -> actor.lastMainHand;
            case OFFHAND -> actor.lastOffHand;
            case HEAD -> actor.lastHead;
            case CHEST -> actor.lastChest;
            case LEGS -> actor.lastLegs;
            case FEET -> actor.lastFeet;
            default -> ItemStack.EMPTY;
        };
        if (itemsMatch(current, stack)) {
            return;
        }
        ItemStack next = copyOrEmpty(stack);
        living.setItemSlot(slot, next);
        switch (slot) {
            case MAINHAND -> actor.lastMainHand = next;
            case OFFHAND -> actor.lastOffHand = next;
            case HEAD -> actor.lastHead = next;
            case CHEST -> actor.lastChest = next;
            case LEGS -> actor.lastLegs = next;
            case FEET -> actor.lastFeet = next;
        }
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

    private void selectPreviewLogActor() {
        previewLogActorId = null;
        previewLogInit = false;
        previewLogTick = Long.MIN_VALUE;
        if (mc.player != null && ghostActors.containsKey(mc.player.getUUID())) {
            previewLogActorId = mc.player.getUUID();
        } else if (!ghostActors.isEmpty()) {
            previewLogActorId = ghostActors.keySet().iterator().next();
        }
        if (previewLogActorId != null) {
            Marallyzen.LOGGER.info("Preview logging actor {}", previewLogActorId);
        }
    }

    private void logPreviewActorState(UUID actorId, ActorPreviewState state, long time) {
        if (previewLogActorId == null || !previewLogActorId.equals(actorId) || state == null) {
            return;
        }
        boolean changed = !previewLogInit
            || previewLogPose != state.pose
            || previewLogCrouching != state.crouching
            || previewLogSprinting != state.sprinting
            || previewLogSwimming != state.swimming
            || previewLogFallFlying != state.fallFlying;
        boolean heartbeat = previewLogInit && time - previewLogTick >= 20;
        if (changed || heartbeat) {
            Marallyzen.LOGGER.info(
                "Preview actor state: t={} pose={} crouch={} sprint={} swim={} fallFly={}",
                time, state.pose, state.crouching, state.sprinting, state.swimming, state.fallFlying
            );
            previewLogPose = state.pose;
            previewLogCrouching = state.crouching;
            previewLogSprinting = state.sprinting;
            previewLogSwimming = state.swimming;
            previewLogFallFlying = state.fallFlying;
            previewLogTick = time;
            previewLogInit = true;
        }
    }

    private void setSharedFlag(Entity entity, int flag, boolean value) {
        if (!cachedSetSharedFlagResolved) {
            cachedSetSharedFlagResolved = true;
            try {
                cachedSetSharedFlag = Entity.class.getDeclaredMethod("setSharedFlag", int.class, boolean.class);
                cachedSetSharedFlag.setAccessible(true);
            } catch (Exception ignored) {
                cachedSetSharedFlag = null;
            }
        }
        if (cachedSetSharedFlag == null) {
            return;
        }
        try {
            cachedSetSharedFlag.invoke(entity, flag, value);
        } catch (Exception ignored) {
        }
    }

    private void applyGhostWalkAnimation(GhostActor actor, LivingEntity living, Vec3 prevPos, Vec3 curPos) {
        if (prevPos == null || curPos == null) {
            return;
        }
        float dx = (float) (curPos.x - prevPos.x);
        float dz = (float) (curPos.z - prevPos.z);
        float speed = Mth.clamp((float) Math.sqrt(dx * dx + dz * dz) * 4.0f, 0.0f, 1.0f);

        Object walkAnimation = resolveWalkAnimationState(living);
        if (walkAnimation != null) {
            if (cachedWalkAnimationUpdate != null) {
                try {
                    cachedWalkAnimationUpdate.invoke(walkAnimation, speed, 1.0f);
                    return;
                } catch (Exception ignored) {
                }
            }
            if (cachedWalkAnimationSetSpeed != null) {
                try {
                    cachedWalkAnimationSetSpeed.invoke(walkAnimation, speed);
                    return;
                } catch (Exception ignored) {
                }
            }
        }

        resolveLimbFields();
        if (cachedLimbSwingField != null && cachedLimbSwingAmountField != null) {
            actor.animationSpeedOld = actor.animationSpeed;
            actor.animationSpeed += (speed - actor.animationSpeed) * 0.4f;
            actor.walkDistO = actor.walkDist;
            actor.walkDist += actor.animationSpeed;
            try {
                cachedLimbSwingAmountField.setFloat(living, actor.animationSpeed);
                cachedLimbSwingField.setFloat(living, actor.walkDist);
                return;
            } catch (Exception ignored) {
            }
        }
    }

    private void resolveLimbFields() {
        if (cachedLimbFieldsResolved) {
            return;
        }
        cachedLimbFieldsResolved = true;
        try {
            cachedLimbSwingField = LivingEntity.class.getDeclaredField("limbSwing");
            cachedLimbSwingField.setAccessible(true);
        } catch (Exception ignored) {
            cachedLimbSwingField = null;
        }
        try {
            cachedLimbSwingAmountField = LivingEntity.class.getDeclaredField("limbSwingAmount");
            cachedLimbSwingAmountField.setAccessible(true);
        } catch (Exception ignored) {
            cachedLimbSwingAmountField = null;
        }
    }

    private Object resolveWalkAnimationState(LivingEntity living) {
        if (cachedWalkAnimationResolved) {
            return getWalkAnimationState(living);
        }
        cachedWalkAnimationResolved = true;
        try {
            cachedWalkAnimationField = LivingEntity.class.getDeclaredField("walkAnimation");
            cachedWalkAnimationField.setAccessible(true);
        } catch (Exception ignored) {
            cachedWalkAnimationField = null;
        }
        Object walkAnimation = getWalkAnimationState(living);
        if (walkAnimation != null) {
            Class<?> type = walkAnimation.getClass();
            try {
                cachedWalkAnimationUpdate = type.getMethod("update", float.class, float.class);
            } catch (Exception ignored) {
                cachedWalkAnimationUpdate = null;
            }
            try {
                cachedWalkAnimationSetSpeed = type.getMethod("setSpeed", float.class);
            } catch (Exception ignored) {
                cachedWalkAnimationSetSpeed = null;
            }
        }
        return walkAnimation;
    }

    private Object getWalkAnimationState(LivingEntity living) {
        if (cachedWalkAnimationField == null) {
            return null;
        }
        try {
            return cachedWalkAnimationField.get(living);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static PreviewState getActorPreviewState(List<CutsceneEditorData.ActorKeyframe> keyframes, float time) {
        if (keyframes == null || keyframes.isEmpty()) {
            return null;
        }

        CutsceneEditorData.ActorKeyframe prev = null;
        CutsceneEditorData.ActorKeyframe next = null;
        for (CutsceneEditorData.ActorKeyframe actorKf : keyframes) {
            if (actorKf.getTime() <= time) {
                prev = actorKf;
            }
            if (actorKf.getTime() > time) {
                next = actorKf;
                break;
            }
        }

        if (prev == null && next == null) {
            return null;
        }
        if (prev == null) {
            prev = next;
        }
        if (next == null) {
            next = prev;
        }

        long span = Math.max(1, next.getTime() - prev.getTime());
        float t = (time - prev.getTime()) / (float) span;
        t = Math.min(1.0f, Math.max(0.0f, t));

        Vec3 pos = lerp(prev.getPosition(), next.getPosition(), t);
        float yaw = lerpAngle(prev.getYaw(), next.getYaw(), t);
        float pitch = lerpAngle(prev.getPitch(), next.getPitch(), t);
        return new PreviewState(pos, yaw, pitch, 70.0f);
    }

    private static ActorPreviewState getRecordedActorState(List<CutsceneRecorder.RecordedActorFrame> keyframes, float time) {
        if (keyframes == null || keyframes.isEmpty()) {
            return null;
        }

        CutsceneRecorder.RecordedActorFrame prev = null;
        CutsceneRecorder.RecordedActorFrame next = null;
        for (CutsceneRecorder.RecordedActorFrame actorKf : keyframes) {
            if (actorKf.getTime() <= time) {
                prev = actorKf;
            }
            if (actorKf.getTime() > time) {
                next = actorKf;
                break;
            }
        }

        if (prev == null && next == null) {
            return null;
        }
        if (prev == null) {
            prev = next;
        }
        if (next == null) {
            next = prev;
        }

        long span = Math.max(1, next.getTime() - prev.getTime());
        float t = (time - prev.getTime()) / (float) span;
        t = Math.min(1.0f, Math.max(0.0f, t));

        Vec3 pos = lerp(prev.getPosition(), next.getPosition(), t);
        float yaw = lerpAngle(prev.getYaw(), next.getYaw(), t);
        float pitch = lerpAngle(prev.getPitch(), next.getPitch(), t);
        float headYaw = lerpAngle(prev.getHeadYaw(), next.getHeadYaw(), t);
        float bodyYaw = lerpAngle(prev.getBodyYaw(), next.getBodyYaw(), t);
        CutsceneRecorder.RecordedActorFrame stateFrame = t >= 0.5f ? next : prev;
        Pose pose = stateFrame.getPose();
        boolean crouching = stateFrame.isCrouching();
        boolean sprinting = stateFrame.isSprinting();
        boolean swimming = stateFrame.isSwimming();
        boolean fallFlying = stateFrame.isFallFlying();
        ItemStack mainHand = stateFrame.getMainHand();
        ItemStack offHand = stateFrame.getOffHand();
        ItemStack head = stateFrame.getHead();
        ItemStack chest = stateFrame.getChest();
        ItemStack legs = stateFrame.getLegs();
        ItemStack feet = stateFrame.getFeet();
        return new ActorPreviewState(pos, yaw, pitch, headYaw, bodyYaw, pose, crouching,
            sprinting, swimming, fallFlying, mainHand, offHand, head, chest, legs, feet);
    }

    private static ActorPreviewState getRecordedActorStateNoLerp(List<CutsceneRecorder.RecordedActorFrame> keyframes, float time) {
        if (keyframes == null || keyframes.isEmpty()) {
            return null;
        }
        CutsceneRecorder.RecordedActorFrame frame = null;
        for (CutsceneRecorder.RecordedActorFrame actorKf : keyframes) {
            if (actorKf.getTime() <= time) {
                frame = actorKf;
            } else {
                break;
            }
        }
        if (frame == null) {
            frame = keyframes.get(0);
        }
        Vec3 pos = frame.getPosition();
        float yaw = frame.getYaw();
        float pitch = frame.getPitch();
        float headYaw = frame.getHeadYaw();
        float bodyYaw = frame.getBodyYaw();
        Pose pose = frame.getPose();
        boolean crouching = frame.isCrouching();
        boolean sprinting = frame.isSprinting();
        boolean swimming = frame.isSwimming();
        boolean fallFlying = frame.isFallFlying();
        ItemStack mainHand = frame.getMainHand();
        ItemStack offHand = frame.getOffHand();
        ItemStack head = frame.getHead();
        ItemStack chest = frame.getChest();
        ItemStack legs = frame.getLegs();
        ItemStack feet = frame.getFeet();
        return new ActorPreviewState(pos, yaw, pitch, headYaw, bodyYaw, pose, crouching,
            sprinting, swimming, fallFlying, mainHand, offHand, head, chest, legs, feet);
    }

    private static Vec3 lerp(Vec3 start, Vec3 end, float t) {
        if (start == null && end == null) {
            return Vec3.ZERO;
        }
        if (start == null) {
            return end;
        }
        if (end == null) {
            return start;
        }
        return new Vec3(
            start.x + (end.x - start.x) * t,
            start.y + (end.y - start.y) * t,
            start.z + (end.z - start.z) * t
        );
    }

    private static float lerp(float start, float end, float t) {
        return start + (end - start) * t;
    }

    private static ResourceLocation safeParseResource(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return ResourceLocation.tryParse(raw);
    }

    private static float lerpAngle(float start, float end, float t) {
        float diff = end - start;
        if (Math.abs(start) <= 180.0f && Math.abs(end) <= 180.0f) {
            if (diff > 180.0f) diff -= 360.0f;
            if (diff < -180.0f) diff += 360.0f;
        }
        return start + diff * t;
    }

    private static ItemStack copyOrEmpty(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return stack.copy();
    }

    private static boolean itemsMatch(ItemStack first, ItemStack second) {
        if (first == null || first.isEmpty()) {
            return second == null || second.isEmpty();
        }
        if (second == null || second.isEmpty()) {
            return false;
        }
        return ItemStack.isSameItemSameComponents(first, second);
    }

    private static ActorTrackStatus evaluateActorTracks(CutsceneRecorder.RecordedActorTracks tracks) {
        if (tracks == null) {
            return new ActorTrackStatus("null", 0, "tracks==null");
        }
        Map<UUID, List<CutsceneRecorder.RecordedActorFrame>> frames = tracks.getFrames();
        Map<UUID, CutsceneRecorder.RecordedActorInfo> actors = tracks.getActors();
        if (frames == null || actors == null) {
            if (frames == null && actors == null) {
                return new ActorTrackStatus("invalid", 0, "frames==null, actors==null");
            }
            return new ActorTrackStatus("invalid", 0, frames == null ? "frames==null" : "actors==null");
        }
        if (frames.isEmpty()) {
            return new ActorTrackStatus("empty", 0, "frames map empty");
        }
        int nonEmpty = 0;
        int total = 0;
        for (Map.Entry<UUID, List<CutsceneRecorder.RecordedActorFrame>> entry : frames.entrySet()) {
            total++;
            List<CutsceneRecorder.RecordedActorFrame> list = entry.getValue();
            if (list != null && !list.isEmpty()) {
                nonEmpty++;
            }
        }
        if (nonEmpty == 0) {
            return new ActorTrackStatus("empty", 0, "all frame lists empty");
        }
        return new ActorTrackStatus("usable", nonEmpty, "frames=" + total);
    }

    private record PreviewState(Vec3 position, float yaw, float pitch, float fov) {}
    private record ActorPreviewState(Vec3 position, float yaw, float pitch, float headYaw, float bodyYaw,
                                     Pose pose, boolean crouching, boolean sprinting, boolean swimming,
                                     boolean fallFlying, ItemStack mainHand, ItemStack offHand, ItemStack head,
                                     ItemStack chest, ItemStack legs, ItemStack feet) {}
    private record ActorTrackStatus(String state, int actorCount, String detail) {}
    private static final class GhostActor {
        private final Entity entity;
        private final List<CutsceneRecorder.RecordedActorFrame> frames;
        private Pose lastPose;
        private boolean lastCrouching;
        private boolean lastSprinting;
        private boolean lastSwimming;
        private boolean lastFallFlying;
        private ItemStack lastMainHand;
        private ItemStack lastOffHand;
        private ItemStack lastHead;
        private ItemStack lastChest;
        private ItemStack lastLegs;
        private ItemStack lastFeet;
        private Vec3 prevLogicPos;
        private float walkDist;
        private float walkDistO;
        private float animationSpeed;
        private float animationSpeedOld;
        private boolean geckoAutoExpression;
        private boolean geckoMoving;

        private GhostActor(Entity entity, List<CutsceneRecorder.RecordedActorFrame> frames) {
            this.entity = entity;
            this.frames = frames;
            this.lastPose = null;
            this.lastCrouching = false;
            this.lastSprinting = false;
            this.lastSwimming = false;
            this.lastFallFlying = false;
            this.lastMainHand = ItemStack.EMPTY;
            this.lastOffHand = ItemStack.EMPTY;
            this.lastHead = ItemStack.EMPTY;
            this.lastChest = ItemStack.EMPTY;
            this.lastLegs = ItemStack.EMPTY;
            this.lastFeet = ItemStack.EMPTY;
            this.prevLogicPos = null;
            this.walkDist = 0.0f;
            this.walkDistO = 0.0f;
            this.animationSpeed = 0.0f;
            this.animationSpeedOld = 0.0f;
            this.geckoAutoExpression = false;
            this.geckoMoving = false;
        }
    }

    public static boolean isPreviewActive() {
        if (!neutka.marallys.marallyzen.replay.LegacyReplayGate.isLegacyReplayEnabled()) {
            return false;
        }
        return activePreviewPlaying && activePreviewData != null;
    }

    public static CutsceneEditorData getActivePreviewData() {
        return activePreviewData;
    }

    public static long getActivePreviewTime() {
        if (!neutka.marallys.marallyzen.replay.LegacyReplayGate.isLegacyReplayEnabled()) {
            return 0L;
        }
        return activePreviewTime;
    }

    public static void setHideHandInPreview(boolean hideHand) {
        hideHandInPreview = hideHand;
    }

    public static boolean shouldHideHandInPreview() {
        return hideHandInPreview;
    }

    public static int getActivePreviewKeyframeIndex() {
        return activePreviewKeyframeIndex;
    }

    public static void setFixedCameraMode(boolean enabled) {
        fixedCameraMode = enabled;
    }

    public static boolean isFixedCameraMode() {
        return fixedCameraMode;
    }

    private static void buildPreviewOverrides(CutsceneEditorData data) {
        if (data == null) {
            return;
        }

        java.util.Map<Integer, java.util.List<CutsceneEditorData.CameraKeyframe>> groups = new java.util.HashMap<>();
        java.util.List<CutsceneEditorData.CameraKeyframe> fallback = new java.util.ArrayList<>();
        java.util.Map<Integer, java.util.List<CutsceneEditorData.ActorKeyframe>> actorGroups = new java.util.HashMap<>();
        java.util.List<CutsceneEditorData.ActorKeyframe> actorFallback = new java.util.ArrayList<>();
        for (CutsceneEditorData.EditorKeyframe keyframe : data.getKeyframes()) {
            if (keyframe instanceof CutsceneEditorData.CameraKeyframe cameraKf) {
                fallback.add(cameraKf);
                int groupId = cameraKf.getGroupId();
                if (groupId >= 0) {
                    groups.computeIfAbsent(groupId, k -> new java.util.ArrayList<>()).add(cameraKf);
                }
            } else if (keyframe instanceof CutsceneEditorData.ActorKeyframe actorKf) {
                actorFallback.add(actorKf);
                int groupId = actorKf.getGroupId();
                if (groupId >= 0) {
                    actorGroups.computeIfAbsent(groupId, k -> new java.util.ArrayList<>()).add(actorKf);
                }
            }
        }

        CutsceneEditorData.CameraKeyframe override = null;
        long overrideTime = Long.MIN_VALUE;
        java.util.List<CutsceneEditorData.CameraKeyframe> path = null;
        long pathTime = Long.MIN_VALUE;
        java.util.List<CutsceneEditorData.ActorKeyframe> actorPath = null;
        long actorPathTime = Long.MIN_VALUE;

        for (var entry : groups.entrySet()) {
            java.util.List<CutsceneEditorData.CameraKeyframe> frames = entry.getValue();
            if (frames.isEmpty()) {
                continue;
            }
            frames.sort(java.util.Comparator.comparingLong(CutsceneEditorData.CameraKeyframe::getTime));
            long start = frames.get(0).getTime();
            if (frames.size() == 1) {
                if (start >= overrideTime) {
                    overrideTime = start;
                    override = frames.get(0);
                }
            } else {
                if (start >= pathTime) {
                    pathTime = start;
                    path = frames;
                }
            }
        }

        for (var entry : actorGroups.entrySet()) {
            java.util.List<CutsceneEditorData.ActorKeyframe> frames = entry.getValue();
            if (frames.isEmpty()) {
                continue;
            }
            frames.sort(java.util.Comparator.comparingLong(CutsceneEditorData.ActorKeyframe::getTime));
            long start = frames.get(0).getTime();
            if (start >= actorPathTime) {
                actorPathTime = start;
                actorPath = frames;
            }
        }

        activePreviewCameraOverride = override;
        if (path != null) {
            activePreviewPath = path;
        } else {
            activePreviewPath = fallback;
        }
        if (activePreviewPath != null) {
            activePreviewPath.sort(java.util.Comparator.comparingLong(CutsceneEditorData.CameraKeyframe::getTime));
        }
        if (actorPath != null) {
            activePreviewActorPath = actorPath;
        } else {
            activePreviewActorPath = actorFallback;
        }
        if (activePreviewActorPath != null) {
            activePreviewActorPath.sort(java.util.Comparator.comparingLong(CutsceneEditorData.ActorKeyframe::getTime));
        }
    }

    private void logPreviewCameraSetup() {
        if (data == null) {
            return;
        }
        int cameraKeyframes = 0;
        for (CutsceneEditorData.EditorKeyframe keyframe : data.getKeyframes()) {
            if (keyframe instanceof CutsceneEditorData.CameraKeyframe) {
                cameraKeyframes++;
            }
        }
        int pathSize = activePreviewPath != null ? activePreviewPath.size() : 0;
        String overrideInfo = activePreviewCameraOverride != null
            ? String.format("t=%d pos=%s", activePreviewCameraOverride.getTime(), activePreviewCameraOverride.getPosition())
            : "none";
        String pathInfo = "empty";
        if (activePreviewPath != null && !activePreviewPath.isEmpty()) {
            CutsceneEditorData.CameraKeyframe first = activePreviewPath.get(0);
            CutsceneEditorData.CameraKeyframe last = activePreviewPath.get(activePreviewPath.size() - 1);
            pathInfo = String.format("size=%d first=(t=%d pos=%s) last=(t=%d pos=%s)",
                activePreviewPath.size(),
                first.getTime(),
                first.getPosition(),
                last.getTime(),
                last.getPosition());
        }
        Marallyzen.LOGGER.info(
            "Preview camera setup: fixedMode={} cameraKeyframes={} override={} path={}",
            fixedCameraMode,
            cameraKeyframes,
            overrideInfo,
            pathInfo
        );
    }
}
