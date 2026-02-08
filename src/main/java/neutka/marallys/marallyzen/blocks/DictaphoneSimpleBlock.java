package neutka.marallys.marallyzen.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import neutka.marallys.marallyzen.util.PermissionHelper;

/**
 * Dictaphone simple block that lies flat on the ground.
 * Uses ModelShapeFacingBlock to automatically generate shapes from the model JSON.
 */
public class DictaphoneSimpleBlock extends ModelShapeFacingBlock {
    public static final BooleanProperty SHOW = BooleanProperty.create("show");

    public DictaphoneSimpleBlock(Properties properties) {
        super(properties, "assets/marallyzen/models/block/dictaphone_simple.json");
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(HorizontalDirectionalBlock.FACING, net.minecraft.core.Direction.NORTH)
            .setValue(SHOW, false));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
            .setValue(HorizontalDirectionalBlock.FACING, context.getHorizontalDirection().getOpposite())
            .setValue(SHOW, false);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SHOW);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hitResult) {
        return handleInteraction(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                            Player player, BlockHitResult hitResult) {
        ItemStack stack = ItemStack.EMPTY;
        InteractionResult result = handleInteraction(stack, state, level, pos, player, InteractionHand.MAIN_HAND, hitResult);
        return result == InteractionResult.CONSUME
            ? InteractionResult.CONSUME
            : InteractionResult.SUCCESS;
    }

    private InteractionResult handleInteraction(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.getItem() instanceof WrittenBookItem) {
            if (!level.isClientSide() && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                var bookContent = stack.get(net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT);
                String scriptName = null;
                if (bookContent != null && bookContent.title() != null) {
                    scriptName = bookContent.title().raw();
                }
                if (scriptName != null) {
                    scriptName = scriptName.trim();
                }
                if (scriptName == null || scriptName.isEmpty()
                    || !neutka.marallys.marallyzen.dictaphone.DictaphoneScriptManager.scriptExists(scriptName)) {
                    neutka.marallys.marallyzen.dictaphone.DictaphoneScriptManager.sendMissingNarration(
                        serverPlayer,
                        scriptName == null || scriptName.isEmpty() ? "?" : scriptName
                    );
                    return InteractionResult.CONSUME;
                }
                neutka.marallys.marallyzen.dictaphone.DictaphoneScriptManager.bindScript(
                    pos,
                    level.dimension(),
                    scriptName,
                    PermissionHelper.isOp(serverPlayer)
                );
                neutka.marallys.marallyzen.dictaphone.DictaphoneScriptManager.sendBindNarration(
                    serverPlayer,
                    scriptName
                );
            }
            return InteractionResult.CONSUME;
        }
        if (level.isClientSide()) {
            if (!neutka.marallys.marallyzen.client.ClientDictaphoneManager.isHidden(pos)) {
                neutka.marallys.marallyzen.client.ClientDictaphoneManager.markHidden(pos.immutable());
            }
            neutka.marallys.marallyzen.client.gui.SimpleBlockPromptHud.getInstance().hidePromptFor(pos);
            neutka.marallys.marallyzen.client.ClientDictaphoneManager.createClientDictaphone(pos.immutable(), state);
        } else if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            if (neutka.marallys.marallyzen.dictaphone.DictaphoneScriptManager.isNarrationLocked(
                serverPlayer.getUUID(), level.getGameTime()
            )) {
                return InteractionResult.CONSUME;
            }
            String scriptName = neutka.marallys.marallyzen.dictaphone.DictaphoneScriptManager.getBoundScript(
                pos,
                level.dimension()
            );
            if (scriptName != null) {
                neutka.marallys.marallyzen.dictaphone.DictaphoneScriptManager.playBoundScript(
                    serverPlayer,
                    (net.minecraft.server.level.ServerLevel) level,
                    pos,
                    scriptName
                );
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        if (player != null && level instanceof Level lvl) {
            if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer && PermissionHelper.isOp(serverPlayer))
                && neutka.marallys.marallyzen.dictaphone.DictaphoneScriptManager.isProtectedByOp(pos, lvl.dimension())) {
                return 0.0f;
            }
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    // onDestroyedByPlayer no longer exists in this NeoForge/Minecraft version.
}
