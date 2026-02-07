package neutka.marallys.marallyzen.api;


/**
 * Public API implementation for Marallyzen.
 * This is the main entry point that other mods should use.
 *
 * Usage example:
 * <pre>
 * IMarallyzenAPI api = MarallyzenAPI.getInstance();
 * INpcManager npcManager = api.getNpcManager();
 * // Use the API...
 * </pre>
 */
public class MarallyzenAPI implements IMarallyzenAPI {

    private static MarallyzenAPI instance;

    private final NpcManagerImpl npcManager;
    private final DialogManagerImpl dialogManager;
    private final ScriptManagerImpl scriptManager;

    private MarallyzenAPI() {
        this.npcManager = new NpcManagerImpl();
        this.dialogManager = new DialogManagerImpl();
        this.scriptManager = new ScriptManagerImpl();
    }

    /**
     * Get the singleton instance of the Marallyzen API.
     *
     * @return The API instance
     */
    public static IMarallyzenAPI getInstance() {
        if (instance == null) {
            instance = new MarallyzenAPI();
        }
        return instance;
    }

    @Override
    public INpcManager getNpcManager() {
        return npcManager;
    }

    @Override
    public IDialogManager getDialogManager() {
        return dialogManager;
    }

    // Cutscene API removed.

    @Override
    public IScriptManager getScriptManager() {
        return scriptManager;
    }
}



