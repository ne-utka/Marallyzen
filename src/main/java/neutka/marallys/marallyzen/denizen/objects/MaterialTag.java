package neutka.marallys.marallyzen.denizen.objects;

import com.denizenscript.denizencore.objects.Fetchable;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ObjectTagProcessor;
import com.denizenscript.denizencore.tags.TagContext;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

public class MaterialTag implements ObjectTag {
    private final Item item;

    public MaterialTag(Item item) {
        this.item = item;
    }

    public Item getItem() {
        return item;
    }

    @Override
    public String getPrefix() {
        return "material";
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
        return "m@" + identifySimple();
    }

    @Override
    public String identifySimple() {
        Identifier key = BuiltInRegistries.ITEM.getKey(item);
        if (key == null) {
            return "unknown";
        }
        if ("minecraft".equals(key.getNamespace())) {
            return key.getPath();
        }
        return key.toString();
    }

    @Override
    public Object getJavaObject() {
        return item;
    }

    @Override
    public ObjectTag getObjectAttribute(Attribute attribute) {
        return tagProcessor.getObjectAttribute(this, attribute);
    }

    @Fetchable("m")
    public static MaterialTag valueOf(String string, TagContext context) {
        if (string == null) {
            return null;
        }
        if (string.startsWith("m@")) {
            string = string.substring("m@".length());
        }
        Identifier key = Identifier.tryParse(string);
        if (key == null) {
            key = Identifier.tryBuild("minecraft", string);
        }
        Item item = BuiltInRegistries.ITEM.getValue(key);
        if (item == null) {
            return null;
        }
        return new MaterialTag(item);
    }

    public static boolean matches(String string) {
        return string != null;
    }

    public static ObjectTagProcessor<MaterialTag> tagProcessor = new ObjectTagProcessor<>();

    public static void register() {
        tagProcessor.registerTag(ElementTag.class, "name", (attribute, object) -> {
            return new ElementTag(object.identifySimple(), true);
        });
        tagProcessor.registerTag(ElementTag.class, "translated_name", (attribute, object) -> {
            return new ElementTag(new net.minecraft.world.item.ItemStack(object.item).getHoverName().getString(), true);
        });
    }
}


