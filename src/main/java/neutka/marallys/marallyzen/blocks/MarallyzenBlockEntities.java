package neutka.marallys.marallyzen.blocks;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.core.registries.Registries;
import neutka.marallys.marallyzen.Marallyzen;

public class MarallyzenBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = 
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Marallyzen.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PosterBlockEntity>> POSTER_BE = 
        BLOCK_ENTITIES.register("poster_be", () ->
            BlockEntityType.Builder.of(PosterBlockEntity::new, 
                MarallyzenBlocks.POSTER_1.get(),
                MarallyzenBlocks.POSTER_2.get(),
                MarallyzenBlocks.POSTER_3.get(),
                MarallyzenBlocks.POSTER_4.get(),
                MarallyzenBlocks.POSTER_5.get(),
                MarallyzenBlocks.POSTER_6.get(),
                MarallyzenBlocks.POSTER_7.get(),
                MarallyzenBlocks.POSTER_8.get(),
                MarallyzenBlocks.POSTER_9.get(),
                MarallyzenBlocks.POSTER_10.get(),
                MarallyzenBlocks.OLD_POSTER.get(),
                MarallyzenBlocks.PAPER_POSTER_1.get(),
                MarallyzenBlocks.PAPER_POSTER_2.get()
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InteractiveChainBlockEntity>> INTERACTIVE_CHAIN_BE =
        BLOCK_ENTITIES.register("interactive_chain_be", () ->
            BlockEntityType.Builder.of(InteractiveChainBlockEntity::new,
                MarallyzenBlocks.INTERACTIVE_CHAIN.get()
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OldTvBlockEntity>> OLD_TV_BE =
        BLOCK_ENTITIES.register("old_tv_be", () ->
            BlockEntityType.Builder.of(OldTvBlockEntity::new,
                MarallyzenBlocks.OLD_TV.get()
            ).build(null));
}




