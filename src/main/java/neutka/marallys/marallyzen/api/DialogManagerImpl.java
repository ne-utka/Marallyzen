package neutka.marallys.marallyzen.api;

import net.minecraft.server.level.ServerPlayer;
import neutka.marallys.marallyzen.MarallyzenClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the Dialog Manager API.
 */
class DialogManagerImpl implements IDialogManager {

    // Simple storage for programmatically registered dialogs
    private final Map<String, DialogData> registeredDialogs = new HashMap<>();

    @Override
    public void openDialog(ServerPlayer player, String dialogId, String title, Map<String, String> buttons) {
        // Send dialog packet to client
        // Note: UUID is set to player's UUID as fallback - this should be replaced with actual NPC UUID when available
        neutka.marallys.marallyzen.network.NetworkHelper.sendToPlayer(
                player,
                new neutka.marallys.marallyzen.network.OpenDialogPacket(dialogId, title, buttons, player.getUUID())
        );
    }

    @Override
    public void registerDialog(String dialogId, String title, Map<String, String> buttons, String scriptName) {
        registeredDialogs.put(dialogId, new DialogData(title, buttons, scriptName));
    }

    @Override
    public void handleDialogButtonClick(ServerPlayer player, String dialogId, String buttonId) {
        // Check if we have a registered dialog with a script
        DialogData dialog = registeredDialogs.get(dialogId);
        if (dialog != null && dialog.scriptName != null) {
            // Execute the associated script
            neutka.marallys.marallyzen.api.MarallyzenAPI.getInstance()
                    .getScriptManager()
                    .executeScript(dialog.scriptName);
        }

        // Log the interaction
        neutka.marallys.marallyzen.Marallyzen.LOGGER.info(
                "Player {} clicked dialog button: {} -> {}",
                player.getName().getString(), dialogId, buttonId
        );
    }

    @Override
    public List<String> getAllDialogIds() {
        return new ArrayList<>(registeredDialogs.keySet());
    }

    @Override
    public boolean hasDialog(String dialogId) {
        return registeredDialogs.containsKey(dialogId);
    }

    private static class DialogData {
        final String title;
        final Map<String, String> buttons;
        final String scriptName;

        DialogData(String title, Map<String, String> buttons, String scriptName) {
            this.title = title;
            this.buttons = buttons;
            this.scriptName = scriptName;
        }
    }
}
