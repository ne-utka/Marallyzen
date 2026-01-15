package neutka.marallys.marallyzen.npc;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.network.AnimationPacket;
import neutka.marallys.marallyzen.network.NetworkHelper;

/**
 * Handles sending emote packets to nearby players for NPC emotes.
 * Uses Emotecraft API via AnimationPacket.
 */
public class NpcAnimationHandler {

    /**
     * Sends an emote packet to all players within the specified radius of the source entity.
     * The source entity itself does not receive the packet (emote plays locally for observers via Emotecraft API).
     * 
     * @param sourceEntity The entity (NPC or player) performing the emote
     * @param animationName The ID of the emote (e.g., "SPE_Air kiss")
     * @param radius The radius in blocks within which players will receive the emote packet
     */
    public static void sendAnimationToNearbyPlayers(Entity sourceEntity, String animationName, int radius) {
        if (sourceEntity == null || animationName == null || animationName.isEmpty()) {
            Marallyzen.LOGGER.warn("NpcAnimationHandler: Invalid parameters for sendAnimationToNearbyPlayers");
            return;
        }
        Marallyzen.LOGGER.info(
            "NpcAnimationHandler: sendAnimationToNearbyPlayers(entity={}, emote={}, radius={})",
            sourceEntity.getName().getString(),
            animationName,
            radius
        );

        if (!(sourceEntity.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            Marallyzen.LOGGER.debug("NpcAnimationHandler: Entity is not in a ServerLevel");
            return;
        }

        AnimationPacket packet = new AnimationPacket(sourceEntity.getUUID(), animationName, radius);

        double radiusSquared = (double) radius * radius;
        int sentCount = 0;

        // Iterate through all players on the server
        for (ServerPlayer targetPlayer : serverLevel.getServer().getPlayerList().getPlayers()) {
            // Skip if same entity or different dimension
            if (targetPlayer.getUUID().equals(sourceEntity.getUUID()) || 
                targetPlayer.level() != sourceEntity.level()) {
                continue;
            }

            // Check if player is within radius
            double distanceSquared = sourceEntity.distanceToSqr(targetPlayer);
            if (distanceSquared <= radiusSquared) {
                NetworkHelper.sendToPlayer(targetPlayer, packet);
                sentCount++;
            }
        }

        Marallyzen.LOGGER.info("NpcAnimationHandler: Sent emote '{}' to {} players within {} blocks of {}", 
                animationName, sentCount, radius, sourceEntity.getName().getString());
    }

    /**
     * Sends an emote packet to all players in the same dimension.
     * 
     * @param sourceEntity The entity performing the emote
     * @param animationName The ID of the emote (e.g., "SPE_Air kiss")
     */
    public static void sendAnimationToDimension(Entity sourceEntity, String animationName) {
        if (sourceEntity == null || animationName == null || animationName.isEmpty()) {
            return;
        }
        Marallyzen.LOGGER.info(
            "NpcAnimationHandler: sendAnimationToDimension(entity={}, emote={})",
            sourceEntity.getName().getString(),
            animationName
        );

        if (!(sourceEntity.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }

        AnimationPacket packet = new AnimationPacket(sourceEntity.getUUID(), animationName, -1);

        for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
            if (player.level() == sourceEntity.level() && 
                !player.getUUID().equals(sourceEntity.getUUID())) {
                NetworkHelper.sendToPlayer(player, packet);
            }
        }
    }

    /**
     * Sends an emote packet to a single player (no broadcast).
     *
     * @param sourceEntity The entity performing the emote
     * @param animationName The ID of the emote
     * @param targetPlayer The player who should see the emote
     */
    public static void sendAnimationToPlayer(Entity sourceEntity, String animationName, ServerPlayer targetPlayer) {
        if (sourceEntity == null || animationName == null || animationName.isEmpty() || targetPlayer == null) {
            return;
        }
        Marallyzen.LOGGER.info(
            "NpcAnimationHandler: sendAnimationToPlayer(entity={}, emote={}, target={})",
            sourceEntity.getName().getString(),
            animationName,
            targetPlayer.getName().getString()
        );
        if (targetPlayer.level() != sourceEntity.level()) {
            return;
        }
        AnimationPacket packet = new AnimationPacket(sourceEntity.getUUID(), animationName, 0);
        NetworkHelper.sendToPlayer(targetPlayer, packet);
    }
}
