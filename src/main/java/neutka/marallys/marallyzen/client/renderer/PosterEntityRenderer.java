package neutka.marallys.marallyzen.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import neutka.marallys.marallyzen.entity.PosterEntity;
import neutka.marallys.marallyzen.client.poster.text.PosterStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PosterEntityRenderer extends EntityRenderer<PosterEntity, PosterEntityRenderer.RenderState> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PosterEntityRenderer.class);
    
    // Poster dimensions (55x72 pixels scaled to blocks)
    // Scale to a reasonable size in game (about 0.8 blocks wide, 1.0 blocks tall)
    private static final float POSTER_WIDTH = 0.8f;
    private static final float POSTER_HEIGHT = 1.0f;
    
    // Small poster dimensions (poster1-poster10 on wall)
    // Poster size: 10x15 pixels = 0.625 x 0.9375 blocks
    private static final float SMALL_POSTER_WIDTH = 10.0f / 16.0f; // 0.625 blocks
    private static final float SMALL_POSTER_HEIGHT = 15.0f / 16.0f; // 0.9375 blocks
    
    public PosterEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }
    
    public Identifier getTextureLocation(PosterEntity entity) {
        int posterNumber = entity.getPosterNumber();
        String variant = entity.getOldposterVariant();
        return PosterTextures.getFullTexture(posterNumber, variant);
    }
    
    @Override
    public RenderState createRenderState() {
        return new RenderState();
    }

    @Override
    public void extractRenderState(PosterEntity entity, RenderState renderState, float partialTick) {
        super.extractRenderState(entity, renderState, partialTick);
        renderState.entity = entity;
        renderState.partialTick = partialTick;
    }

    @Override
    public void submit(RenderState renderState, PoseStack poseStack, SubmitNodeCollector renderTasks, CameraRenderState cameraState) {
        PosterEntity entity = renderState.entity;
        if (entity == null) {
            return;
        }
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        renderPoster(entity, renderState.partialTick, poseStack, bufferSource, renderState.lightCoords);
        bufferSource.endBatch();
        super.submit(renderState, poseStack, renderTasks, cameraState);
    }

    private void renderPoster(PosterEntity entity, float partialTick, PoseStack poseStack,
                              MultiBufferSource.BufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        
        // AAA+ smooth animation: compute position on client in real-time
        // IMPORTANT: Visual fly-out completion is decoupled from logical entity state to avoid end-of-animation jumps.
        PosterEntity.State state = entity.getCurrentState();
        Vec3 renderPos = entity.position();

        // Positions used for fly-out animation
        Vec3 startPos = entity.getStartPosition();
        Vec3 targetPos = entity.getTargetPosition();

        // If startPos is null, compute it from originPos as fallback
        // DO NOT recompute targetPos - it must come from server/entity data to preserve correct direction
        if (startPos == null) {
            BlockPos originPos = entity.getOriginPos();
            if (originPos != null) {
                startPos = Vec3.atCenterOf(originPos);
            } else {
                // Last resort: use entity position
                startPos = entity.position();
            }
        }

        // Compute fly-out progress independently of entity state (handles ping/state-sync delay)
        int animationStartTick = entity.getAnimationStartTick();
        float flyOutProgress = 1.0f;
        if (animationStartTick >= 0) {
            int elapsedTicks = entity.tickCount - animationStartTick;
            flyOutProgress = (elapsedTicks + partialTick) / PosterEntity.FLY_OUT_DURATION_TICKS;
        }
        float flyOutProgressClamped = Mth.clamp(flyOutProgress, 0.0f, 1.0f);

        // If targetPos isn't available yet, never animate - keep at startPos until it syncs
        if (targetPos == null) {
            renderPos = (startPos != null) ? startPos : entity.position();
        }
        // Force rendering fly-out while it is not visually complete (even if state already switched to VIEWING)
        else if (startPos != null && flyOutProgress < 1.0f) {
            float eased = easeOutCubic(flyOutProgressClamped);
            renderPos = startPos.lerp(targetPos, eased);

            // Targeted debug: detect when we are overriding logical state for smoothness
            if (state != PosterEntity.State.FLYING_OUT && partialTick == 0.0f && entity.tickCount % 5 == 0) {
                LOGGER.debug("[RENDER] Forcing FLYING_OUT visuals while state={} (flyOutProgress={}, eased={}, tickCount={})",
                    state, String.format("%.5f", flyOutProgressClamped), String.format("%.5f", eased), entity.tickCount);
            }
        }
        else if (state == PosterEntity.State.RETURNING) {
            // RETURNING state: smooth animation from viewing position to origin
            // Uses the same easing as FLYING_OUT (easeOutCubic) for consistency
            Vec3 returnStartPos = entity.getReturnStartPosition();
            BlockPos originPos = entity.getOriginPos();
            Vec3 targetPosReturning = entity.getTargetPosition();
            Vec3 startPosReturning = entity.getStartPosition();

            // returnStartPos should be the position where poster was viewing (targetPosition from VIEWING state)
            // If returnStartPos is null (NBT not synced yet), use targetPosition directly
            // targetPosition is more reliable as it's set during FLYING_OUT and saved to NBT early
            if (returnStartPos == null) {
                if (targetPosReturning != null) {
                    returnStartPos = targetPosReturning;
                } else {
                    // Last resort: use current entity position
                    // This happens if we transitioned to RETURNING before targetPos was set
                    returnStartPos = entity.position();
                }
            }

            // Compute return target position: use originPos if available, otherwise use startPosition (block center)
            // startPosition should always be available as it's set during initialization and saved to NBT
            // On server, startPosition is always the center of originPos block, so we can use it directly
            Vec3 returnTargetPos = null;
            if (originPos != null) {
                returnTargetPos = Vec3.atCenterOf(originPos);
            } else {
                // originPos is null - log warning and use startPos as fallback
                if (entity.tickCount % 20 == 0) {
                    LOGGER.warn("[RENDER] RETURNING: originPos is null! startPos={}, targetPos={}, returnStartPos={}",
                        startPosReturning, targetPosReturning, returnStartPos);
                }
                if (startPosReturning != null) {
                    returnTargetPos = startPosReturning;
                } else if (returnStartPos != null) {
                    // Last resort: estimate from returnStartPos by moving back ~1.5 blocks
                    // This assumes the poster flew out ~1.5 blocks from the wall
                    Vec3 entityPos = entity.position();
                    Vec3 toStart = returnStartPos.subtract(entityPos);
                    if (toStart.lengthSqr() > 0.01) {
                        Vec3 direction = toStart.normalize().scale(-1.5);
                        returnTargetPos = returnStartPos.add(direction);
                    } else {
                        BlockPos estimatedBlockPos = BlockPos.containing(entityPos);
                        returnTargetPos = Vec3.atCenterOf(estimatedBlockPos);
                    }
                } else {
                    Vec3 entityPos = entity.position();
                    BlockPos estimatedBlockPos = BlockPos.containing(entityPos);
                    returnTargetPos = Vec3.atCenterOf(estimatedBlockPos);
                }
            }

            if (returnStartPos != null && returnTargetPos != null) {
                int returnAnimationStartTick = entity.getReturnAnimationStartTick();

                // returnAnimationStartTick should now be synced via EntityDataAccessor and available immediately
                // If still < 0 (shouldn't happen with EntityDataAccessor), estimate it started 1-2 ticks ago
                if (returnAnimationStartTick < 0) {
                    returnAnimationStartTick = entity.tickCount - 2;
                    if (returnAnimationStartTick < 0) {
                        returnAnimationStartTick = 0;
                    }
                    LOGGER.warn("[RENDER] RETURNING: returnAnimationStartTick was < 0, estimated to {}", returnAnimationStartTick);
                }

                int elapsedTicks = entity.tickCount - returnAnimationStartTick;
                float totalTicks = PosterEntity.RETURN_DURATION_TICKS;
                float progress = (elapsedTicks + partialTick) / totalTicks;
                progress = Mth.clamp(progress, 0.0f, 1.0f);

                if (progress <= 0.0f) {
                    renderPos = returnStartPos;
                } else {
                    float eased = easeOutCubic(progress);
                    renderPos = returnStartPos.lerp(returnTargetPos, eased);
                }
            } else {
                renderPos = entity.position();
            }
        } else if (state == PosterEntity.State.VIEWING) {
            // In VIEWING state, use targetPos directly for smooth transition from FLYING_OUT
            // Don't blend with entity.position() to avoid jerking - targetPos should already match
            Vec3 entityPos = entity.position();
            
            if (targetPos != null) {
                // Use targetPos directly - it should be the final position from FLYING_OUT animation
                // This ensures smooth transition without backward movement
                renderPos = targetPos;
                
                // Detailed logging for first 10 ticks in VIEWING state to debug transition
                // Log EVERY frame (including partialTick > 0) to catch any jumps
                if (entity.tickCount < 10 || (entity.tickCount == 10 && partialTick == 0.0f)) {
                    double distance = targetPos.distanceTo(entityPos);
                    LOGGER.info("[RENDER] VIEWING state: renderPos={}, targetPos={}, entityPos={}, distance={}, tickCount={}, partialTick={}", 
                        renderPos, targetPos, entityPos, String.format("%.6f", distance), entity.tickCount, String.format("%.3f", partialTick));
                } else if (partialTick == 0.0f && entity.tickCount % 20 == 0) {
                    double distance = targetPos.distanceTo(entityPos);
                    LOGGER.info("[RENDER] VIEWING state: renderPos={}, targetPos={}, entityPos={}, distance={}, tickCount={}", 
                        renderPos, targetPos, entityPos, String.format("%.4f", distance), entity.tickCount);
                }
            } else {
                // Fallback: use entity position if targetPos is not available
                renderPos = entityPos;
                if (partialTick == 0.0f && entity.tickCount % 20 == 0) {
                    LOGGER.warn("[RENDER] VIEWING state: targetPos is null, using entityPos={}, tickCount={}", 
                        entityPos, entity.tickCount);
                }
            }
        } else if (state == PosterEntity.State.FLYING_OUT) {
            // FLYING_OUT is visually complete (or we can't compute progress); render at final target if available.
            if (targetPos != null) {
                renderPos = targetPos;
            } else if (startPos != null) {
                renderPos = startPos;
            } else {
                renderPos = entity.position();
            }
        }
        
        // Translate to computed render position relative to the *interpolated* entity position.
        // IMPORTANT: the pose stack is already translated by the dispatcher to lerp(xo->x, partialTick).
        // If we subtract entity.getX() (current), then on ticks where the server/client changes entity position
        // (e.g. FLYING_OUT -> VIEWING setsPos to target), vanilla interpolation can make us briefly render "behind"
        // and then snap forward within the same tick. Using the interpolated base removes that end-of-animation jerk.
        double baseX = Mth.lerp(partialTick, entity.xo, entity.getX());
        double baseY = Mth.lerp(partialTick, entity.yo, entity.getY());
        double baseZ = Mth.lerp(partialTick, entity.zo, entity.getZ());
        poseStack.translate(renderPos.x - baseX, renderPos.y - baseY, renderPos.z - baseZ);
        
        // Apply sway rotation in VIEWING state
        if (state == PosterEntity.State.VIEWING) {
            float swayX = entity.getSwayRotationX();
            float swayY = entity.getSwayRotationY();
            
            // Apply rotation around X and Y axes for sway effect
            poseStack.mulPose(Axis.XP.rotationDegrees(swayX));
            poseStack.mulPose(Axis.YP.rotationDegrees(swayY));
        }
        
        // Calculate size interpolation based on animation state
        float currentWidth = POSTER_WIDTH;
        float currentHeight = POSTER_HEIGHT;
        
        if (state == PosterEntity.State.FLYING_OUT && flyOutProgress < 1.0f) {
            // During fly-out: interpolate from small poster size to full size
            float eased = easeOutCubic(flyOutProgressClamped);
            currentWidth = Mth.lerp(eased, SMALL_POSTER_WIDTH, POSTER_WIDTH);
            currentHeight = Mth.lerp(eased, SMALL_POSTER_HEIGHT, POSTER_HEIGHT);
        } else if (state == PosterEntity.State.RETURNING) {
            // During return: interpolate from full size to small poster size
            int returnAnimationStartTick = entity.getReturnAnimationStartTick();
            float returnProgress = 1.0f;
            if (returnAnimationStartTick >= 0) {
                int elapsedTicks = entity.tickCount - returnAnimationStartTick;
                returnProgress = (elapsedTicks + partialTick) / PosterEntity.RETURN_DURATION_TICKS;
            }
            returnProgress = Mth.clamp(returnProgress, 0.0f, 1.0f);
            float eased = easeOutCubic(returnProgress);
            currentWidth = Mth.lerp(eased, POSTER_WIDTH, SMALL_POSTER_WIDTH);
            currentHeight = Mth.lerp(eased, POSTER_HEIGHT, SMALL_POSTER_HEIGHT);
        }
            // In VIEWING state, use full size (POSTER_WIDTH, POSTER_HEIGHT)
            
            // Rotate to face camera (billboard effect)
            // This makes the poster always face the player
            poseStack.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            
            // Get flip rotation for animation
            int posterNumber = entity.getPosterNumber();
            boolean canFlip = (posterNumber >= 1 && posterNumber <= 10) || (posterNumber == 12 || posterNumber == 13);
            float flipRotation = 0.0f;
            if (canFlip) {
                flipRotation = entity.getFlipRotation(partialTick);
            }
            
            // Apply flip rotation BEFORE scaling and rendering
            // This ensures text rotates together with the poster
            if (canFlip) {
                // Always apply rotation (even if 0) to ensure smooth animation
                // Rotate around Y axis to flip the poster (smooth animation)
                poseStack.mulPose(Axis.YP.rotationDegrees(flipRotation));
            }
            
            // Scale and position
            poseStack.scale(currentWidth, currentHeight, 1.0f);
            
            Matrix4f matrix = poseStack.last().pose();

            Identifier texture = getTextureLocation(entity);
            // Force nearest sampling for the entity texture every frame.
            // Rationale: unlike block textures in the atlas (where we can reliably use cutout + mcmeta),
            // entity textures can still end up with filtering that visually "fills" transparent holes
            // when the quad moves/scales. This makes posterfull match poster1..poster10 behavior.
            // NOTE: second param is mipmap filtering; we explicitly DISABLE it for crisp mask edges.
            // Texture filtering is controlled by the render pipeline in this version.
            // Clamp is handled via the .png.mcmeta (clamp=true). AbstractTexture doesn't expose setClamp in this version.
            // IMPORTANT: Use CUTOUT, not TRANSLUCENT.
            // Translucent blending + filtering will "smear/fill" fully-transparent pixels (visible in holes),
            // especially during motion, scaling, and at distance.
            
            // Render the textured poster
            RenderType renderType = RenderTypes.entityCutoutNoCull(texture);
            VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);
            
            // Render a single quad.
            // NOTE: We use entityCutoutNoCull, so the quad is visible from both sides without needing a second
            // coplanar "back face" quad (which causes z-fighting and mirrored artifacts).
            renderQuad(vertexConsumer, matrix, packedLight, false);
            
            // Render text on BOTH sides. Prefer direct text for legacy-accurate layout.
            net.minecraft.resources.Identifier frontTexture = entity.getTextTexture();
            net.minecraft.resources.Identifier backTexture = entity.getTextTextureBack();
            var frontTextData = entity.getFrontTextData();
            var backTextData = entity.getBackTextData();
            PosterStyle posterStyle = PosterStyle.fromPosterNumber(posterNumber);
            float textOffsetX = posterStyle == PosterStyle.PAPER ? -0.095f : 0.0f;
            
            if (posterNumber != 11 && (frontTextData != null || backTextData != null)) {
                if (frontTextData != null) {
                    poseStack.pushPose();
                    poseStack.translate(textOffsetX, 0, -0.01f);
                    renderPosterText(poseStack, bufferSource, frontTextData, true, packedLight);
                    poseStack.popPose();
                }
                if (backTextData != null) {
                    poseStack.pushPose();
                    poseStack.translate(textOffsetX, 0, 0.01f);
                    renderPosterText(poseStack, bufferSource, backTextData, false, packedLight);
                    poseStack.popPose();
                }
            } else if (posterNumber != 11 && (frontTexture != null || backTexture != null)) {
                if (frontTexture != null) {
                    poseStack.pushPose();
                    poseStack.translate(textOffsetX, 0, -0.01f);
                    Matrix4f textMatrixFront = poseStack.last().pose();
                    
                    RenderType textRenderType = RenderTypes.entityCutoutNoCull(frontTexture);
                    VertexConsumer textVertexConsumer = bufferSource.getBuffer(textRenderType);
                    
                    renderTexturedQuad(textVertexConsumer, textMatrixFront, packedLight, true);
                    
                    poseStack.popPose();
                }
                
                if (backTexture != null) {
                    poseStack.pushPose();
                    poseStack.translate(textOffsetX, 0, 0.01f);
                    Matrix4f textMatrixBack = poseStack.last().pose();
                    
                    RenderType backTextRenderType = RenderTypes.entityCutoutNoCull(backTexture);
                    VertexConsumer textVertexConsumerBack = bufferSource.getBuffer(backTextRenderType);
                    
                    renderTexturedQuad(textVertexConsumerBack, textMatrixBack, packedLight, false);
                    
                    poseStack.popPose();
                }
            }
            
            // Render player head(s) for oldposter variants (alive/band/dead)
            if (posterNumber == 11) {
                String variant = entity.getOldposterVariant();
                if (variant != null && (variant.equals("alive") || variant.equals("band") || variant.equals("dead"))) {
                    if (variant.equals("band")) {
                        // For band: render up to 3 heads
                        java.util.List<String> playerNames = entity.getTargetPlayerNames();
                        if (playerNames != null && !playerNames.isEmpty()) {
                            // Centers: (11, 24), (28, 24), (45, 24) in pixels on 55x73 texture
                            float[] headCentersX = {11.0f, 28.0f, 45.0f};
                            float headCenterY = 34.0f;
                            
                            for (int i = 0; i < Math.min(3, playerNames.size()); i++) {
                                String playerName = playerNames.get(i);
                                if (playerName != null && !playerName.isEmpty()) {
                                    // Request head texture (async, may return null if not yet loaded)
                                    net.minecraft.resources.Identifier headTexture = 
                                        neutka.marallys.marallyzen.client.head.HeadCacheManager.getOrRequestHead(playerName);
                                    
                                    if (headTexture != null) {
                                        // Render head on front side
                                        poseStack.pushPose();
                                        poseStack.translate(0, 0, -0.0005f); // Slightly in front of text to avoid z-fighting
                                        
                                        // Apply same scaling as poster (so heads scale with poster during animation)
                                        // Compensate poster's currentWidth/currentHeight scaling to get back to "base" size
                                        // Then heads will naturally scale with the poster since they're rendered after poster scaling
                                        poseStack.scale(1.0f / POSTER_WIDTH, 1.0f / POSTER_HEIGHT, 1.0f);
                                        
                                        Matrix4f headMatrix = poseStack.last().pose();
                                        
                                        RenderType headRenderType = RenderTypes.entityCutoutNoCull(headTexture);
                                        VertexConsumer headVertexConsumer = bufferSource.getBuffer(headRenderType);
                                        
                                        // Render head quad with specific position and size (16x16 for band)
                                        renderHeadQuadBand(headVertexConsumer, headMatrix, packedLight, 
                                            headCentersX[i], headCenterY);
                                        
                                        poseStack.popPose();
                                    } else {
                                        // Log when texture is not yet loaded (only once per frame to avoid spam)
                                        if (i == 0) {
                                            org.slf4j.LoggerFactory.getLogger(PosterEntityRenderer.class)
                                                .debug("PosterEntityRenderer: Head texture not yet loaded for player: {}", playerName);
                                        }
                                    }
                                }
                            }
                        } else {
                            org.slf4j.LoggerFactory.getLogger(PosterEntityRenderer.class)
                                .warn("PosterEntityRenderer: band variant but playerNames is null or empty");
                        }
                    } else {
                        // For alive/dead: render single head
                        String playerName = entity.getTargetPlayerName();
                        if (playerName != null && !playerName.isEmpty()) {
                            // Request head texture (async, may return null if not yet loaded)
                            net.minecraft.resources.Identifier headTexture = 
                                neutka.marallys.marallyzen.client.head.HeadCacheManager.getOrRequestHead(playerName);
                            
                            if (headTexture != null) {
                                // Render head on front side
                                poseStack.pushPose();
                                poseStack.translate(0, 0, -0.0005f); // Slightly in front of text to avoid z-fighting
                                
                                // Apply same scaling as poster (so heads scale with poster during animation)
                                // Compensate poster's currentWidth/currentHeight scaling to get back to "base" size
                                // Then heads will naturally scale with the poster since they're rendered after poster scaling
                                poseStack.scale(1.0f / POSTER_WIDTH, 1.0f / POSTER_HEIGHT, 1.0f);
                                
                                Matrix4f headMatrix = poseStack.last().pose();
                                
                                RenderType headRenderType = RenderTypes.entityCutoutNoCull(headTexture);
                                VertexConsumer headVertexConsumer = bufferSource.getBuffer(headRenderType);
                                
                                // Render head quad with specific UV coordinates
                                renderHeadQuad(headVertexConsumer, headMatrix, packedLight);
                                
                                poseStack.popPose();
                            }
                        }
                    }
                }
            }
        
        poseStack.popPose();
    }

    public static final class RenderState extends EntityRenderState {
        private PosterEntity entity;
        private float partialTick;
    }
    
    /**
     * Renders a player head quad on the poster.
     * Head is 26x26 pixels on a 55x73 texture, positioned at (12, 30) to (37, 55).
     * Adjusted: 2px left, 4px down from original (14, 26) to (39, 51).
     * Head texture is 26x26 pixels, displayed at full size on the head quad.
     */
    private void renderHeadQuad(VertexConsumer buffer, Matrix4f matrix, int packedLight) {
        // Extract packed light
        int lightU = packedLight & 0xFFFF;
        int lightV = (packedLight >> 16) & 0xFFFF;
        int overlayU = OverlayTexture.NO_OVERLAY & 0xFFFF;
        int overlayV = (OverlayTexture.NO_OVERLAY >> 16) & 0xFFFF;
        
        // White color with full alpha
        int r = 255, g = 255, b = 255, a = 255;
        
        // Head dimensions on poster texture: 26x26 pixels
        // Poster texture: 55x73 pixels
        // Head position on texture: left-top (16, 30), right-bottom (41, 55)
        // Adjusted: +2px left (in rendering), +4px down from original (14, 26) to (39, 51)
        // Head center on texture: (28.5, 42.5) in pixels
        // Note: In texture coordinates, Y increases downward. In rendering, negative Y is down.
        // To move head down in rendering, we need to DECREASE Y in texture (make headCenterY more negative)
        
        // Head size in poster coordinates (scaled to poster size)
        // Head is 17.5x17.5 pixels (square) on texture 55x73
        // To keep square shape, we need to use the same scaling factor for both dimensions
        // Calculate size based on poster dimensions, but use same value for both width and height
        // Use width-based calculation and apply to both to ensure square
        float headSizeInPosterUnits = (17.5f / 55.0f) * POSTER_WIDTH * (26.0f / 22.0f);
        float headWidth = headSizeInPosterUnits;
        float headHeight = headSizeInPosterUnits; // Same as width to keep square shape
        
        // Head center position in poster coordinates (relative to poster center at 0,0)
        // Head center in texture: (28.0, 34.0) out of (55, 73)
        // Convert to normalized coordinates: (28.0/55 - 0.5, 34.0/73 - 0.5)
        // In rendering: negative Y = down, positive Y = up
        // Increasing texture Y makes headCenterY more negative = moves head down
        float headCenterX = ((28.0f / 55.0f) - 0.5f) * POSTER_WIDTH;
        // Y coordinate in texture: 34.0
        float headCenterY = ((34.0f / 73.0f) - 0.5f) * POSTER_HEIGHT;
        
        float halfWidth = headWidth * 0.5f;
        float halfHeight = headHeight * 0.5f;
        
        // Normal vector (pointing towards camera)
        float normalX = 0.0f;
        float normalY = 0.0f;
        float normalZ = 1.0f;
        
        // UV coordinates for head texture (full texture, 0.0-1.0)
        // Head texture is 26x26, we use the full texture
        float uMin = 0.0f;
        float uMax = 1.0f;
        float vMin = 0.0f;
        float vMax = 1.0f;
        
        // Render quad: bottom-left, bottom-right, top-right, top-left
        // Bottom-left
        buffer.addVertex(matrix, headCenterX - halfWidth, headCenterY - halfHeight, 0.0f)
            .setColor(r, g, b, a)
            .setUv(uMin, vMax)
            .setUv1(overlayU, overlayV)
            .setUv2(lightU, lightV)
            .setNormal(normalX, normalY, normalZ);
        
        // Bottom-right
        buffer.addVertex(matrix, headCenterX + halfWidth, headCenterY - halfHeight, 0.0f)
            .setColor(r, g, b, a)
            .setUv(uMax, vMax)
            .setUv1(overlayU, overlayV)
            .setUv2(lightU, lightV)
            .setNormal(normalX, normalY, normalZ);
        
        // Top-right
        buffer.addVertex(matrix, headCenterX + halfWidth, headCenterY + halfHeight, 0.0f)
            .setColor(r, g, b, a)
            .setUv(uMax, vMin)
            .setUv1(overlayU, overlayV)
            .setUv2(lightU, lightV)
            .setNormal(normalX, normalY, normalZ);
        
        // Top-left
        buffer.addVertex(matrix, headCenterX - halfWidth, headCenterY + halfHeight, 0.0f)
            .setColor(r, g, b, a)
            .setUv(uMin, vMin)
            .setUv1(overlayU, overlayV)
            .setUv2(lightU, lightV)
            .setNormal(normalX, normalY, normalZ);
    }
    
    /**
     * Renders a player head quad for band variant.
     * Head is 16x16 pixels on a 55x73 texture.
     * @param headCenterX Center X position in pixels (11, 28, or 45)
     * @param headCenterY Center Y position in pixels (41)
     */
    private void renderHeadQuadBand(VertexConsumer buffer, Matrix4f matrix, int packedLight, 
                                    float headCenterX, float headCenterY) {
        // Extract packed light
        int lightU = packedLight & 0xFFFF;
        int lightV = (packedLight >> 16) & 0xFFFF;
        int overlayU = OverlayTexture.NO_OVERLAY & 0xFFFF;
        int overlayV = (OverlayTexture.NO_OVERLAY >> 16) & 0xFFFF;
        
        // White color with full alpha
        int r = 255, g = 255, b = 255, a = 255;
        
        // Head size in poster coordinates (scaled to poster size)
        // Head is 16x16 pixels (square) on texture 55x73, reduced by 20% (0.8)
        float headSizeInPosterUnits = (16.0f / 55.0f) * POSTER_WIDTH * (26.0f / 22.0f) * 0.8f;
        float headWidth = headSizeInPosterUnits;
        float headHeight = headSizeInPosterUnits; // Same as width to keep square shape
        
        // Head center position in poster coordinates (relative to poster center at 0,0)
        // Head center in texture: (headCenterX, headCenterY) out of (55, 73)
        // Convert to normalized coordinates: (headCenterX/55 - 0.5, headCenterY/73 - 0.5)
        float headCenterXNormalized = ((headCenterX / 55.0f) - 0.5f) * POSTER_WIDTH;
        float headCenterYNormalized = ((headCenterY / 73.0f) - 0.5f) * POSTER_HEIGHT;
        
        float halfWidth = headWidth * 0.5f;
        float halfHeight = headHeight * 0.5f;
        
        // Normal vector (pointing towards camera)
        float normalX = 0.0f;
        float normalY = 0.0f;
        float normalZ = 1.0f;
        
        // UV coordinates for head texture (full texture, 0.0-1.0)
        // Head texture is 26x26, we use the full texture
        float uMin = 0.0f;
        float uMax = 1.0f;
        float vMin = 0.0f;
        float vMax = 1.0f;
        
        // Render quad: bottom-left, bottom-right, top-right, top-left
        // Bottom-left
        buffer.addVertex(matrix, headCenterXNormalized - halfWidth, headCenterYNormalized - halfHeight, 0.0f)
            .setColor(r, g, b, a)
            .setUv(uMin, vMax)
            .setUv1(overlayU, overlayV)
            .setUv2(lightU, lightV)
            .setNormal(normalX, normalY, normalZ);
        
        // Bottom-right
        buffer.addVertex(matrix, headCenterXNormalized + halfWidth, headCenterYNormalized - halfHeight, 0.0f)
            .setColor(r, g, b, a)
            .setUv(uMax, vMax)
            .setUv1(overlayU, overlayV)
            .setUv2(lightU, lightV)
            .setNormal(normalX, normalY, normalZ);
        
        // Top-right
        buffer.addVertex(matrix, headCenterXNormalized + halfWidth, headCenterYNormalized + halfHeight, 0.0f)
            .setColor(r, g, b, a)
            .setUv(uMax, vMin)
            .setUv1(overlayU, overlayV)
            .setUv2(lightU, lightV)
            .setNormal(normalX, normalY, normalZ);
        
        // Top-left
        buffer.addVertex(matrix, headCenterXNormalized - halfWidth, headCenterYNormalized + halfHeight, 0.0f)
            .setColor(r, g, b, a)
            .setUv(uMin, vMin)
            .setUv1(overlayU, overlayV)
            .setUv2(lightU, lightV)
            .setNormal(normalX, normalY, normalZ);
    }
    
    /**
     * Renders a textured quad for the text texture.
     * Uses the same dimensions as the poster quad.
     * @param mirrorHorizontal if true, mirrors UV coordinates horizontally (like poster front)
     */
    private void renderTexturedQuad(VertexConsumer buffer, Matrix4f matrix, int packedLight, boolean mirrorHorizontal) {
        // Extract packed light
        int lightU = packedLight & 0xFFFF;
        int lightV = (packedLight >> 16) & 0xFFFF;
        int overlayU = OverlayTexture.NO_OVERLAY & 0xFFFF;
        int overlayV = (OverlayTexture.NO_OVERLAY >> 16) & 0xFFFF;
        
        // White color with full alpha
        int r = 255, g = 255, b = 255, a = 255;
        
        // Half size for centering (same as poster quad)
        float halfWidth = 0.5f;
        float halfHeight = 0.5f;
        
        // Normal (towards camera)
        float normalX = 0.0f;
        float normalY = 0.0f;
        float normalZ = -1.0f;
        
        // UV coordinates - mirror horizontally like poster texture
        float uLeft, uRight;
        if (mirrorHorizontal) {
            // Front side: mirrored (left-right flipped)
            uLeft = 1.0f;
            uRight = 0.0f;
        } else {
            // Back side: normal (not mirrored)
            uLeft = 0.0f;
            uRight = 1.0f;
        }
        float vBottom = 1.0f;
        float vTop = 0.0f;
        
        // Bottom-left
        buffer.addVertex(matrix, -halfWidth, -halfHeight, 0.0f)
            .setColor(r, g, b, a)
            .setUv(uLeft, vBottom)
            .setUv1(overlayU, overlayV)
            .setUv2(lightU, lightV)
            .setNormal(normalX, normalY, normalZ);
        
        // Bottom-right
        buffer.addVertex(matrix, halfWidth, -halfHeight, 0.0f)
            .setColor(r, g, b, a)
            .setUv(uRight, vBottom)
            .setUv1(overlayU, overlayV)
            .setUv2(lightU, lightV)
            .setNormal(normalX, normalY, normalZ);
        
        // Top-right
        buffer.addVertex(matrix, halfWidth, halfHeight, 0.0f)
            .setColor(r, g, b, a)
            .setUv(uRight, vTop)
            .setUv1(overlayU, overlayV)
            .setUv2(lightU, lightV)
            .setNormal(normalX, normalY, normalZ);
        
        // Top-left
        buffer.addVertex(matrix, -halfWidth, halfHeight, 0.0f)
            .setColor(r, g, b, a)
            .setUv(uLeft, vTop)
            .setUv1(overlayU, overlayV)
            .setUv2(lightU, lightV)
            .setNormal(normalX, normalY, normalZ);
    }

    private void renderPosterText(PoseStack poseStack, MultiBufferSource bufferSource,
                                  neutka.marallys.marallyzen.client.poster.text.PosterTextData data,
                                  boolean mirrorHorizontal, int packedLight) {
        if (data == null || data.isEmpty()) {
            return;
        }
        var font = Minecraft.getInstance().font;
        if (font == null) {
            return;
        }

        // Match legacy layout at 2x scale (110x146 px canvas, margins 12/working 86x122).
        final int textureWidth = 110;
        final int textureHeight = 146;
        int margin = 12;
        int workingWidth = 86;
        int workingHeight = 122;

        int titleColor = data.style() == PosterStyle.PAPER ? 0xFF1A1A1A : 0xFF2B1B0E;
        int textColor = titleColor;
        int authorColor = data.style() == PosterStyle.PAPER ? 0xFF555555 : titleColor;
        float titleScale = data.style() == PosterStyle.PAPER ? 1.25f : 1.3f;
        float authorScale = data.style() == PosterStyle.PAPER ? 0.8f : 0.7f;
        boolean isAllCaps = data.style() != PosterStyle.PAPER;
        int textLineHeight = data.style() == PosterStyle.PAPER ? 14 : 13;

        if (data.style() == PosterStyle.PAPER) {
            margin = 14;
            workingWidth = 82;
            workingHeight = 116;
        }

        poseStack.pushPose();
        // Map poster quad to 110x146 px space (2x legacy layout).
        poseStack.translate(-0.5f, 0.5f, 0.0f);
        float scaleX = 1.0f / textureWidth;
        float scaleY = -1.0f / textureHeight;
        poseStack.scale(scaleX, scaleY, 1.0f);
        if (mirrorHorizontal) {
            poseStack.translate(textureWidth, 0.0f, 0.0f);
            poseStack.scale(-1.0f, 1.0f, 1.0f);
        }

        int currentY = margin;
        int centerX = textureWidth / 2;
        int maxY = margin + workingHeight;

        // TITLE
        if (data.title() != null && !data.title().isEmpty() && !data.title().equals("Poster")) {
            String titleText = isAllCaps ? data.title().toUpperCase() : data.title();
            var titleComponent = net.minecraft.network.chat.Component.literal(titleText)
                .withStyle(net.minecraft.network.chat.Style.EMPTY.withBold(true));
            int maxTitleWidth = workingWidth;

            poseStack.pushPose();
            poseStack.scale(titleScale, titleScale, 1.0f);

            int titleWidth = (int)(font.width(titleComponent) * titleScale);
            if (titleWidth > maxTitleWidth) {
                var titleLines = font.split(titleComponent, (int)(maxTitleWidth / titleScale));
                int titleY = currentY;
                for (var line : titleLines) {
                    int lineWidth = (int)(font.width(line) * titleScale);
                    int titleX = centerX - lineWidth / 2;
                    poseStack.pushPose();
                    poseStack.translate(titleX / titleScale, titleY / titleScale, 0);
                    font.drawInBatch(line, 0, 0, titleColor, true, poseStack.last().pose(), bufferSource,
                        net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, packedLight);
                    poseStack.popPose();
                    titleY += (int)(font.lineHeight * titleScale) + 2;
                }
                currentY = titleY;
            } else {
                int titleX = centerX - titleWidth / 2;
                poseStack.pushPose();
                poseStack.translate(titleX / titleScale, currentY / titleScale, 0);
                font.drawInBatch(titleComponent, 0, 0, titleColor, true, poseStack.last().pose(), bufferSource,
                    net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, packedLight);
                poseStack.popPose();
                currentY += (int)(font.lineHeight * titleScale) + 4;
            }
            poseStack.popPose();

            currentY += 2;
        }

        // BODY
        int authorHeight = 0;
        if (data.author() != null && !data.author().isEmpty()) {
            authorHeight = font.lineHeight + margin + 6;
        }
        int maxYForText = Math.min(textureHeight - margin - authorHeight, maxY - authorHeight);

        if (data.pages() != null && !data.pages().isEmpty()) {
            int textY = currentY;
            int wrapWidth = workingWidth;

            for (String page : data.pages()) {
                if (page == null || page.isEmpty()) {
                    textY += textLineHeight;
                    continue;
                }
                String[] paragraphs = page.split("\n\n");
                for (String paragraph : paragraphs) {
                    if (paragraph.isEmpty()) {
                        textY += textLineHeight;
                        continue;
                    }
                    String[] lines = paragraph.split("\n");
                    for (String line : lines) {
                        if (line.isEmpty()) {
                            textY += textLineHeight;
                            continue;
                        }
                        var wrappedLines = font.split(net.minecraft.network.chat.Component.literal(line), wrapWidth);
                        for (var wrappedLine : wrappedLines) {
                            if (textY + textLineHeight > maxYForText) {
                                break;
                            }
                            int lineWidth = font.width(wrappedLine);
                            int lineX = centerX - lineWidth / 2;
                            font.drawInBatch(wrappedLine, lineX, textY, textColor, true, poseStack.last().pose(), bufferSource,
                                net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, packedLight);
                            textY += textLineHeight;
                        }
                        if (textY > maxYForText) {
                            break;
                        }
                    }
                    if (textY + textLineHeight > maxYForText) {
                        break;
                    }
                    if (textY <= maxYForText) {
                        textY += textLineHeight;
                    }
                }
            }
        }

        // AUTHOR
        if (data.author() != null && !data.author().isEmpty()) {
            String authorStr = "— " + data.author();
            var authorComponent = net.minecraft.network.chat.Component.literal(authorStr);
            poseStack.pushPose();
            poseStack.scale(authorScale, authorScale, 1.0f);

            int authorWidth = (int)(font.width(authorComponent) * authorScale);
            int authorY = textureHeight - margin - 6 - (int)(font.lineHeight * authorScale);
            int authorX = (data.style() == PosterStyle.PAPER)
                ? margin + workingWidth - authorWidth
                : centerX - authorWidth / 2;

            poseStack.pushPose();
            poseStack.translate(authorX / authorScale, authorY / authorScale, 0);
            font.drawInBatch(authorComponent, 0, 0, authorColor, true, poseStack.last().pose(), bufferSource,
                net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, packedLight);
            poseStack.popPose();
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    /**
     * Ultra-smooth easing function - ease-out-cubic
     * Starts fast and smoothly decelerates for natural, cinematic motion
     */
    private float easeOutCubic(float t) {
        float f = t - 1.0f;
        return 1.0f + f * f * f;
    }
    
    /**
     * Ease-in-cubic for return animation
     * Starts slow and accelerates smoothly for natural return motion
     */
    private float easeInCubic(float t) {
        return t * t * t;
    }
    
    /**
     * Renders a single quad (one side of the poster)
     */
    private void renderQuad(VertexConsumer buffer, Matrix4f matrix, int packedLight, boolean backFace) {
        // Extract packed light (use actual lighting from level)
        int lightU = packedLight & 0xFFFF;
        int lightV = (packedLight >> 16) & 0xFFFF;
        int overlayU = OverlayTexture.NO_OVERLAY & 0xFFFF;
        int overlayV = (OverlayTexture.NO_OVERLAY >> 16) & 0xFFFF;
        
        // White color with full alpha
        int r = 255, g = 255, b = 255, a = 255;
        
        // Half size for centering
        float halfWidth = 0.5f;
        float halfHeight = 0.5f;
        
        // Normal (towards camera for front, away for back)
        float normalX = 0.0f;
        float normalY = 0.0f;
        float normalZ = backFace ? 1.0f : -1.0f;
        
        // Mirror horizontally (left-right), keep vertical orientation upright
        float uLeft = 1.0f;
        float uRight = 0.0f;
        float vBottom = 1.0f;
        float vTop = 0.0f;

        if (backFace) {
            // Back face - flip UV coordinates
            // Bottom-left
            buffer.addVertex(matrix, -halfWidth, -halfHeight, 0.0f)
                .setColor(r, g, b, a)
                .setUv(uLeft, vBottom)
                .setUv1(overlayU, overlayV)
                .setUv2(lightU, lightV)
                .setNormal(normalX, normalY, normalZ);
            
            // Bottom-right
            buffer.addVertex(matrix, halfWidth, -halfHeight, 0.0f)
                .setColor(r, g, b, a)
                .setUv(uRight, vBottom)
                .setUv1(overlayU, overlayV)
                .setUv2(lightU, lightV)
                .setNormal(normalX, normalY, normalZ);
            
            // Top-right
            buffer.addVertex(matrix, halfWidth, halfHeight, 0.0f)
                .setColor(r, g, b, a)
                .setUv(uRight, vTop)
                .setUv1(overlayU, overlayV)
                .setUv2(lightU, lightV)
                .setNormal(normalX, normalY, normalZ);
            
            // Top-left
            buffer.addVertex(matrix, -halfWidth, halfHeight, 0.0f)
                .setColor(r, g, b, a)
                .setUv(uLeft, vTop)
                .setUv1(overlayU, overlayV)
                .setUv2(lightU, lightV)
                .setNormal(normalX, normalY, normalZ);
        } else {
            // Front face
            // Bottom-left
            buffer.addVertex(matrix, -halfWidth, -halfHeight, 0.0f)
                .setColor(r, g, b, a)
                .setUv(uLeft, vBottom)
                .setUv1(overlayU, overlayV)
                .setUv2(lightU, lightV)
                .setNormal(normalX, normalY, normalZ);
            
            // Bottom-right
            buffer.addVertex(matrix, halfWidth, -halfHeight, 0.0f)
                .setColor(r, g, b, a)
                .setUv(uRight, vBottom)
                .setUv1(overlayU, overlayV)
                .setUv2(lightU, lightV)
                .setNormal(normalX, normalY, normalZ);
            
            // Top-right
            buffer.addVertex(matrix, halfWidth, halfHeight, 0.0f)
                .setColor(r, g, b, a)
                .setUv(uRight, vTop)
                .setUv1(overlayU, overlayV)
                .setUv2(lightU, lightV)
                .setNormal(normalX, normalY, normalZ);
            
            // Top-left
            buffer.addVertex(matrix, -halfWidth, halfHeight, 0.0f)
                .setColor(r, g, b, a)
                .setUv(uLeft, vTop)
                .setUv1(overlayU, overlayV)
                .setUv2(lightU, lightV)
                .setNormal(normalX, normalY, normalZ);
        }
    }
}







