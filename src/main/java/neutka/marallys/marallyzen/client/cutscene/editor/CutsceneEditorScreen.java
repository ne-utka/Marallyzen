package neutka.marallys.marallyzen.client.cutscene.editor;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.client.cutscene.world.CutsceneWorldManager;
import neutka.marallys.marallyzen.replay.ReplayMarker;
import neutka.marallys.marallyzen.replay.client.ReplayManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Main GUI screen for cutscene editor.
 * Provides interface for creating and editing cutscenes.
 */
public class CutsceneEditorScreen extends Screen {
    private static CutsceneEditorData lastEditorData;
    private static int nextGroupId = 1;
    private static CutsceneEditorScreen activeFixedRecordingScreen;

    private CutsceneEditorData editorData;
    private CutsceneRecorder recorder;
    private CutsceneCameraController cameraController;
    private CutscenePreviewPlayer previewPlayer;
    
    // UI Components
    private EditBox sceneIdBox;
    private Button newSceneButton;
    private Button openSceneButton;
    private Button saveSceneButton;
    private Button previewButton;
    private Button cameraModeButton;
    private Checkbox hideHandCheckbox;
    private Button recordButton;
    private Button pauseButton;
    private Button stopButton;
    private Button addKeyframeButton;
    private Button addPauseButton;
    private Button addEmotionButton;
    
    private int selectedKeyframeIndex = -1;
    private boolean isPreviewing = false;
    private int keyframeScrollOffset = 0;
    private int keyframeMaxScroll = 0;
    private static final int KEYFRAME_LINE_HEIGHT = 15;
    private boolean previewFixedCamera = true;
    private boolean previewHideHand = false;
    private boolean fixedRecordingActive = false;
    private CutsceneEditorData.CameraKeyframe fixedRecordingCamera;
    private Boolean prevSmoothCamera = null;
    private boolean fixedRecordingMouseGrabbed = false;

    public boolean isRecording() {
        return recorder != null && recorder.isRecording();
    }

    public boolean isPreviewing() {
        return isPreviewing;
    }

    public boolean shouldBlockPlayerInput() {
        if (isPreviewing) {
            return true;
        }
        if (recorder != null && recorder.isRecording()) {
            return !fixedRecordingActive;
        }
        return false;
    }

    public boolean isFixedRecordingActive() {
        return fixedRecordingActive;
    }

    public CutsceneEditorData.CameraKeyframe getFixedRecordingCamera() {
        return fixedRecordingCamera;
    }

    public CutsceneEditorScreen(CutsceneEditorData existingData) {
        super(Component.translatable("screen.marallyzen.cutscene_editor.title"));
        this.editorData = existingData != null
            ? existingData
            : (lastEditorData != null ? lastEditorData : new CutsceneEditorData("new_cutscene"));
        lastEditorData = this.editorData;
        updateNextGroupId();
        this.recorder = CutsceneRecorder.getInstance();
        this.cameraController = new CutsceneCameraController();
        this.previewPlayer = new CutscenePreviewPlayer();
    }

    public CutsceneEditorData getEditorData() {
        return editorData;
    }

    public static CutsceneEditorData getLastEditorData() {
        return lastEditorData;
    }

    public static CutsceneEditorScreen getActiveFixedRecordingScreen() {
        return activeFixedRecordingScreen;
    }

    public void stopRecordingFromKeybind() {
        stopRecording();
        if (minecraft != null) {
            minecraft.setScreen(this);
        }
    }

    @Override
    protected void init() {
        super.init();
        
        int buttonWidth = 120;
        int buttonHeight = 20;
        int spacing = 5;
        int startY = 10;
        int startX = 10;

        // Top panel - Scene management
        sceneIdBox = new EditBox(this.font, startX, startY, 200, 20,
            Component.translatable("screen.marallyzen.cutscene_editor.scene_id"));
        sceneIdBox.setValue(editorData.getId());
        sceneIdBox.setResponder(value -> editorData.setId(value));
        this.addRenderableWidget(sceneIdBox);

        newSceneButton = Button.builder(
            Component.translatable("screen.marallyzen.cutscene_editor.new"),
            btn -> createNewScene()
        ).bounds(startX + 210, startY, buttonWidth, buttonHeight).build();
        this.addRenderableWidget(newSceneButton);

        openSceneButton = Button.builder(
            Component.translatable("screen.marallyzen.cutscene_editor.open"),
            btn -> openSceneDialog()
        ).bounds(startX + 210 + buttonWidth + spacing, startY, buttonWidth, buttonHeight).build();
        this.addRenderableWidget(openSceneButton);

        saveSceneButton = Button.builder(
            Component.translatable("screen.marallyzen.cutscene_editor.save"),
            btn -> saveScene()
        ).bounds(startX + 210 + (buttonWidth + spacing) * 2, startY, buttonWidth, buttonHeight).build();
        this.addRenderableWidget(saveSceneButton);

        previewButton = Button.builder(
            Component.translatable("screen.marallyzen.cutscene_editor.preview"),
            btn -> togglePreview()
        ).bounds(startX + 210 + (buttonWidth + spacing) * 3, startY, buttonWidth, buttonHeight).build();
        this.addRenderableWidget(previewButton);

        cameraModeButton = Button.builder(
            getCameraModeLabel(),
            btn -> toggleCameraMode()
        ).bounds(startX + 210 + (buttonWidth + spacing) * 4, startY, buttonWidth, buttonHeight).build();
        this.addRenderableWidget(cameraModeButton);

        hideHandCheckbox = createCheckbox(
            startX + 210 + (buttonWidth + spacing) * 4,
            startY + buttonHeight + spacing,
            buttonWidth,
            getHideHandLabel(),
            previewHideHand
        );
        if (hideHandCheckbox != null) {
            this.addRenderableWidget(hideHandCheckbox);
        }

        // Bottom panel - Recording controls
        int bottomY = this.height - 40;
        recordButton = Button.builder(
            Component.translatable("screen.marallyzen.cutscene_editor.record"),
            btn -> toggleRecording()
        ).bounds(startX, bottomY, buttonWidth, buttonHeight).build();
        this.addRenderableWidget(recordButton);

        pauseButton = Button.builder(
            Component.translatable("screen.marallyzen.cutscene_editor.pause"),
            btn -> togglePause()
        ).bounds(startX + buttonWidth + spacing, bottomY, buttonWidth, buttonHeight).build();
        this.addRenderableWidget(pauseButton);

        stopButton = Button.builder(
            Component.translatable("screen.marallyzen.cutscene_editor.stop"),
            btn -> stopRecording()
        ).bounds(startX + (buttonWidth + spacing) * 2, bottomY, buttonWidth, buttonHeight).build();
        this.addRenderableWidget(stopButton);

        // Keyframe controls
        addKeyframeButton = Button.builder(
            Component.translatable("screen.marallyzen.cutscene_editor.add_camera"),
            btn -> addCameraKeyframe()
        ).bounds(startX + (buttonWidth + spacing) * 3, bottomY, buttonWidth, buttonHeight).build();
        this.addRenderableWidget(addKeyframeButton);

        addPauseButton = Button.builder(
            Component.translatable("screen.marallyzen.cutscene_editor.add_pause"),
            btn -> addPauseKeyframe()
        ).bounds(startX + (buttonWidth + spacing) * 4, bottomY, buttonWidth, buttonHeight).build();
        this.addRenderableWidget(addPauseButton);

        addEmotionButton = Button.builder(
            Component.translatable("screen.marallyzen.cutscene_editor.add_emotion"),
            btn -> addEmotionKeyframe()
        ).bounds(startX + (buttonWidth + spacing) * 5, bottomY, buttonWidth, buttonHeight).build();
        this.addRenderableWidget(addEmotionButton);

        updateButtonStates();
        updateCameraModeButton();
        syncHideHandFromCheckbox();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Render semi-transparent background
        guiGraphics.fill(0, 0, this.width, this.height, 0x80000000);

        // Render keyframe list on the left
        renderKeyframeList(guiGraphics, mouseX, mouseY);

        // Render timeline at the bottom
        renderTimeline(guiGraphics, mouseX, mouseY);

        // Render status text
        renderStatus(guiGraphics);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderKeyframeList(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int panelX = 10;
        int panelY = 40;
        int panelWidth = 250;
        int panelHeight = this.height - 100;
        
        // Background
        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF2C2C2C);
        guiGraphics.renderOutline(panelX, panelY, panelWidth, panelHeight, 0xFF555555);

        // Title
        guiGraphics.drawString(this.font,
            Component.translatable("screen.marallyzen.cutscene_editor.keyframes"),
            panelX + 5, panelY + 5, 0xFFFFFF);

        // List keyframes
        List<CutsceneEditorData.EditorKeyframe> keyframes = editorData.getKeyframes();
        java.util.List<RenderLine> lines = buildKeyframeLines(keyframes);
        int listTop = panelY + 25;
        int listBottom = panelY + panelHeight - 5;
        int listHeight = Math.max(0, listBottom - listTop);
        keyframeMaxScroll = Math.max(0, lines.size() * KEYFRAME_LINE_HEIGHT - listHeight);
        if (keyframeScrollOffset > keyframeMaxScroll) {
            keyframeScrollOffset = keyframeMaxScroll;
        }

        int startIndex = keyframeScrollOffset / KEYFRAME_LINE_HEIGHT;
        int yOffset = listTop - (keyframeScrollOffset % KEYFRAME_LINE_HEIGHT);
        for (int i = startIndex; i < lines.size() && yOffset < listBottom; i++) {
            RenderLine line = lines.get(i);
            guiGraphics.drawString(this.font, line.text(), panelX + 5, yOffset, line.color());
            yOffset += KEYFRAME_LINE_HEIGHT;
        }
    }

    private void renderTimeline(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int timelineY = this.height - 80;
        int timelineHeight = 30;
        int timelineX = 270;
        int timelineWidth = this.width - 280;

        // Background
        guiGraphics.fill(timelineX, timelineY, timelineX + timelineWidth, timelineY + timelineHeight, 0xFF1A1A1A);
        guiGraphics.renderOutline(timelineX, timelineY, timelineWidth, timelineHeight, 0xFF555555);

        // Draw keyframe markers
        long maxTime = Math.max(editorData.getTotalDuration(), 100);
        for (CutsceneEditorData.EditorKeyframe kf : editorData.getKeyframes()) {
            int x = timelineX + (int) ((kf.getTime() / (float) maxTime) * timelineWidth);
            guiGraphics.fill(x - 2, timelineY, x + 2, timelineY + timelineHeight, 0xFFD48E03);
        }

        // Draw current time indicator if previewing
        if (isPreviewing && previewPlayer != null) {
            long currentTime = previewPlayer.getCurrentTime();
            int x = timelineX + (int) ((currentTime / (float) maxTime) * timelineWidth);
            guiGraphics.fill(x - 1, timelineY, x + 1, timelineY + timelineHeight, 0xFF00FF00);
        }
    }

    private void renderStatus(GuiGraphics guiGraphics) {
        int statusY = this.height - 20;
        String status = "";
        
        if (recorder.isRecording()) {
            status = recorder.isPaused()
                ? Component.translatable("screen.marallyzen.cutscene_editor.status_recording_paused").getString()
                : Component.translatable("screen.marallyzen.cutscene_editor.status_recording").getString();
        } else if (isPreviewing) {
            status = Component.translatable("screen.marallyzen.cutscene_editor.status_previewing").getString();
        } else {
            status = Component.translatable("screen.marallyzen.cutscene_editor.status_ready").getString();
        }

        guiGraphics.drawString(this.font, status, 10, statusY, 0xFFFFFF);
    }

    private void createNewScene() {
        editorData = new CutsceneEditorData("new_cutscene");
        lastEditorData = editorData;
        editorData.setRecordedActorTracks(null);
        editorData.setWorldTrack(null);
        updateNextGroupId();
        sceneIdBox.setValue("new_cutscene");
        selectedKeyframeIndex = -1;
        updateButtonStates();
    }

    private void openSceneDialog() {
        // Load scene from file
        // For now, use the scene ID from the text box
        String sceneId = sceneIdBox.getValue();
        if (sceneId == null || sceneId.isEmpty()) {
            return;
        }
        
        try {
            CutsceneEditorData loadedData = CutsceneEditorSerializer.load(sceneId);
            this.editorData = loadedData;
            lastEditorData = loadedData;
            loadedData.setRecordedActorTracks(null);
            updateNextGroupId();
            sceneIdBox.setValue(loadedData.getId());
            selectedKeyframeIndex = -1;
            Marallyzen.LOGGER.info("Loaded cutscene: {}", sceneId);
        } catch (IOException e) {
            Marallyzen.LOGGER.error("Failed to load cutscene: {}", sceneId, e);
        }
    }

    private void saveScene() {
        try {
            CutsceneEditorSerializer.save(editorData);
            // Refresh scene registry so newly saved cutscenes can be played immediately.
            neutka.marallys.marallyzen.client.camera.SceneLoader.loadScenes();
            // Show success message
            Marallyzen.LOGGER.info("Cutscene saved: {}", editorData.getId());
        } catch (IOException e) {
            Marallyzen.LOGGER.error("Failed to save cutscene", e);
        }
    }

    private void togglePreview() {
        if (isPreviewing) {
            previewPlayer.stop();
            isPreviewing = false;
        } else {
            syncHideHandFromCheckbox();
            CutscenePreviewPlayer.setFixedCameraMode(previewFixedCamera);
            CutscenePreviewPlayer.setHideHandInPreview(previewHideHand);
            previewPlayer.start(editorData);
            isPreviewing = true;
        }
        updateButtonStates();
    }

    private void toggleRecording() {
        if (recorder.isRecording()) {
            stopRecording();
        } else {
            startRecording();
        }
        updateButtonStates();
    }

    private void startRecording() {
        recorder.startRecording();
        CutsceneEditorData.CameraKeyframe fixedCamera = null;
        if (previewFixedCamera) {
            fixedCamera = findFixedCamera(editorData);
        }
        if (fixedCamera != null) {
            BlockPos center = BlockPos.containing(fixedCamera.getPosition());
            CutsceneWorldManager.startRecording(editorData.getId(), center.getX() >> 4, center.getZ() >> 4);
        } else {
            CutsceneWorldManager.startRecording(editorData.getId());
        }
        ReplayManager.startRecording(editorData.getId(), buildReplayMarkers());
        cameraController.saveCameraType();
        fixedRecordingMouseGrabbed = false;
        editorData.setRecordedActorTracks(null);
        editorData.setWorldTrack(null);
        fixedRecordingActive = false;
        fixedRecordingCamera = null;
        if (previewFixedCamera) {
            if (fixedCamera != null) {
                fixedRecordingActive = true;
                fixedRecordingCamera = fixedCamera;
                activeFixedRecordingScreen = this;
                cameraController.setThirdPerson();
                var camController = neutka.marallys.marallyzen.client.camera.CameraManager.getInstance().getCameraController();
                camController.activate(false);
                camController.setRawState(
                    fixedCamera.getPosition(),
                    fixedCamera.getYaw(),
                    fixedCamera.getPitch(),
                    fixedCamera.getFov()
                );
                if (minecraft != null && minecraft.screen == this) {
                    minecraft.setScreen(null);
                }
                grabMouseForFixedRecording();
                if (minecraft != null && minecraft.options != null) {
                    prevSmoothCamera = minecraft.options.smoothCamera;
                    minecraft.options.smoothCamera = false;
                }
            }
        }
        cameraController.setBlockInput(!fixedRecordingActive);
        updateButtonStates();
    }

    private void stopRecording() {
        if (recorder.isRecording()) {
            recorder.stopRecording();
            ReplayManager.updateMarkers(buildReplayMarkers());
            ReplayManager.stopRecording();
            CutsceneWorldManager.stopRecording(editorData.getId());

            if (recorder.hasRecordedActors()) {
                editorData.setRecordedActorTracks(recorder.snapshotRecordedActors());
            } else {
                editorData.setRecordedActorTracks(null);
            }
            
            // Convert recorded frames to keyframes
            List<CutsceneRecorder.RecordedFrame> frames = recorder.getRecordedFrames();
            if (!frames.isEmpty()) {
                // Find the last keyframe time or use 0
                long baseTime = 0;
                if (!editorData.getKeyframes().isEmpty()) {
                    CutsceneEditorData.EditorKeyframe lastKf = editorData.getKeyframes().get(editorData.getKeyframes().size() - 1);
                    baseTime = lastKf.getTime() + 20; // Add 1 second after last keyframe
                }
                
                int groupId = nextGroupId++;
                if (previewFixedCamera && hasFixedCamera(editorData)) {
                    List<CutsceneEditorData.ActorKeyframe> keyframes = recorder.convertToActorKeyframes(baseTime);
                    for (CutsceneEditorData.ActorKeyframe kf : keyframes) {
                        kf.setGroupId(groupId);
                        editorData.addKeyframe(kf);
                    }
                    List<CutsceneEditorData.EmotionKeyframe> emoteKeyframes = recorder.convertToActorEmoteKeyframes(baseTime);
                    for (CutsceneEditorData.EmotionKeyframe kf : emoteKeyframes) {
                        kf.setGroupId(groupId);
                        editorData.addKeyframe(kf);
                    }
                } else {
                    List<CutsceneEditorData.CameraKeyframe> keyframes = recorder.convertToKeyframes(baseTime);
                    for (CutsceneEditorData.CameraKeyframe kf : keyframes) {
                        kf.setGroupId(groupId);
                        editorData.addKeyframe(kf);
                    }
                }
            }
            
            recorder.clear();
        }
        if (fixedRecordingActive) {
            var camController = neutka.marallys.marallyzen.client.camera.CameraManager.getInstance().getCameraController();
            camController.deactivate();
            fixedRecordingActive = false;
            fixedRecordingCamera = null;
            if (activeFixedRecordingScreen == this) {
                activeFixedRecordingScreen = null;
            }
            releaseMouseForFixedRecording();
            if (minecraft != null && minecraft.options != null && prevSmoothCamera != null) {
                minecraft.options.smoothCamera = prevSmoothCamera;
                prevSmoothCamera = null;
            }
        }
        cameraController.setBlockInput(false);
        cameraController.restoreCameraType();
        updateButtonStates();
    }

    private void togglePause() {
        if (recorder.isRecording()) {
            if (recorder.isPaused()) {
                recorder.resumeRecording();
                ReplayManager.resume();
                CutsceneWorldManager.resumeRecording(editorData.getId());
            } else {
                recorder.pauseRecording();
                ReplayManager.pause();
                CutsceneWorldManager.pauseRecording(editorData.getId());
            }
        }
        updateButtonStates();
    }

    private void addCameraKeyframe() {
        Vec3 position = cameraController.getCurrentPosition();
        float[] rotation = cameraController.getCurrentRotation();
        float fov = cameraController.getCurrentFov();
        
        long time = editorData.getKeyframes().isEmpty() ? 0 : 
            editorData.getKeyframes().get(editorData.getKeyframes().size() - 1).getTime() + 20;
        
        CutsceneEditorData.CameraKeyframe keyframe = new CutsceneEditorData.CameraKeyframe(
            time, position, rotation[0], rotation[1], fov, 20, true
        );
        keyframe.setGroupId(nextGroupId++);
        editorData.addKeyframe(keyframe);
    }

    private void addPauseKeyframe() {
        long time = editorData.getKeyframes().isEmpty() ? 0 : 
            editorData.getKeyframes().get(editorData.getKeyframes().size() - 1).getTime() + 20;
        
        CutsceneEditorData.PauseKeyframe keyframe = new CutsceneEditorData.PauseKeyframe(time, 20);
        keyframe.setGroupId(nextGroupId++);
        editorData.addKeyframe(keyframe);
    }

    private void addEmotionKeyframe() {
        // Open emotion selector dialog
        this.minecraft.setScreen(new EmoteSelectorScreen(this, emoteId -> {
            // Get NPC ID (for now, use a placeholder - could be selected from a list)
            String npcId = "npc_1"; // TODO: Allow user to select NPC
            
            long time = editorData.getKeyframes().isEmpty() ? 0 : 
                editorData.getKeyframes().get(editorData.getKeyframes().size() - 1).getTime() + 20;
            
            CutsceneEditorData.EmotionKeyframe keyframe = new CutsceneEditorData.EmotionKeyframe(time, npcId, emoteId);
            keyframe.setGroupId(nextGroupId++);
            editorData.addKeyframe(keyframe);
            
            // Return to editor
            this.minecraft.setScreen(this);
        }));
    }

    private void updateButtonStates() {
        boolean recording = recorder.isRecording();
        boolean paused = recorder.isPaused();
        
        recordButton.setMessage(recording
            ? Component.translatable("screen.marallyzen.cutscene_editor.stop")
            : Component.translatable("screen.marallyzen.cutscene_editor.record"));
        pauseButton.active = recording;
        stopButton.active = recording;
        previewButton.active = !recording && !isPreviewing;
    }

    private void toggleCameraMode() {
        previewFixedCamera = !previewFixedCamera;
        CutscenePreviewPlayer.setFixedCameraMode(previewFixedCamera);
        updateCameraModeButton();
    }

    private void updateCameraModeButton() {
        cameraModeButton.setMessage(getCameraModeLabel());
    }

    private Component getCameraModeLabel() {
        Component mode = Component.translatable(
            previewFixedCamera
                ? "screen.marallyzen.cutscene_editor.camera_mode_fixed"
                : "screen.marallyzen.cutscene_editor.camera_mode_follow"
        );
        return Component.translatable("screen.marallyzen.cutscene_editor.camera_mode", mode);
    }

    private Checkbox createCheckbox(int x, int y, int width, Component label, boolean selected) {
        try {
            java.lang.reflect.Constructor<Checkbox> ctor = Checkbox.class.getDeclaredConstructor(
                int.class,
                int.class,
                int.class,
                Component.class,
                net.minecraft.client.gui.Font.class,
                boolean.class,
                Checkbox.OnValueChange.class
            );
            ctor.setAccessible(true);
            Checkbox.OnValueChange handler = (checkbox, value) -> {
                previewHideHand = value;
                CutscenePreviewPlayer.setHideHandInPreview(previewHideHand);
            };
            Object[] args = new Object[] { x, y, width, label, this.font, selected, handler };
            return ctor.newInstance(args);
        } catch (ReflectiveOperationException e) {
            Marallyzen.LOGGER.error("Failed to create preview checkbox", e);
            return null;
        }
    }

    private Component getHideHandLabel() {
        return Component.translatable("screen.marallyzen.cutscene_editor.hide_hand");
    }

    private void syncHideHandFromCheckbox() {
        if (hideHandCheckbox == null) {
            return;
        }
        boolean selected = hideHandCheckbox.selected();
        if (previewHideHand != selected) {
            previewHideHand = selected;
            CutscenePreviewPlayer.setHideHandInPreview(previewHideHand);
        }
    }

    @Override
    public void tick() {
        super.tick();
        syncHideHandFromCheckbox();
        
        if (recorder != null && recorder.isRecording() && !fixedRecordingActive) {
            tickRecordingInternal();
        }
        
        if (isPreviewing && previewPlayer != null) {
            previewPlayer.tick();
            if (!previewPlayer.isPlaying()) {
                isPreviewing = false;
                updateButtonStates();
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Handle hotkeys
        if (keyCode == 82) { // R key
            toggleRecording();
            return true;
        }
        if (keyCode == 32) { // Space
            if (recorder.isRecording()) {
                togglePause();
            }
            return true;
        }
        if (keyCode == 75) { // K key
            addCameraKeyframe();
            return true;
        }
        if (keyCode == 80) { // P key
            togglePreview();
            return true;
        }
        if (keyCode == 256) { // ESC
            onClose();
            return true;
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        int panelX = 10;
        int panelY = 40;
        int panelWidth = 250;
        int panelHeight = this.height - 100;
        if (mouseX >= panelX && mouseX <= panelX + panelWidth
            && mouseY >= panelY && mouseY <= panelY + panelHeight) {
            int scrollDelta = (int) Math.signum(deltaY) * KEYFRAME_LINE_HEIGHT;
            keyframeScrollOffset = Math.max(0, Math.min(keyframeMaxScroll, keyframeScrollOffset - scrollDelta));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public boolean isPauseScreen() {
        return false; // Allow game to continue running
    }

    @Override
    public void onClose() {
        lastEditorData = editorData;
        if (!fixedRecordingActive) {
            releaseMouseForFixedRecording();
        }
        if (isPreviewing) {
            previewPlayer.stop();
        }
        if (!recorder.isRecording()) {
            cameraController.setBlockInput(false);
            cameraController.restoreCameraType();
        }
        super.onClose();
    }

    public void tickFixedRecordingBackground() {
        if (!fixedRecordingActive || recorder == null || !recorder.isRecording()) {
            if (!fixedRecordingActive && activeFixedRecordingScreen == this) {
                activeFixedRecordingScreen = null;
            }
            return;
        }
        tickRecordingInternal();
    }

    private void tickRecordingInternal() {
        recorder.tick();
        if (fixedRecordingActive && fixedRecordingCamera != null) {
            var camController = neutka.marallys.marallyzen.client.camera.CameraManager.getInstance().getCameraController();
            camController.setRawState(
                fixedRecordingCamera.getPosition(),
                fixedRecordingCamera.getYaw(),
                fixedRecordingCamera.getPitch(),
                fixedRecordingCamera.getFov()
            );
            recorder.recordActors(
                fixedRecordingCamera.getPosition(),
                fixedRecordingCamera.getYaw(),
                fixedRecordingCamera.getPitch(),
                fixedRecordingCamera.getFov()
            );
        } else if (minecraft != null && minecraft.player != null) {
            float fov = 70.0f;
            if (minecraft.options != null) {
                fov = minecraft.options.fov().get().floatValue();
            }
            Vec3 pos = minecraft.player.getEyePosition();
            recorder.recordActors(
                pos,
                minecraft.player.getYRot(),
                minecraft.player.getXRot(),
                fov
            );
        }
    }

    private void updateNextGroupId() {
        int max = 0;
        for (CutsceneEditorData.EditorKeyframe keyframe : editorData.getKeyframes()) {
            if (keyframe.getGroupId() > max) {
                max = keyframe.getGroupId();
            }
        }
        nextGroupId = Math.max(1, max + 1);
    }

    private List<ReplayMarker> buildReplayMarkers() {
        List<ReplayMarker> markers = new ArrayList<>();
        List<CutsceneEditorData.EditorKeyframe> keyframes = editorData.getKeyframes();
        for (int i = 0; i < keyframes.size(); i++) {
            CutsceneEditorData.EditorKeyframe keyframe = keyframes.get(i);
            String label = keyframe.getType().name();
            if (keyframe.getGroupId() >= 0) {
                label = label + "#" + keyframe.getGroupId();
            }
            markers.add(new ReplayMarker(keyframe.getTime(), label));
        }
        return markers;
    }

    private static java.util.Map<Integer, long[]> buildGroupRanges(List<CutsceneEditorData.EditorKeyframe> keyframes) {
        java.util.Map<Integer, long[]> ranges = new java.util.HashMap<>();
        for (CutsceneEditorData.EditorKeyframe keyframe : keyframes) {
            int groupId = keyframe.getGroupId();
            if (groupId < 0) {
                continue;
            }
            long start = keyframe.getTime();
            long end = keyframe.getTime();
            if (keyframe instanceof CutsceneEditorData.CameraKeyframe cameraKf) {
                end += cameraKf.getDuration();
            } else if (keyframe instanceof CutsceneEditorData.ActorKeyframe actorKf) {
                end += actorKf.getDuration();
            } else if (keyframe instanceof CutsceneEditorData.PauseKeyframe pauseKf) {
                end += pauseKf.getDuration();
            }
            long[] range = ranges.get(groupId);
            if (range == null) {
                ranges.put(groupId, new long[] { start, end });
            } else {
                range[0] = Math.min(range[0], start);
                range[1] = Math.max(range[1], end);
            }
        }
        return ranges;
    }

    private static java.util.Map<Integer, String> buildGroupLabels(List<CutsceneEditorData.EditorKeyframe> keyframes) {
        java.util.Map<Integer, String> labels = new java.util.HashMap<>();
        for (CutsceneEditorData.EditorKeyframe keyframe : keyframes) {
            int groupId = keyframe.getGroupId();
            if (groupId < 0 || labels.containsKey(groupId)) {
                continue;
            }
            String label = switch (keyframe.getType()) {
                case CAMERA -> Component.translatable("screen.marallyzen.cutscene_editor.group_camera").getString();
                case PAUSE -> Component.translatable("screen.marallyzen.cutscene_editor.group_pause").getString();
                case EMOTION -> Component.translatable("screen.marallyzen.cutscene_editor.group_emotion").getString();
                case CAMERA_MODE -> Component.translatable("screen.marallyzen.cutscene_editor.group_mode").getString();
                case ACTOR -> Component.translatable("screen.marallyzen.cutscene_editor.group_actor").getString();
            };
            labels.put(groupId, label);
        }
        return labels;
    }

    private record RenderLine(Component text, int color) {}

    private List<RenderLine> buildKeyframeLines(List<CutsceneEditorData.EditorKeyframe> keyframes) {
        java.util.List<RenderLine> lines = new java.util.ArrayList<>();
        java.util.Map<Integer, long[]> groupRanges = buildGroupRanges(keyframes);
        java.util.Map<Integer, String> groupLabels = buildGroupLabels(keyframes);
        java.util.Set<Integer> drawnGroups = new java.util.HashSet<>();
        for (int i = 0; i < keyframes.size(); i++) {
            CutsceneEditorData.EditorKeyframe kf = keyframes.get(i);
            if (kf.getGroupId() >= 0) {
                int groupId = kf.getGroupId();
                if (!drawnGroups.contains(groupId)) {
                    drawnGroups.add(groupId);
                    long[] range = groupRanges.get(groupId);
                    long start = range != null ? range[0] : kf.getTime();
                    long end = range != null ? range[1] : kf.getTime();
                    String label = groupLabels.getOrDefault(groupId,
                        Component.translatable("screen.marallyzen.cutscene_editor.group_generic").getString());
                    Component header = Component.translatable(
                        "screen.marallyzen.cutscene_editor.group_header",
                        label,
                        groupId,
                        start,
                        end
                    );
                    lines.add(new RenderLine(header, 0xAAAAAA));
                }
            }
            Component text = Component.literal(
                String.format("%d: %s @ %d", i, kf.getType().name(), kf.getTime()));
            int color = (i == selectedKeyframeIndex) ? 0xFFD48E03 : 0xFFFFFF;
            lines.add(new RenderLine(text, color));
        }
        return lines;
    }

    private static boolean hasFixedCamera(CutsceneEditorData data) {
        if (data == null) {
            return false;
        }
        java.util.Map<Integer, Integer> counts = new java.util.HashMap<>();
        for (CutsceneEditorData.EditorKeyframe keyframe : data.getKeyframes()) {
            if (keyframe instanceof CutsceneEditorData.CameraKeyframe && keyframe.getGroupId() >= 0) {
                counts.merge(keyframe.getGroupId(), 1, Integer::sum);
            }
        }
        return counts.values().stream().anyMatch(count -> count == 1);
    }

    private static CutsceneEditorData.CameraKeyframe findFixedCamera(CutsceneEditorData data) {
        if (data == null) {
            return null;
        }
        java.util.Map<Integer, java.util.List<CutsceneEditorData.CameraKeyframe>> groups = new java.util.HashMap<>();
        for (CutsceneEditorData.EditorKeyframe keyframe : data.getKeyframes()) {
            if (keyframe instanceof CutsceneEditorData.CameraKeyframe cameraKf && keyframe.getGroupId() >= 0) {
                groups.computeIfAbsent(keyframe.getGroupId(), k -> new java.util.ArrayList<>()).add(cameraKf);
            }
        }
        CutsceneEditorData.CameraKeyframe lastSingle = null;
        long lastTime = Long.MIN_VALUE;
        for (var entry : groups.entrySet()) {
            java.util.List<CutsceneEditorData.CameraKeyframe> frames = entry.getValue();
            if (frames.size() != 1) {
                continue;
            }
            CutsceneEditorData.CameraKeyframe cam = frames.get(0);
            if (cam.getTime() >= lastTime) {
                lastTime = cam.getTime();
                lastSingle = cam;
            }
        }
        return lastSingle;
    }

    private void grabMouseForFixedRecording() {
        if (minecraft == null) {
            return;
        }
        fixedRecordingMouseGrabbed = true;
        try {
            java.lang.reflect.Method grab = minecraft.mouseHandler.getClass().getMethod("grabMouse");
            grab.invoke(minecraft.mouseHandler);
        } catch (Exception ignored) {
        }
    }

    private void releaseMouseForFixedRecording() {
        if (!fixedRecordingMouseGrabbed || minecraft == null) {
            fixedRecordingMouseGrabbed = false;
            return;
        }
        try {
            java.lang.reflect.Method release = minecraft.mouseHandler.getClass().getMethod("releaseMouse");
            release.invoke(minecraft.mouseHandler);
        } catch (Exception ignored) {
        }
        fixedRecordingMouseGrabbed = false;
    }
}

