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
    public static final DeferredBlock<PosterBlock> POSTER_1 = BLOCKS.registerBlock("poster1",
        props -> new PosterBlock(1, props),
        BlockBehaviour.Properties.of()
            .strength(0.5f)
            .noOcclusion());
    
    public static final DeferredBlock<PosterBlock> POSTER_2 = BLOCKS.registerBlock("poster2",
        props -> new PosterBlock(2, props),
        BlockBehaviour.Properties.of()
            .strength(0.5f)
            .noOcclusion());
    
    public static final DeferredBlock<PosterBlock> POSTER_3 = BLOCKS.registerBlock("poster3",
        props -> new PosterBlock(3, props),
        BlockBehaviour.Properties.of()
            .strength(0.5f)
            .noOcclusion());
    
    public static final DeferredBlock<PosterBlock> POSTER_4 = BLOCKS.registerBlock("poster4",
        props -> new PosterBlock(4, props),
        BlockBehaviour.Properties.of()
            .strength(0.5f)
            .noOcclusion());
    
    public static final DeferredBlock<PosterBlock> POSTER_5 = BLOCKS.registerBlock("poster5",
        props -> new PosterBlock(5, props),
        BlockBehaviour.Properties.of()
            .strength(0.5f)
            .noOcclusion());
    
    public static final DeferredBlock<PosterBlock> POSTER_6 = BLOCKS.registerBlock("poster6",
        props -> new PosterBlock(6, props),
        BlockBehaviour.Properties.of()
            .strength(0.5f)
            .noOcclusion());
    
    public static final DeferredBlock<PosterBlock> POSTER_7 = BLOCKS.registerBlock("poster7",
        props -> new PosterBlock(7, props),
        BlockBehaviour.Properties.of()
            .strength(0.5f)
            .noOcclusion());
    
    public static final DeferredBlock<PosterBlock> POSTER_8 = BLOCKS.registerBlock("poster8",
        props -> new PosterBlock(8, props),
        BlockBehaviour.Properties.of()
            .strength(0.5f)
            .noOcclusion());
    
    public static final DeferredBlock<PosterBlock> POSTER_9 = BLOCKS.registerBlock("poster9",
        props -> new PosterBlock(9, props),
        BlockBehaviour.Properties.of()
            .strength(0.5f)
            .noOcclusion());
    
    public static final DeferredBlock<PosterBlock> POSTER_10 = BLOCKS.registerBlock("poster10",
        props -> new PosterBlock(10, props),
        BlockBehaviour.Properties.of()
            .strength(0.5f)
            .noOcclusion());

    // New posters
    public static final DeferredBlock<PosterBlock> OLD_POSTER = BLOCKS.registerBlock("oldposter",
        props -> new PosterBlock(11, props),
        BlockBehaviour.Properties.of()
            .strength(0.5f)
            .noOcclusion());

    public static final DeferredBlock<PosterBlock> PAPER_POSTER_1 = BLOCKS.registerBlock("paperposter1",
        props -> new PosterBlock(12, props),
        BlockBehaviour.Properties.of()
            .strength(0.5f)
            .noOcclusion());

    public static final DeferredBlock<PosterBlock> PAPER_POSTER_2 = BLOCKS.registerBlock("paperposter2",
        props -> new PosterBlock(13, props),
        BlockBehaviour.Properties.of()
            .strength(0.5f)
            .noOcclusion());

    public static final DeferredBlock<Block> OLD_LAPTOP = BLOCKS.registerBlock("old_laptop",
        props -> new ModelShapeFacingBlock(props,
            "assets/marallyzen/models/block/old_laptop.json"),
        BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.METAL)
            .noOcclusion());

    public static final DeferredBlock<Block> RADIO = BLOCKS.registerBlock("radio",
        props -> new ModelShapeFacingBlock(props,
            "assets/marallyzen/models/block/radio.json"),
        BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.METAL)
            .noOcclusion());

    public static final DeferredBlock<Block> OLD_TV = BLOCKS.registerBlock("old_tv",
        props -> new OldTvBlock(props,
            "assets/marallyzen/models/block/old_tv.json"),
        BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.METAL)
            .noOcclusion());

    public static final DeferredBlock<Block> DICTAPHONE = BLOCKS.registerBlock("dictaphone",
        props -> new DictaphoneBlock(props,
            "assets/marallyzen/models/block/dictaphone.json"),
        BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.METAL)
            .noOcclusion());

    public static final DeferredBlock<Block> MIRROR = BLOCKS.registerBlock("mirror",
        props -> new ModelShapeFacingBlock(props,
            "assets/marallyzen/models/block/mirror.json"),
        BlockBehaviour.Properties.of()
            .strength(0.5f)
            .sound(SoundType.GLASS)
            .noOcclusion());

    public static final DeferredBlock<Block> NAILED_PLANKS = BLOCKS.registerBlock("nailed_planks",
        HiddenOutlineBlock::new,
        BlockBehaviour.Properties.of()
            .strength(1.5f)
            .sound(SoundType.WOOD)
            .noOcclusion());

    public static final DeferredBlock<Block> STACK_OF_NAILED_PLANKS = BLOCKS.registerBlock("stack_of_nailed_planks",
        HiddenOutlineBlock::new,
        BlockBehaviour.Properties.of()
            .strength(1.5f)
            .sound(SoundType.WOOD)
            .noOcclusion());

    public static final DeferredBlock<Block> TEST_BLOCK = BLOCKS.registerBlock("test_block",
        Block::new,
        BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.STONE));

    public static final DeferredBlock<Block> VIDEO_CAMERA = BLOCKS.registerBlock("video_camera",
        props -> new ModelShapeFacingBlock(props,
            "assets/marallyzen/models/block/video_camera.json"),
        BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.METAL)
            .noOcclusion());


    public static final DeferredBlock<InteractiveLeverBlock> INTERACTIVE_LEVER = BLOCKS.registerBlock("interactive_lever",
        InteractiveLeverBlock::new,
        BlockBehaviour.Properties.of()
            .strength(0.5f)
            .sound(SoundType.METAL)
            .noOcclusion());

    public static final DeferredBlock<InteractiveValveBlock> INTERACTIVE_VALVE = BLOCKS.registerBlock("interactive_valve",
        InteractiveValveBlock::new,
        BlockBehaviour.Properties.of()
            .strength(0.5f)
            .sound(SoundType.METAL)
            .noOcclusion());

    public static final DeferredBlock<Block> DICTAPHONE_SIMPLE = BLOCKS.registerBlock("dictaphone_simple",
        DictaphoneSimpleBlock::new,
        BlockBehaviour.Properties.of()
            .strength(0.5f)
            .sound(SoundType.METAL)
            .noOcclusion());

    public static final DeferredBlock<Block> BANK_SIGN = BLOCKS.registerBlock("bank_sign",
        props -> new ModelShapeFacingBlock(props,
            "assets/marallyzen/models/block/bank_sign.json"),
        BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion());

    public static final DeferredBlock<Block> BAR_SIGN = BLOCKS.registerBlock("bar_sign",
        props -> new ModelShapeFacingBlock(props,
            "assets/marallyzen/models/block/bar_sign.json"),
        BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion());

    public static final DeferredBlock<Block> BARREL_FULL = BLOCKS.registerBlock("barrel_full",
        props -> new ModelShapeFacingBlock(props,
            "assets/marallyzen/models/block/barrel_full.json"),
        BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion());

    public static final DeferredBlock<Block> BARREL_FULL_PILE = BLOCKS.registerBlock("barrel_full_pile",
        props -> new ModelShapeFacingBlock(props,
            "assets/marallyzen/models/block/barrel_full_pile.json",
            true,
            true),
        BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion());

    public static final DeferredBlock<Block> COACH = BLOCKS.registerBlock("coach",
        props -> new ModelShapeFacingBlock(props,
            "assets/marallyzen/models/block/coach.json",
            true,
            true),
        BlockBehaviour.Properties.of()
            .strength(1.5f)
            .sound(SoundType.WOOD)
            .noOcclusion());

    public static final DeferredBlock<Block> LARGE_CACTUS_POT = BLOCKS.registerBlock("large_cactus_pot",
        props -> new ModelShapeFacingBlock(props,
            "assets/marallyzen/models/block/large_cactus_pot.json"),
        BlockBehaviour.Properties.of()
            .strength(0.8f)
            .sound(SoundType.STONE)
            .noOcclusion());

    public static final DeferredBlock<Block> MEDIUM_CACTUS_POT = BLOCKS.registerBlock("medium_cactus_pot",
        props -> new ModelShapeFacingBlock(props,
            "assets/marallyzen/models/block/medium_cactus_pot.json"),
        BlockBehaviour.Properties.of()
            .strength(0.8f)
            .sound(SoundType.STONE)
            .noOcclusion());

    public static final DeferredBlock<Block> MINI_CACTUS_POT = BLOCKS.registerBlock("mini_cactus_pot",
        props -> new ModelShapeFacingBlock(props,
            "assets/marallyzen/models/block/mini_cactus_pot.json"),
        BlockBehaviour.Properties.of()
            .strength(0.8f)
            .sound(SoundType.STONE)
            .noOcclusion());

    public static final DeferredBlock<Block> WEST_TABLE_BAR = BLOCKS.registerBlock("west_table_bar",
        props -> new ModelShapeFacingBlock(props,
            "assets/marallyzen/models/block/west_table_bar.json"),
        BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion());

    public static final DeferredBlock<Block> WEST_CHAIR_BAR = BLOCKS.registerBlock("west_chair_bar",
        props -> new ModelShapeFacingBlock(props,
            "assets/marallyzen/models/block/west_chair_bar.json"),
        BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion());

    public static final DeferredBlock<Block> WOODEN_BUCKET = BLOCKS.registerBlock("wooden_bucket",
        props -> new ModelShapeFacingBlock(props,
            "assets/marallyzen/models/block/wooden_bucket.json"),
        BlockBehaviour.Properties.of()
            .strength(0.8f)
            .sound(SoundType.WOOD)
            .noOcclusion());

    public static final DeferredBlock<Block> DRYING_FISH_RACK = BLOCKS.registerBlock("drying_fish_rack",
        props -> new ModelShapeFacingBlock(props,
            "assets/marallyzen/models/block/drying_fish_rack.json"),
        BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion());

    public static final DeferredBlock<Block> FISH = BLOCKS.registerBlock("fish",
        props -> new ModelShapeFacingBlock(props,
            "assets/marallyzen/models/block/fish.json"),
        BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion());

    public static final DeferredBlock<Block> FISHING_NET_WALL_DECORATION = BLOCKS.registerBlock("fishing_net_wall_decoration",
        props -> new ModelShapeFacingBlock(props,
            "assets/marallyzen/models/block/fishing_net_wall_decoration.json"),
        BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion());

    public static final DeferredBlock<Block> FISHING_ROD = BLOCKS.registerBlock("fishing_rod",
        props -> new ModelShapeFacingBlock(props,
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
            -0.625),
        BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion());

    public static final DeferredBlock<Block> FISHING_ROD_RACK = BLOCKS.registerBlock("fishing_rod_rack",
        props -> new ModelShapeFacingBlock(props,
            "assets/marallyzen/models/block/fishing_rod_rack.json",
            true,
            false,
            0,
            true,
            0.0,
            0.0,
            false,
            false,
            true),
        BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion());

    public static final DeferredBlock<Block> FISH_BOX = BLOCKS.registerBlock("fish_box",
        props -> new ModelShapeFacingBlock(props,
            "assets/marallyzen/models/block/fish_box.json"),
        BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion());

    public static final DeferredBlock<Block> FISH_BOX_EMPTY = BLOCKS.registerBlock("fish_box_empty",
        props -> new ModelShapeFacingBlock(props,
            "assets/marallyzen/models/block/fish_box_empty.json"),
        BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion());

    public static final DeferredBlock<Block> FISH_PILE = BLOCKS.registerBlock("fish_pile",
        props -> new ModelShapeFacingBlock(props,
            "assets/marallyzen/models/block/fish_pile.json"),
        BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion());

    public static final DeferredBlock<Block> FISH_PRIZE_WALL_DECORATION = BLOCKS.registerBlock("fish_prize_wall_decoration",
        props -> new ModelShapeFacingBlock(props,
            "assets/marallyzen/models/block/fish_prize_wall_decoration.json"),
        BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion());

    public static final DeferredBlock<Block> LEANING_FISHING_ROD = BLOCKS.registerBlock("leaning_fishing_rod",
        props -> new ModelShapeFacingBlock(props,
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
            -0.625),
        BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion());

    public static final DeferredBlock<Block> TAVERN_BENCH = BLOCKS.registerBlock("tavern_bench",
        props -> new ModelShapeFacingBlock(props,
            "assets/marallyzen/models/block/tavern_furniture/tavern_bench.json"),
        BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion());

    public static final DeferredBlock<Block> TAVERN_BIG_KEG = BLOCKS.registerBlock("tavern_big_keg",
        props -> new ModelShapeFacingBlock(props,
            "assets/marallyzen/models/block/tavern_furniture/tavern_big_keg.json"),
        BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion());

    public static final DeferredBlock<Block> TAVERN_BIG_KEG2 = BLOCKS.registerBlock("tavern_big_keg2",
        props -> new ModelShapeFacingBlock(props,
            "assets/marallyzen/models/block/tavern_furniture/tavern_big_keg2.json"),
        BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion());

    public static final DeferredBlock<Block> TAVERN_BIG_TABLE = BLOCKS.registerBlock("tavern_big_table",
        props -> new ModelShapeFacingBlock(props,
            "assets/marallyzen/models/block/tavern_furniture/tavern_big_table.json"),
        BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion());

    public static final DeferredBlock<Block> TAVERN_KEG = BLOCKS.registerBlock("tavern_keg",
        props -> new ModelShapeFacingBlock(props,
            "assets/marallyzen/models/block/tavern_furniture/tavern_keg.json"),
        BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion());

    public static final DeferredBlock<Block> TAVERN_KEG2 = BLOCKS.registerBlock("tavern_keg2",
        props -> new ModelShapeFacingBlock(props,
            "assets/marallyzen/models/block/tavern_furniture/tavern_keg2.json"),
        BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion());

    public static final DeferredBlock<Block> TAVERN_KEG_SUPPORT = BLOCKS.registerBlock("tavern_keg_support",
        props -> new ModelShapeFacingBlock(props,
            "assets/marallyzen/models/block/tavern_furniture/tavern_keg_support.json"),
        BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion());

    public static final DeferredBlock<Block> TAVERN_KEG_SUPPORT_DOUBLE = BLOCKS.registerBlock("tavern_keg_support_double",
        props -> new ModelShapeFacingBlock(props,
            "assets/marallyzen/models/block/tavern_furniture/tavern_keg_support_double.json"),
        BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion());

    public static final DeferredBlock<Block> TAVERN_MULTIPLE_BOTTLES = BLOCKS.registerBlock("tavern_multiple_bottles",
        props -> new ModelShapeFacingBlock(props,
            "assets/marallyzen/models/block/tavern_furniture/tavern_multiple_bottles.json"),
        BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion());

    public static final DeferredBlock<Block> TAVERN_MURAL_SHELF = BLOCKS.registerBlock("tavern_mural_shelf",
        props -> new ModelShapeFacingBlock(props,
            "assets/marallyzen/models/block/tavern_furniture/tavern_mural_shelf.json"),
        BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion());

    public static final DeferredBlock<Block> TAVERN_PILE_BOTTLES = BLOCKS.registerBlock("tavern_pile_bottles",
        props -> new ModelShapeFacingBlock(props,
            "assets/marallyzen/models/block/tavern_furniture/tavern_pile_bottles.json"),
        BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion());

    public static final DeferredBlock<Block> TAVERN_RED_BOTTLE = BLOCKS.registerBlock("tavern_red_bottle",
        props -> new ModelShapeFacingBlock(props,
            "assets/marallyzen/models/block/tavern_furniture/tavern_red_bottle.json"),
        BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion());

    public static final DeferredBlock<Block> TAVERN_SMALL_GREEN_BOTTLE = BLOCKS.registerBlock("tavern_small_green_bottle",
        props -> new ModelShapeFacingBlock(props,
            "assets/marallyzen/models/block/tavern_furniture/tavern_small_green_bottle.json"),
        BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion());

    public static final DeferredBlock<Block> TAVERN_STOOL = BLOCKS.registerBlock("tavern_stool",
        props -> new ModelShapeFacingBlock(props,
            "assets/marallyzen/models/block/tavern_furniture/tavern_stool.json"),
        BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion());

    public static final DeferredBlock<Block> TAVERN_TABLE = BLOCKS.registerBlock("tavern_table",
        props -> new ModelShapeFacingBlock(props,
            "assets/marallyzen/models/block/tavern_furniture/tavern_table.json"),
        BlockBehaviour.Properties.of()
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .noOcclusion());

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
