package neutka.marallys.marallyzen.audio;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import neutka.marallys.marallyzen.Marallyzen;

public final class MarallyzenSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, Marallyzen.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> TV =
            SOUND_EVENTS.register("tv", () -> SoundEvent.createVariableRangeEvent(id("tv")));
    public static final DeferredHolder<SoundEvent, SoundEvent> TV_ON =
            SOUND_EVENTS.register("tv_on", () -> SoundEvent.createVariableRangeEvent(id("tv_on")));
    public static final DeferredHolder<SoundEvent, SoundEvent> TV_OFF =
            SOUND_EVENTS.register("tv_off", () -> SoundEvent.createVariableRangeEvent(id("tv_off")));
    public static final DeferredHolder<SoundEvent, SoundEvent> FLASHLIGHT_TURN_ON =
            SOUND_EVENTS.register("flashlight_turn_on", () -> SoundEvent.createVariableRangeEvent(id("flashlight_turn_on")));
    public static final DeferredHolder<SoundEvent, SoundEvent> FLASHLIGHT_TURN_OFF =
            SOUND_EVENTS.register("flashlight_turn_off", () -> SoundEvent.createVariableRangeEvent(id("flashlight_turn_off")));
    public static final DeferredHolder<SoundEvent, SoundEvent> POSTER_WELCOME =
            SOUND_EVENTS.register("poster_welcome", () -> SoundEvent.createVariableRangeEvent(id("poster_welcome")));
    public static final DeferredHolder<SoundEvent, SoundEvent> POSTER_BYE =
            SOUND_EVENTS.register("poster_bye", () -> SoundEvent.createVariableRangeEvent(id("poster_bye")));
    public static final DeferredHolder<SoundEvent, SoundEvent> POSTER_SHAKE =
            SOUND_EVENTS.register("poster_shake", () -> SoundEvent.createVariableRangeEvent(id("poster_shake")));
    public static final DeferredHolder<SoundEvent, SoundEvent> DICTAPHONE_START =
            SOUND_EVENTS.register("dictophone_start", () -> SoundEvent.createVariableRangeEvent(id("dictophone_start")));
    public static final DeferredHolder<SoundEvent, SoundEvent> DICTAPHONE_STOP =
            SOUND_EVENTS.register("dictophone_stop", () -> SoundEvent.createVariableRangeEvent(id("dictophone_stop")));

    private MarallyzenSounds() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Marallyzen.MODID, path);
    }
}
