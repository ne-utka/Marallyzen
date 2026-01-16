package neutka.marallys.marallyzen.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import neutka.marallys.marallyzen.audio.MarallyzenSounds;

public class OldTvBlock extends ModelShapeFacingBlock implements EntityBlock {
    public static final BooleanProperty ON = BooleanProperty.create("on");

    public OldTvBlock(Properties properties, String modelPath) {
        super(properties, modelPath);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(HorizontalDirectionalBlock.FACING, net.minecraft.core.Direction.NORTH)
                .setValue(ON, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ON);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                             Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.getItem() instanceof WrittenBookItem) {
            if (level.isClientSide) {
                if (player.hasPermissions(2)) {
                    markProtectedByOpClient(level, pos);
                }
                neutka.marallys.marallyzen.network.NetworkHelper.sendToServer(
                    new neutka.marallys.marallyzen.network.OldTvBookBindPacket(pos, hand == InteractionHand.MAIN_HAND)
                );
                var bookContent = stack.get(net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT);
                if (bookContent != null && bookContent.title() != null) {
                    String mediaName = bookContent.title().raw();
                    if (mediaName != null) {
                        mediaName = mediaName.trim();
                    }
                    if (mediaName != null && !mediaName.isEmpty()) {
                        boolean bound = neutka.marallys.marallyzen.client.OldTvMediaManager.bindMedia(
                                pos,
                                level.dimension(),
                                mediaName
                        );
                        if (bound) {
                            neutka.marallys.marallyzen.client.narration.NarrationManager.getInstance().startNarration(
                                    neutka.marallys.marallyzen.client.OldTvMediaManager.buildBindNarration(mediaName),
                                    null,
                                    5,
                                    100,
                                    3
                            );
                            return ItemInteractionResult.CONSUME;
                        }
                    }
                }
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("Book title must match the media name."),
                        true
                );
            }
            return ItemInteractionResult.CONSUME;
        }
        if (!level.isClientSide) {
            BlockState nextState = state.cycle(ON);
            level.setBlock(pos, nextState, 3);
            if (nextState.getValue(ON)) {
                level.playSound(null, pos, MarallyzenSounds.TV_ON.get(), net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
            } else {
                level.playSound(null, pos, MarallyzenSounds.TV_OFF.get(), net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
            }
        }
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        if (player != null) {
            if (level.getBlockEntity(pos) instanceof OldTvBlockEntity tvEntity) {
                if (tvEntity.isProtectedByOp() && !player.hasPermissions(2)) {
                    return 0.0f;
                }
            }
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        if (!level.isClientSide) {
            if (player != null) {
                if (level.getBlockEntity(pos) instanceof OldTvBlockEntity tvEntity) {
                    if (tvEntity.isProtectedByOp() && !player.hasPermissions(2)) {
                        return false;
                    }
                }
            }
        }
        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }

    private static void markProtectedByOpClient(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof OldTvBlockEntity tvEntity) {
            tvEntity.setProtectedByOp(true);
        }
        if (level instanceof net.minecraft.client.multiplayer.ClientLevel clientLevel) {
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            if (minecraft.getSingleplayerServer() != null) {
                net.minecraft.server.level.ServerLevel serverLevel = minecraft.getSingleplayerServer().getLevel(clientLevel.dimension());
                if (serverLevel != null) {
                    final BlockPos finalPos = pos;
                    minecraft.getSingleplayerServer().execute(() -> {
                        BlockEntity serverBe = serverLevel.getBlockEntity(finalPos);
                        if (serverBe instanceof OldTvBlockEntity serverTvEntity) {
                            serverTvEntity.setProtectedByOp(true);
                        }
                    });
                }
            }
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OldTvBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return (lvl, pos, blockState, be) -> {
                if (be instanceof OldTvBlockEntity tvEntity) {
                    tvEntity.clientTick();
                }
            };
        }
        return null;
    }
}
