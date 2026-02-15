package neutka.marallys.marallyzen.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.items.MarallyzenItems;

public class TransparentGlowItemFrameEntity extends GlowItemFrame {
    public TransparentGlowItemFrameEntity(EntityType<? extends ItemFrame> type, Level level) {
        super(type, level);
    }

    public TransparentGlowItemFrameEntity(Level level, BlockPos pos, Direction direction) {
        super(Marallyzen.TRANSPARENT_GLOW_ITEM_FRAME_ENTITY.get(), level);
        this.pos = pos;
        this.setDirection(direction);
        this.setInvisible(false);
    }

    @Override
    protected ItemStack getFrameItemStack() {
        return new ItemStack(MarallyzenItems.TRANSPARENT_GLOW_ITEM_FRAME.get());
    }
}
