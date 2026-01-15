package neutka.marallys.marallyzen.blocks;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.SoundType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.items.MarallyzenItems;
import neutka.marallys.marallyzen.blocks.PosterBlock;
import neutka.marallys.marallyzen.blocks.InteractiveChainBlock;
import neutka.marallys.marallyzen.blocks.FacingBlock;
import neutka.marallys.marallyzen.blocks.ModelShapeFacingBlock;
import neutka.marallys.marallyzen.blocks.HiddenOutlineBlock;
import neutka.marallys.marallyzen.blocks.DictaphoneBlock;
import neutka.marallys.marallyzen.blocks.DictaphoneSimpleBlock;

public class MarallyzenBlocks {
    // DeferredRegister for blocks
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Marallyzen.MODID);
    
    // Blocks (with directional placement)
    
    // Poster blocks (poster1 - poster10)
    public static final DeferredBlock<PosterBlock> POSTER_1 = BLOCKS.register("poster1", 
        () -> new PosterBlock(1, BlockBehaviour.Properties.of()
            .strength(0.5f)
            .noOcclusion()));
    
    public static final DeferredBlock<PosterBlock> POSTER_2 = BLOCKS.register("poster2", 
        () -> new PosterBlock(2, BlockBehaviour.Properties.of()
            .strength(0.5f)
            .noOcclusion()));
    
    public static final DeferredBlock<PosterBlock> POSTER_3 = BLOCKS.register("poster3", 
        () -> new PosterBlock(3, BlockBehaviour.Properties.of()
            .strength(0.5f)
            .noOcclusion()));
    
    public static final DeferredBlock<PosterBlock> POSTER_4 = BLOCKS.register("poster4", 
        () -> new PosterBlock(4, BlockBehaviour.Properties.of()
            .strength(0.5f)
            .noOcclusion()));
    
    public static final DeferredBlock<PosterBlock> POSTER_5 = BLOCKS.register("poster5", 
        () -> new PosterBlock(5, BlockBehaviour.Properties.of()
            .strength(0.5f)
            .noOcclusion()));
    
    public static final DeferredBlock<PosterBlock> POSTER_6 = BLOCKS.register("poster6", 
        () -> new PosterBlock(6, BlockBehaviour.Properties.of()
            .strength(0.5f)
            .noOcclusion()));
    
    public static final DeferredBlock<PosterBlock> POSTER_7 = BLOCKS.register("poster7", 
        () -> new PosterBlock(7, BlockBehaviour.Properties.of()
            .strength(0.5f)
            .noOcclusion()));
    
    public static final DeferredBlock<PosterBlock> POSTER_8 = BLOCKS.register("poster8", 
        () -> new PosterBlock(8, BlockBehaviour.Properties.of()
            .strength(0.5f)
            .noOcclusion()));
    
    public static final DeferredBlock<PosterBlock> POSTER_9 = BLOCKS.register("poster9", 
        () -> new PosterBlock(9, BlockBehaviour.Properties.of()
            .strength(0.5f)
            .noOcclusion()));
    
    public static final DeferredBlock<PosterBlock> POSTER_10 = BLOCKS.register("poster10", 
        () -> new PosterBlock(10, BlockBehaviour.Properties.of()
            .strength(0.5f)
            .noOcclusion()));

    // New posters
    public static final DeferredBlock<PosterBlock> OLD_POSTER = BLOCKS.register("oldposter",
        () -> new PosterBlock(11, BlockBehaviour.Properties.of()
            .strength(0.5f)
            .noOcclusion()));

    public static final DeferredBlock<PosterBlock> PAPER_POSTER_1 = BLOCKS.register("paperposter1",
        () -> new PosterBlock(12, BlockBehaviour.Properties.of()
            .strength(0.5f)
            .noOcclusion()));

    public static final DeferredBlock<PosterBlock> PAPER_POSTER_2 = BLOCKS.register("paperposter2",
        () -> new PosterBlock(13, BlockBehaviour.Properties.of()
            .strength(0.5f)
            .noOcclusion()));

    public static final DeferredBlock<Block> OLD_LAPTOP = BLOCKS.register("old_laptop",
        () -> new ModelShapeFacingBlock(BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.METAL)
            .noOcclusion(),
            "assets/marallyzen/models/block/old_laptop.json"));

    public static final DeferredBlock<Block> OLD_TV = BLOCKS.register("old_tv",
        () -> new OldTvBlock(BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.METAL)
            .noOcclusion(),
            "assets/marallyzen/models/block/old_tv.json"));

    public static final DeferredBlock<Block> DICTAPHONE = BLOCKS.register("dictaphone",
        () -> new DictaphoneBlock(BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.METAL)
            .noOcclusion(),
            "assets/marallyzen/models/block/dictaphone.json"));

    public static final DeferredBlock<Block> MIRROR = BLOCKS.register("mirror",
        () -> new ModelShapeFacingBlock(BlockBehaviour.Properties.of()
            .strength(0.5f)
            .sound(SoundType.GLASS)
            .noOcclusion(),
            "assets/marallyzen/models/block/mirror.json"));

    public static final DeferredBlock<Block> NAILED_PLANKS = BLOCKS.register("nailed_planks",
        () -> new HiddenOutlineBlock(BlockBehaviour.Properties.of()
            .strength(1.5f)
            .sound(SoundType.WOOD)
            .noOcclusion()));

    public static final DeferredBlock<Block> STACK_OF_NAILED_PLANKS = BLOCKS.register("stack_of_nailed_planks",
        () -> new HiddenOutlineBlock(BlockBehaviour.Properties.of()
            .strength(1.5f)
            .sound(SoundType.WOOD)
            .noOcclusion()));

    public static final DeferredBlock<Block> VIDEO_CAMERA = BLOCKS.register("video_camera",
        () -> new ModelShapeFacingBlock(BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.METAL)
            .noOcclusion(),
            "assets/marallyzen/models/block/video_camera.json"));

    public static final DeferredBlock<InteractiveChainBlock> INTERACTIVE_CHAIN = BLOCKS.register("interactive_chain",
        () -> new InteractiveChainBlock(BlockBehaviour.Properties.of()
            .strength(3.0f)
            .sound(SoundType.CHAIN)
            .noOcclusion()));

    public static final DeferredBlock<Block> DICTAPHONE_SIMPLE = BLOCKS.register("dictaphone_simple",
        () -> new DictaphoneSimpleBlock(BlockBehaviour.Properties.of()
            .strength(0.5f)
            .sound(SoundType.METAL)
            .noOcclusion()));
}
