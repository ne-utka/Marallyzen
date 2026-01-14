package neutka.marallys.marallyzen;

import com.denizenscript.denizencore.scripts.ScriptEntryData;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.utilities.YamlConfiguration;
import neutka.marallys.marallyzen.denizen.objects.EntityTag;
import neutka.marallys.marallyzen.denizen.objects.PlayerTag;

public class MarallyzenScriptEntryData extends ScriptEntryData {
    private PlayerTag player;
    private EntityTag entity;

    public PlayerTag getPlayer() {
        return player;
    }

    public void setPlayer(PlayerTag player) {
        this.player = player;
    }

    public EntityTag getEntity() {
        return entity;
    }

    public void setEntity(EntityTag entity) {
        this.entity = entity;
    }

    @Override
    public void transferDataFrom(ScriptEntryData scriptEntryData) {
        if (scriptEntryData == null) {
            return;
        }
        scriptEntry = scriptEntryData.scriptEntry;
        if (scriptEntryData instanceof MarallyzenScriptEntryData data) {
            player = data.player;
            entity = data.entity;
        }
    }

    @Override
    public TagContext getTagContext() {
        return new MarallyzenTagContext(scriptEntry);
    }

    @Override
    public YamlConfiguration save() {
        return new YamlConfiguration();
    }

    @Override
    public void load(YamlConfiguration yamlConfiguration) {
        // no-op
    }
}


