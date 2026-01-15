package neutka.marallys.marallyzen.denizen.commands;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.ArgumentHelper;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.tags.TagManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.denizen.text.DenizenTextFormatter;

public class AnnounceCommand extends AbstractCommand {

    public AnnounceCommand() {
        setName("announce");
        setSyntax("announce (to_console) [<text>]");
        setRequiredArguments(1, 2);
        isProcedural = true;
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        for (Argument arg : ArgumentHelper.interpret(scriptEntry, scriptEntry.getOriginalArguments())) {
            if (!scriptEntry.hasObject("to_console") && arg.matches("to_console")) {
                scriptEntry.addObject("to_console", arg.asElement());
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
        boolean toConsole = scriptEntry.hasObject("to_console");
        if (toConsole) {
            Marallyzen.LOGGER.info("[Announce] {}", message.getString());
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(message);
        }
    }
}
