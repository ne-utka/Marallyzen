package neutka.marallys.marallyzen.npc;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import neutka.marallys.marallyzen.Marallyzen;

import java.util.UUID;

/**
 * Fake player entity for NPCs with custom skins.
 * Based on Denizen's EntityFakePlayerImpl but adapted for NeoForge.
 */
public class FakePlayerEntity extends ServerPlayer {
    
    public FakePlayerEntity(MinecraftServer server, ServerLevel level, GameProfile gameProfile) {
        super(server, level, gameProfile, ClientInformation.createDefault());
        
        try {
            // Create fake network connection to prevent NullPointerException
            FakeNetworkManagerImpl networkManager = new FakeNetworkManagerImpl(PacketFlow.CLIENTBOUND);
            CommonListenerCookie cookie = new CommonListenerCookie(gameProfile, 0, ClientInformation.createDefault(), false);
            connection = new FakePlayerConnectionImpl(server, networkManager, this, cookie);
            // Note: Connection.setListener() is called automatically by ServerGamePacketListenerImpl constructor
            // No need to set it manually
        } catch (Exception e) {
            Marallyzen.LOGGER.error("Failed to create fake network connection for NPC", e);
        }
        
        // Set player mode customization to show all layers
        getEntityData().set(Player.DATA_PLAYER_MODE_CUSTOMISATION, (byte) 127);
        
        // Set basic properties
        setGameMode(GameType.SURVIVAL);
        setInvisible(false);
    }
    
    /**
     * Applies skin texture to the GameProfile.
     */
    public static GameProfile createGameProfileWithSkin(String name, String texture, String signature, String model) {
        // Try to extract UUID from texture JSON if available
        UUID baseUuid = null;
        if (texture != null && !texture.isEmpty()) {
            try {
                // Decode base64 texture JSON to extract profileId
                byte[] decodedBytes = java.util.Base64.getDecoder().decode(texture);
                String textureJson = new String(decodedBytes, java.nio.charset.StandardCharsets.UTF_8);
                com.google.gson.JsonObject jsonObject = com.google.gson.JsonParser.parseString(textureJson).getAsJsonObject();
                if (jsonObject.has("profileId")) {
                    String profileIdStr = jsonObject.get("profileId").getAsString();
                    // Convert hex string to UUID (format: 7da2ab3a93ca48ee83048afc3b80e68e)
                    // UUID format: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
                    if (profileIdStr.length() == 32) {
                        String uuidStr = profileIdStr.substring(0, 8) + "-" + 
                                        profileIdStr.substring(8, 12) + "-" + 
                                        profileIdStr.substring(12, 16) + "-" + 
                                        profileIdStr.substring(16, 20) + "-" + 
                                        profileIdStr.substring(20, 32);
                        baseUuid = UUID.fromString(uuidStr);
                        Marallyzen.LOGGER.debug("Extracted UUID from texture JSON: {}", baseUuid);
                    }
                }
            } catch (Exception e) {
                Marallyzen.LOGGER.debug("Could not extract UUID from texture JSON: {}", e.getMessage());
            }
        }
        
        // Fallback to name-based UUID if extraction failed
        if (baseUuid == null) {
            baseUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + (name != null ? name : "NPC")).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            Marallyzen.LOGGER.debug("Using name-based UUID: {}", baseUuid);
        }
        
        // In Minecraft, slim model is determined by the least significant bit of the UUID
        // If (uuid.getLeastSignificantBits() & 0x1) == 0x1, then slim model is used
        UUID uuid;
        if ("slim".equalsIgnoreCase(model)) {
            // For slim model, set the least significant bit to 1
            long mostSignificant = baseUuid.getMostSignificantBits();
            long leastSignificant = baseUuid.getLeastSignificantBits();
            // Set the least significant bit to 1 for slim model
            // This is the standard way Minecraft determines slim vs wide model
            leastSignificant = (leastSignificant & 0xFFFFFFFFFFFFFFFEL) | 0x1L;
            uuid = new UUID(mostSignificant, leastSignificant);
            Marallyzen.LOGGER.info("Creating GameProfile with SLIM model - Name: {}, Base UUID: {}, Final UUID: {}, LSB: {}, slim bit: {}", 
                    name, baseUuid, uuid, leastSignificant, (leastSignificant & 0x1L) == 0x1L);
        } else {
            // For default model, set the least significant bit to 0
            long mostSignificant = baseUuid.getMostSignificantBits();
            long leastSignificant = baseUuid.getLeastSignificantBits();
            // Clear the least significant bit for default model
            leastSignificant = leastSignificant & 0xFFFFFFFFFFFFFFFEL;
            uuid = new UUID(mostSignificant, leastSignificant);
            Marallyzen.LOGGER.info("Creating GameProfile with DEFAULT model - Name: {}, Base UUID: {}, Final UUID: {}, LSB: {}, slim bit: {}", 
                    name, baseUuid, uuid, leastSignificant, (leastSignificant & 0x1L) == 0x1L);
        }
        
        GameProfile profile = new GameProfile(uuid, name != null ? name : "NPC");
        
        if (texture != null && !texture.isEmpty()) {
            // DO NOT modify texture JSON - it will invalidate the signature!
            // Minecraft determines slim model ONLY by UUID, not by JSON content
            // The UUID already has the correct bit set above
            Property skinProperty = new Property("textures", texture, signature != null ? signature : "");
            profile.getProperties().put("textures", skinProperty);
            
            // Verify the UUID has the correct bit set
            boolean slimBitSet = (uuid.getLeastSignificantBits() & 0x1L) == 0x1L;
            Marallyzen.LOGGER.info("Created GameProfile - Name: {}, UUID: {}, Model: {}, Slim bit set: {}, Expected slim: {}", 
                    name, uuid, model, slimBitSet, "slim".equalsIgnoreCase(model));
            
            if ("slim".equalsIgnoreCase(model) && !slimBitSet) {
                Marallyzen.LOGGER.error("ERROR: Slim model requested but UUID does not have slim bit set! UUID: {}", uuid);
            }
        }
        
        return profile;
    }
    
    @Override
    public boolean isSpectator() {
        return false;
    }
    
    @Override
    public boolean isCreative() {
        return false;
    }
}

