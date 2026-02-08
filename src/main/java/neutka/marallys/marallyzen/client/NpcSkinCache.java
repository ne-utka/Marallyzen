package neutka.marallys.marallyzen.client;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.resources.Identifier;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NpcSkinCache {
    private static final NpcSkinCache INSTANCE = new NpcSkinCache();
    private static final Identifier DEFAULT_SKIN = Identifier.withDefaultNamespace("textures/entity/steve.png");

    private final Map<String, SkinEntry> skins = new ConcurrentHashMap<>();

    public static NpcSkinCache getInstance() {
        return INSTANCE;
    }

    public void updateAppearance(String npcId, String texture, String signature, String model) {
        if (npcId == null || npcId.isEmpty()) {
            return;
        }
        SkinEntry entry = skins.computeIfAbsent(npcId, key -> new SkinEntry());
        entry.model = model != null ? model : "default";

        if (texture == null || texture.isEmpty()) {
            entry.texture = DEFAULT_SKIN;
            return;
        }

        GameProfile profile = new GameProfile(stableUuid(npcId), npcId);
        profile.properties().put("textures", new Property("textures", texture, signature));
        SkinManager skinManager = Minecraft.getInstance().getSkinManager();
        skinManager.get(profile).thenAccept(optionalSkin -> {
            if (optionalSkin != null && optionalSkin.isPresent()) {
                var skin = optionalSkin.get();
                if (skin != null && skin.body() != null) {
                    entry.texture = skin.body().texturePath();
                }
            }
        });
    }

    public Identifier getSkin(String npcId) {
        SkinEntry entry = npcId != null ? skins.get(npcId) : null;
        if (entry == null || entry.texture == null) {
            return DEFAULT_SKIN;
        }
        return entry.texture;
    }

    public boolean isSlim(String npcId) {
        SkinEntry entry = npcId != null ? skins.get(npcId) : null;
        return entry != null && "slim".equalsIgnoreCase(entry.model);
    }

    private static UUID stableUuid(String npcId) {
        return UUID.nameUUIDFromBytes(("marallyzen-npc:" + npcId).getBytes(StandardCharsets.UTF_8));
    }

    private static class SkinEntry {
        private Identifier texture;
        private String model;
    }
}


