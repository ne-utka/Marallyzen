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
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import neutka.marallys.marallyzen.denizen.objects.PlayerTag;
import neutka.marallys.marallyzen.denizen.util.DenizenSoundResolver;

import java.util.List;

public class PlaySoundCommand extends AbstractCommand {

    public PlaySoundCommand() {
        setName("playsound");
        setSyntax("playsound [<targets>] sound:<sound> (pitch:<#.#>) (volume:<#.#>)");
        setRequiredArguments(2, 4);
        isProcedural = false;
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        for (Argument arg : ArgumentHelper.interpret(scriptEntry, scriptEntry.getOriginalArguments())) {
            if (!scriptEntry.hasObject("sound") && arg.matchesPrefix("sound")) {
                scriptEntry.addObject("sound", arg.getRawElement());
            }
            else if (!scriptEntry.hasObject("pitch") && arg.matchesPrefix("pitch")) {
                scriptEntry.addObject("pitch", arg.getRawElement());
            }
            else if (!scriptEntry.hasObject("volume") && arg.matchesPrefix("volume")) {
                scriptEntry.addObject("volume", arg.getRawElement());
            }
            else if (!scriptEntry.hasObject("targets")) {
                scriptEntry.addObject("targets", ListTag.getListFor(TagManager.tagObject(arg.getValue(), scriptEntry.getContext()), scriptEntry.getContext())
                        .filter(PlayerTag.class, scriptEntry));
            }
            else {
                arg.reportUnhandled();
            }
        }
        if (!scriptEntry.hasObject("sound")) {
            throw new InvalidArgumentsException("Missing sound.");
        }
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {
        ElementTag soundText = scriptEntry.getElement("sound");
        float pitch = scriptEntry.getElement("pitch") != null ? (float) scriptEntry.getElement("pitch").asDouble() : 1f;
        float volume = scriptEntry.getElement("volume") != null ? (float) scriptEntry.getElement("volume").asDouble() : 1f;
        List<PlayerTag> targets = (List<PlayerTag>) scriptEntry.getObject("targets");
        if (targets == null || targets.isEmpty()) {
            return;
        }
        SoundEvent sound = DenizenSoundResolver.resolve(soundText.asString());
        if (sound == null) {
            return;
        }
        for (PlayerTag target : targets) {
            ServerPlayer player = target.getPlayer();
            player.playNotifySound(sound, SoundSource.PLAYERS, volume, pitch);
        }
    }
}
