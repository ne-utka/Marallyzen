package neutka.marallys.marallyzen;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.utilities.CoreConfiguration;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.events.ScriptEvent;
import com.denizenscript.denizencore.scripts.ScriptRegistry;
import com.denizenscript.denizencore.scripts.containers.core.WorldScriptContainer;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.loading.FMLPaths;
import neutka.marallys.marallyzen.denizen.MarallyzenDenizenBootstrap;
import neutka.marallys.marallyzen.denizen.commands.CommandScriptRegistry;
import neutka.marallys.marallyzen.denizen.events.PlayerRightClicksPlayerScriptEvent;
import neutka.marallys.marallyzen.denizen.events.PlayerChatsScriptEvent;

/**
 * Minimal DenizenCore bootstrap for NeoForge.
 *
 * This only brings up the core engine + file-based scripts folder.
 * Events/commands will be wired in later.
 */
public final class DenizenService {
    private DenizenService() {
    }

    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);
    private static volatile String version = "unknown";

    public static boolean isInitialized() {
        return INITIALIZED.get();
    }

    public static File getScriptsFolder() {
        return FMLPaths.CONFIGDIR.get().resolve(Marallyzen.MODID).resolve("scripts").toFile();
    }

    public static void initIfNeeded(ModContainer modContainer) {
        if (!INITIALIZED.compareAndSet(false, true)) {
            return;
        }
        try {
            version = modContainer.getModInfo().getVersion().toString();

            // Set up baseline contexts (DenizenCore expects these statics)
            CoreUtilities.noDebugContext = new MarallyzenTagContext(false, null, null);
            CoreUtilities.noDebugContext.showErrors = () -> false;
            CoreUtilities.basicContext = new MarallyzenTagContext(true, null, null);
            CoreUtilities.errorButNoDebugContext = new MarallyzenTagContext(false, null, null);

            DenizenCore.init(new MarallyzenDenizenImpl());
            DenizenCore.reloadSaves();

            MarallyzenDenizenBootstrap.init();
            ScriptEvent.registerScriptEvent(PlayerRightClicksPlayerScriptEvent.class);
            ScriptEvent.registerScriptEvent(PlayerChatsScriptEvent.class);

            // Safety defaults (mirrors Clientizen's restrictions)
            CoreConfiguration.allowConsoleRedirection = false;
            CoreConfiguration.allowFileCopy = false;
            CoreConfiguration.allowFileRead = false;
            CoreConfiguration.allowFileWrite = false;
            CoreConfiguration.allowLog = false;
            CoreConfiguration.allowRedis = false;
            CoreConfiguration.allowRestrictedActions = false;
            CoreConfiguration.allowSQL = false;
            CoreConfiguration.allowStrangeFileSaves = false;
            CoreConfiguration.allowWebget = false;

            File scripts = DenizenCore.implementation.getScriptFolder();
            if (!scripts.exists()) {
                scripts.mkdirs();
            }
            
            // Register Marallyzen Denizen commands
            neutka.marallys.marallyzen.denizen.MarallyzenCommandRegistry.registerCommands();
            
            DenizenCore.reloadScripts(false, null);
            CommandScriptRegistry.refresh();
            logScriptSummary("init");
            Marallyzen.LOGGER.info("DenizenCore initialized. scripts_folder={}", scripts.getAbsolutePath());
        }
        catch (Throwable ex) {
            // Fail-safe: don't crash the mod load hard yet, just disable scripting
            Marallyzen.LOGGER.error("Failed to initialize DenizenCore (scripting will be disabled).", ex);
            INITIALIZED.set(false);
        }
    }

    public static String getImplementationVersion() {
        return version;
    }

    public static void tickServer() {
        if (!INITIALIZED.get()) {
            return;
        }
        try {
            DenizenCore.tick(50);
        }
        catch (Throwable ex) {
            Debug.echoError(ex);
        }
    }

    public static void reload() {
        if (!INITIALIZED.get()) {
            return;
        }
        logScriptFolderStatus("reload:before");
        DenizenCore.reloadScripts(false, null);
        CommandScriptRegistry.refresh();
        logScriptSummary("reload:after");
    }

    public static void shutdown() {
        if (!INITIALIZED.get()) {
            return;
        }
        try {
            DenizenCore.shutdown();
        }
        catch (Throwable ex) {
            Marallyzen.LOGGER.warn("Error while shutting down DenizenCore.", ex);
        }
        finally {
            INITIALIZED.set(false);
        }
    }

    private static void logScriptFolderStatus(String phase) {
        try {
            File scripts = DenizenService.getScriptsFolder();
            File[] files = scripts.listFiles((dir, name) -> name.toLowerCase().endsWith(".dsc"));
            int count = files == null ? 0 : files.length;
            Marallyzen.LOGGER.info("Denizen scripts folder ({}) path={} files={}", phase, scripts.getAbsolutePath(), count);
        }
        catch (Exception e) {
            Marallyzen.LOGGER.warn("Failed to inspect scripts folder ({})", phase, e);
        }
    }

    private static void logScriptSummary(String phase) {
        try {
            int total = ScriptRegistry.scriptContainers.size();
            int commandCount = 0;
            StringBuilder commandNames = new StringBuilder();
            for (var entry : ScriptRegistry.scriptContainers.values()) {
                if (entry instanceof neutka.marallys.marallyzen.denizen.commands.CommandScriptContainer cmd) {
                    if (commandCount > 0) {
                        commandNames.append(", ");
                    }
                    commandNames.append(cmd.getName());
                    commandCount++;
                }
            }
            Marallyzen.LOGGER.info("Denizen scripts summary ({}) total={} command_containers={} names=[{}]", phase, total, commandCount, commandNames);
            int worldCount = com.denizenscript.denizencore.events.ScriptEvent.worldContainers.size();
            StringBuilder worldNames = new StringBuilder();
            for (WorldScriptContainer container : com.denizenscript.denizencore.events.ScriptEvent.worldContainers) {
                if (worldNames.length() > 0) {
                    worldNames.append(", ");
                }
                worldNames.append(container.getName());
            }
            com.denizenscript.denizencore.events.ScriptEvent clickEvent =
                    com.denizenscript.denizencore.events.ScriptEvent.eventLookup.get("playerrightclicksplayer");
            int clickPaths = clickEvent == null ? 0 : clickEvent.eventPaths.size();
            boolean clickEnabled = clickEvent != null && clickEvent.eventData.isEnabled;
            Marallyzen.LOGGER.info("Denizen world scripts summary ({}) world_containers={} names=[{}] right_click_paths={} enabled={}",
                    phase, worldCount, worldNames, clickPaths, clickEnabled);
        }
        catch (Exception e) {
            Marallyzen.LOGGER.warn("Failed to summarize scripts ({})", phase, e);
        }
    }
}
