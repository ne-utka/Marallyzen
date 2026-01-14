package neutka.marallys.marallyzen.denizen.commands;

import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.containers.ScriptContainer;
import com.denizenscript.denizencore.scripts.queues.ContextSource;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;
import com.denizenscript.denizencore.scripts.queues.core.InstantQueue;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.YamlConfiguration;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import neutka.marallys.marallyzen.MarallyzenScriptEntryData;
import neutka.marallys.marallyzen.denizen.objects.PlayerTag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CommandScriptContainer extends ScriptContainer {

    public CommandScriptContainer(YamlConfiguration configurationSection, String scriptContainerName) {
        super(configurationSection, scriptContainerName);
        CommandScriptRegistry.add(this);
        String cmdName = getCommandName();
        if (cmdName != null) {
            neutka.marallys.marallyzen.Marallyzen.LOGGER.info("Loaded command script: {} (name={})", getName(), cmdName);
        }
    }

    public String getCommandName() {
        String name = getString("name", null);
        if (name == null) {
            Debug.echoError("Command script '" + getName() + "' missing 'name'.");
            return null;
        }
        return CoreUtilities.toLowerCase(name);
    }

    public List<ScriptEntry> getCommandEntries(MarallyzenScriptEntryData data) {
        return getBaseEntries(data);
    }

    public boolean hasTabCompleteProcedure() {
        return containsScriptSection("tab complete");
    }

    public List<String> runTabCompleteProcedure(MarallyzenScriptEntryData data, Map<String, ObjectTag> context) {
        if (!hasTabCompleteProcedure()) {
            return List.of();
        }
        List<ScriptEntry> entries = getEntries(data, "tab complete");
        if (entries == null) {
            return List.of();
        }
        ScriptQueue queue = new InstantQueue(getName());
        queue.addEntries(entries);
        if (context != null) {
            ContextSource.SimpleMap src = new ContextSource.SimpleMap();
            src.contexts = context;
            queue.setContextSource(src);
        }
        queue.start();
        if (queue.determinations == null || queue.determinations.isEmpty()) {
            return List.of();
        }
        ListTag list = ListTag.getListFor(queue.determinations.getObject(0), queue.getLastEntryExecuted().getContext());
        List<String> output = new ArrayList<>(list.size());
        for (String entry : list) {
            output.add(entry);
        }
        return output;
    }
}
