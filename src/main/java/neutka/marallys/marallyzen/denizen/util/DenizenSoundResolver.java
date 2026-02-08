package neutka.marallys.marallyzen.denizen.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class DenizenSoundResolver {
    private DenizenSoundResolver() {
    }

    public static SoundEvent resolve(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        Identifier id;
        if (input.contains(":")) {
            id = Identifier.tryParse(input.toLowerCase());
        }
        else {
            String name = input.toLowerCase().replace('_', '.');
            id = Identifier.tryParse("minecraft:" + name);
        }
        if (id == null) {
            return null;
        }
        return BuiltInRegistries.SOUND_EVENT.getValue(id);
    }
}


