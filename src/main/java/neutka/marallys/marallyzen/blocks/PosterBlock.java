package neutka.marallys.marallyzen.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;
import neutka.marallys.marallyzen.client.ClientPosterManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.function.Consumer;

public class PosterBlock extends HorizontalDirectionalBlock implements EntityBlock {
    private static final Logger LOGGER = LoggerFactory.getLogger(PosterBlock.class);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private final int posterNumber;
    
    // Poster texture dimensions: 0.8 blocks wide, 1.0 blocks tall
    // Centered in the block: x from 1.6 to 14.4 (0.1 to 0.9 in blocks), y from 0.0 to 16.0 (0.0 to 1.0 in blocks)
    private static final double POSTER_WIDTH = 0.8; // blocks
    private static final double POSTER_HEIGHT = 1.0; // blocks
    private static final double POSTER_X_OFFSET = (1.0 - POSTER_WIDTH) / 2.0; // 0.1 blocks = 1.6 pixels
    private static final double POSTER_X_START = POSTER_X_OFFSET * 16.0; // 1.6 pixels
    private static final double POSTER_X_END = (POSTER_X_OFFSET + POSTER_WIDTH) * 16.0; // 14.4 pixels
    private static final double POSTER_Y_START = 0.0; // 0.0 pixels (bottom of block)
    private static final double POSTER_Y_END = POSTER_HEIGHT * 16.0; // 16.0 pixels (top of block)
    
    // Voxel shapes for each facing direction (matching poster texture size)
    // NORTH: poster on north face, centered horizontally
    private static final VoxelShape SHAPE_NORTH = Block.box(POSTER_X_START, POSTER_Y_START, 15.0, POSTER_X_END, POSTER_Y_END, 16.0);
    // SOUTH: poster on south face, centered horizontally
    private static final VoxelShape SHAPE_SOUTH = Block.box(POSTER_X_START, POSTER_Y_START, 0.0, POSTER_X_END, POSTER_Y_END, 1.0);
    // EAST: poster on east face, centered horizontally (z-axis)
    private static final VoxelShape SHAPE_EAST = Block.box(0.0, POSTER_Y_START, POSTER_X_START, 1.0, POSTER_Y_END, POSTER_X_END);
    // WEST: poster on west face, centered horizontally (z-axis)
    private static final VoxelShape SHAPE_WEST = Block.box(15.0, POSTER_Y_START, POSTER_X_START, 16.0, POSTER_Y_END, POSTER_X_END);
    
    private static final java.util.Map<Direction, VoxelShape> SHAPES = java.util.Map.of(
        Direction.NORTH, SHAPE_NORTH,
        Direction.SOUTH, SHAPE_SOUTH,
        Direction.EAST, SHAPE_EAST,
        Direction.WEST, SHAPE_WEST
    );
    
    public PosterBlock(int posterNumber, BlockBehaviour.Properties properties) {
        super(properties);
        this.posterNumber = posterNumber;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }
    
    public int getPosterNumber() {
        return posterNumber;
    }
    
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return MapCodec.unit(() -> {
            throw new UnsupportedOperationException("PosterBlock cannot be deserialized directly");
        });
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
    
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
    
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }
    
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty(); // No collision for the poster itself
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (player != null) {
            if (level.getBlockEntity(pos) instanceof PosterBlockEntity posterBe) {
                if (posterBe.isProtectedByOp() && !player.hasPermissions(2)) {
                    return 0.0f;
                }
            }
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        if (!level.isClientSide()) {
            if (player != null) {
                if (level.getBlockEntity(pos) instanceof PosterBlockEntity posterBe) {
                    if (posterBe.isProtectedByOp() && !player.hasPermissions(2)) {
                        return false;
                    }
                }
            }
        }
        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }
    
    @Override
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PosterBlockEntity(pos, state);
    }
    
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        // Remove BlockEntity completely when block is removed
        // This ensures new poster at same position starts with fresh BlockEntity
        if (!level.isClientSide() && !newState.is(state.getBlock())) {
            // Block is being removed (not just state change)
            // Remove BlockEntity completely to ensure fresh start
            net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(pos);
            if (be != null) {
                level.removeBlockEntity(pos);
                LOGGER.warn("PosterBlock: Removed BlockEntity at {} after block removal", pos);
            }
        }
        
        // Clear client-side cache when block is removed
        // This ensures new poster at same position starts with default values
        if (level.isClientSide()) {
            neutka.marallys.marallyzen.client.ClientPosterManager.clearCacheForPosition(pos);
            LOGGER.warn("PosterBlock: Cleared client cache for position {} after block removal", pos);
        }
        
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
    
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        
        // When placing a new block, ensure BlockEntity is fresh with default values
        // Remove old BlockEntity if it exists and create new one
        if (!level.isClientSide()) {
            // If this is a different block type, remove old BlockEntity
            if (!oldState.is(state.getBlock())) {
                net.minecraft.world.level.block.entity.BlockEntity oldBe = level.getBlockEntity(pos);
                if (oldBe != null) {
                    level.removeBlockEntity(pos);
                    LOGGER.warn("PosterBlock: Removed old BlockEntity at {} on new block placement", pos);
                }
            }
            
            // Create new BlockEntity with default values
            // This will be called by Minecraft automatically, but we ensure it's fresh
            net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof neutka.marallys.marallyzen.blocks.PosterBlockEntity posterBe) {
                // Force reset to default values
                posterBe.setOldposterVariant("default");
                posterBe.setTargetPlayerName("");
                posterBe.setTargetPlayerNames(new java.util.ArrayList<>());
                posterBe.setChanged();
                LOGGER.warn("PosterBlock: Reset BlockEntity to default values at {} on block placement", pos);
            } else if (be == null) {
                // Create new BlockEntity if it doesn't exist
                be = newBlockEntity(pos, state);
                if (be != null) {
                    level.setBlockEntity(be);
                    LOGGER.warn("PosterBlock: Created new BlockEntity at {} on block placement", pos);
                }
            }
        } else {
            // On client: clear cache when placing new block (if different block type)
            if (!oldState.is(state.getBlock())) {
                neutka.marallys.marallyzen.client.ClientPosterManager.clearCacheForPosition(pos);
                LOGGER.warn("PosterBlock: Cleared client cache for position {} on new block placement", pos);
            }
        }
    }
    
    @Override
    @OnlyIn(Dist.CLIENT)
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide) {
            if (stack.getItem() instanceof net.minecraft.world.item.WrittenBookItem && player.hasPermissions(2)) {
                markProtectedByOpClient(level, pos, state);
            }
            if (stack.getItem() instanceof net.minecraft.world.item.WrittenBookItem) {
                neutka.marallys.marallyzen.network.NetworkHelper.sendToServer(
                    new neutka.marallys.marallyzen.network.PosterBookBindPacket(pos, hand == InteractionHand.MAIN_HAND)
                );
                return ItemInteractionResult.CONSUME;
            }
            // Check if player is holding a written book and this is oldposter (ID 11)
            if (posterNumber == 11 && stack.getItem() instanceof net.minecraft.world.item.WrittenBookItem) {
                // Extract book title to determine variant using DataComponents
                var bookContent = stack.get(net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT);
                if (bookContent != null) {
                    var titleFilterable = bookContent.title();
                    if (titleFilterable != null) {
                        String bookTitle = titleFilterable.raw().toLowerCase().trim();
                        
                        // Check if book title matches one of the variants
                        String variant = null;
                        if (bookTitle.equals("dead")) {
                            variant = "dead";
                        } else if (bookTitle.equals("alive")) {
                            variant = "alive";
                        } else if (bookTitle.equals("band")) {
                            variant = "band";
                        }
                        
                        // If variant found, save it to BlockEntity
                        if (variant != null) {
                            // Extract player name(s) from first page
                            // For "band" variant: extract up to 3 lines
                            // For other variants: extract first line only
                            java.util.List<String> playerNames = new java.util.ArrayList<>();
                            var pages = bookContent.pages();
                            if (pages != null && !pages.isEmpty()) {
                                var firstPage = pages.get(0);
                                if (firstPage != null) {
                                    // firstPage is Filterable<Component>, need to get Component then convert to String
                                    net.minecraft.network.chat.Component pageComponent = firstPage.raw();
                                    if (pageComponent != null) {
                                        String firstPageText = pageComponent.getString();
                                        if (firstPageText != null && !firstPageText.isEmpty()) {
                                            String[] lines = firstPageText.split("\\n");
                                            if (variant.equals("band")) {
                                                // For band: extract up to 3 lines
                                                for (int i = 0; i < Math.min(3, lines.length); i++) {
                                                    String name = lines[i].trim();
                                                    if (!name.isEmpty()) {
                                                        playerNames.add(name);
                                                    }
                                                }
                                            } else {
                                                // For alive/dead: extract first line only
                                                if (lines.length > 0) {
                                                    String name = lines[0].trim();
                                                    if (!name.isEmpty()) {
                                                        playerNames.add(name);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            
                            net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(pos);
                            // Create BlockEntity if it doesn't exist
                            if (be == null) {
                                be = newBlockEntity(pos, state);
                                if (be != null && level instanceof net.minecraft.client.multiplayer.ClientLevel clientLevel) {
                                    clientLevel.setBlockEntity(be);
                                    LOGGER.warn("PosterBlock: Created new BlockEntity for variant: {}", variant);
                                }
                            }
                            
                            if (be instanceof neutka.marallys.marallyzen.blocks.PosterBlockEntity posterBe) {
                                String oldVariant = posterBe.getOldposterVariant();
                                posterBe.setOldposterVariant(variant);
                                if (player.hasPermissions(2)) {
                                    posterBe.setProtectedByOp(true);
                                }
                                
                                // Save player name(s) based on variant
                                if (!playerNames.isEmpty()) {
                                    if (variant.equals("band")) {
                                        // For band: save as list
                                        posterBe.setTargetPlayerNames(playerNames);
                                        // Save to cache for persistence
                                        neutka.marallys.marallyzen.client.ClientPosterManager.saveTargetPlayerNamesToCache(pos, playerNames);
                                        LOGGER.warn("PosterBlock: Set target player names to {} for variant {}", playerNames, variant);
                                    } else {
                                        // For alive/dead: save as single name
                                        String playerName = playerNames.get(0);
                                        posterBe.setTargetPlayerName(playerName);
                                        // Save to cache for persistence
                                        neutka.marallys.marallyzen.client.ClientPosterManager.saveTargetPlayerNameToCache(pos, playerName);
                                        LOGGER.warn("PosterBlock: Set target player name to '{}' for variant {}", playerName, variant);
                                    }
                                }
                                
                                // Force save the BlockEntity
                                posterBe.setChanged();
                                
                                // Save variant to cache for persistence
                                neutka.marallys.marallyzen.client.ClientPosterManager.saveOldposterVariantToCache(pos, variant);
                                
                                LOGGER.warn("PosterBlock: Set oldposter variant from '{}' to '{}' at {}", oldVariant, variant, pos);
                                
                                // IMPORTANT: If this is on client, also save to server BlockEntity in singleplayer
                                // This ensures data persists across world reloads
                                if (level.isClientSide() && level instanceof net.minecraft.client.multiplayer.ClientLevel clientLevel) {
                                    net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
                                    if (minecraft.getSingleplayerServer() != null) {
                                        net.minecraft.server.level.ServerLevel serverLevel = minecraft.getSingleplayerServer().getLevel(clientLevel.dimension());
                                        if (serverLevel != null) {
                                            // Make variables final for lambda
                                            final String finalVariant = variant;
                                            final BlockPos finalPos = pos;
                                            final java.util.List<String> finalPlayerNames = new java.util.ArrayList<>(playerNames);
                                            // Execute on server thread to ensure thread safety
                                            final boolean protectByOp = player.hasPermissions(2);
                                            minecraft.getSingleplayerServer().execute(() -> {
                                                net.minecraft.world.level.block.entity.BlockEntity serverBe = serverLevel.getBlockEntity(finalPos);
                                                // Create BlockEntity if it doesn't exist
                                                if (serverBe == null) {
                                                    serverBe = newBlockEntity(finalPos, serverLevel.getBlockState(finalPos));
                                                    if (serverBe != null) {
                                                        serverLevel.setBlockEntity(serverBe);
                                                        LOGGER.warn("PosterBlock: Created SERVER BlockEntity for variant: {}", finalVariant);
                                                    }
                                                }
                                                if (serverBe instanceof neutka.marallys.marallyzen.blocks.PosterBlockEntity serverPosterBe) {
                                                    serverPosterBe.setOldposterVariant(finalVariant);
                                                    if (protectByOp) {
                                                        serverPosterBe.setProtectedByOp(true);
                                                    }
                                                    if (!finalPlayerNames.isEmpty()) {
                                                        if (finalVariant.equals("band")) {
                                                            serverPosterBe.setTargetPlayerNames(finalPlayerNames);
                                                        } else {
                                                            serverPosterBe.setTargetPlayerName(finalPlayerNames.get(0));
                                                        }
                                                    }
                                                    serverPosterBe.setChanged();
                                                    // Force mark chunk as dirty to ensure it's saved
                                                    serverLevel.getChunkAt(finalPos).setUnsaved(true);
                                                    LOGGER.warn("PosterBlock: Saved variant '{}' to SERVER BlockEntity at {} (chunk marked dirty)", finalVariant, finalPos);
                                                } else {
                                                    LOGGER.warn("PosterBlock: SERVER BlockEntity is not PosterBlockEntity at {}", finalPos);
                                                }
                                            });
                                        }
                                    }
                                }
                                
                                // If there's an active PosterEntity, recreate it with new variant
                                if (ClientPosterManager.hasClientPoster(pos)) {
                                    ClientPosterManager.removeClientPoster(pos);
                                }
                                // Always create PosterEntity after setting variant to show the correct texture
                                ClientPosterManager.createClientPoster(pos, posterNumber, state);
                                
                                // Return CONSUME to prevent book GUI from opening
                                return ItemInteractionResult.CONSUME;
                            } else {
                                LOGGER.warn("PosterBlock: BlockEntity is not PosterBlockEntity, cannot set variant");
                            }
                        }
                    }
                }
            }
            
            // If there's already an active PosterEntity at this position, don't create a new one
            // and immediately hide the block if it became visible
            if (ClientPosterManager.hasClientPoster(pos)) {
                // Block should be hidden, but if it became visible (e.g., server synced it back),
                // hide it immediately
                if (level instanceof net.minecraft.client.multiplayer.ClientLevel clientLevel) {
                    BlockState currentState = clientLevel.getBlockState(pos);
                    if (currentState.getBlock() != net.minecraft.world.level.block.Blocks.AIR) {
                        clientLevel.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 18);
                    }
                }
                return ItemInteractionResult.SUCCESS; // Already active, don't do anything
            }
            
            // Client-side: create client-only PosterEntity
            ClientPosterManager.createClientPoster(pos, posterNumber, state);

            // Enable FPV context and play configured emote if available
            neutka.marallys.marallyzen.client.fpv.MarallyzenRenderContext.setFpvEmoteEnabled(true);
            neutka.marallys.marallyzen.client.fpv.FpvEmoteInvoker.play(player, "SPE_Poke");
            
            // Send packet to server to trigger poke animation for nearby players
            neutka.marallys.marallyzen.network.NetworkHelper.sendToServer(
                    new neutka.marallys.marallyzen.network.PosterInteractPacket(pos)
            );
        }
        return ItemInteractionResult.SUCCESS;
    }

    @OnlyIn(Dist.CLIENT)
    private static void markProtectedByOpClient(Level level, BlockPos pos, BlockState state) {
        net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(pos);
        if (be == null) {
            be = new PosterBlockEntity(pos, state);
            if (be != null && level instanceof net.minecraft.client.multiplayer.ClientLevel clientLevel) {
                clientLevel.setBlockEntity(be);
            }
        }
        if (be instanceof PosterBlockEntity posterBe) {
            posterBe.setProtectedByOp(true);
        }

        if (level instanceof net.minecraft.client.multiplayer.ClientLevel clientLevel) {
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            if (minecraft.getSingleplayerServer() != null) {
                net.minecraft.server.level.ServerLevel serverLevel = minecraft.getSingleplayerServer().getLevel(clientLevel.dimension());
                if (serverLevel != null) {
                    final BlockPos finalPos = pos;
                    minecraft.getSingleplayerServer().execute(() -> {
                        net.minecraft.world.level.block.entity.BlockEntity serverBe = serverLevel.getBlockEntity(finalPos);
                        if (serverBe instanceof PosterBlockEntity serverPosterBe) {
                            serverPosterBe.setProtectedByOp(true);
                        }
                    });
                }
            }
        }
    }
    
    @Override
    @OnlyIn(Dist.CLIENT)
    public void initializeClient(Consumer<IClientBlockExtensions> consumer) {
        consumer.accept(new IClientBlockExtensions() {
            @Override
            public boolean addDestroyEffects(BlockState state, Level level, BlockPos pos, net.minecraft.client.particle.ParticleEngine manager) {
                return false; // Use default particles
            }
        });
    }
}
