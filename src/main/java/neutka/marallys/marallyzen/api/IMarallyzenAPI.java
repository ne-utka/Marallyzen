package neutka.marallys.marallyzen.api;

/**
 * Main entry point for Marallyzen's public API.
 * Provides access to all Marallyzen subsystems.
 */
public interface IMarallyzenAPI {

    /**
     * Get the NPC manager for creating and managing NPCs.
     */
    INpcManager getNpcManager();

    /**
     * Get the dialog manager for handling NPC conversations.
     */
    IDialogManager getDialogManager();

    /**
     * Get the cutscene manager for cinematic camera control.
     */
    ICutsceneManager getCutsceneManager();

    /**
     * Get the script manager for executing Denizen scripts.
     */
    IScriptManager getScriptManager();
}



