package neutka.marallys.marallyzen.denizen.commands;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.ArgumentHelper;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.tags.TagManager;
import net.minecraft.server.level.ServerPlayer;
import neutka.marallys.marallyzen.MarallyzenScriptEntryData;
import neutka.marallys.marallyzen.denizen.objects.PlayerTag;
import neutka.marallys.marallyzen.denizen.text.DenizenTextFormatter;

import java.util.List;

public class NarrateCommand extends AbstractCommand {

    public NarrateCommand() {
        setName("narrate");
        setSyntax("narrate [<text>] (targets:<player>|...) (fadein:<ticks>) (stay:<ticks>|duration:<ticks>) (fadeout:<ticks>)");
        setRequiredArguments(1, 6);
        isProcedural = true;
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        for (Argument arg : ArgumentHelper.interpret(scriptEntry, scriptEntry.getOriginalArguments())) {
            if (!scriptEntry.hasObject("targets")
                && arg.matchesPrefix("targets", "target", "t")) {
                scriptEntry.addObject("targets", ListTag.getListFor(TagManager.tagObject(arg.getValue(), scriptEntry.getContext()), scriptEntry.getContext())
                        .filter(PlayerTag.class, scriptEntry));
            }
            else if (!scriptEntry.hasObject("fadein") && arg.matchesPrefix("fadein")) {
                scriptEntry.addObject("fadein", arg.asElement());
            }
            else if (!scriptEntry.hasObject("stay") && (arg.matchesPrefix("stay") || arg.matchesPrefix("duration"))) {
                scriptEntry.addObject("stay", arg.asElement());
            }
            else if (!scriptEntry.hasObject("fadeout") && arg.matchesPrefix("fadeout")) {
                scriptEntry.addObject("fadeout", arg.asElement());
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
        var message = DenizenTextFormatter.format(tagged);
        int fadeIn = scriptEntry.hasObject("fadein") ? scriptEntry.getElement("fadein").asInt() : 5;
        int stay = scriptEntry.hasObject("stay") ? scriptEntry.getElement("stay").asInt() : 60;
        int fadeOut = scriptEntry.hasObject("fadeout") ? scriptEntry.getElement("fadeout").asInt() : 3;
        for (PlayerTag target : targets) {
            ServerPlayer player = target.getPlayer();
            neutka.marallys.marallyzen.network.NetworkHelper.sendToPlayer(
                    player,
                    new neutka.marallys.marallyzen.network.NarratePacket(message, null, fadeIn, stay, fadeOut)
            );
        }
    }
}
