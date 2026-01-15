package neutka.marallys.marallyzen.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;

import java.util.function.Consumer;

public class DirectionalDmcBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<DirectionalDmcBlock> CODEC = MapCodec.unit(() -> {
        throw new UnsupportedOperationException("DirectionalDmcBlock cannot be deserialized directly");
    });

    public DirectionalDmcBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
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
    @OnlyIn(Dist.CLIENT)
    public void initializeClient(Consumer<IClientBlockExtensions> consumer) {
        consumer.accept(new IClientBlockExtensions() {
            @Override
            public boolean addDestroyEffects(BlockState state, Level level, BlockPos pos, net.minecraft.client.particle.ParticleEngine manager) {
                // Use default particles - they should use texture from BlockItem automatically
                // The ParticleEngine will automatically use the ItemStack texture for particles
                return false; // Return false to use default particles with BlockItem texture
            }
        });
    }
}




