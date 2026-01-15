package neutka.marallys.marallyzen.api;

import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Map;

/**
 * API for managing NPC dialogs in Marallyzen.
 * Allows other mods to trigger and customize NPC conversations.
 */
public interface IDialogManager {

    /**
     * Open a dialog for a player with an NPC.
     *
     * @param player The player to show the dialog to
     * @param dialogId The unique dialog ID
     * @param title The dialog title
     * @param buttons Map of button IDs to button text
     */
    void openDialog(ServerPlayer player, String dialogId, String title, Map<String, String> buttons);

    /**
     * Register a dialog programmatically.
     *
     * @param dialogId The unique dialog ID
     * @param title The dialog title
     * @param buttons Map of button IDs to button text
     * @param scriptName Optional Denizen script to execute on button click
     */
    void registerDialog(String dialogId, String title, Map<String, String> buttons, String scriptName);

    /**
     * Handle a dialog button click.
     * This is called automatically by the network system, but can be used
     * by other mods to simulate button clicks.
     *
     * @param player The player who clicked
     * @param dialogId The dialog ID
     * @param buttonId The button that was clicked
     */
    void handleDialogButtonClick(ServerPlayer player, String dialogId, String buttonId);

    /**
     * Get all registered dialog IDs.
     *
     * @return List of dialog IDs
     */
    List<String> getAllDialogIds();

    /**
     * Check if a dialog is registered.
     *
     * @param dialogId The dialog ID
     * @return true if the dialog exists
     */
    boolean hasDialog(String dialogId);
}



