package neutka.marallys.marallyzen.denizen.commands;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.ArgumentHelper;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.tags.TagManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import neutka.marallys.marallyzen.denizen.objects.PlayerTag;

public class AnimateCommand extends AbstractCommand {

    public AnimateCommand() {
        setName("animate");
        setSyntax("animate animation:<name> (for:<player>)");
        setRequiredArguments(1, 2);
        isProcedural = false;
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        for (Argument arg : ArgumentHelper.interpret(scriptEntry, scriptEntry.getOriginalArguments())) {
            if (!scriptEntry.hasObject("animation") && arg.matchesPrefix("animation", "anim")) {
                scriptEntry.addObject("animation", arg.getRawElement());
            }
            else if (!scriptEntry.hasObject("player") && arg.matchesPrefix("for", "player")) {
                scriptEntry.addObject("player", TagManager.tagObject(arg.getValue(), scriptEntry.getContext()));
            }
            else {
                arg.reportUnhandled();
            }
        }
        if (!scriptEntry.hasObject("animation")) {
            throw new InvalidArgumentsException("Missing animation.");
        }
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {
        String animation = scriptEntry.getElement("animation").asString();
        Object playerObj = scriptEntry.getObject("player");
        if (!(playerObj instanceof PlayerTag playerTag)) {
            return;
        }
        ServerPlayer player = playerTag.getPlayer();
        if ("arm_swing".equalsIgnoreCase(animation)) {
            player.swing(InteractionHand.MAIN_HAND);
        }
    }
}
