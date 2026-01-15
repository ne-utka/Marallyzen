package neutka.marallys.marallyzen.denizen;

import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.generator.ArgName;
import com.denizenscript.denizencore.scripts.commands.generator.ArgPrefixed;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.network.NetworkHelper;
import neutka.marallys.marallyzen.network.ScreenFadePacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

/**
 * Denizen command for playing screen fade cutscenes.
 * Usage: screenfade [fade_out:<duration>] [black_screen:<duration>] [fade_in:<duration>] [title:<text>] [subtitle:<text>] [block_input:<true/false>] (target:<player_name_or_uuid>)
 */
public class ScreenFadeCommand extends AbstractCommand {

    // <--[command]
    // @Name ScreenFade
    // @Syntax screenfade [fade_out:<duration>] [black_screen:<duration>] [fade_in:<duration>] [title:<text>] [subtitle:<text>] [sound:<sound_id>] [block_input:<true/false>] (target:<player_name_or_uuid>)
    // @Required 0
    // @Maximum 8
    // @Short Plays a screen fade cutscene (black screen transition with text).
    // @Group marallyzen
    //
    // @Description
    // Plays a screen fade cutscene with a black screen transition and optional text overlay.
    // Duration formats: "1s" (seconds), "20t" (ticks), or plain number (ticks).
    // Sound ID format: ResourceLocation (e.g., "minecraft:block.anvil.land").
    // Sound plays when text appears (when screen becomes black).
    // If no target is specified, attempts to find player from script context.
    //
    // @Tags
    // None
    //
    // @Usage
    // Use to play a screen fade with default timings.
    // - screenfade fade_out:1s black_screen:5s fade_in:1s title:"30 минут спустя" subtitle:"11 августа 2024 г."
    //
    // @Usage
    // Use to play a screen fade with sound.
    // - screenfade fade_out:1s black_screen:5s fade_in:1s title:"30 минут спустя" subtitle:"11 августа 2024 г." sound:minecraft:block.anvil.land
    //
    // @Usage
    // Use to play a screen fade with blocked input.
    // - screenfade fade_out:1s black_screen:5s fade_in:1s title:"Переход" block_input:true
    // -->
    public ScreenFadeCommand() {
        setName("screenfade");
        setSyntax("screenfade [fade_out:<duration>] [black_screen:<duration>] [fade_in:<duration>] [title:<text>] [subtitle:<text>] [sound:<sound_id>] [block_input:<true/false>] (target:<player_name_or_uuid>)");
        setRequiredArguments(0, 8);
        autoCompile();
    }

    public static void autoExecute(ScriptEntry scriptEntry,
                                   @ArgPrefixed @ArgName("fade_out") String fadeOutStr,
                                   @ArgPrefixed @ArgName("black_screen") String blackScreenStr,
                                   @ArgPrefixed @ArgName("fade_in") String fadeInStr,
                                   @ArgPrefixed @ArgName("title") String titleStr,
                                   @ArgPrefixed @ArgName("subtitle") String subtitleStr,
                                   @ArgPrefixed @ArgName("sound") String soundStr,
                                   @ArgPrefixed @ArgName("block_input") Boolean blockInput,
                                   @ArgPrefixed @ArgName("target") String target) {
        // Parse durations (default: 1s, 5s, 1s)
        int fadeOutTicks = parseDuration(fadeOutStr != null ? fadeOutStr : "1s");
        int blackScreenTicks = parseDuration(blackScreenStr != null ? blackScreenStr : "5s");
        int fadeInTicks = parseDuration(fadeInStr != null ? fadeInStr : "1s");
        
        // Default values
        Component titleText = titleStr != null ? Component.literal(titleStr) : null;
        Component subtitleText = subtitleStr != null ? Component.literal(subtitleStr) : null;
        String soundId = soundStr != null && !soundStr.isEmpty() ? soundStr : null;
        boolean blockPlayerInput = blockInput != null ? blockInput : false;
        
        // Get target player
        ServerPlayer player = null;
        
        if (target != null && !target.isEmpty()) {
            // Try to find player by name or UUID
            player = findPlayerByNameOrUuid(target);
            if (player == null) {
                Debug.echoError(scriptEntry, "Player '" + target + "' not found!");
                return;
            }
        } else {
            // Try to get player from script context
            // For now, we'll need to get it from the server's player list
            Debug.echoError(scriptEntry, "No target player specified. Please use target:<player_name_or_uuid>");
            return;
        }
        
        // Get server level for parsing JSON components if needed
        ServerLevel serverLevel = null;
        if (player.level() instanceof ServerLevel level) {
            serverLevel = level;
        }
        
        // Parse title and subtitle as JSON components if they look like JSON
        if (titleStr != null && serverLevel != null) {
            titleText = parseTextComponent(titleStr, serverLevel);
        }
        if (subtitleStr != null && serverLevel != null) {
            subtitleText = parseTextComponent(subtitleStr, serverLevel);
        }
        
        // Send screen fade packet to client
        NetworkHelper.sendToPlayer(player, new ScreenFadePacket(
                fadeOutTicks,
                blackScreenTicks,
                fadeInTicks,
                titleText,
                subtitleText,
                blockPlayerInput,
                soundId
        ));
        
        if (scriptEntry.shouldDebug()) {
            Debug.echoDebug(scriptEntry, "Playing screen fade for player " + player.getName().getString() + 
                    " (fadeOut=" + fadeOutTicks + "t, blackScreen=" + blackScreenTicks + "t, fadeIn=" + fadeInTicks + "t, sound=" + (soundId != null ? soundId : "none") + ")");
        }
    }
    
    /**
     * Parses a duration string to ticks.
     * Supports formats: "1s" (seconds), "20t" (ticks), or plain number (ticks).
     * 
     * @param durationStr Duration string
     * @return Duration in ticks
     */
    private static int parseDuration(String durationStr) {
        if (durationStr == null || durationStr.isEmpty()) {
            return 20; // Default: 1 second
        }
        
        durationStr = durationStr.trim().toLowerCase();
        
        // Check for seconds format: "1s", "5s", etc.
        if (durationStr.endsWith("s")) {
            try {
                int seconds = Integer.parseInt(durationStr.substring(0, durationStr.length() - 1));
                return seconds * 20; // Convert seconds to ticks
            } catch (NumberFormatException e) {
                Marallyzen.LOGGER.warn("Invalid duration format: {}", durationStr);
                return 20;
            }
        }
        
        // Check for ticks format: "20t", "100t", etc.
        if (durationStr.endsWith("t")) {
            try {
                return Integer.parseInt(durationStr.substring(0, durationStr.length() - 1));
            } catch (NumberFormatException e) {
                Marallyzen.LOGGER.warn("Invalid duration format: {}", durationStr);
                return 20;
            }
        }
        
        // Plain number (assumed to be ticks)
        try {
            return Integer.parseInt(durationStr);
        } catch (NumberFormatException e) {
            Marallyzen.LOGGER.warn("Invalid duration format: {}", durationStr);
            return 20;
        }
    }
    
    /**
     * Parses a text component from JSON string or plain text.
     * Supports JSON format: {"text":"Name","color":"blue"}
     */
    private static Component parseTextComponent(String text, ServerLevel level) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        
        // Try to parse as JSON first
        if (text.trim().startsWith("{") || text.trim().startsWith("[")) {
            try {
                return Component.Serializer.fromJson(text, level.registryAccess());
            } catch (Exception e) {
                // If JSON parsing fails, treat as plain text
                Marallyzen.LOGGER.debug("Failed to parse text as JSON, using plain text: " + text, e);
            }
        }
        
        // Return as plain text
        return Component.literal(text);
    }
    
    /**
     * Finds a ServerPlayer by name or UUID string.
     * Searches through all spawned NPCs' levels to find the server instance.
     */
    private static ServerPlayer findPlayerByNameOrUuid(String identifier) {
        // Get server from any available source
        // Try to get server from NPC registry's levels
        net.minecraft.server.MinecraftServer server = null;
        
        // Try to get server from any spawned NPC's level
        var npcRegistry = neutka.marallys.marallyzen.npc.NpcClickHandler.getRegistry();
        for (var npcEntity : npcRegistry.getSpawnedNpcs()) {
            if (npcEntity.level() instanceof ServerLevel level && level.getServer() != null) {
                server = level.getServer();
                break;
            }
        }
        
        // If no NPCs are spawned, try to get server from any level
        // This is a fallback - in practice, NPCs should be spawned
        if (server == null) {
            // We can't easily get server without a level reference
            // This is a limitation - in a full implementation, you'd store server reference
            Marallyzen.LOGGER.warn("Cannot find server instance to locate player '{}'. Make sure NPCs are spawned first.", identifier);
            return null;
        }
        
        // Try UUID first
        try {
            UUID uuid = UUID.fromString(identifier);
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player != null) {
                return player;
            }
        } catch (IllegalArgumentException e) {
            // Not a valid UUID, try name
        }
        
        // Try by name
        return server.getPlayerList().getPlayerByName(identifier);
    }
}
