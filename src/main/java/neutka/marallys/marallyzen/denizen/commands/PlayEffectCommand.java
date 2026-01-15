package neutka.marallys.marallyzen.denizen.commands;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.ArgumentHelper;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.tags.TagManager;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;
import neutka.marallys.marallyzen.denizen.objects.PlayerTag;
import neutka.marallys.marallyzen.objects.LocationTag;

import java.util.List;

public class PlayEffectCommand extends AbstractCommand {

    public PlayEffectCommand() {
        setName("playeffect");
        setSyntax("playeffect (targets:<player>|...) effect:<effect> (quantity:<#>) (data:<#.#>) (offset:<#.#>) (at:<location>) (special_data:<text>)");
        setRequiredArguments(1, 7);
        isProcedural = false;
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        for (Argument arg : ArgumentHelper.interpret(scriptEntry, scriptEntry.getOriginalArguments())) {
            if (!scriptEntry.hasObject("effect") && arg.matchesPrefix("effect")) {
                scriptEntry.addObject("effect", arg.getRawElement());
            }
            else if (!scriptEntry.hasObject("quantity") && arg.matchesPrefix("quantity", "q")) {
                scriptEntry.addObject("quantity", arg.getRawElement());
            }
            else if (!scriptEntry.hasObject("data") && arg.matchesPrefix("data")) {
                scriptEntry.addObject("data", arg.getRawElement());
            }
            else if (!scriptEntry.hasObject("offset") && arg.matchesPrefix("offset")) {
                scriptEntry.addObject("offset", arg.getRawElement());
            }
            else if (!scriptEntry.hasObject("special_data") && arg.matchesPrefix("special_data")) {
                scriptEntry.addObject("special_data", arg.getRawElement());
            }
            else if (!scriptEntry.hasObject("at") && arg.matchesPrefix("at")) {
                scriptEntry.addObject("at", TagManager.tagObject(arg.getValue(), scriptEntry.getContext()));
            }
            else if (!scriptEntry.hasObject("targets") && arg.matchesPrefix("targets", "target", "t")) {
                scriptEntry.addObject("targets", ListTag.getListFor(TagManager.tagObject(arg.getValue(), scriptEntry.getContext()), scriptEntry.getContext())
                        .filter(PlayerTag.class, scriptEntry));
            }
            else {
                arg.reportUnhandled();
            }
        }
        if (!scriptEntry.hasObject("effect")) {
            throw new InvalidArgumentsException("Missing effect.");
        }
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {
        String effect = scriptEntry.getElement("effect").asString();
        int quantity = scriptEntry.getElement("quantity") != null ? scriptEntry.getElement("quantity").asInt() : 1;
        double data = scriptEntry.getElement("data") != null ? scriptEntry.getElement("data").asDouble() : 0.0;
        double offset = scriptEntry.getElement("offset") != null ? scriptEntry.getElement("offset").asDouble() : 0.0;
        ElementTag special = scriptEntry.getElement("special_data");
        List<PlayerTag> targets = (List<PlayerTag>) scriptEntry.getObject("targets");
        Object atObj = scriptEntry.getObject("at");
        LocationTag location = atObj instanceof LocationTag loc ? loc : null;
        if (targets == null || targets.isEmpty()) {
            return;
        }
        if (location == null) {
            location = new LocationTag(targets.get(0).getPlayer().position(), targets.get(0).getPlayer().level());
        }
        Level level = location.getLevel();
        if (level == null) {
            return;
        }
        ParticleOptions particle = resolveParticle(effect, special != null ? special.asString() : null);
        if (particle == null) {
            return;
        }
        for (PlayerTag target : targets) {
            ServerPlayer player = target.getPlayer();
            if (player.level() != level) {
                continue;
            }
            player.serverLevel().sendParticles(player, particle, true,
                    location.getX(), location.getY(), location.getZ(),
                    quantity, offset, offset, offset, data);
        }
    }

    private ParticleOptions resolveParticle(String effect, String special) {
        if (effect == null) {
            return null;
        }
        String lower = effect.toLowerCase();
        if ("redstone".equals(lower)) {
            float size = 1.0f;
            int rgb = 0xFFFFFF;
            if (special != null) {
                String[] parts = special.split("\\|");
                if (parts.length >= 1) {
                    try {
                        size = Float.parseFloat(parts[0]);
                    }
                    catch (NumberFormatException ignored) {
                    }
                }
                if (parts.length >= 2 && parts[1].startsWith("#")) {
                    try {
                        rgb = Integer.parseInt(parts[1].substring(1), 16);
                    }
                    catch (NumberFormatException ignored) {
                    }
                }
            }
            float r = ((rgb >> 16) & 0xFF) / 255.0f;
            float g = ((rgb >> 8) & 0xFF) / 255.0f;
            float b = (rgb & 0xFF) / 255.0f;
            return new DustParticleOptions(new Vector3f(r, g, b), size);
        }
        if ("heart".equals(lower)) {
            return ParticleTypes.HEART;
        }
        return null;
    }
}
