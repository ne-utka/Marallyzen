package neutka.marallys.marallyzen.denizen;

import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.generator.ArgName;
import com.denizenscript.denizencore.scripts.commands.generator.ArgPrefixed;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.network.NetworkHelper;
import neutka.marallys.marallyzen.network.EyesClosePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

/**
 * Denizen command for playing "eyes close" cutscenes.
 * Triggers a Bedrock-style cinematic effect where eyelids close and open.
 * 
 * Syntax:
 *   eyescutscene [close_duration:<duration>] [black_duration:<duration>] [open_duration:<duration>] [lock_player:<true/false>] (target:<player_name_or_uuid>)
 * 
 * Examples:
 *   - eyescutscene close_duration:1s black_duration:5s open_duration:1s lock_player:true
 *   - eyescutscene close_duration:16t black_duration:100t open_duration:20t lock_player:false target:Steve
 */
public class EyesCutsceneCommand extends AbstractCommand {
    
    public EyesCutsceneCommand() {
        setName("eyescutscene");
        setSyntax("eyescutscene [close_duration:<duration>] [black_duration:<duration>] [open_duration:<duration>] [lock_player:<true/false>] (target:<player_name_or_uuid>)");
        setRequiredArguments(0, 5);
        autoCompile();
    }
    
    public static void autoExecute(ScriptEntry scriptEntry,
                                   @ArgPrefixed @ArgName("close_duration") String closeDurationStr,
                                   @ArgPrefixed @ArgName("black_duration") String blackDurationStr,
                                   @ArgPrefixed @ArgName("open_duration") String openDurationStr,
                                   @ArgPrefixed @ArgName("lock_player") Boolean lockPlayer,
                                   @ArgPrefixed @ArgName("target") String target) {
        
        // Parse durations (default: 1s close, 5s black, 1s open)
        int closeDurationTicks = parseDuration(closeDurationStr, 20); // Default 1 second
        int blackDurationTicks = parseDuration(blackDurationStr, 100); // Default 5 seconds
        int openDurationTicks = parseDuration(openDurationStr, 20); // Default 1 second
        
        // Parse lock_player (default: true for immersion)
        boolean blockPlayerInput = lockPlayer != null ? lockPlayer : true;
        
        // Find target player
        ServerPlayer player = findTargetPlayer(scriptEntry, target);
        if (player == null) {
            Debug.echoError(scriptEntry, "Could not find target player for eyescutscene command");
            return;
        }
        
        // Send eyes close packet to client
        NetworkHelper.sendToPlayer(player, new EyesClosePacket(
                closeDurationTicks,
                blackDurationTicks,
                openDurationTicks,
                blockPlayerInput
        ));
        
        if (scriptEntry.shouldDebug()) {
            Debug.echoDebug(scriptEntry, "Playing eyes close cutscene for player " + player.getName().getString() + 
                    " (close=" + closeDurationTicks + "t, black=" + blackDurationTicks + "t, open=" + openDurationTicks + "t, lock=" + blockPlayerInput + ")");
        }
    }
    
    /**
     * Parses a duration string to ticks.
     * Supports formats: "1s" (seconds), "20t" (ticks), or plain number (ticks).
     */
    private static int parseDuration(String durationStr, int defaultValue) {
        if (durationStr == null || durationStr.isEmpty()) {
            return defaultValue;
        }
        
        try {
            durationStr = durationStr.trim().toLowerCase();
            
            if (durationStr.endsWith("s")) {
                // Seconds to ticks (1 second = 20 ticks)
                float seconds = Float.parseFloat(durationStr.substring(0, durationStr.length() - 1));
                return (int) (seconds * 20);
            } else if (durationStr.endsWith("t")) {
                // Already in ticks
                return Integer.parseInt(durationStr.substring(0, durationStr.length() - 1));
            } else {
                // Plain number - treat as ticks
                return Integer.parseInt(durationStr);
            }
        } catch (NumberFormatException e) {
            Marallyzen.LOGGER.warn("[EyesCutsceneCommand] Invalid duration format: '{}', using default: {}", durationStr, defaultValue);
            return defaultValue;
        }
    }
    
    /**
     * Finds a ServerPlayer by name or UUID string.
     * Uses the same approach as ScreenFadeCommand.
     */
    private static ServerPlayer findTargetPlayer(ScriptEntry scriptEntry, String target) {
        // Get server from any available source
        net.minecraft.server.MinecraftServer server = null;
        
        // Try to get server from any spawned NPC's level
        var npcRegistry = neutka.marallys.marallyzen.npc.NpcClickHandler.getRegistry();
        for (var npcEntity : npcRegistry.getSpawnedNpcs()) {
            if (npcEntity.level() instanceof ServerLevel level && level.getServer() != null) {
                server = level.getServer();
                break;
            }
        }
        
        // If no NPCs are spawned, we can't easily get server without a level reference
        if (server == null) {
            Marallyzen.LOGGER.warn("[EyesCutsceneCommand] Cannot find server instance. Make sure NPCs are spawned first.");
            return null;
        }
        
        // If target specified, find that player
        if (target != null && !target.isEmpty()) {
            // Try UUID first
            try {
                UUID uuid = UUID.fromString(target);
                ServerPlayer player = server.getPlayerList().getPlayer(uuid);
                if (player != null) {
                    return player;
                }
            } catch (IllegalArgumentException e) {
                // Not a valid UUID, try name
            }
            
            // Try by name
            ServerPlayer player = server.getPlayerList().getPlayerByName(target);
            if (player != null) {
                return player;
            }
            
            Marallyzen.LOGGER.warn("[EyesCutsceneCommand] Could not find player: {}", target);
            return null;
        }
        
        // No target specified - fallback: get first online player (for testing)
        var players = server.getPlayerList().getPlayers();
        if (!players.isEmpty()) {
            Marallyzen.LOGGER.info("[EyesCutsceneCommand] No target specified, using first online player: {}", 
                    players.get(0).getName().getString());
            return players.get(0);
        }
        
        return null;
    }
}
