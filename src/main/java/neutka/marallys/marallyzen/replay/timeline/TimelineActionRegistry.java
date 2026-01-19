package neutka.marallys.marallyzen.replay.timeline;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import neutka.marallys.marallyzen.client.emote.ClientEmoteHandler;
import neutka.marallys.marallyzen.client.narration.NarrationManager;
import neutka.marallys.marallyzen.replay.camera.ReplayCameraDirector;

public final class TimelineActionRegistry {
    private static final Map<String, TimelineAction> ACTIONS = new HashMap<>();

    private TimelineActionRegistry() {
    }

    public static void registerDefaults() {
        register("camera", (event, scheduler) -> {
            String trackId = event.getValue();
            if (trackId == null || trackId.isBlank()) {
                return;
            }
            ReplayCameraDirector.getInstance().playTrack(trackId);
        });

        register("dialog", (event, scheduler) -> {
            String text = event.getValue();
            if (text == null || text.isBlank()) {
                return;
            }
            JsonObject data = event.getData();
            String speaker = data != null && data.has("speaker") ? data.get("speaker").getAsString() : null;
            int fadeIn = data != null && data.has("fadeIn") ? data.get("fadeIn").getAsInt() : 5;
            int stay = data != null && data.has("stay") ? data.get("stay").getAsInt() : 60;
            int fadeOut = data != null && data.has("fadeOut") ? data.get("fadeOut").getAsInt() : 10;

            Component message = speaker != null && !speaker.isBlank()
                ? Component.literal(speaker + ": " + text)
                : Component.literal(text);
            NarrationManager.getInstance().startNarration(message, null, fadeIn, stay, fadeOut);
        });

        register("sound", (event, scheduler) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) {
                return;
            }
            JsonObject data = event.getData();
            String soundId = event.getValue();
            if (data != null && data.has("sound")) {
                soundId = data.get("sound").getAsString();
            }
            if (soundId == null || soundId.isBlank()) {
                return;
            }
            ResourceLocation soundKey = ResourceLocation.tryParse(soundId);
            if (soundKey == null) {
                return;
            }
            SoundEvent soundEvent = BuiltInRegistries.SOUND_EVENT.get(soundKey);
            if (soundEvent == null) {
                return;
            }
            float volume = data != null && data.has("volume") ? data.get("volume").getAsFloat() : 1.0f;
            float pitch = data != null && data.has("pitch") ? data.get("pitch").getAsFloat() : 1.0f;
            SoundSource category = SoundSource.MASTER;
            if (data != null && data.has("category")) {
                String name = data.get("category").getAsString();
                try {
                    category = SoundSource.valueOf(name.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ignored) {
                }
            }
            Vec3 pos = mc.player != null ? mc.player.position() : Vec3.ZERO;
            if (data != null && data.has("pos")) {
                Vec3 parsed = readVec3(data.get("pos"));
                if (parsed != null) {
                    pos = parsed;
                }
            }
            mc.level.playLocalSound(pos.x, pos.y, pos.z, soundEvent, category, volume, pitch, false);
        });

        register("npc_emote", (event, scheduler) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) {
                return;
            }
            String target = event.getTarget();
            String emoteId = event.getValue();
            if (emoteId == null || emoteId.isBlank()) {
                return;
            }
            JsonObject data = event.getData();
            boolean stop = data != null && data.has("stop") && data.get("stop").getAsBoolean();

            Entity entity = resolveTargetEntity(mc, target);
            if (entity == null) {
                return;
            }
            if (stop) {
                ClientEmoteHandler.stop(entity);
            } else {
                ClientEmoteHandler.handleEntity(entity, emoteId, false);
            }
        });

        register("flag", (event, scheduler) -> {
            String flagName = event.getTarget();
            if (flagName == null || flagName.isBlank()) {
                flagName = event.getValue();
                if (flagName == null || flagName.isBlank()) {
                    return;
                }
                scheduler.setFlag(flagName, "true");
                return;
            }
            String value = event.getValue();
            if (value == null || value.isBlank()) {
                value = "true";
            }
            scheduler.setFlag(flagName, value);
        });

        register("wait", (event, scheduler) -> {
            // Intentional no-op: placeholder for human-readable timelines.
        });
    }

    public static void register(String type, TimelineAction action) {
        if (type == null || action == null) {
            return;
        }
        ACTIONS.put(type.toLowerCase(Locale.ROOT), action);
    }

    public static TimelineAction get(String type) {
        if (type == null) {
            return null;
        }
        return ACTIONS.get(type.toLowerCase(Locale.ROOT));
    }

    private static Vec3 readVec3(JsonElement element) {
        if (element == null) {
            return null;
        }
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            if (array.size() >= 3) {
                return new Vec3(array.get(0).getAsDouble(), array.get(1).getAsDouble(), array.get(2).getAsDouble());
            }
        } else if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            return new Vec3(obj.get("x").getAsDouble(), obj.get("y").getAsDouble(), obj.get("z").getAsDouble());
        }
        return null;
    }

    private static Entity resolveTargetEntity(Minecraft mc, String target) {
        if (target == null || target.isBlank() || mc.level == null) {
            return mc.player;
        }
        UUID uuid = null;
        try {
            uuid = UUID.fromString(target);
        } catch (IllegalArgumentException ignored) {
        }
        if (uuid != null) {
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (entity.getUUID().equals(uuid)) {
                    return entity;
                }
            }
        }
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (matchesNpcId(entity, target)) {
                return entity;
            }
            String name = entity.getName().getString();
            if (name != null && name.equalsIgnoreCase(target)) {
                return entity;
            }
        }
        return null;
    }

    private static boolean matchesNpcId(Entity entity, String npcId) {
        if (entity == null || npcId == null || npcId.isBlank()) {
            return false;
        }
        if (entity instanceof neutka.marallys.marallyzen.npc.NpcEntity npc) {
            return npcId.equalsIgnoreCase(npc.getNpcId());
        }
        if (entity instanceof neutka.marallys.marallyzen.npc.GeckoNpcEntity gecko) {
            return npcId.equalsIgnoreCase(gecko.getNpcId());
        }
        return false;
    }
}
