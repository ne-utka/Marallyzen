package neutka.marallys.marallyzen.items;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.items.InteractiveChainItem;

import java.util.function.Supplier;

public class MarallyzenItems {
    // DeferredRegister for items
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Marallyzen.MODID);
    
    // DeferredRegister for creative mode tabs
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = 
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Marallyzen.MODID);
    
    // Register all DMC items
    public static final DeferredItem<Item> CORD = ITEMS.registerSimpleItem("cord");
    public static final DeferredItem<Item> LOCATOR = ITEMS.registerSimpleItem("locator");
    public static final DeferredItem<Item> MAGNIFYING_GLASS = ITEMS.registerSimpleItem("magnifying_glass");
    public static final DeferredItem<Item> FLASHLIGHT = ITEMS.register("flashlight", () -> new FlashlightItem(new Item.Properties()));
    public static final DeferredItem<Item> CROWBAR = ITEMS.registerSimpleItem("crowbar");
    public static final DeferredItem<Item> CUTTERS = ITEMS.registerSimpleItem("cutters");
    public static final DeferredItem<Item> PIPE_WRENCH = ITEMS.registerSimpleItem("pipe_wrench");
    public static final DeferredItem<Item> WRENCH = ITEMS.registerSimpleItem("wrench");
    public static final DeferredItem<Item> CELLPHONE = ITEMS.registerSimpleItem("cellphone");
    public static final DeferredItem<Item> PUMPGUN = ITEMS.registerSimpleItem("pumpgun");
    
    // Poster block items
    @SuppressWarnings("unchecked")
    public static final DeferredItem<Item> POSTER_1 = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.POSTER_1);
    @SuppressWarnings("unchecked")
    public static final DeferredItem<Item> POSTER_2 = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.POSTER_2);
    @SuppressWarnings("unchecked")
    public static final DeferredItem<Item> POSTER_3 = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.POSTER_3);
    @SuppressWarnings("unchecked")
    public static final DeferredItem<Item> POSTER_4 = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.POSTER_4);
    @SuppressWarnings("unchecked")
    public static final DeferredItem<Item> POSTER_5 = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.POSTER_5);
    @SuppressWarnings("unchecked")
    public static final DeferredItem<Item> POSTER_6 = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.POSTER_6);
    @SuppressWarnings("unchecked")
    public static final DeferredItem<Item> POSTER_7 = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.POSTER_7);
    @SuppressWarnings("unchecked")
    public static final DeferredItem<Item> POSTER_8 = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.POSTER_8);
    @SuppressWarnings("unchecked")
    public static final DeferredItem<Item> POSTER_9 = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.POSTER_9);
    @SuppressWarnings("unchecked")
    public static final DeferredItem<Item> POSTER_10 = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.POSTER_10);
    @SuppressWarnings("unchecked")
    public static final DeferredItem<Item> OLD_POSTER = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.OLD_POSTER);
    @SuppressWarnings("unchecked")
    public static final DeferredItem<Item> PAPER_POSTER_1 = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.PAPER_POSTER_1);
    @SuppressWarnings("unchecked")
    public static final DeferredItem<Item> PAPER_POSTER_2 = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.PAPER_POSTER_2);
    @SuppressWarnings("unchecked")
    public static final DeferredItem<Item> OLD_LAPTOP = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.OLD_LAPTOP);
    @SuppressWarnings("unchecked")
    public static final DeferredItem<Item> RADIO = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.RADIO);
    @SuppressWarnings("unchecked")
    public static final DeferredItem<Item> OLD_TV = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.OLD_TV);
    @SuppressWarnings("unchecked")
    public static final DeferredItem<Item> MIRROR = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.MIRROR);
    @SuppressWarnings("unchecked")
    public static final DeferredItem<Item> NAILED_PLANKS = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.NAILED_PLANKS);
    @SuppressWarnings("unchecked")
    public static final DeferredItem<Item> STACK_OF_NAILED_PLANKS = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.STACK_OF_NAILED_PLANKS);
    @SuppressWarnings("unchecked")
    public static final DeferredItem<Item> TEST_BLOCK = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.TEST_BLOCK);
    @SuppressWarnings("unchecked")
    public static final DeferredItem<Item> VIDEO_CAMERA = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.VIDEO_CAMERA);
    @SuppressWarnings("unchecked")
    public static final DeferredItem<Item> DICTAPHONE_SIMPLE = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.DICTAPHONE_SIMPLE);

    public static final DeferredItem<Item> BANK_SIGN = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.BANK_SIGN);

    public static final DeferredItem<Item> BAR_SIGN = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.BAR_SIGN);

    public static final DeferredItem<Item> BARREL_FULL = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.BARREL_FULL);

    public static final DeferredItem<Item> BARREL_FULL_PILE = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.BARREL_FULL_PILE);

    public static final DeferredItem<Item> COACH = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.COACH);

    public static final DeferredItem<Item> LARGE_CACTUS_POT = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.LARGE_CACTUS_POT);

    public static final DeferredItem<Item> MEDIUM_CACTUS_POT = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.MEDIUM_CACTUS_POT);

    public static final DeferredItem<Item> MINI_CACTUS_POT = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.MINI_CACTUS_POT);

    public static final DeferredItem<Item> WEST_TABLE_BAR = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.WEST_TABLE_BAR);

    public static final DeferredItem<Item> WEST_CHAIR_BAR = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.WEST_CHAIR_BAR);

    public static final DeferredItem<Item> WOODEN_BUCKET = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.WOODEN_BUCKET);
    public static final DeferredItem<Item> DRYING_FISH_RACK = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.DRYING_FISH_RACK);
    public static final DeferredItem<Item> FISH = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.FISH);
    public static final DeferredItem<Item> FISHING_NET_WALL_DECORATION = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.FISHING_NET_WALL_DECORATION);
    public static final DeferredItem<Item> FISHING_ROD = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.FISHING_ROD);
    public static final DeferredItem<Item> FISHING_ROD_RACK = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.FISHING_ROD_RACK);
    public static final DeferredItem<Item> FISH_BOX = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.FISH_BOX);
    public static final DeferredItem<Item> FISH_BOX_EMPTY = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.FISH_BOX_EMPTY);
    public static final DeferredItem<Item> FISH_PILE = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.FISH_PILE);
    public static final DeferredItem<Item> FISH_PRIZE_WALL_DECORATION = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.FISH_PRIZE_WALL_DECORATION);
    public static final DeferredItem<Item> LEANING_FISHING_ROD = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.LEANING_FISHING_ROD);
    public static final DeferredItem<Item> TAVERN_BENCH = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.TAVERN_BENCH);
    public static final DeferredItem<Item> TAVERN_BIG_KEG = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.TAVERN_BIG_KEG);
    public static final DeferredItem<Item> TAVERN_BIG_KEG2 = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.TAVERN_BIG_KEG2);
    public static final DeferredItem<Item> TAVERN_BIG_TABLE = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.TAVERN_BIG_TABLE);
    public static final DeferredItem<Item> TAVERN_KEG = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.TAVERN_KEG);
    public static final DeferredItem<Item> TAVERN_KEG2 = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.TAVERN_KEG2);
    public static final DeferredItem<Item> TAVERN_KEG_SUPPORT = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.TAVERN_KEG_SUPPORT);
    public static final DeferredItem<Item> TAVERN_KEG_SUPPORT_DOUBLE = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.TAVERN_KEG_SUPPORT_DOUBLE);
    public static final DeferredItem<Item> TAVERN_MULTIPLE_BOTTLES = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.TAVERN_MULTIPLE_BOTTLES);
    public static final DeferredItem<Item> TAVERN_MURAL_SHELF = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.TAVERN_MURAL_SHELF);
    public static final DeferredItem<Item> TAVERN_PILE_BOTTLES = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.TAVERN_PILE_BOTTLES);
    public static final DeferredItem<Item> TAVERN_RED_BOTTLE = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.TAVERN_RED_BOTTLE);
    public static final DeferredItem<Item> TAVERN_SMALL_GREEN_BOTTLE = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.TAVERN_SMALL_GREEN_BOTTLE);
    public static final DeferredItem<Item> TAVERN_STOOL = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.TAVERN_STOOL);
    public static final DeferredItem<Item> TAVERN_TABLE = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.TAVERN_TABLE);
    public static final DeferredItem<Item> INTERACTIVE_CHAIN = ITEMS.register(
        "interactive_chain",
        () -> new InteractiveChainItem(neutka.marallys.marallyzen.blocks.MarallyzenBlocks.INTERACTIVE_CHAIN.get(), new Item.Properties())
    );

    // Creative Mode Tab for DMC items
    public static final Supplier<CreativeModeTab> MARALLYZEN_TAB = CREATIVE_MODE_TABS.register(
        "marallyzen_tab",
        () -> CreativeModeTab.builder()
            .icon(() -> new ItemStack(CORD.get()))
            .title(net.minecraft.network.chat.Component.translatable("itemGroup.marallyzen.items"))
            .displayItems((parameters, output) -> {
                // Weapons
                
                // Instruments/Tools
                output.accept(CORD.get());
                output.accept(LOCATOR.get());
                output.accept(MAGNIFYING_GLASS.get());
                output.accept(CROWBAR.get());
                output.accept(CUTTERS.get());
                output.accept(PIPE_WRENCH.get());
                output.accept(WRENCH.get());
                output.accept(PUMPGUN.get());
                output.accept(VIDEO_CAMERA.get());
                output.accept(CELLPHONE.get());
                
                // Items
                // Фонарик всегда выключен в креатив табе
                ItemStack flashlightStack = new ItemStack(FLASHLIGHT.get());
                FlashlightItem.setOn(flashlightStack, false);
                output.accept(flashlightStack);
                
                // Posters
                output.accept(POSTER_1.get());
                output.accept(POSTER_2.get());
                output.accept(POSTER_3.get());
                output.accept(POSTER_4.get());
                output.accept(POSTER_5.get());
                output.accept(POSTER_6.get());
                output.accept(POSTER_7.get());
                output.accept(POSTER_8.get());
                output.accept(POSTER_9.get());
                output.accept(POSTER_10.get());
                output.accept(OLD_POSTER.get());
                output.accept(PAPER_POSTER_1.get());
                output.accept(PAPER_POSTER_2.get());
                output.accept(OLD_LAPTOP.get());
                output.accept(RADIO.get());
                output.accept(OLD_TV.get());
                output.accept(MIRROR.get());
                output.accept(NAILED_PLANKS.get());
                output.accept(STACK_OF_NAILED_PLANKS.get());
                output.accept(VIDEO_CAMERA.get());
                output.accept(DICTAPHONE_SIMPLE.get());
                output.accept(INTERACTIVE_CHAIN.get());
            })
            .build()
    );

    public static final Supplier<CreativeModeTab> MARALLYZEN_BLOCKS_TAB = CREATIVE_MODE_TABS.register(
        "marallyzen_blocks_tab",
        () -> CreativeModeTab.builder()
            .icon(() -> new ItemStack(POSTER_1.get()))
            .title(net.minecraft.network.chat.Component.translatable("itemGroup.marallyzen.blocks"))
            .displayItems((parameters, output) -> {
                output.accept(BANK_SIGN.get());
                output.accept(BAR_SIGN.get());
                output.accept(BARREL_FULL.get());
                output.accept(BARREL_FULL_PILE.get());
                output.accept(COACH.get());
                output.accept(LARGE_CACTUS_POT.get());
                output.accept(MEDIUM_CACTUS_POT.get());
                output.accept(MINI_CACTUS_POT.get());
                output.accept(WEST_TABLE_BAR.get());
                output.accept(WEST_CHAIR_BAR.get());
                output.accept(WOODEN_BUCKET.get());
                output.accept(TEST_BLOCK.get());
                output.accept(DRYING_FISH_RACK.get());
                output.accept(FISH.get());
                output.accept(FISHING_NET_WALL_DECORATION.get());
                output.accept(FISHING_ROD.get());
                output.accept(FISHING_ROD_RACK.get());
                output.accept(FISH_BOX.get());
                output.accept(FISH_BOX_EMPTY.get());
                output.accept(FISH_PILE.get());
                output.accept(FISH_PRIZE_WALL_DECORATION.get());
                output.accept(LEANING_FISHING_ROD.get());
                output.accept(TAVERN_BENCH.get());
                output.accept(TAVERN_BIG_KEG.get());
                output.accept(TAVERN_BIG_KEG2.get());
                output.accept(TAVERN_BIG_TABLE.get());
                output.accept(TAVERN_KEG.get());
                output.accept(TAVERN_KEG2.get());
                output.accept(TAVERN_KEG_SUPPORT.get());
                output.accept(TAVERN_KEG_SUPPORT_DOUBLE.get());
                output.accept(TAVERN_MULTIPLE_BOTTLES.get());
                output.accept(TAVERN_MURAL_SHELF.get());
                output.accept(TAVERN_PILE_BOTTLES.get());
                output.accept(TAVERN_RED_BOTTLE.get());
                output.accept(TAVERN_SMALL_GREEN_BOTTLE.get());
                output.accept(TAVERN_STOOL.get());
                output.accept(TAVERN_TABLE.get());
            })
            .build()
    );
    
}

