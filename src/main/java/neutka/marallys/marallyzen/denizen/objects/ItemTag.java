package neutka.marallys.marallyzen.denizen.objects;

import com.denizenscript.denizencore.objects.Fetchable;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ObjectTagProcessor;
import com.denizenscript.denizencore.tags.TagContext;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemTag implements ObjectTag {
    private final ItemStack stack;

    public ItemTag(ItemStack stack) {
        this.stack = stack == null ? ItemStack.EMPTY : stack.copy();
    }

    public ItemStack getStack() {
        return stack;
    }

    @Override
    public String getPrefix() {
        return "item";
    }

    @Override
    public ObjectTag setPrefix(String prefix) {
        return this;
    }

    @Override
    public boolean isUnique() {
        return false;
    }

    @Override
    public String identify() {
        return "i@" + identifySimple();
    }

    @Override
    public String identifySimple() {
        Item item = stack.getItem();
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
        return key != null ? key.toString() : "unknown";
    }

    @Override
    public Object getJavaObject() {
        return stack;
    }

    @Override
    public ObjectTag getObjectAttribute(Attribute attribute) {
        return tagProcessor.getObjectAttribute(this, attribute);
    }

    @Fetchable("i")
    public static ItemTag valueOf(String string, TagContext context) {
        if (string == null) {
            return null;
        }
        if (string.startsWith("i@")) {
            string = string.substring("i@".length());
        }
        ResourceLocation key = ResourceLocation.tryParse(string);
        if (key == null) {
            key = ResourceLocation.tryBuild("minecraft", string);
        }
        Item item = BuiltInRegistries.ITEM.get(key);
        if (item == null) {
            return null;
        }
        return new ItemTag(new ItemStack(item));
    }

    public static boolean matches(String string) {
        return string != null;
    }

    public static ObjectTagProcessor<ItemTag> tagProcessor = new ObjectTagProcessor<>();

    public static void register() {
        tagProcessor.registerTag(MaterialTag.class, "material", (attribute, object) -> {
            return new MaterialTag(object.stack.getItem());
        });
        tagProcessor.registerTag(ElementTag.class, "display", (attribute, object) -> {
            var customName = object.stack.get(DataComponents.CUSTOM_NAME);
            if (customName != null) {
                return new ElementTag(customName.getString(), true);
            }
            return null;
        });
        tagProcessor.registerTag(ElementTag.class, "book_title", (attribute, object) -> {
            var bookContent = object.stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
            if (bookContent != null) {
                var titleFilterable = bookContent.title();
                if (titleFilterable != null && titleFilterable.raw() != null) {
                    return new ElementTag(titleFilterable.raw(), true);
                }
            }
            return null;
        });
        tagProcessor.registerTag(ListTag.class, "inventory_contents", (attribute, object) -> {
            return new ListTag();
        });
        tagProcessor.registerTag(ListTag.class, "skull_skin", (attribute, object) -> {
            return new ListTag();
        });
    }
}
