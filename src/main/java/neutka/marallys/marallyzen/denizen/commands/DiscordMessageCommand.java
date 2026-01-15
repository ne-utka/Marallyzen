package neutka.marallys.marallyzen.denizen.commands;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.ArgumentHelper;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import neutka.marallys.marallyzen.Marallyzen;

public class DiscordMessageCommand extends AbstractCommand {

    public DiscordMessageCommand() {
        setName("discordmessage");
        setSyntax("discordmessage (id:<text>) (channel:<text>) [<text>]");
        setRequiredArguments(0, 3);
        isProcedural = true;
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        for (Argument arg : ArgumentHelper.interpret(scriptEntry, scriptEntry.getOriginalArguments())) {
            if (!scriptEntry.hasObject("id") && arg.matchesPrefix("id")) {
                scriptEntry.addObject("id", arg.getRawElement());
            }
            else if (!scriptEntry.hasObject("channel") && arg.matchesPrefix("channel")) {
                scriptEntry.addObject("channel", arg.getRawElement());
            }
            else if (!scriptEntry.hasObject("text")) {
                scriptEntry.addObject("text", arg.getRawElement());
            }
            else {
                arg.reportUnhandled();
            }
        }
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {
        String id = scriptEntry.hasObject("id") ? scriptEntry.getElement("id").asString() : "";
        String channel = scriptEntry.hasObject("channel") ? scriptEntry.getElement("channel").asString() : "";
        String text = scriptEntry.hasObject("text") ? scriptEntry.getElement("text").asString() : "";
        Marallyzen.LOGGER.info("discordmessage (noop): id={} channel={} text={}", id, channel, text);
    }
}
