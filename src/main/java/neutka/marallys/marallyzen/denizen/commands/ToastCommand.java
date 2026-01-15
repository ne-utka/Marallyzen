package neutka.marallys.marallyzen.denizen.commands;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.ArgumentHelper;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.tags.TagManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import neutka.marallys.marallyzen.MarallyzenScriptEntryData;
import neutka.marallys.marallyzen.denizen.objects.PlayerTag;
import neutka.marallys.marallyzen.denizen.text.DenizenTextFormatter;

import java.util.List;

public class ToastCommand extends AbstractCommand {

    public ToastCommand() {
        setName("toast");
        setSyntax("toast (targets:<player>|...) [<text>] (frame:<text>) (icon:<text>)");
        setRequiredArguments(1, 4);
        isProcedural = true;
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        for (Argument arg : ArgumentHelper.interpret(scriptEntry, scriptEntry.getOriginalArguments())) {
            if (!scriptEntry.hasObject("targets") && arg.matchesPrefix("targets", "target", "t")) {
                scriptEntry.addObject("targets", ListTag.getListFor(TagManager.tagObject(arg.getValue(), scriptEntry.getContext()), scriptEntry.getContext())
                        .filter(PlayerTag.class, scriptEntry));
            }
            else if (!scriptEntry.hasObject("frame") && arg.matchesPrefix("frame")) {
                scriptEntry.addObject("frame", arg.getRawElement());
            }
            else if (!scriptEntry.hasObject("icon") && arg.matchesPrefix("icon")) {
                scriptEntry.addObject("icon", arg.getRawElement());
            }
            else if (!scriptEntry.hasObject("text")) {
                scriptEntry.addObject("text", arg.getRawElement());
            }
            else {
                arg.reportUnhandled();
            }
        }
        if (!scriptEntry.hasObject("text")) {
            throw new InvalidArgumentsException("Missing text.");
        }
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {
        String raw = scriptEntry.getElement("text").asString();
        String tagged = TagManager.tag(raw, scriptEntry.getContext());
        Component message = DenizenTextFormatter.format(tagged);
        List<PlayerTag> targets = (List<PlayerTag>) scriptEntry.getObject("targets");
        if (targets == null || targets.isEmpty()) {
            var data = scriptEntry.entryData instanceof MarallyzenScriptEntryData d ? d : null;
            if (data != null && data.getPlayer() != null) {
                targets = List.of(data.getPlayer());
            }
        }
        if (targets == null || targets.isEmpty()) {
            return;
        }
        for (PlayerTag target : targets) {
            ServerPlayer player = target.getPlayer();
            player.sendSystemMessage(message);
        }
    }
}
