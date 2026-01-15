package neutka.marallys.marallyzen;

import com.denizenscript.denizencore.objects.core.ScriptTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import com.denizenscript.denizencore.scripts.containers.ScriptContainer;
import com.denizenscript.denizencore.tags.TagContext;

public class MarallyzenTagContext extends TagContext {

    public MarallyzenTagContext(ScriptEntry entry) {
        super(entry);
    }

    public MarallyzenTagContext(boolean debug, ScriptEntry entry, ScriptTag script) {
        super(debug, entry, script);
    }

    public MarallyzenTagContext(ScriptContainer container) {
        super(container == null || container.shouldDebug(), null, container == null ? null : new ScriptTag(container));
    }

    @Override
    public ScriptEntryData getScriptEntryData() {
        if (entry != null && entry.entryData instanceof MarallyzenScriptEntryData data) {
            return data;
        }
        MarallyzenScriptEntryData entryData = new MarallyzenScriptEntryData();
        entryData.scriptEntry = entry;
        return entryData;
    }
}


