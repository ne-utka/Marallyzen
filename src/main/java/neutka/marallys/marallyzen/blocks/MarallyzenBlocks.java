package neutka.marallys.marallyzen.blocks;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
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
import neutka.marallys.marallyzen.blocks.InteractiveLeverBlock;
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

    public static final DeferredBlock<Block> RADIO = BLOCKS.register("radio",
        () -> new ModelShapeFacingBlock(BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.METAL)
            .noOcclusion(),
            "assets/marallyzen/models/block/radio.json"));

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

    public static final DeferredBlock<Block> TEST_BLOCK = BLOCKS.register("test_block",
        () -> new Block(BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.STONE)));

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

    public static final DeferredBlock<InteractiveLeverBlock> INTERACTIVE_LEVER = BLOCKS.register("interactive_lever",
        () -> new InteractiveLeverBlock(BlockBehaviour.Properties.of()
            .strength(0.5f)
            .sound(SoundType.METAL)
            .noOcclusion()));

    public static final DeferredBlock<Block> DICTAPHONE_SIMPLE = BLOCKS.register("dictaphone_simple",
        () -> new DictaphoneSimpleBlock(BlockBehaviour.Properties.of()
            .strength(0.5f)
            .sound(SoundType.METAL)
            .noOcclusion()));

    public static final DeferredBlock<Block> BANK_SIGN = BLOCKS.register("bank_sign",
        () -> new ModelShapeFacingBlock(BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion(),
            "assets/marallyzen/models/block/bank_sign.json"));

    public static final DeferredBlock<Block> BAR_SIGN = BLOCKS.register("bar_sign",
        () -> new ModelShapeFacingBlock(BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion(),
            "assets/marallyzen/models/block/bar_sign.json"));

    public static final DeferredBlock<Block> BARREL_FULL = BLOCKS.register("barrel_full",
        () -> new ModelShapeFacingBlock(BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion(),
            "assets/marallyzen/models/block/barrel_full.json"));

    public static final DeferredBlock<Block> BARREL_FULL_PILE = BLOCKS.register("barrel_full_pile",
        () -> new ModelShapeFacingBlock(BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion(),
            "assets/marallyzen/models/block/barrel_full_pile.json",
            true,
            true));

    public static final DeferredBlock<Block> COACH = BLOCKS.register("coach",
        () -> new ModelShapeFacingBlock(BlockBehaviour.Properties.of()
            .strength(1.5f)
            .sound(SoundType.WOOD)
            .noOcclusion(),
            "assets/marallyzen/models/block/coach.json",
            true,
            true));

    public static final DeferredBlock<Block> LARGE_CACTUS_POT = BLOCKS.register("large_cactus_pot",
        () -> new ModelShapeFacingBlock(BlockBehaviour.Properties.of()
            .strength(0.8f)
            .sound(SoundType.STONE)
            .noOcclusion(),
            "assets/marallyzen/models/block/large_cactus_pot.json"));

    public static final DeferredBlock<Block> MEDIUM_CACTUS_POT = BLOCKS.register("medium_cactus_pot",
        () -> new ModelShapeFacingBlock(BlockBehaviour.Properties.of()
            .strength(0.8f)
            .sound(SoundType.STONE)
            .noOcclusion(),
            "assets/marallyzen/models/block/medium_cactus_pot.json"));

    public static final DeferredBlock<Block> MINI_CACTUS_POT = BLOCKS.register("mini_cactus_pot",
        () -> new ModelShapeFacingBlock(BlockBehaviour.Properties.of()
            .strength(0.8f)
            .sound(SoundType.STONE)
            .noOcclusion(),
            "assets/marallyzen/models/block/mini_cactus_pot.json"));

    public static final DeferredBlock<Block> WEST_TABLE_BAR = BLOCKS.register("west_table_bar",
        () -> new ModelShapeFacingBlock(BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion(),
            "assets/marallyzen/models/block/west_table_bar.json"));

    public static final DeferredBlock<Block> WEST_CHAIR_BAR = BLOCKS.register("west_chair_bar",
        () -> new ModelShapeFacingBlock(BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion(),
            "assets/marallyzen/models/block/west_chair_bar.json"));

    public static final DeferredBlock<Block> WOODEN_BUCKET = BLOCKS.register("wooden_bucket",
        () -> new ModelShapeFacingBlock(BlockBehaviour.Properties.of()
            .strength(0.8f)
            .sound(SoundType.WOOD)
            .noOcclusion(),
            "assets/marallyzen/models/block/wooden_bucket.json"));

    public static final DeferredBlock<Block> DRYING_FISH_RACK = BLOCKS.register("drying_fish_rack",
        () -> new ModelShapeFacingBlock(BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion(),
            "assets/marallyzen/models/block/drying_fish_rack.json"));

    public static final DeferredBlock<Block> FISH = BLOCKS.register("fish",
        () -> new ModelShapeFacingBlock(BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion(),
            "assets/marallyzen/models/block/fish.json"));

    public static final DeferredBlock<Block> FISHING_NET_WALL_DECORATION = BLOCKS.register("fishing_net_wall_decoration",
        () -> new ModelShapeFacingBlock(BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion(),
            "assets/marallyzen/models/block/fishing_net_wall_decoration.json"));

    public static final DeferredBlock<Block> FISHING_ROD = BLOCKS.register("fishing_rod",
        () -> new ModelShapeFacingBlock(BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion(),
            "assets/marallyzen/models/block/fishing_rod.json",
            true,
            false,
            0,
            true,
            -0.125,
            0.235,
            false,
            false,
            true,
            0.21875,
            -0.625));

    public static final DeferredBlock<Block> FISHING_ROD_RACK = BLOCKS.register("fishing_rod_rack",
        () -> new ModelShapeFacingBlock(BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion(),
            "assets/marallyzen/models/block/fishing_rod_rack.json",
            true,
            false,
            0,
            true,
            0.0,
            0.0,
            false,
            false,
            true));

    public static final DeferredBlock<Block> FISH_BOX = BLOCKS.register("fish_box",
        () -> new ModelShapeFacingBlock(BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion(),
            "assets/marallyzen/models/block/fish_box.json"));

    public static final DeferredBlock<Block> FISH_BOX_EMPTY = BLOCKS.register("fish_box_empty",
        () -> new ModelShapeFacingBlock(BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion(),
            "assets/marallyzen/models/block/fish_box_empty.json"));

    public static final DeferredBlock<Block> FISH_PILE = BLOCKS.register("fish_pile",
        () -> new ModelShapeFacingBlock(BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion(),
            "assets/marallyzen/models/block/fish_pile.json"));

    public static final DeferredBlock<Block> FISH_PRIZE_WALL_DECORATION = BLOCKS.register("fish_prize_wall_decoration",
        () -> new ModelShapeFacingBlock(BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion(),
            "assets/marallyzen/models/block/fish_prize_wall_decoration.json"));

    public static final DeferredBlock<Block> LEANING_FISHING_ROD = BLOCKS.register("leaning_fishing_rod",
        () -> new ModelShapeFacingBlock(BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion(),
            "assets/marallyzen/models/block/leaning_fishing_rod.json",
            true,
            false,
            0,
            true,
            -0.125,
            0.234375,
            false,
            false,
            true,
            0.21875,
            -0.625));

    public static final DeferredBlock<Block> TAVERN_BENCH = BLOCKS.register("tavern_bench",
        () -> new ModelShapeFacingBlock(BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion(),
            "assets/marallyzen/models/block/tavern_furniture/tavern_bench.json"));

    public static final DeferredBlock<Block> TAVERN_BIG_KEG = BLOCKS.register("tavern_big_keg",
        () -> new ModelShapeFacingBlock(BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion(),
            "assets/marallyzen/models/block/tavern_furniture/tavern_big_keg.json"));

    public static final DeferredBlock<Block> TAVERN_BIG_KEG2 = BLOCKS.register("tavern_big_keg2",
        () -> new ModelShapeFacingBlock(BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion(),
            "assets/marallyzen/models/block/tavern_furniture/tavern_big_keg2.json"));

    public static final DeferredBlock<Block> TAVERN_BIG_TABLE = BLOCKS.register("tavern_big_table",
        () -> new ModelShapeFacingBlock(BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion(),
            "assets/marallyzen/models/block/tavern_furniture/tavern_big_table.json"));

    public static final DeferredBlock<Block> TAVERN_KEG = BLOCKS.register("tavern_keg",
        () -> new ModelShapeFacingBlock(BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion(),
            "assets/marallyzen/models/block/tavern_furniture/tavern_keg.json"));

    public static final DeferredBlock<Block> TAVERN_KEG2 = BLOCKS.register("tavern_keg2",
        () -> new ModelShapeFacingBlock(BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion(),
            "assets/marallyzen/models/block/tavern_furniture/tavern_keg2.json"));

    public static final DeferredBlock<Block> TAVERN_KEG_SUPPORT = BLOCKS.register("tavern_keg_support",
        () -> new ModelShapeFacingBlock(BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion(),
            "assets/marallyzen/models/block/tavern_furniture/tavern_keg_support.json"));

    public static final DeferredBlock<Block> TAVERN_KEG_SUPPORT_DOUBLE = BLOCKS.register("tavern_keg_support_double",
        () -> new ModelShapeFacingBlock(BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion(),
            "assets/marallyzen/models/block/tavern_furniture/tavern_keg_support_double.json"));

    public static final DeferredBlock<Block> TAVERN_MULTIPLE_BOTTLES = BLOCKS.register("tavern_multiple_bottles",
        () -> new ModelShapeFacingBlock(BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion(),
            "assets/marallyzen/models/block/tavern_furniture/tavern_multiple_bottles.json"));

    public static final DeferredBlock<Block> TAVERN_MURAL_SHELF = BLOCKS.register("tavern_mural_shelf",
        () -> new ModelShapeFacingBlock(BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion(),
            "assets/marallyzen/models/block/tavern_furniture/tavern_mural_shelf.json"));

    public static final DeferredBlock<Block> TAVERN_PILE_BOTTLES = BLOCKS.register("tavern_pile_bottles",
        () -> new ModelShapeFacingBlock(BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion(),
            "assets/marallyzen/models/block/tavern_furniture/tavern_pile_bottles.json"));

    public static final DeferredBlock<Block> TAVERN_RED_BOTTLE = BLOCKS.register("tavern_red_bottle",
        () -> new ModelShapeFacingBlock(BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion(),
            "assets/marallyzen/models/block/tavern_furniture/tavern_red_bottle.json"));

    public static final DeferredBlock<Block> TAVERN_SMALL_GREEN_BOTTLE = BLOCKS.register("tavern_small_green_bottle",
        () -> new ModelShapeFacingBlock(BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion(),
            "assets/marallyzen/models/block/tavern_furniture/tavern_small_green_bottle.json"));

    public static final DeferredBlock<Block> TAVERN_STOOL = BLOCKS.register("tavern_stool",
        () -> new ModelShapeFacingBlock(BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion(),
            "assets/marallyzen/models/block/tavern_furniture/tavern_stool.json"));

    public static final DeferredBlock<Block> TAVERN_TABLE = BLOCKS.register("tavern_table",
        () -> new ModelShapeFacingBlock(BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion(),
            "assets/marallyzen/models/block/tavern_furniture/tavern_table.json"));

    public static boolean isBlocksTabBlock(BlockState state) {
        return state.is(BANK_SIGN.get())
            || state.is(BAR_SIGN.get())
            || state.is(BARREL_FULL.get())
            || state.is(BARREL_FULL_PILE.get())
            || state.is(COACH.get())
            || state.is(LARGE_CACTUS_POT.get())
            || state.is(MEDIUM_CACTUS_POT.get())
            || state.is(MINI_CACTUS_POT.get())
            || state.is(WEST_TABLE_BAR.get())
            || state.is(WEST_CHAIR_BAR.get())
            || state.is(WOODEN_BUCKET.get())
            || state.is(DRYING_FISH_RACK.get())
            || state.is(FISH.get())
            || state.is(FISHING_NET_WALL_DECORATION.get())
            || state.is(FISHING_ROD.get())
            || state.is(FISHING_ROD_RACK.get())
            || state.is(FISH_BOX.get())
            || state.is(FISH_BOX_EMPTY.get())
            || state.is(FISH_PILE.get())
            || state.is(FISH_PRIZE_WALL_DECORATION.get())
            || state.is(LEANING_FISHING_ROD.get())
            || state.is(TAVERN_BENCH.get())
            || state.is(TAVERN_BIG_KEG.get())
            || state.is(TAVERN_BIG_KEG2.get())
            || state.is(TAVERN_BIG_TABLE.get())
            || state.is(TAVERN_KEG.get())
            || state.is(TAVERN_KEG2.get())
            || state.is(TAVERN_KEG_SUPPORT.get())
            || state.is(TAVERN_KEG_SUPPORT_DOUBLE.get())
            || state.is(TAVERN_MULTIPLE_BOTTLES.get())
            || state.is(TAVERN_MURAL_SHELF.get())
            || state.is(TAVERN_PILE_BOTTLES.get())
            || state.is(TAVERN_RED_BOTTLE.get())
            || state.is(TAVERN_SMALL_GREEN_BOTTLE.get())
            || state.is(TAVERN_STOOL.get())
            || state.is(TAVERN_TABLE.get())
            || state.is(TEST_BLOCK.get());
    }
}
