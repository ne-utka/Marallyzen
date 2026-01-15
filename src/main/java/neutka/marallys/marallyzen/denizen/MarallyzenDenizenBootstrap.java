package neutka.marallys.marallyzen.denizen;

import com.denizenscript.denizencore.objects.ObjectFetcher;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.MapTag;
import com.denizenscript.denizencore.scripts.ScriptRegistry;
import com.denizenscript.denizencore.scripts.containers.core.WorldScriptContainer;
import com.denizenscript.denizencore.tags.TagManager;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import neutka.marallys.marallyzen.MarallyzenScriptEntryData;
import neutka.marallys.marallyzen.denizen.commands.CommandScriptContainer;
import neutka.marallys.marallyzen.denizen.scripts.AssignmentScriptContainer;
import neutka.marallys.marallyzen.denizen.scripts.InteractScriptContainer;
import neutka.marallys.marallyzen.denizen.objects.EntityTag;
import neutka.marallys.marallyzen.denizen.objects.ItemTag;
import neutka.marallys.marallyzen.denizen.objects.MaterialTag;
import neutka.marallys.marallyzen.denizen.objects.PlayerTag;
import neutka.marallys.marallyzen.denizen.objects.ServerTag;
import neutka.marallys.marallyzen.objects.LocationTag;
import neutka.marallys.marallyzen.objects.WorldTag;

public final class MarallyzenDenizenBootstrap {
    private MarallyzenDenizenBootstrap() {
    }

    public static void init() {
        ScriptRegistry._registerType("command", CommandScriptContainer.class);
        ScriptRegistry._registerType("world", WorldScriptContainer.class);
        ScriptRegistry._registerType("assignment", AssignmentScriptContainer.class);
        ScriptRegistry._registerType("interact", InteractScriptContainer.class);

        ObjectFetcher.registerWithObjectFetcher(PlayerTag.class, PlayerTag.tagProcessor);
        ObjectFetcher.registerWithObjectFetcher(EntityTag.class, EntityTag.tagProcessor);
        ObjectFetcher.registerWithObjectFetcher(ServerTag.class, ServerTag.tagProcessor);
        ObjectFetcher.registerWithObjectFetcher(LocationTag.class, LocationTag.tagProcessor);
        ObjectFetcher.registerWithObjectFetcher(ItemTag.class, ItemTag.tagProcessor);
        ObjectFetcher.registerWithObjectFetcher(MaterialTag.class, MaterialTag.tagProcessor);
        ObjectFetcher.registerWithObjectFetcher(WorldTag.class, WorldTag.tagProcessor);

        PlayerTag.register();
        EntityTag.register();
        ServerTag.register();
        LocationTag.register();
        ItemTag.register();
        MaterialTag.register();
        WorldTag.register();

        TagManager.registerTagHandler(PlayerTag.class, "player", (attribute) -> {
            if (attribute.hasParam()) {
                return PlayerTag.valueOf(attribute.getParam(), attribute.context);
            }
            if (attribute.context == null) {
                return null;
            }
            var entryData = attribute.context.getScriptEntryData();
            if (entryData instanceof MarallyzenScriptEntryData marallyzenData) {
                return marallyzenData.getPlayer();
            }
            return null;
        });

        TagManager.registerTagHandler(ServerTag.class, "server", (attribute) -> ServerTag.get());
        TagManager.registerTagHandler(ElementTag.class, "placeholder", (attribute) -> {
            return new ElementTag("", true);
        });

        registerColorTags();

        ElementTag.tagProcessor.registerTag(ElementTag.class, MapTag.class, "color_gradient", (attribute, object, input) -> {
            return object;
        });

        ElementTag.tagProcessor.registerTag(ElementTag.class, "custom_color", (attribute, object) -> {
            return object;
        });

        ElementTag.tagProcessor.registerTag(ElementTag.class, ElementTag.class, "on_click", (attribute, object, input) -> {
            return object;
        });

        ElementTag.tagProcessor.registerTag(ElementTag.class, ElementTag.class, "on_hover", (attribute, object, input) -> {
            return object;
        });

        ElementTag.tagProcessor.registerTag(ElementTag.class, ElementTag.class, "type", (attribute, object, input) -> {
            return object;
        });

        ElementTag.tagProcessor.registerTag(ElementTag.class, PlayerTag.class, "player", (attribute, object, input) -> {
            return object;
        });
    }

    private static void registerColorTags() {
        String codes = "0123456789abcdefklmnor";
        for (int i = 0; i < codes.length(); i++) {
            char code = codes.charAt(i);
            String tag = "&" + code;
            TagManager.registerTagHandler(ElementTag.class, tag, (attribute) -> new ElementTag("&" + code, true));
        }
        TagManager.registerTagHandler(ElementTag.class, "&color", (attribute) -> {
            if (attribute.hasParam()) {
                return new ElementTag("<&color[" + attribute.getParam() + "]>", true);
            }
            return new ElementTag("", true);
        });
    }
}
