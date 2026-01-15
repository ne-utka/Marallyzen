package neutka.marallys.marallyzen.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * HUD component for displaying NPC dialog in the game world.
 * Renders dialog box next to NPC with smooth animation.
 */
public class DialogHud {
    private static DialogHud instance;
    
    private String dialogId;
    private String dialogTitle;
    private List<DialogOption> options;
    private Entity npcEntity;
    private UUID npcUuid;
    private int selectedIndex = 0;
    private float animationProgress = 0.0f; // 0.0 = hidden, 1.0 = fully visible (legacy, kept for compatibility)
    private boolean isVisible = false;
    
    // Animation state tracking
    private float fadeInProgress = 0.0f; // Progress of fade-in animation (0.0 → 1.0)
    private float fadeOutProgress = 1.0f; // Progress of fade-out animation (1.0 → 0.0)
    private int lastSelectedIndex = -1; // Track selected index changes for pulse animation
    private float pulseProgress = 0.0f; // Progress of pulse animation (0.0 → 1.0)
    private boolean isPulsing = false; // Flag indicating if pulse animation is active
    
    // Previous animation values for smooth interpolation with partialTick
    private float previousFadeInProgress = 0.0f;
    private float previousFadeOutProgress = 1.0f;
    private float previousPulseProgress = 0.0f;
    private float previousBackgroundSwitchProgress = 0.0f;
    
    // Background fill state tracking
    private int previousSelectedIndex = -1; // Previous selected index for tracking background switch
    private float backgroundSwitchProgress = 0.0f; // Progress of background switch animation (0.0 → 1.0)
    private boolean isBackgroundSwitching = false; // Flag indicating if background switch animation is active
    private int npcNameColor = 0xD48E03FF; // NPC name color in ARGB format (default: prime color)
    private static final int PRIME_COLOR = 0xD48E03;
    private static final int NARRATION_BG_COLOR = 0x000000;
    
    // Cooldown removed - state machine handles transitions now
    
    // Animation timing constants (Bedrock Edition style — fast & snappy)
    private static final int FADE_IN_DURATION_TICKS = 3; // 0.15 seconds at 20 TPS (Bedrock fast appear)
    private static final int FADE_OUT_DURATION_TICKS = 2; // 0.10 seconds at 20 TPS (snappy exit)
    private static final float VERTICAL_OFFSET_MAX = 0.1f; // Maximum vertical offset in blocks
    private static final float SCALE_BASE = 0.0171f; // Base text scale (reduced by 10% then 5%: 0.02 * 0.9 * 0.95)
    private static final float SCALE_OVERSHOOT = 0.01881f; // Overshoot scale during appearance (reduced by 10% then 5%: 0.022 * 0.9 * 0.95)
    private static final float SCALE_DECREASE_ON_HIDE = 0.01f; // Scale decrease on hide (in blocks)
    private static final int PULSE_DURATION_TICKS = 6; // 0.3 seconds at 20 TPS (Bedrock quick micro-pulse)
    private static final float PULSE_SCALE_AMPLITUDE = 0.003f; // Amplitude of pulse scale effect (legacy, now using 0.04f in code)
    
    // Background fill animation constants
    private static final int BACKGROUND_SWITCH_DURATION_TICKS = 4; // 0.2 seconds at 20 TPS (fast Bedrock switch)
    private static final float BACKGROUND_PADDING_X = 3.42f; // Horizontal padding of background from text (reduced by 10% then 5%: 4.0 * 0.9 * 0.95)
    private static final float BACKGROUND_PADDING_Y = 0.855f; // Vertical padding of background from text (reduced by 10% then 5%: 1.0 * 0.9 * 0.95)
    
    // Legacy constants (kept for compatibility, will be replaced)
    private static final float ANIMATION_SPEED = 0.1f; // How fast the dialog appears
    private static final float TEXT_SCALE = 0.0171f; // Scale for 3D text rendering (reduced by 10% then 5%: 0.02 * 0.9 * 0.95)
    private static final float OFFSET_RIGHT = 1.0f; // Offset to the right of NPC (in blocks)
    private static final int OPTION_HEIGHT_PIXELS = 13; // Height in pixels (reduced by 10% then 5%: 16 * 0.9 * 0.95, rounded)
    private static final int OPTION_SPACING_PIXELS = 4; // Spacing between options (increased for better readability)
    private static final int PADDING_PIXELS = 7; // Padding (reduced by 10% then 5%: 8 * 0.9 * 0.95, rounded)
    private static final int SELECTED_TEXT_OFFSET_PIXELS = 4; // Offset in pixels when option is selected
    private static final float SELECTED_HORIZONTAL_OFFSET = 6.84f; // Horizontal offset in pixels for selected option (reduced by 10% then 5%: 8.0 * 0.9 * 0.95)
    private static final double MAX_DIALOG_DISTANCE = 8.0; // Maximum distance from NPC to keep dialog open (in blocks)
    
    public static DialogHud getInstance() {
        if (instance == null) {
            instance = new DialogHud();
        }
        return instance;
    }
    
    private DialogHud() {
        // Subscribe to state machine changes
        DialogStateMachine.getInstance().addStateChangeListener(this::onStateChanged);
    }
    
    /**
     * Gets the UUID of the NPC this dialog is associated with.
     * @return The NPC UUID, or null if no NPC is associated
     */
    public UUID getNpcUuid() {
        return npcUuid;
    }
    
    /**
     * Called when dialog state changes.
     * This is a subscriber callback from DialogStateMachine.
     */
    private void onStateChanged(DialogState newState) {
        neutka.marallys.marallyzen.Marallyzen.LOGGER.info("DialogHud: State changed to {}", newState);
        
        switch (newState) {
            case IDLE:
            case CLOSED:
                // Dialog should be hidden
                this.isVisible = false;
                neutka.marallys.marallyzen.Marallyzen.LOGGER.debug("DialogHud: Dialog hidden (state: {})", newState);
                break;
            case OPENING:
                // Start opening animation
                this.isVisible = true;
                this.fadeInProgress = 0.0f;
                this.fadeOutProgress = 1.0f;
                neutka.marallys.marallyzen.Marallyzen.LOGGER.debug("DialogHud: Starting opening animation");
                break;
            case CHOICE:
                // Dialog is fully visible and interactive
                this.isVisible = true;
                this.fadeInProgress = 1.0f;
                this.fadeOutProgress = 1.0f;
                neutka.marallys.marallyzen.Marallyzen.LOGGER.debug("DialogHud: Dialog fully visible and interactive");
                break;
            case EXECUTING:
            case TRANSITION:
                // Dialog should be hidden during execution/transition
                this.isVisible = false;
                // Immediately set fade-out to start hiding animation
                this.fadeOutProgress = 1.0f; // Start fade-out from current visibility
                break;
        }
    }
    
    /**
     * Updates dialog content and triggers state transition to OPENING.
     * This is called when OpenDialogPacket is received.
     */
    public void updateDialogContent(String dialogId, String dialogTitle, Map<String, String> buttons, Entity npcEntity) {
        neutka.marallys.marallyzen.Marallyzen.LOGGER.info("DialogHud: Updating dialog content '{}' with {} options", dialogId, buttons.size());
        
        this.dialogId = dialogId;
        this.dialogTitle = dialogTitle;
        this.npcEntity = npcEntity;
        this.npcUuid = npcEntity != null ? npcEntity.getUUID() : null;
        this.selectedIndex = 0;
        
        // Extract and save NPC name color
        this.npcNameColor = extractNpcNameColor(npcEntity);
        
        // Reset all animation states
        this.animationProgress = 0.0f;
        this.fadeInProgress = 0.0f;
        this.fadeOutProgress = 1.0f;
        this.pulseProgress = 0.0f;
        this.isPulsing = false;
        this.lastSelectedIndex = 0;
        this.previousSelectedIndex = 0;
        this.backgroundSwitchProgress = 0.0f;
        this.isBackgroundSwitching = false;
        
        // Convert buttons map to options list
        this.options = new ArrayList<>();
        int index = 0;
        for (Map.Entry<String, String> entry : buttons.entrySet()) {
            options.add(new DialogOption(entry.getKey(), entry.getValue(), index));
            index++;
        }
        
        // Trigger state transition to OPENING (which will automatically transition to CHOICE)
        neutka.marallys.marallyzen.Marallyzen.LOGGER.info("DialogHud: Triggering transition to OPENING state");
        DialogStateMachine.getInstance().transitionTo(DialogState.OPENING);
    }
    
    /**
     * Updates animation based on current state.
     * This is called every tick to update visual animations.
     */
    public void tick() {
        DialogStateMachine stateMachine = DialogStateMachine.getInstance();
        
        // IMPORTANT: Update state machine to handle automatic transitions (e.g., OPENING -> CHOICE)
        stateMachine.tick();
        
        DialogState currentState = stateMachine.getCurrentState();
        
        // Save previous values for smooth interpolation
        previousFadeInProgress = fadeInProgress;
        previousFadeOutProgress = fadeOutProgress;
        previousPulseProgress = pulseProgress;
        previousBackgroundSwitchProgress = backgroundSwitchProgress;
        
        // Update animations based on state
        if (currentState == DialogState.OPENING || currentState == DialogState.CHOICE) {
            // Fade-in animation: progress from 0.0 to 1.0 over FADE_IN_DURATION_TICKS
            if (fadeInProgress < 1.0f) {
                fadeInProgress = Mth.clamp(fadeInProgress + (1.0f / FADE_IN_DURATION_TICKS), 0.0f, 1.0f);
            }
            // Reset fade-out progress when visible
            fadeOutProgress = 1.0f;
            
            // Legacy animationProgress for compatibility
            animationProgress = fadeInProgress;
            
            // Track selected index changes for background switch animation
            if (selectedIndex != previousSelectedIndex && previousSelectedIndex >= 0) {
                isBackgroundSwitching = true;
                backgroundSwitchProgress = 0.0f;
                previousBackgroundSwitchProgress = 0.0f;
            }
            previousSelectedIndex = selectedIndex;
            
            // Update background switch animation
            if (isBackgroundSwitching) {
                backgroundSwitchProgress = Mth.clamp(backgroundSwitchProgress + (1.0f / BACKGROUND_SWITCH_DURATION_TICKS), 0.0f, 1.0f);
                if (backgroundSwitchProgress >= 1.0f) {
                    isBackgroundSwitching = false;
                }
            }
            
            // Track selected index changes for pulse animation
            if (selectedIndex != lastSelectedIndex) {
                isPulsing = true;
                pulseProgress = 0.0f;
                previousPulseProgress = 0.0f;
                lastSelectedIndex = selectedIndex;
            }
            
            // Update pulse animation
            if (isPulsing) {
                pulseProgress = Mth.clamp(pulseProgress + (1.0f / PULSE_DURATION_TICKS), 0.0f, 1.0f);
                if (pulseProgress >= 1.0f) {
                    isPulsing = false;
                }
            }
            
            // Check if NPC is still valid
            if (npcEntity == null || !npcEntity.isAlive() || npcEntity.isRemoved()) {
                // Send close packet to server to clear server-side state
                if (npcUuid != null) {
                    neutka.marallys.marallyzen.network.NetworkHelper.sendToServer(
                            new neutka.marallys.marallyzen.network.DialogClosePacket(npcUuid)
                    );
                }
                stateMachine.transitionTo(DialogState.CLOSED);
                return;
            }
            
            // Check distance from player to NPC
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.level != null) {
                double distance = mc.player.distanceTo(npcEntity);
                if (distance > MAX_DIALOG_DISTANCE) {
                    // Player moved too far away - close dialog and notify server
                    if (npcUuid != null) {
                        neutka.marallys.marallyzen.network.NetworkHelper.sendToServer(
                                new neutka.marallys.marallyzen.network.DialogClosePacket(npcUuid)
                        );
                    }
                    stateMachine.transitionTo(DialogState.CLOSED);
                }
            }
        } else if (currentState == DialogState.EXECUTING || currentState == DialogState.TRANSITION) {
            // Fade-out animation: progress from current fadeInProgress to 0.0 over FADE_OUT_DURATION_TICKS
            if (fadeOutProgress > 0.0f) {
                fadeOutProgress = Mth.clamp(fadeOutProgress - (1.0f / FADE_OUT_DURATION_TICKS), 0.0f, 1.0f);
            }
            // Legacy animationProgress for compatibility
            animationProgress = fadeOutProgress;
            
            // Stop pulse animation when hiding
            isPulsing = false;
            pulseProgress = 0.0f;
            previousPulseProgress = 0.0f;
        }
    }
    
    /**
     * Handles mouse wheel scroll to change selected option.
     * Triggers pulse animation when selection changes.
     */
    /**
     * Handles mouse wheel scroll to change selected option.
     * Only works in CHOICE state.
     */
    public boolean handleMouseScroll(double scrollDelta) {
        DialogStateMachine stateMachine = DialogStateMachine.getInstance();
        DialogState currentState = stateMachine.getCurrentState();
        int optionsCount = options != null ? options.size() : 0;
        
        neutka.marallys.marallyzen.Marallyzen.LOGGER.info("[DialogHud] handleMouseScroll called - state={}, optionsCount={}, currentIndex={}, scrollDelta={}", 
                currentState, optionsCount, selectedIndex, scrollDelta);
        
        if (currentState != DialogState.CHOICE || options == null || options.isEmpty()) {
            neutka.marallys.marallyzen.Marallyzen.LOGGER.warn("[DialogHud] handleMouseScroll - blocked: state={}, options={}", 
                    currentState, optionsCount);
            return false;
        }
        
        int oldIndex = selectedIndex;
        if (scrollDelta > 0) {
            // Scroll up (decrease index)
            selectedIndex = Math.max(0, selectedIndex - 1);
            neutka.marallys.marallyzen.Marallyzen.LOGGER.info("[DialogHud] Scroll UP: {} -> {}", oldIndex, selectedIndex);
        } else if (scrollDelta < 0) {
            // Scroll down (increase index)
            selectedIndex = Math.min(options.size() - 1, selectedIndex + 1);
            neutka.marallys.marallyzen.Marallyzen.LOGGER.info("[DialogHud] Scroll DOWN: {} -> {}", oldIndex, selectedIndex);
        } else {
            neutka.marallys.marallyzen.Marallyzen.LOGGER.warn("[DialogHud] Scroll delta is zero, ignoring");
            return false;
        }
        
        // Trigger pulse animation if selection changed
        if (selectedIndex != oldIndex) {
            isPulsing = true;
            pulseProgress = 0.0f;
            lastSelectedIndex = selectedIndex;
            neutka.marallys.marallyzen.Marallyzen.LOGGER.info("[DialogHud] Selection changed from {} to {}, pulse animation triggered", oldIndex, selectedIndex);
        } else {
            neutka.marallys.marallyzen.Marallyzen.LOGGER.info("[DialogHud] Selection unchanged (already at boundary: index={})", selectedIndex);
        }
        
        return true;
    }
    
    /**
     * Handles right click to confirm selection.
     * Only works in CHOICE state. Sends packet to server and transitions to EXECUTING.
     */
    public boolean handleRightClick() {
        DialogStateMachine stateMachine = DialogStateMachine.getInstance();
        DialogState currentState = stateMachine.getCurrentState();
        
        neutka.marallys.marallyzen.Marallyzen.LOGGER.info("[DialogHud] handleRightClick called - state={}, options={}, selectedIndex={}", 
                currentState, options != null ? options.size() : "null", selectedIndex);
        
        if (currentState != DialogState.CHOICE) {
            neutka.marallys.marallyzen.Marallyzen.LOGGER.warn("[DialogHud] handleRightClick - blocked: not in CHOICE state (current: {})", currentState);
            return false;
        }
        
        if (options == null || options.isEmpty()) {
            neutka.marallys.marallyzen.Marallyzen.LOGGER.warn("[DialogHud] handleRightClick - blocked: no options available");
            return false;
        }
        
        if (selectedIndex < 0 || selectedIndex >= options.size()) {
            neutka.marallys.marallyzen.Marallyzen.LOGGER.warn("[DialogHud] handleRightClick - blocked: invalid selectedIndex={} (options size: {})", 
                    selectedIndex, options.size());
            return false;
        }
        
        DialogOption selected = options.get(selectedIndex);
        neutka.marallys.marallyzen.Marallyzen.LOGGER.info("[DialogHud] handleRightClick - sending packet for option '{}' (id: '{}')", 
                selected.text, selected.id);
        
        // Enable FPV context and play dialog-select emote locally
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player != null) {
            neutka.marallys.marallyzen.client.fpv.MarallyzenRenderContext.setFpvEmoteEnabled(true);
            neutka.marallys.marallyzen.client.fpv.FpvEmoteInvoker.play(mc.player, "SPE_Poke");
        }
        
        // Send packet to server with selected option
        neutka.marallys.marallyzen.network.NetworkHelper.sendToServer(
                new neutka.marallys.marallyzen.network.DialogButtonClickPacket(dialogId, selected.id, npcUuid)
        );
        
        // Transition to EXECUTING state (server will send state change packet, but we do it locally for immediate feedback)
        stateMachine.transitionTo(DialogState.EXECUTING);
        
        neutka.marallys.marallyzen.Marallyzen.LOGGER.info("[DialogHud] handleRightClick - success, transitioned to EXECUTING");
        return true;
    }
    
    /**
     * Renders the dialog in 3D world space next to the NPC.
     * Bedrock Edition style animation with fade-in/out, vertical offset, scale overshoot, and pulse effects.
     */
    public void renderInWorld(PoseStack poseStack, Camera camera, float partialTick) {
        DialogStateMachine stateMachine = DialogStateMachine.getInstance();
        DialogState currentState = stateMachine.getCurrentState();
        
        // Only render if state machine says we should show the dialog window
        if (!stateMachine.shouldShowDialogWindow()) {
            return;
        }
        
        // Interpolate animation progress with partialTick for smooth 60+ FPS animation
        float interpolatedFadeIn = Mth.lerp(partialTick, previousFadeInProgress, fadeInProgress);
        float interpolatedFadeOut = Mth.lerp(partialTick, previousFadeOutProgress, fadeOutProgress);
        float interpolatedPulse = Mth.lerp(partialTick, previousPulseProgress, pulseProgress);
        float interpolatedBackgroundSwitch = Mth.lerp(partialTick, previousBackgroundSwitchProgress, backgroundSwitchProgress);
        
        // Check visibility using interpolated fade-out progress (allows fade-out animation to complete)
        float effectiveProgress = isVisible ? interpolatedFadeIn : interpolatedFadeOut;
        if (effectiveProgress <= 0.0f || npcEntity == null || options == null || options.isEmpty()) {
            // Clean up npcEntity when fade-out completes
            if (!isVisible && interpolatedFadeOut <= 0.0f) {
                this.npcEntity = null;
            }
            return;
        }
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        
        // Get NPC position (simplified - use current position to prevent jumping)
        // Add extra offset above head for better visibility
        double npcX = npcEntity.getX();
        double npcY = npcEntity.getY() + npcEntity.getBbHeight() + 0.45; // Add 0.45 blocks above head (increased for higher position)
        double npcZ = npcEntity.getZ();
        
        // Get camera (player) position
        double camX = camera.getPosition().x;
        double camY = camera.getPosition().y;
        double camZ = camera.getPosition().z;
        
        // Calculate distance from camera
        double dx = npcX - camX;
        double dy = npcY - camY;
        double dz = npcZ - camZ;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        
        // Don't render if too far away
        if (distance > 32.0) {
            return;
        }
        
        // ═══════════════════════════════════════════════════════════════════
        // BEDROCK CORE: One master curve drives opacity, scale, and offset
        // ═══════════════════════════════════════════════════════════════════
        float appearT = isVisible ? interpolatedFadeIn : interpolatedFadeOut;
        
        // Master appear curve — Bedrock style
        float appear = isVisible
                ? easeOutCubic(appearT)
                : 1.0f - easeInCubic(1.0f - appearT);
        
        appear = Mth.clamp(appear, 0.0f, 1.0f);
        
        poseStack.pushPose();
        
        // Calculate direction from NPC to camera for right-side offset
        double dirX = camX - npcX;
        double dirZ = camZ - npcZ;
        double dirDistance = Math.sqrt(dirX * dirX + dirZ * dirZ);
        
        // Translate to dialog position (relative to camera)
        double offsetX = npcX - camX;
        double offsetY = npcY - camY;
        double offsetZ = npcZ - camZ;
        
        // Vertical offset driven by master curve (soft slide up)
        offsetY -= VERTICAL_OFFSET_MAX * (1.0f - appear);
        
        // Add right-side offset if we have valid direction
        if (dirDistance > 0.001) {
            // Normalize direction
            dirX /= dirDistance;
            dirZ /= dirDistance;
            // Rotate 90 degrees clockwise to get right side: (x, z) -> (z, -x)
            double rightX = dirZ;
            double rightZ = -dirX;
            offsetX += rightX * OFFSET_RIGHT;
            offsetZ += rightZ * OFFSET_RIGHT;
        }
        
        poseStack.translate(offsetX, offsetY, offsetZ);
        
        // Make dialog face camera (billboard effect) - same as NpcTalkIconRenderer
        float cameraYaw = camera.getYRot();
        float cameraPitch = camera.getXRot();
        poseStack.mulPose(Axis.YP.rotationDegrees(-cameraYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(cameraPitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0f)); // Fix text orientation
        
        // ═══════════════════════════════════════════════════════════════════
        // BEDROCK SCALE: Simple easeOutBack "exhale" — no complex overshoot
        // ═══════════════════════════════════════════════════════════════════
        float scale = SCALE_BASE;
        
        if (isVisible) {
            // Gentle Bedrock-style "exhale" on appear
            float back = easeOutBack(appear);
            scale = SCALE_BASE * (0.92f + 0.08f * back);
        } else {
            // Quick collapse on hide
            scale = SCALE_BASE * appear;
        }
        
        // Prevent zero scale
        scale = Math.max(0.001f, scale);
        poseStack.scale(scale, scale, scale);
        
        // Get render buffer and font
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        Font font = mc.font;
        Matrix4f baseMatrix = poseStack.last().pose(); // Base matrix before any option-specific transformations
        
        // Calculate starting Y position (start from top, going down from head/eyes)
        // Text starts from head/eye level and goes downward
        // Increased offset to position text higher (reduced by 10% then 5%: 16.0 * 0.9 * 0.95)
        float startY = PADDING_PIXELS + 13.68f; // Add extra 13.68 pixels above
        
        // Render options with background fill for selected option
        float currentY = startY;
        for (int i = 0; i < options.size(); i++) {
            DialogOption option = options.get(i);
            boolean isSelected = i == selectedIndex;
            boolean isBye = "bye".equals(option.id);
            
            // Prepare option text
            String optionText = isBye ? "◄ " + option.text : (i + 1) + ". " + option.text;
            
                // Calculate background alpha for this option using interpolated progress
                // Bedrock uses easeOutCubic for smooth transitions
                float backgroundAlpha = 0.0f;
                if (isSelected) {
                    // Selected option: fade in if switching, otherwise fully opaque
                    if (isBackgroundSwitching) {
                        backgroundAlpha = easeOutCubic(interpolatedBackgroundSwitch);
                    } else {
                        backgroundAlpha = 1.0f;
                    }
                } else if (i == previousSelectedIndex && isBackgroundSwitching) {
                    // Previous selected option: fade out
                    backgroundAlpha = 1.0f - easeOutCubic(interpolatedBackgroundSwitch);
                }
            
            // Render background fill using base matrix (before transformations)
            // This ensures background is always rendered in the correct position
            if (backgroundAlpha > 0.0f) {
                float textWidth = font.width(optionText);
                float textHeight = font.lineHeight; // Get actual text height
                float bgHeight = textHeight + 4; // Text height + 2px top + 2px bottom
                
                // Calculate text center Y position (where text is actually rendered)
                float textCenterY = currentY + OPTION_HEIGHT_PIXELS / 2 - 4 + textHeight / 2;
                
                // Center background on text center: bgY + bgHeight/2 = textCenterY
                float bgY = textCenterY - bgHeight / 2;
                
                // Apply horizontal offset for currently selected option
                float bgX = -BACKGROUND_PADDING_X + (isSelected ? SELECTED_HORIZONTAL_OFFSET : 0.0f);
                float bgWidth = textWidth + BACKGROUND_PADDING_X * 2;
                
                // Apply background color with calculated alpha using narration fill color
                int bgAlpha = (int)(backgroundAlpha * 120); // ~47% of 255, same as proximity
                int bgColor = (bgAlpha << 24) | (NARRATION_BG_COLOR & 0xFFFFFF);
                
                fillRect(baseMatrix, bufferSource, bgX, bgY, bgWidth, bgHeight, bgColor);
            }
            
            // Bedrock-style micro-pulse: subtle scale bump that fades out
            float pulseScale = 1.0f;
            if (isSelected && isPulsing) {
                float p = easeOutCubic(interpolatedPulse);
                pulseScale = 1.0f + 0.04f * (1.0f - p);  // 4% max scale bump
            }
            
            // Calculate horizontal offset for selected option
            float horizontalOffset = isSelected ? SELECTED_HORIZONTAL_OFFSET : 0.0f;
            
            // Apply transformations for selected option (pulse scale and horizontal offset)
            Matrix4f textMatrix = baseMatrix;
            if (isSelected) {
                poseStack.pushPose();
                // Apply horizontal offset
                poseStack.translate(horizontalOffset, 0, 0);
                // Apply pulse scale around text center
                if (pulseScale != 1.0f) {
                    float textCenterY = currentY + OPTION_HEIGHT_PIXELS / 2 - 4;
                    poseStack.translate(0, textCenterY, 0);
                    poseStack.scale(pulseScale, pulseScale, pulseScale);
                    poseStack.translate(0, -textCenterY, 0);
                }
                textMatrix = poseStack.last().pose();
            }
            
            // Render text (NPC name color for selected, white for others)
            // Alpha driven by master appear curve for clean Bedrock look
            int alpha = (int)(appear * 255);
            int textColor;
            if (isSelected) {
                // Selected text uses prime color with appear alpha
                textColor = (alpha << 24) | (PRIME_COLOR & 0xFFFFFF);
            } else {
                // Other text is white with appear alpha
                textColor = (alpha << 24) | 0xFFFFFF;
            }
            // Use proper text rendering with see-through flag
            drawText(textMatrix, bufferSource, font, optionText, 0, currentY + OPTION_HEIGHT_PIXELS / 2 - 4, textColor);
            
            // Pop transformations if applied
            if (isSelected) {
                poseStack.popPose();
            }
            
            currentY += OPTION_HEIGHT_PIXELS + OPTION_SPACING_PIXELS;
        }
        
        bufferSource.endBatch();
        poseStack.popPose();
    }
    
    /**
     * Renders a simple rectangle (quad) in 3D space.
     * Simplified version for highlighting selected options.
     */
    private void fillRect(Matrix4f matrix, MultiBufferSource.BufferSource bufferSource,
                         float x, float y, float width, float height, int color) {
        // Use entityTranslucent render type with white texture for proper transparency in 3D
        // This ensures proper depth testing and transparency support
        com.mojang.blaze3d.vertex.VertexConsumer vertexConsumer = bufferSource.getBuffer(
                net.minecraft.client.renderer.RenderType.entityTranslucent(
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft", "textures/misc/white.png")
                )
        );
        
        // Extract color components (ARGB format)
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        
        float left = x;
        float top = y;
        float right = x + width;
        float bottom = y + height;
        float z = -0.002f; // Slightly behind text to avoid z-fighting
        
        // Full bright lighting and no overlay for entityTranslucent
        int light = net.minecraft.client.renderer.LightTexture.FULL_BRIGHT;
        int overlay = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
        int lightU = light & 0xFFFF;
        int lightV = (light >> 16) & 0xFFFF;
        int overlayU = overlay & 0xFFFF;
        int overlayV = (overlay >> 16) & 0xFFFF;
        
        // Normal pointing towards camera (negative Z)
        float normalX = 0.0f;
        float normalY = 0.0f;
        float normalZ = -1.0f;
        
        // Render quad (4 vertices) for entityTranslucent RenderType
        // Order: bottom-left, bottom-right, top-right, top-left
        vertexConsumer.addVertex(matrix, left, bottom, z)
                .setColor(r, g, b, a)
                .setUv(0.0f, 1.0f)
                .setUv1(overlayU, overlayV)
                .setUv2(lightU, lightV)
                .setNormal(normalX, normalY, normalZ);
        vertexConsumer.addVertex(matrix, right, bottom, z)
                .setColor(r, g, b, a)
                .setUv(1.0f, 1.0f)
                .setUv1(overlayU, overlayV)
                .setUv2(lightU, lightV)
                .setNormal(normalX, normalY, normalZ);
        vertexConsumer.addVertex(matrix, right, top, z)
                .setColor(r, g, b, a)
                .setUv(1.0f, 0.0f)
                .setUv1(overlayU, overlayV)
                .setUv2(lightU, lightV)
                .setNormal(normalX, normalY, normalZ);
        vertexConsumer.addVertex(matrix, left, top, z)
                .setColor(r, g, b, a)
                .setUv(0.0f, 0.0f)
                .setUv1(overlayU, overlayV)
                .setUv2(lightU, lightV)
                .setNormal(normalX, normalY, normalZ);
    }
    
    /**
     * Draws text in 3D space.
     * Simplified version for rendering dialog options.
     */
    private void drawText(Matrix4f matrix, MultiBufferSource.BufferSource bufferSource, Font font,
                         String text, float x, float y, int color) {
        // Use see-through mode to avoid z-fighting with world geometry
        font.drawInBatch(
                text,
                x,
                y,
                color,
                false,
                matrix,
                bufferSource,
                Font.DisplayMode.SEE_THROUGH, // Use see-through to avoid flickering
                0,
                15728880 // Full bright lighting
        );
    }
    
    /**
     * Extracts the color from NPC's name component.
     * Returns ARGB color format, defaulting to prime color if no color is found.
     * Handles both parsed Component with color and JSON string in Component text.
     */
    private int extractNpcNameColor(Entity npcEntity) {
        if (npcEntity == null) {
            return 0xD48E03FF; // Default prime color with full opacity
        }
        
        Component nameComponent = npcEntity.getCustomName();
        if (nameComponent == null) {
            nameComponent = npcEntity.getName();
        }
        
        if (nameComponent == null) {
            return 0xD48E03FF; // Default prime color with full opacity
        }
        
        // First, try to find color in the Component itself
        net.minecraft.network.chat.TextColor color = findFirstColorInComponent(nameComponent);
        
        // If no color found, try to parse JSON from the Component's text
        if (color == null) {
            String componentText = nameComponent.getString();
            // Check if the text looks like JSON
            if (componentText != null && (componentText.trim().startsWith("{") || componentText.trim().startsWith("["))) {
                try {
                    // Get RegistryAccess from client level if available
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.level != null) {
                        net.minecraft.core.RegistryAccess registryAccess = mc.level.registryAccess();
                        // Try to parse as JSON Component
                        Component parsedComponent = Component.Serializer.fromJson(componentText, registryAccess);
                        if (parsedComponent != null) {
                            color = findFirstColorInComponent(parsedComponent);
                        }
                    }
                } catch (Exception e) {
                    // JSON parsing failed, continue with default
                    neutka.marallys.marallyzen.Marallyzen.LOGGER.debug("Failed to parse NPC name JSON: {}", componentText, e);
                }
            }
        }
        
        int rgb = color != null ? color.getValue() : 0xD48E03; // Default yellow from name
        // Map Minecraft yellow to the project's prime gold tone for dialog UI.
        if (rgb == 0xD48E03 || rgb == 0xD48E03) {
            rgb = 0xD48E03;
        }
        return 0xFF000000 | (rgb & 0xFFFFFF); // ARGB format
    }
    
    /**
     * Recursively finds the first color in a Component and its siblings.
     */
    private net.minecraft.network.chat.TextColor findFirstColorInComponent(Component component) {
        if (component.getStyle().getColor() != null) {
            return component.getStyle().getColor();
        }
        for (Component sibling : component.getSiblings()) {
            net.minecraft.network.chat.TextColor color = findFirstColorInComponent(sibling);
            if (color != null) {
                return color;
            }
        }
        return null;
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // BEDROCK-STYLE EASING FUNCTIONS
    // One master curve drives everything — no independent animations
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Bedrock core easing: easeOutCubic
     * Fast start, gentle slowdown — the foundation of all Bedrock UI.
     */
    private float easeOutCubic(float t) {
        return 1.0f - (float) Math.pow(1.0f - t, 3);
    }
    
    /**
     * Bedrock easing for disappearance: easeInCubic
     * Gentle start, fast end — snappy exit.
     */
    private float easeInCubic(float t) {
        return t * t * t;
    }
    
    /**
     * Bedrock "exhale" for scale: easeOutBack
     * Slight overshoot then settle — organic breathing feel.
     */
    private float easeOutBack(float t) {
        float c1 = 1.70158f;
        float c3 = c1 + 1.0f;
        return 1.0f + c3 * (float) Math.pow(t - 1, 3) + c1 * (float) Math.pow(t - 1, 2);
    }
    
    /**
     * Calculates the total height of the dialog in pixels.
     */
    private float calculateDialogHeight() {
        if (options == null || options.isEmpty()) {
            return 100.0f;
        }
        
        return options.size() * (OPTION_HEIGHT_PIXELS + OPTION_SPACING_PIXELS) + PADDING_PIXELS * 2;
    }
    
    /**
     * Gets the width of an option text in pixels.
     */
    private float getOptionWidth(DialogOption option) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        boolean isBye = "bye".equals(option.id);
        String optionText = isBye ? "◄ " + option.text : (option.index + 1) + ". " + option.text;
        return font.width(optionText);
    }
    
    public boolean isVisible() {
        // Check both internal flag and state machine to determine if dialog should be visible
        DialogStateMachine stateMachine = DialogStateMachine.getInstance();
        return isVisible && stateMachine.shouldShowDialogWindow();
    }
    
    /**
     * Represents a dialog option.
     */
    private static class DialogOption {
        final String id;
        final String text;
        final int index;
        
        DialogOption(String id, String text, int index) {
            this.id = id;
            this.text = text;
            this.index = index;
        }
    }
}

