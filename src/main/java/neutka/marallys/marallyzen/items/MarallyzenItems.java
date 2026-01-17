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
    public static final DeferredItem<Item> VIDEO_CAMERA = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.VIDEO_CAMERA);
    @SuppressWarnings("unchecked")
    public static final DeferredItem<Item> DICTAPHONE_SIMPLE = (DeferredItem<Item>) (Object) ITEMS.registerSimpleBlockItem(
        neutka.marallys.marallyzen.blocks.MarallyzenBlocks.DICTAPHONE_SIMPLE);
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
    
}

