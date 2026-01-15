package neutka.marallys.marallyzen.denizen;

import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.generator.ArgLinear;
import com.denizenscript.denizencore.scripts.commands.generator.ArgName;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.api.MarallyzenAPI;
import neutka.marallys.marallyzen.network.NetworkHelper;
import neutka.marallys.marallyzen.network.PlayScenePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

/**
 * Denizen command for playing cutscenes.
 * Usage: cutscene [<scene_name>] (target:<player_name_or_uuid>)
 */
public class CutsceneCommand extends AbstractCommand {

    // <--[command]
    // @Name Cutscene
    // @Syntax cutscene [<scene_name>] (target:<player_name_or_uuid>)
    // @Required 1
    // @Maximum 2
    // @Short Plays a cutscene for a player.
    // @Group marallyzen
    //
    // @Description
    // Plays a cutscene by name. If no target is specified, attempts to find player from script context.
    // The cutscene must be defined in config/marallyzen/scenes/ as a JSON file.
    //
    // @Tags
    // None
    //
    // @Usage
    // Use to play a cutscene for a specific player by name.
    // - cutscene "intro_scene" target:PlayerName
    //
    // @Usage
    // Use to play a cutscene for a specific player by UUID.
    // - cutscene "intro_scene" target:<uuid>
    // -->
    public CutsceneCommand() {
        setName("cutscene");
        setSyntax("cutscene [<scene_name>] (target:<player_name_or_uuid>)");
        setRequiredArguments(1, 2);
        autoCompile();
    }

    public static void autoExecute(ScriptEntry scriptEntry,
                                   @ArgLinear @ArgName("scene_name") String sceneName) {
        autoExecute(scriptEntry, sceneName, null);
    }

    public static void autoExecute(ScriptEntry scriptEntry,
                                   @ArgLinear @ArgName("scene_name") String sceneName,
                                   @ArgName("target") String target) {
        if (sceneName == null || sceneName.isEmpty()) {
            Debug.echoError(scriptEntry, "Cutscene name cannot be empty!");
            return;
        }

        // Check if scene exists
        if (!MarallyzenAPI.getInstance().getCutsceneManager().hasScene(sceneName)) {
            Debug.echoError(scriptEntry, "Cutscene '" + sceneName + "' not found!");
            return;
        }

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
            // This is a simplified approach - in a full implementation, you'd store player reference in ScriptEntryData
            Debug.echoError(scriptEntry, "No target player specified. Please use target:<player_name_or_uuid>");
            return;
        }

        // Get scene data to check for audio keyframes
        var sceneData = neutka.marallys.marallyzen.client.camera.SceneLoader.getScene(sceneName);
        if (sceneData != null && !sceneData.getAudioKeyframes().isEmpty()) {
            // Play audio keyframes on server
            net.minecraft.server.level.ServerLevel serverLevel = (net.minecraft.server.level.ServerLevel) player.level();
            for (var audioKeyframe : sceneData.getAudioKeyframes()) {
                // Schedule audio playback based on keyframe timing
                // For now, play immediately (timing can be improved later)
                long duration = neutka.marallys.marallyzen.audio.MarallyzenAudioService.playCutsceneAudio(
                        serverLevel,
                        audioKeyframe.position,
                        audioKeyframe.filePath,
                        audioKeyframe.radius,
                        audioKeyframe.positional,
                        java.util.Collections.singletonList(player)
                );
                if (scriptEntry.shouldDebug()) {
                    Debug.echoDebug(scriptEntry, "Playing audio keyframe: " + audioKeyframe.filePath + " (duration: " + duration + "ms)");
                }
            }
        }
        
        // Send cutscene packet to client
        NetworkHelper.sendToPlayer(player, new PlayScenePacket(sceneName));
        
        if (scriptEntry.shouldDebug()) {
            Debug.echoDebug(scriptEntry, "Playing cutscene '" + sceneName + "' for player " + player.getName().getString());
        }
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

