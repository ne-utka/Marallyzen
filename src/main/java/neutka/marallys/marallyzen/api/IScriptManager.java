package neutka.marallys.marallyzen.api;

/**
 * API for managing Denizen scripts in Marallyzen.
 * Allows other mods to execute scripts and interact with the scripting system.
 */
public interface IScriptManager {

    /**
     * Execute a Denizen script by name.
     *
     * @param scriptName The script name to execute
     * @return true if the script was found and executed
     */
    boolean executeScript(String scriptName);

    /**
     * Execute a script with custom context variables.
     *
     * @param scriptName The script name
     * @param context A map of context variables
     * @return true if the script was found and executed
     */
    boolean executeScript(String scriptName, java.util.Map<String, Object> context);

    /**
     * Reload all scripts from disk.
     */
    void reloadScripts();

    /**
     * Check if a script exists.
     *
     * @param scriptName The script name
     * @return true if the script exists
     */
    boolean hasScript(String scriptName);

    /**
     * Get the names of all loaded scripts.
     *
     * @return List of script names
     */
    java.util.List<String> getAllScriptNames();

    /**
     * Get the implementation version of DenizenCore.
     *
     * @return The version string
     */
    String getDenizenVersion();
}



