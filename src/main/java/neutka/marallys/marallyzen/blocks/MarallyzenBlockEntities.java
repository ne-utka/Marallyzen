package neutka.marallys.marallyzen.blocks;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.core.registries.Registries;
import neutka.marallys.marallyzen.Marallyzen;
import java.util.Set;

public class MarallyzenBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = 
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Marallyzen.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PosterBlockEntity>> POSTER_BE = 
        BLOCK_ENTITIES.register("poster_be", () ->
            new BlockEntityType<>(PosterBlockEntity::new,
                Set.of(
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
                )));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InteractiveLeverBlockEntity>> INTERACTIVE_LEVER_BE =
        BLOCK_ENTITIES.register("interactive_lever_be", () ->
            new BlockEntityType<>(InteractiveLeverBlockEntity::new,
                Set.of(MarallyzenBlocks.INTERACTIVE_LEVER.get())));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InteractiveValveBlockEntity>> INTERACTIVE_VALVE_BE =
        BLOCK_ENTITIES.register("interactive_valve_be", () ->
            new BlockEntityType<>(InteractiveValveBlockEntity::new,
                Set.of(MarallyzenBlocks.INTERACTIVE_VALVE.get())));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OldTvBlockEntity>> OLD_TV_BE =
        BLOCK_ENTITIES.register("old_tv_be", () ->
            new BlockEntityType<>(OldTvBlockEntity::new,
                Set.of(MarallyzenBlocks.OLD_TV.get())));
}





