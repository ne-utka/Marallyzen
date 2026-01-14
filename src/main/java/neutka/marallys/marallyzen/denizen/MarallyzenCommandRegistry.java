package neutka.marallys.marallyzen.denizen;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import neutka.marallys.marallyzen.Marallyzen;

/**
 * Registry for Marallyzen Denizen commands.
 * Registers custom commands that can be used in Denizen scripts.
 */
public class MarallyzenCommandRegistry {

    /**
     * Registers all Marallyzen Denizen commands.
     * Should be called during mod initialization.
     */
    public static void registerCommands() {
        registerCommand(neutka.marallys.marallyzen.denizen.commands.NarrateCommand.class);
        registerCommand(neutka.marallys.marallyzen.denizen.commands.ActionBarCommand.class);
        registerCommand(neutka.marallys.marallyzen.denizen.commands.PlaySoundCommand.class);
        registerCommand(neutka.marallys.marallyzen.denizen.commands.PlayEffectCommand.class);
        registerCommand(neutka.marallys.marallyzen.denizen.commands.AnimateCommand.class);
        registerCommand(neutka.marallys.marallyzen.denizen.commands.AnnounceCommand.class);
        registerCommand(neutka.marallys.marallyzen.denizen.commands.ToastCommand.class);
        registerCommand(neutka.marallys.marallyzen.denizen.commands.DiscordMessageCommand.class);
        registerCommand(CutsceneCommand.class);
        registerCommand(ScreenFadeCommand.class);
        registerCommand(EyesCutsceneCommand.class);
        Marallyzen.LOGGER.info("Registered Marallyzen Denizen commands");
    }

    /**
     * Registers a single Denizen command.
     */
    private static void registerCommand(Class<? extends AbstractCommand> command) {
        try {
            DenizenCore.commandRegistry.registerCommand(command);
            Marallyzen.LOGGER.debug("Registered Denizen command: {}", command.getSimpleName());
        } catch (Exception e) {
            Marallyzen.LOGGER.error("Failed to register Denizen command: {}", command.getSimpleName(), e);
        }
    }
}
