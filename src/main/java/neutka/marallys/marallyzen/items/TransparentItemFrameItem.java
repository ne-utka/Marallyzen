package neutka.marallys.marallyzen.items;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HangingEntityItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

public class TransparentItemFrameItem extends HangingEntityItem {
    @FunctionalInterface
    public interface EntityFactory {
        HangingEntity create(Level level, BlockPos pos, Direction direction);
    }

    private final EntityFactory entityFactory;

    public TransparentItemFrameItem(EntityType<? extends HangingEntity> entityType, Properties properties, EntityFactory entityFactory) {
        super(entityType, properties);
        this.entityFactory = entityFactory;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockPos clickedPos = context.getClickedPos();
        Direction clickedFace = context.getClickedFace();
        BlockPos framePos = clickedPos.relative(clickedFace);
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (player != null && !this.mayPlace(player, clickedFace, stack, framePos)) {
            return InteractionResult.FAIL;
        }

        Level level = context.getLevel();
        HangingEntity hangingEntity = this.entityFactory.create(level, framePos, clickedFace);
        EntityType.<HangingEntity>createDefaultStackConfig(level, stack, player).accept(hangingEntity);
        if (!hangingEntity.survives()) {
            return InteractionResult.CONSUME;
        }

        if (!level.isClientSide()) {
            hangingEntity.playPlacementSound();
            level.gameEvent(player, GameEvent.ENTITY_PLACE, hangingEntity.position());
            level.addFreshEntity(hangingEntity);
        }

        stack.shrink(1);
        return InteractionResult.SUCCESS;
    }
}
