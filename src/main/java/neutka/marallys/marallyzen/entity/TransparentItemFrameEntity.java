package neutka.marallys.marallyzen.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.items.MarallyzenItems;

public class TransparentItemFrameEntity extends ItemFrame {
    public TransparentItemFrameEntity(EntityType<? extends ItemFrame> type, Level level) {
        super(type, level);
    }

    public TransparentItemFrameEntity(Level level, BlockPos pos, Direction direction) {
        super(Marallyzen.TRANSPARENT_ITEM_FRAME_ENTITY.get(), level, pos, direction);
    }

    @Override
    protected ItemStack getFrameItemStack() {
        return new ItemStack(MarallyzenItems.TRANSPARENT_ITEM_FRAME.get());
    }
}
