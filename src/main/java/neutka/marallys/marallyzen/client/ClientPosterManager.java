package neutka.marallys.marallyzen.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.audio.MarallyzenSounds;
import neutka.marallys.marallyzen.entity.PosterEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.lang.reflect.Method;

/**
 * Manages client-side PosterEntity instances.
 * These entities are only visible to the local player and are not synchronized with the server.
 */
public class ClientPosterManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientPosterManager.class);
    private static final Map<BlockPos, PosterEntity> clientPosters = new HashMap<>();
    private static final Set<BlockPos> hiddenBlocks = new HashSet<>();
    // Store original block states to restore them later
    private static final Map<BlockPos, BlockState> originalBlockStates = new HashMap<>();
    private static int nextClientEntityId = -1; // Negative IDs for client-only entities
    private static Method cachedBlockChanged;
    private static Method cachedSetBlockDirty;
    
    // Separate cache for poster text data (independent of PosterEntity and BlockEntity)
    private static class PosterTextData {
        String text;
        String title;
        String author;
        String backText;
        
        PosterTextData(String text, String title, String author, String backText) {
            this.text = text != null ? text : "";
            this.title = title != null ? title : "";
            this.author = author != null ? author : "";
            this.backText = backText != null ? backText : "";
        }
    }
    private static final Map<BlockPos, PosterTextData> posterTextCache = new HashMap<>();
    // Cache for oldposter variant (ID 11)
    private static final Map<BlockPos, String> oldposterVariantCache = new HashMap<>();
    // Cache for target player name (ID 11) - single name for alive/dead
    private static final Map<BlockPos, String> targetPlayerNameCache = new HashMap<>();
    // Cache for target player names (ID 11) - list for band variant
    private static final Map<BlockPos, java.util.List<String>> targetPlayerNamesCache = new HashMap<>();
    
    /**
     * Creates a client-side PosterEntity at the specified block position.
     * This entity will only be visible to the local player.
     */
    public static void createClientPoster(BlockPos pos, int posterNumber, BlockState originalBlockState) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.level instanceof ClientLevel clientLevel)) {
            LOGGER.warn("Cannot create client poster: not in a client level");
            return;
        }
        
        // Save text from existing poster to cache before removing it (if any)
        PosterEntity existingPoster = clientPosters.get(pos);
        if (existingPoster != null) {
            String existingText = existingPoster.getPosterText();
            if (!existingText.isEmpty()) {
                posterTextCache.put(pos, new PosterTextData(
                    existingText,
                    existingPoster.getPosterTitle(),
                    existingPoster.getPosterAuthor(),
                    ""
                ));
                LOGGER.info("ClientPosterManager.createClientPoster: Saved text to cache from existing poster, len={}", existingText.length());
            }
        }
        
        // Remove existing client poster at this position if any
        removeClientPoster(pos);
        hiddenBlocks.add(pos);
        forceRebuild(clientLevel, pos, originalBlockState);
        
        // Create new PosterEntity
        PosterEntity posterEntity = new PosterEntity(
            (net.minecraft.world.entity.EntityType<PosterEntity>) Marallyzen.POSTER_ENTITY.get(),
            clientLevel
        );
        
        // Set negative ID to mark it as client-only (won't sync with server)
        posterEntity.setId(nextClientEntityId--);
        
        // Set position
        Vec3 entityPos = Vec3.atCenterOf(pos);
        posterEntity.setPos(entityPos.x, entityPos.y, entityPos.z);
        
        // Initialize entity with block data
        posterEntity.initializeFromBlock(pos, posterNumber, originalBlockState);

        playLocalSound(clientLevel, pos, MarallyzenSounds.POSTER_WELCOME.get());
        
        LOGGER.info("========== ClientPosterManager: START Loading text for poster at {} ==========", pos);
        
        // Load text data before hiding the block
        String textToUse = "";
        String titleToUse = "";
        String authorToUse = "";
        String backTextToUse = "";

        // Try to get BE (even if block is hidden)
        net.minecraft.world.level.block.entity.BlockEntity be = null;
        BlockState currentBlockState = clientLevel.getBlockState(pos);
        
        LOGGER.info("ClientPosterManager: currentBlockState={}, originalBlockState={}", currentBlockState.getBlock(), originalBlockState != null ? originalBlockState.getBlock() : "null");
        
        if (currentBlockState.getBlock() != Blocks.AIR) {
            be = clientLevel.getBlockEntity(pos);
            LOGGER.info("ClientPosterManager: Block is not AIR, got BE: {}", be != null ? be.getClass().getSimpleName() : "null");
            // If BE doesn't exist, try to get it from the level's BE cache first
            // Only create new BE if it really doesn't exist
            if (be == null && currentBlockState.getBlock() instanceof neutka.marallys.marallyzen.blocks.PosterBlock) {
                // Try to get from level's block entity map directly
                be = clientLevel.getBlockEntity(pos);
                if (be == null) {
                    LOGGER.info("ClientPosterManager: Creating new BlockEntity for block");
                    be = ((neutka.marallys.marallyzen.blocks.PosterBlock) currentBlockState.getBlock()).newBlockEntity(pos, currentBlockState);
                    if (be != null) {
                        clientLevel.setBlockEntity(be);
                        // Load variant and player name from cache if this is oldposter
                        if (posterNumber == 11 && be instanceof neutka.marallys.marallyzen.blocks.PosterBlockEntity posterBe) {
                            String cachedVariant = oldposterVariantCache.get(pos);
                            if (cachedVariant != null && !cachedVariant.isEmpty()) {
                                posterBe.setOldposterVariant(cachedVariant);
                                posterBe.setChanged();
                                LOGGER.warn("ClientPosterManager: Loaded variant '{}' from cache to new BlockEntity", cachedVariant);
                                
                                // Load player name(s) from cache based on variant
                                if (cachedVariant.equals("band")) {
                                    // For band: load list of names
                                    java.util.List<String> cachedPlayerNames = targetPlayerNamesCache.get(pos);
                                    if (cachedPlayerNames != null && !cachedPlayerNames.isEmpty()) {
                                        posterBe.setTargetPlayerNames(cachedPlayerNames);
                                        posterBe.setChanged();
                                        LOGGER.warn("ClientPosterManager: Loaded player names {} from cache to new BlockEntity", cachedPlayerNames);
                                    }
                                } else {
                                    // For alive/dead: load single name
                                    String cachedPlayerName = targetPlayerNameCache.get(pos);
                                    if (cachedPlayerName != null && !cachedPlayerName.isEmpty()) {
                                        posterBe.setTargetPlayerName(cachedPlayerName);
                                        posterBe.setChanged();
                                        LOGGER.warn("ClientPosterManager: Loaded player name '{}' from cache to new BlockEntity", cachedPlayerName);
                                    }
                                }
                            }
                        }
                        LOGGER.info("ClientPosterManager: Created and set BlockEntity");
                    }
                }
            }
        } else if (originalBlockState != null && originalBlockState.getBlock() != Blocks.AIR) {
            LOGGER.info("ClientPosterManager: Block is AIR, temporarily restoring to get BE");
            // Temporarily restore block to get BE
            // Use flag 2 (BLOCK_UPDATE) to preserve BlockEntity data
            clientLevel.setBlock(pos, originalBlockState, 2);
            be = clientLevel.getBlockEntity(pos);
            LOGGER.info("ClientPosterManager: After restore, got BE: {}", be != null ? be.getClass().getSimpleName() : "null");
            // If BE doesn't exist, create it
            if (be == null && originalBlockState.getBlock() instanceof neutka.marallys.marallyzen.blocks.PosterBlock) {
                LOGGER.info("ClientPosterManager: Creating new BlockEntity for restored block");
                be = ((neutka.marallys.marallyzen.blocks.PosterBlock) originalBlockState.getBlock()).newBlockEntity(pos, originalBlockState);
                if (be != null) {
                    clientLevel.setBlockEntity(be);
                    // Load variant from cache if this is oldposter
                    if (posterNumber == 11 && be instanceof neutka.marallys.marallyzen.blocks.PosterBlockEntity posterBe) {
                        String cachedVariant = oldposterVariantCache.get(pos);
                        if (cachedVariant != null && !cachedVariant.isEmpty()) {
                            posterBe.setOldposterVariant(cachedVariant);
                            posterBe.setChanged();
                            LOGGER.warn("ClientPosterManager: Loaded variant '{}' from cache to restored BlockEntity", cachedVariant);
                        }
                    }
                    LOGGER.info("ClientPosterManager: Created and set BlockEntity for restored block");
                }
            }
            // Hide block again, but preserve BlockEntity
            clientLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        }

        // Try BE first
        if (be instanceof neutka.marallys.marallyzen.blocks.PosterBlockEntity posterBe) {
            String beText = posterBe.getPosterText();
            String beBackText = posterBe.getPosterBackText();
            LOGGER.info("ClientPosterManager: Got PosterBlockEntity, text length={}, title='{}', author='{}'", beText.length(), posterBe.getPosterTitle(), posterBe.getPosterAuthor());
            if (!beText.isEmpty()) {
                textToUse = beText;
                titleToUse = posterBe.getPosterTitle();
                authorToUse = "";
                backTextToUse = beBackText;
                // Update cache with BE data
                posterTextCache.put(pos, new PosterTextData(textToUse, titleToUse, authorToUse, backTextToUse));
                LOGGER.info("ClientPosterManager: Loaded text from BE, len={}", beText.length());
            } else {
                LOGGER.info("ClientPosterManager: BE text is empty");
            }
            
            // Load oldposter variant if this is oldposter (ID 11)
            if (posterNumber == 11) {
                // Check cache first for persistence, then BE, then default
                // Cache is cleared when block is removed, so if cache exists, it's valid
                String variant = oldposterVariantCache.get(pos);
                if (variant == null || variant.isEmpty()) {
                    // Cache miss - try BE
                    variant = posterBe.getOldposterVariant();
                    if (variant == null || variant.isEmpty()) {
                        variant = "default";
                    } else {
                        // Save BE variant to cache for persistence
                        oldposterVariantCache.put(pos, variant);
                    }
                    LOGGER.warn("ClientPosterManager: Cache miss, loaded variant from BE: {}", variant);
                } else {
                    // Cache has variant - use it and update BE to match (for server sync)
                    posterBe.setOldposterVariant(variant);
                    posterBe.setChanged();
                    LOGGER.warn("ClientPosterManager: Loaded variant from cache: {}, updated BE", variant);
                }
                posterEntity.setOldposterVariant(variant);
                
                // Load target player name(s) from cache first, then BE
                if (variant.equals("band")) {
                    // For band: load list of names from cache first
                    java.util.List<String> playerNames = targetPlayerNamesCache.get(pos);
                    if (playerNames == null || playerNames.isEmpty()) {
                        // Try BE if cache doesn't have names
                        playerNames = posterBe.getTargetPlayerNames();
                        if (playerNames != null && !playerNames.isEmpty()) {
                            // Save BE names to cache for persistence
                            targetPlayerNamesCache.put(pos, new java.util.ArrayList<>(playerNames));
                            LOGGER.warn("ClientPosterManager: Loaded player names from BE: {}", playerNames);
                        }
                    } else {
                        // Cache has names - use them and update BE to match
                        posterBe.setTargetPlayerNames(playerNames);
                        posterBe.setChanged();
                        LOGGER.warn("ClientPosterManager: Loaded player names from cache: {}, updated BE", playerNames);
                    }
                    if (playerNames != null && !playerNames.isEmpty()) {
                        posterEntity.setTargetPlayerNames(playerNames);
                        LOGGER.warn("ClientPosterManager: Set target player names: {} (at {})", playerNames, pos);
                    }
                } else {
                    // For alive/dead: load single name from cache first
                    String playerName = targetPlayerNameCache.get(pos);
                    if (playerName == null || playerName.isEmpty()) {
                        // Try BE if cache doesn't have name
                        playerName = posterBe.getTargetPlayerName();
                        if (playerName != null && !playerName.isEmpty()) {
                            // Save BE name to cache for persistence
                            targetPlayerNameCache.put(pos, playerName);
                            LOGGER.warn("ClientPosterManager: Loaded player name from BE: {}", playerName);
                        }
                    } else {
                        // Cache has name - use it and update BE to match
                        posterBe.setTargetPlayerName(playerName);
                        posterBe.setChanged();
                        LOGGER.warn("ClientPosterManager: Loaded player name from cache: {}, updated BE", playerName);
                    }
                    if (playerName != null && !playerName.isEmpty()) {
                        posterEntity.setTargetPlayerName(playerName);
                        LOGGER.warn("ClientPosterManager: Set target player name: {} (at {})", playerName, pos);
                    }
                }
                
                LOGGER.warn("ClientPosterManager: Final oldposter variant: {} (at {})", variant, pos);
            }
        } else {
            LOGGER.info("ClientPosterManager: BE is not PosterBlockEntity or is null");
            // For oldposter, try cache if BE is null
            if (posterNumber == 11) {
                String variant = oldposterVariantCache.getOrDefault(pos, "default");
                posterEntity.setOldposterVariant(variant);
                
                // Load target player name(s) from cache
                if (variant.equals("band")) {
                    // For band: load list of names
                    java.util.List<String> playerNames = targetPlayerNamesCache.get(pos);
                    if (playerNames != null && !playerNames.isEmpty()) {
                        posterEntity.setTargetPlayerNames(playerNames);
                        LOGGER.warn("ClientPosterManager: BE is null for oldposter, using cached player names: {}", playerNames);
                    }
                } else {
                    // For alive/dead: load single name
                    String playerName = targetPlayerNameCache.get(pos);
                    if (playerName != null && !playerName.isEmpty()) {
                        posterEntity.setTargetPlayerName(playerName);
                        LOGGER.warn("ClientPosterManager: BE is null for oldposter, using cached player name: {}", playerName);
                    }
                }
                
                LOGGER.warn("ClientPosterManager: BE is null for oldposter, using cached variant: {}", variant);
            }
        }

        // If BE didn't provide text, try cache
        if (textToUse.isEmpty()) {
            PosterTextData cached = posterTextCache.get(pos);
            if (cached != null && !cached.text.isEmpty()) {
                textToUse = cached.text;
                titleToUse = cached.title;
                authorToUse = "";
                backTextToUse = cached.backText;
                LOGGER.info("ClientPosterManager: Loaded text from cache, len={}", textToUse.length());
            } else {
                LOGGER.info("ClientPosterManager: Cache is empty or null");
                
                // TEMPORARY: Add test text for debugging (remove this later)
                // Only for non-OLD posters
                if (posterNumber != 11) {
                    LOGGER.warn("ClientPosterManager: Using TEST TEXT for debugging");
                    textToUse = "Это тестовый текст для проверки системы рендеринга.\n\nЕсли вы видите этот текст, значит система работает!";
                    titleToUse = "Тестовый заголовок";
                    authorToUse = "Тестовый автор";
                    
                    // Save to cache for persistence
                    posterTextCache.put(pos, new PosterTextData(textToUse, titleToUse, authorToUse, ""));
                    
                    // Also save to BlockEntity if it exists
                    if (be instanceof neutka.marallys.marallyzen.blocks.PosterBlockEntity posterBe) {
                        posterBe.setPosterText(textToUse);
                        posterBe.setPosterTitle(titleToUse);
                        posterBe.setPosterAuthor(authorToUse);
                        LOGGER.info("ClientPosterManager: Saved test text to BlockEntity");
                    }
                }
            }
        }

        LOGGER.info("========== ClientPosterManager: textToUse.isEmpty()={}, text.length={} ==========", textToUse.isEmpty(), textToUse.length());

        // Set text to entity using new texture system
            if (!textToUse.isEmpty()) {
                neutka.marallys.marallyzen.client.poster.text.PosterStyle style = 
                    neutka.marallys.marallyzen.client.poster.text.PosterStyle.fromPosterNumber(posterNumber);

            java.util.List<String> pages = java.util.Arrays.asList(textToUse.split("\n\n"));

            neutka.marallys.marallyzen.client.poster.text.PosterTextData textData = 
                new neutka.marallys.marallyzen.client.poster.text.PosterTextData(
                    titleToUse != null ? titleToUse : "",
                    pages,
                    "",
                    style
                );

            LOGGER.info("ClientPosterManager: Creating PosterTextData - title='{}', pages={}, author='{}', style={}", 
                titleToUse, pages.size(), authorToUse, style);
            
            // Set text for both front and back sides
            neutka.marallys.marallyzen.client.poster.text.PosterTextData backData = null;
            if (backTextToUse != null && !backTextToUse.isEmpty()) {
                java.util.List<String> backPages = java.util.Arrays.asList(backTextToUse.split("\n\n"));
                backData = new neutka.marallys.marallyzen.client.poster.text.PosterTextData(
                    "",
                    backPages,
                    "",
                    style
                );
            }
            posterEntity.setText(textData, backData);
            LOGGER.info("ClientPosterManager: Called posterEntity.setText(), frontTexture={}, backTexture={}", 
                posterEntity.getTextTexture(), posterEntity.getTextTextureBack());
        } else {
            LOGGER.warn("========== ClientPosterManager: TEXT IS EMPTY - NOT CALLING setText() ==========");
        }
        
        // Mark as client-only
        posterEntity.setClientOnly(true);
        
        
        // Add to client level (addEntity takes only Entity parameter)
        clientLevel.addEntity(posterEntity);
        
        // Store original block state and hide block on client (visual only, doesn't affect server)
        originalBlockStates.put(pos, originalBlockState);
        
        // Store in map first
        clientPosters.put(pos, posterEntity);
        
        // Set block to air on client to hide it visually
        // IMPORTANT: Use flag 2 (BLOCK_UPDATE) instead of 18 to preserve BlockEntity
        // Flag 18 includes NOTIFY_NEIGHBORS which may cause BlockEntity to be removed
        BlockState currentState = clientLevel.getBlockState(pos);
        if (currentState.getBlock() != Blocks.AIR) {
            // Save BlockEntity data before hiding block
            if (be instanceof neutka.marallys.marallyzen.blocks.PosterBlockEntity posterBe && posterNumber == 11) {
                String variant = posterBe.getOldposterVariant();
                if (variant != null && !variant.isEmpty()) {
                    oldposterVariantCache.put(pos, variant);
                    LOGGER.warn("ClientPosterManager: Saved variant '{}' to cache before hiding block", variant);
                    
                    // Save player name(s) to cache based on variant
                    if (variant.equals("band")) {
                        // For band: save list of names
                        java.util.List<String> playerNames = posterBe.getTargetPlayerNames();
                        if (playerNames != null && !playerNames.isEmpty()) {
                            targetPlayerNamesCache.put(pos, new java.util.ArrayList<>(playerNames));
                            LOGGER.warn("ClientPosterManager: Saved player names {} to cache before hiding block", playerNames);
                        }
                    } else {
                        // For alive/dead: save single name
                        String playerName = posterBe.getTargetPlayerName();
                        if (playerName != null && !playerName.isEmpty()) {
                            targetPlayerNameCache.put(pos, playerName);
                            LOGGER.warn("ClientPosterManager: Saved player name '{}' to cache before hiding block", playerName);
                        }
                    }
                }
            }
            clientLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), 2); // Use flag 2 to preserve BlockEntity
            LOGGER.info("Hid block at {} by setting to AIR (was: {})", pos, currentState.getBlock());
        }
        
        LOGGER.debug("Created client-side PosterEntity at {} with ID {}, will hide block visually", pos, posterEntity.getId());
    }

    /**
     * Removes a client-side PosterEntity at the specified block position.
     */
    public static void removeClientPoster(BlockPos pos) {
        PosterEntity existing = clientPosters.remove(pos);
        BlockState originalState = originalBlockStates.remove(pos);
        hiddenBlocks.remove(pos);
        
        if (existing != null) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level instanceof ClientLevel clientLevel) {
                playLocalSound(clientLevel, pos, MarallyzenSounds.POSTER_BYE.get());
                existing.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
                
                // Restore block on client (visual only, doesn't affect server)
                if (originalState != null) {
                    clientLevel.setBlock(pos, originalState, 3);
                    forceRebuild(clientLevel, pos, originalState);
                    LOGGER.debug("Removed client-side PosterEntity at {} and restored block", pos);
                } else {
                    LOGGER.debug("Removed client-side PosterEntity at {}", pos);
                }
            }
        }
    }

    private static void playLocalSound(ClientLevel level, BlockPos pos, net.minecraft.sounds.SoundEvent sound) {
        level.playLocalSound(
            pos.getX() + 0.5,
            pos.getY() + 0.5,
            pos.getZ() + 0.5,
            sound,
            SoundSource.BLOCKS,
            1.0f,
            1.0f,
            false
        );
    }
    
    /**
     * Checks if there is a client-side PosterEntity at the specified block position.
     */
    public static boolean hasClientPoster(BlockPos pos) {
        return clientPosters.containsKey(pos);
    }

    public static boolean isPosterHidden(BlockPos pos) {
        return hiddenBlocks.contains(pos);
    }
    
    /**
     * Gets the client-side PosterEntity at the specified block position.
     */
    public static PosterEntity getClientPoster(BlockPos pos) {
        return clientPosters.get(pos);
    }
    
    /**
     * Ticks client poster manager - ensures blocks stay hidden if server tries to sync them back.
     * Should be called every client tick.
     */
    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.level instanceof ClientLevel clientLevel)) {
            return;
        }

        // Cleanup stale client posters to avoid leaving blocks hidden.
        java.util.Iterator<Map.Entry<BlockPos, PosterEntity>> iterator = clientPosters.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, PosterEntity> entry = iterator.next();
            PosterEntity entity = entry.getValue();
            if (entity == null || entity.isRemoved() || !entity.isAlive()) {
                BlockPos pos = entry.getKey();
                BlockState originalState = originalBlockStates.remove(pos);
                hiddenBlocks.remove(pos);
                if (originalState != null) {
                    clientLevel.setBlock(pos, originalState, 3);
                    forceRebuild(clientLevel, pos, originalState);
                    LOGGER.debug("ClientPosterManager.tick: Restored block for stale poster at {}", pos);
                }
                iterator.remove();
            }
        }
        
        // Check all active client posters and ensure their blocks are hidden
        for (Map.Entry<BlockPos, PosterEntity> entry : clientPosters.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState currentState = clientLevel.getBlockState(pos);
            
            // Update entity text from BE or cache to keep it in sync
            net.minecraft.world.level.block.entity.BlockEntity be = clientLevel.getBlockEntity(pos);
            PosterEntity entity = entry.getValue();
            if (entity != null) {
                String entityText = entity.getPosterText();
                
                // Try BE first
                if (be instanceof neutka.marallys.marallyzen.blocks.PosterBlockEntity posterBe) {
                    String beText = posterBe.getPosterText();
                    if (!beText.isEmpty()) {
                        // Update entity and cache from BE
                        if (!beText.equals(entityText)) {
                            // Create PosterTextData and update texture
                            int posterNumber = entity.getPosterNumber();
                            neutka.marallys.marallyzen.client.poster.text.PosterStyle style = 
                                neutka.marallys.marallyzen.client.poster.text.PosterStyle.fromPosterNumber(posterNumber);
                            java.util.List<String> pages = java.util.Arrays.asList(beText.split("\n\n"));
                            neutka.marallys.marallyzen.client.poster.text.PosterTextData textData = 
                                new neutka.marallys.marallyzen.client.poster.text.PosterTextData(
                                    posterBe.getPosterTitle(),
                                    pages,
                                    "",
                                    style
                                );
                            neutka.marallys.marallyzen.client.poster.text.PosterTextData backData = null;
                            String beBackText = posterBe.getPosterBackText();
                            if (beBackText != null && !beBackText.isEmpty()) {
                                java.util.List<String> backPages = java.util.Arrays.asList(beBackText.split("\n\n"));
                                backData = new neutka.marallys.marallyzen.client.poster.text.PosterTextData(
                                    "",
                                    backPages,
                                    "",
                                    style
                                );
                            }
                            entity.setText(textData, backData);
                            
                            // Also update legacy fields and cache
                            entity.setPosterText(beText);
                            entity.setPosterTitle(posterBe.getPosterTitle());
                            entity.setPosterAuthor("");
                            posterTextCache.put(pos, new PosterTextData(
                                beText,
                                posterBe.getPosterTitle(),
                                "",
                                posterBe.getPosterBackText()
                            ));
                        }
                    } else if (entityText.isEmpty()) {
                        // BE is empty and entity is empty - try cache
                        PosterTextData cached = posterTextCache.get(pos);
                        if (cached != null && !cached.text.isEmpty()) {
                            // Create PosterTextData from cache
                            int posterNumber = entity.getPosterNumber();
                            neutka.marallys.marallyzen.client.poster.text.PosterStyle style = 
                                neutka.marallys.marallyzen.client.poster.text.PosterStyle.fromPosterNumber(posterNumber);
                            java.util.List<String> pages = java.util.Arrays.asList(cached.text.split("\n\n"));
                            neutka.marallys.marallyzen.client.poster.text.PosterTextData textData = 
                                new neutka.marallys.marallyzen.client.poster.text.PosterTextData(
                                    cached.title,
                                    pages,
                                    "",
                                    style
                                );
                            neutka.marallys.marallyzen.client.poster.text.PosterTextData backData = null;
                            if (cached.backText != null && !cached.backText.isEmpty()) {
                                java.util.List<String> backPages = java.util.Arrays.asList(cached.backText.split("\n\n"));
                                backData = new neutka.marallys.marallyzen.client.poster.text.PosterTextData(
                                    "",
                                    backPages,
                                    "",
                                    style
                                );
                            }
                            entity.setText(textData, backData);
                            
                            // Also update legacy fields
                            entity.setPosterText(cached.text);
                            entity.setPosterTitle(cached.title);
                            entity.setPosterAuthor("");
                        }
                    }
                } else {
                    // BE is null - try cache if entity is empty
                    if (entityText.isEmpty()) {
                        PosterTextData cached = posterTextCache.get(pos);
                        if (cached != null && !cached.text.isEmpty()) {
                            // Create PosterTextData from cache
                            int posterNumber = entity.getPosterNumber();
                            neutka.marallys.marallyzen.client.poster.text.PosterStyle style = 
                                neutka.marallys.marallyzen.client.poster.text.PosterStyle.fromPosterNumber(posterNumber);
                            java.util.List<String> pages = java.util.Arrays.asList(cached.text.split("\n\n"));
                            neutka.marallys.marallyzen.client.poster.text.PosterTextData textData = 
                                new neutka.marallys.marallyzen.client.poster.text.PosterTextData(
                                    cached.title,
                                    pages,
                                    "",
                                    style
                                );
                            neutka.marallys.marallyzen.client.poster.text.PosterTextData backData = null;
                            if (cached.backText != null && !cached.backText.isEmpty()) {
                                java.util.List<String> backPages = java.util.Arrays.asList(cached.backText.split("\n\n"));
                                backData = new neutka.marallys.marallyzen.client.poster.text.PosterTextData(
                                    "",
                                    backPages,
                                    "",
                                    style
                                );
                            }
                            entity.setText(textData, backData);
                            
                            // Also update legacy fields
                            entity.setPosterText(cached.text);
                            entity.setPosterTitle(cached.title);
                            entity.setPosterAuthor("");
                        }
                    }
                }
            }
            
            // If block is not air (server synced it back), hide it again
            if (currentState.getBlock() != Blocks.AIR) {
                clientLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);
                LOGGER.debug("Re-hid block at {} (server synced it back to {})", pos, currentState.getBlock());
            }
        }
    }
    
    /**
     * Saves text data to cache for a poster at the specified position.
     * This ensures text persists even when PosterEntity is removed.
     */
    public static void saveTextToCache(BlockPos pos, String text, String title, String author) {
        if (text != null && !text.isEmpty()) {
            posterTextCache.put(pos, new PosterTextData(text, title, author, ""));
            LOGGER.info("ClientPosterManager.saveTextToCache: Saved text to cache at {}, len={}", pos, text.length());
        }
    }
    
    /**
     * Saves oldposter variant to cache for a poster at the specified position.
     * This ensures variant persists even when PosterEntity is removed.
     */
    public static void saveOldposterVariantToCache(BlockPos pos, String variant) {
        if (variant != null && !variant.isEmpty()) {
            oldposterVariantCache.put(pos, variant);
            LOGGER.warn("ClientPosterManager.saveOldposterVariantToCache: Saved variant '{}' to cache at {}", variant, pos);
        }
    }
    
    /**
     * Saves target player name to cache for a poster at the specified position.
     * This ensures player name persists even when PosterEntity is removed.
     */
    public static void saveTargetPlayerNameToCache(BlockPos pos, String playerName) {
        if (playerName != null && !playerName.isEmpty()) {
            targetPlayerNameCache.put(pos, playerName);
            LOGGER.warn("ClientPosterManager.saveTargetPlayerNameToCache: Saved player name '{}' to cache at {}", playerName, pos);
        }
    }
    
    /**
     * Saves target player names (list) to cache for a poster at the specified position.
     * This ensures player names persist even when PosterEntity is removed.
     */
    public static void saveTargetPlayerNamesToCache(BlockPos pos, java.util.List<String> playerNames) {
        if (playerNames != null && !playerNames.isEmpty()) {
            targetPlayerNamesCache.put(pos, new java.util.ArrayList<>(playerNames));
            LOGGER.warn("ClientPosterManager.saveTargetPlayerNamesToCache: Saved player names {} to cache at {}", playerNames, pos);
        }
    }
    
    /**
     * Clears cache for a specific position.
     * Should be called when a poster block is removed/destroyed.
     */
    public static void clearCacheForPosition(BlockPos pos) {
        oldposterVariantCache.remove(pos);
        targetPlayerNameCache.remove(pos);
        targetPlayerNamesCache.remove(pos);
        posterTextCache.remove(pos);
        LOGGER.warn("ClientPosterManager.clearCacheForPosition: Cleared all cache for position {}", pos);
    }
    
    /**
     * Clears all client-side PosterEntity instances.
     * Should be called when disconnecting from a server or switching worlds.
     * IMPORTANT: This must be called on the render thread to avoid OpenGL errors with Sodium.
     */
    public static void clearAll() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level instanceof ClientLevel clientLevel) {
            // Before clearing, save all cached data to BlockEntity on SERVER to ensure persistence
            // In singleplayer, we can access the server directly
            if (minecraft.getSingleplayerServer() != null) {
                net.minecraft.server.level.ServerLevel serverLevel = minecraft.getSingleplayerServer().getLevel(clientLevel.dimension());
                if (serverLevel != null) {
                    // Create final copies of cache data for lambda
                    final Map<BlockPos, String> finalVariantCache = new HashMap<>(oldposterVariantCache);
                    final Map<BlockPos, String> finalPlayerNameCache = new HashMap<>(targetPlayerNameCache);
                    final Map<BlockPos, java.util.List<String>> finalPlayerNamesCache = new HashMap<>();
                    for (Map.Entry<BlockPos, java.util.List<String>> entry : targetPlayerNamesCache.entrySet()) {
                        finalPlayerNamesCache.put(entry.getKey(), new java.util.ArrayList<>(entry.getValue()));
                    }
                    // Execute on server thread to ensure thread safety
                    minecraft.getSingleplayerServer().execute(() -> {
                        for (Map.Entry<BlockPos, String> entry : finalVariantCache.entrySet()) {
                            BlockPos pos = entry.getKey();
                            String variant = entry.getValue();
                            net.minecraft.world.level.block.entity.BlockEntity be = serverLevel.getBlockEntity(pos);
                            // Create BlockEntity if it doesn't exist
                            if (be == null) {
                                be = neutka.marallys.marallyzen.blocks.MarallyzenBlockEntities.POSTER_BE.get().create(pos, serverLevel.getBlockState(pos));
                                if (be != null) {
                                    serverLevel.setBlockEntity(be);
                                    LOGGER.warn("ClientPosterManager.clearAll: Created SERVER BlockEntity at {} for variant: {}", pos, variant);
                                }
                            }
                            if (be instanceof neutka.marallys.marallyzen.blocks.PosterBlockEntity posterBe) {
                                posterBe.setOldposterVariant(variant);
                                // Also save player names if available
                                if (variant.equals("band")) {
                                    java.util.List<String> playerNames = finalPlayerNamesCache.get(pos);
                                    if (playerNames != null && !playerNames.isEmpty()) {
                                        posterBe.setTargetPlayerNames(playerNames);
                                    }
                                } else {
                                    String playerName = finalPlayerNameCache.get(pos);
                                    if (playerName != null && !playerName.isEmpty()) {
                                        posterBe.setTargetPlayerName(playerName);
                                    }
                                }
                                posterBe.setChanged();
                                // Force mark chunk as dirty to ensure it's saved
                                serverLevel.getChunkAt(pos).setUnsaved(true);
                                LOGGER.warn("ClientPosterManager.clearAll: Saved variant '{}' to SERVER BlockEntity at {} before clearing (chunk marked dirty)", variant, pos);
                            }
                        }
                    });
                }
            } else {
                // Fallback: save to client BlockEntity (for multiplayer, would need packet)
                for (Map.Entry<BlockPos, String> entry : oldposterVariantCache.entrySet()) {
                    BlockPos pos = entry.getKey();
                    String variant = entry.getValue();
                    net.minecraft.world.level.block.entity.BlockEntity be = clientLevel.getBlockEntity(pos);
                    if (be instanceof neutka.marallys.marallyzen.blocks.PosterBlockEntity posterBe) {
                        posterBe.setOldposterVariant(variant);
                        // Also save player names if available
                        if (variant.equals("band")) {
                            java.util.List<String> playerNames = targetPlayerNamesCache.get(pos);
                            if (playerNames != null && !playerNames.isEmpty()) {
                                posterBe.setTargetPlayerNames(playerNames);
                            }
                        } else {
                            String playerName = targetPlayerNameCache.get(pos);
                            if (playerName != null && !playerName.isEmpty()) {
                                posterBe.setTargetPlayerName(playerName);
                            }
                        }
                        posterBe.setChanged();
                        LOGGER.warn("ClientPosterManager.clearAll: Saved variant '{}' to CLIENT BlockEntity at {} before clearing (no server)", variant, pos);
                    }
                }
            }
            
            // Restore all blocks
            for (Map.Entry<BlockPos, BlockState> entry : originalBlockStates.entrySet()) {
                clientLevel.setBlock(entry.getKey(), entry.getValue(), 3);
            }
            
            // Remove all entities
            for (PosterEntity entity : clientPosters.values()) {
                entity.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
            }
        }
        
        clientPosters.clear();
        originalBlockStates.clear();
        hiddenBlocks.clear();
        posterTextCache.clear();
        oldposterVariantCache.clear();
        targetPlayerNameCache.clear();
        targetPlayerNamesCache.clear();
        
        // Clear texture cache when exiting world
        neutka.marallys.marallyzen.client.poster.text.PosterTextTextureCache.clear();
        
        LOGGER.debug("Cleared all client-side PosterEntity instances, text cache, variant cache, texture cache, and restored blocks");
    }

    private static void forceRebuild(ClientLevel level, BlockPos pos, BlockState state) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.levelRenderer == null) {
            return;
        }
        if (state == null) {
            state = level.getBlockState(pos);
        }
        try {
            if (cachedBlockChanged == null) {
                cachedBlockChanged = minecraft.levelRenderer.getClass().getMethod(
                    "blockChanged", ClientLevel.class, BlockPos.class, BlockState.class, BlockState.class, int.class
                );
                cachedBlockChanged.setAccessible(true);
            }
        } catch (ReflectiveOperationException ignored) {
            cachedBlockChanged = null;
        }
        try {
            if (cachedSetBlockDirty == null) {
                cachedSetBlockDirty = minecraft.levelRenderer.getClass().getMethod(
                    "setBlockDirty", BlockPos.class, BlockState.class, BlockState.class
                );
                cachedSetBlockDirty.setAccessible(true);
            }
        } catch (ReflectiveOperationException ignored) {
            cachedSetBlockDirty = null;
        }

        if (cachedBlockChanged != null) {
            try {
                cachedBlockChanged.invoke(minecraft.levelRenderer, level, pos, state, state, 0);
            } catch (ReflectiveOperationException ignored) {
                // Ignore and fall back.
            }
        }
        if (cachedSetBlockDirty != null) {
            try {
                cachedSetBlockDirty.invoke(minecraft.levelRenderer, pos, state, state);
            } catch (ReflectiveOperationException ignored) {
                // Ignore and fall back.
            }
        }
        level.setSectionDirtyWithNeighbors(
            SectionPos.blockToSectionCoord(pos.getX()),
            SectionPos.blockToSectionCoord(pos.getY()),
            SectionPos.blockToSectionCoord(pos.getZ())
        );
    }
}
