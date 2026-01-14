package neutka.marallys.marallyzen.denizen.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public final class DenizenSoundResolver {
    private DenizenSoundResolver() {
    }

    public static SoundEvent resolve(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        ResourceLocation id;
        if (input.contains(":")) {
            id = ResourceLocation.tryParse(input.toLowerCase());
        }
        else {
            String name = input.toLowerCase().replace('_', '.');
            id = ResourceLocation.tryParse("minecraft:" + name);
        }
        if (id == null) {
            return null;
        }
        return BuiltInRegistries.SOUND_EVENT.get(id);
    }
}
