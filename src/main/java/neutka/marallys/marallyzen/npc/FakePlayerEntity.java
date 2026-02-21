package neutka.marallys.marallyzen.npc;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fake player entity for NPCs with custom skins.
 * Based on Denizen's EntityFakePlayerImpl but adapted for NeoForge.
 */
public class FakePlayerEntity extends ServerPlayer {

    private static final Pattern TEXTURE_URL_HASH_PATTERN =
            Pattern.compile("https?://textures\\.minecraft\\.net/texture/([0-9a-fA-F]+)");
    
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
        String normalizedTexture = normalizeTexturePayload(texture);

        // Try to extract UUID from texture JSON if available
        UUID baseUuid = null;
        if (normalizedTexture != null && !normalizedTexture.isEmpty()) {
            try {
                // Decode base64 texture JSON to extract profileId
                byte[] decodedBytes = java.util.Base64.getDecoder().decode(normalizedTexture);
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
        
        if (normalizedTexture != null && !normalizedTexture.isEmpty()) {
            // DO NOT modify texture JSON - it will invalidate the signature!
            // Minecraft determines slim model ONLY by UUID, not by JSON content
            // The UUID already has the correct bit set above
            String normalizedSignature = signature != null ? signature.trim() : "";
            Property skinProperty = normalizedSignature.isEmpty()
                    ? new Property("textures", normalizedTexture)
                    : new Property("textures", normalizedTexture, normalizedSignature);
            GameProfile profileWithTexture = createProfileWithProperty(uuid, name, "textures", skinProperty);
            if (profileWithTexture != null) {
                // Verify the UUID has the correct bit set
                boolean slimBitSet = (uuid.getLeastSignificantBits() & 0x1L) == 0x1L;
                Marallyzen.LOGGER.info("Created GameProfile - Name: {}, UUID: {}, Model: {}, Slim bit set: {}, Expected slim: {}", 
                        name, uuid, model, slimBitSet, "slim".equalsIgnoreCase(model));
                if ("slim".equalsIgnoreCase(model) && !slimBitSet) {
                    Marallyzen.LOGGER.error("ERROR: Slim model requested but UUID does not have slim bit set! UUID: {}", uuid);
                }
                return profileWithTexture;
            }
            // Fallback for environments without PropertyMap constructor
            GameProfile profile = new GameProfile(uuid, name != null ? name : "NPC");
            addProfileProperty(profile, "textures", skinProperty);
            
            // Verify the UUID has the correct bit set
            boolean slimBitSet = (uuid.getLeastSignificantBits() & 0x1L) == 0x1L;
            Marallyzen.LOGGER.info("Created GameProfile - Name: {}, UUID: {}, Model: {}, Slim bit set: {}, Expected slim: {}", 
                    name, uuid, model, slimBitSet, "slim".equalsIgnoreCase(model));
            
            if ("slim".equalsIgnoreCase(model) && !slimBitSet) {
                Marallyzen.LOGGER.error("ERROR: Slim model requested but UUID does not have slim bit set! UUID: {}", uuid);
            }
            return profile;
        }

        return new GameProfile(uuid, name != null ? name : "NPC");
    }

    private static String normalizeTexturePayload(String texture) {
        if (texture == null) {
            return null;
        }
        String value = texture.trim();
        if (value.isEmpty()) {
            return null;
        }

        // Accept only payloads that decode to a textures.minecraft.net URL with a sane hash length.
        // Invalid hashes (like the 69-char value seen in logs) cause repeated 404 spam on the client.
        try {
            byte[] decodedBytes = java.util.Base64.getDecoder().decode(value);
            String json = new String(decodedBytes, java.nio.charset.StandardCharsets.UTF_8);
            com.google.gson.JsonObject root = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
            if (!root.has("textures")) {
                return null;
            }
            com.google.gson.JsonObject textures = root.getAsJsonObject("textures");
            if (textures == null || !textures.has("SKIN")) {
                return null;
            }
            com.google.gson.JsonObject skin = textures.getAsJsonObject("SKIN");
            if (skin == null || !skin.has("url")) {
                return null;
            }
            String url = skin.get("url").getAsString();
            Matcher matcher = TEXTURE_URL_HASH_PATTERN.matcher(url);
            if (!matcher.matches()) {
                Marallyzen.LOGGER.warn("Skipping NPC skin texture: malformed URL '{}'", url);
                return null;
            }
            String hash = matcher.group(1);
            if (hash.length() != 64) {
                Marallyzen.LOGGER.warn("Skipping NPC skin texture: invalid textures.minecraft.net hash length {} for URL '{}'", hash.length(), url);
                return null;
            }
            return value;
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("Skipping NPC skin texture: invalid base64 payload ({})", e.getMessage());
            return null;
        }
    }

    private static void addProfileProperty(GameProfile profile, String key, Property property) {
        if (profile == null || property == null) {
            return;
        }
        try {
            PropertyMap map = null;
            try {
                Object props = profile.getClass().getMethod("getProperties").invoke(profile);
                if (props instanceof PropertyMap pm) {
                    map = pm;
                }
            } catch (NoSuchMethodException ignored) {
                Object props = profile.getClass().getMethod("properties").invoke(profile);
                if (props instanceof PropertyMap pm) {
                    map = pm;
                }
            }
            if (map == null) {
                return;
            }
            map.put(key, property);
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("Failed to set GameProfile property {}", key, e);
        }
    }

    private static GameProfile createProfileWithProperty(UUID uuid, String name, String key, Property property) {
        try {
            com.google.common.collect.Multimap<String, Property> backing = com.google.common.collect.HashMultimap.create();
            backing.put(key, property);
            PropertyMap props = new PropertyMap(backing);
            for (var ctor : GameProfile.class.getDeclaredConstructors()) {
                Class<?>[] params = ctor.getParameterTypes();
                Object[] args = new Object[params.length];
                boolean supported = true;
                for (int i = 0; i < params.length; i++) {
                    Class<?> param = params[i];
                    if (param == UUID.class) {
                        args[i] = uuid;
                    } else if (param == String.class) {
                        args[i] = name != null ? name : "NPC";
                    } else if (PropertyMap.class.isAssignableFrom(param)) {
                        args[i] = props;
                    } else if (param == boolean.class || param == Boolean.class) {
                        args[i] = false;
                    } else if (param == int.class || param == Integer.class) {
                        args[i] = 0;
                    } else if (param == long.class || param == Long.class) {
                        args[i] = 0L;
                    } else {
                        supported = false;
                        break;
                    }
                }
                if (!supported) {
                    continue;
                }
                ctor.setAccessible(true);
                return (GameProfile) ctor.newInstance(args);
            }
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("Failed to construct GameProfile with properties", e);
        }
        return null;
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
