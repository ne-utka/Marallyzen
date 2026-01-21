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
    public static final DeferredHolder<SoundEvent, SoundEvent> QUEST_TEST_WORLD_QUEST_01_HELLO =
            SOUND_EVENTS.register("quest/test_world_quest/01_hello", () -> SoundEvent.createVariableRangeEvent(id("quest/test_world_quest/01_hello")));
    public static final DeferredHolder<SoundEvent, SoundEvent> QUEST_TEST_WORLD_QUEST_02_POSTERS =
            SOUND_EVENTS.register("quest/test_world_quest/02_posters", () -> SoundEvent.createVariableRangeEvent(id("quest/test_world_quest/02_posters")));
    public static final DeferredHolder<SoundEvent, SoundEvent> QUEST_TEST_WORLD_QUEST_03_MOSCOW =
            SOUND_EVENTS.register("quest/test_world_quest/03_moscow", () -> SoundEvent.createVariableRangeEvent(id("quest/test_world_quest/03_moscow")));
    public static final DeferredHolder<SoundEvent, SoundEvent> QUEST_TEST_WORLD_QUEST_04_POSTERS_DONE =
            SOUND_EVENTS.register("quest/test_world_quest/04_posters_done", () -> SoundEvent.createVariableRangeEvent(id("quest/test_world_quest/04_posters_done")));
    public static final DeferredHolder<SoundEvent, SoundEvent> QUEST_TEST_WORLD_QUEST_05_DICTAPHONE_PROMPT =
            SOUND_EVENTS.register("quest/test_world_quest/05_dictaphone_prompt", () -> SoundEvent.createVariableRangeEvent(id("quest/test_world_quest/05_dictaphone_prompt")));
    public static final DeferredHolder<SoundEvent, SoundEvent> QUEST_TEST_WORLD_QUEST_06_DICTAPHONE_DONE_1 =
            SOUND_EVENTS.register("quest/test_world_quest/06_dictaphone_done_1", () -> SoundEvent.createVariableRangeEvent(id("quest/test_world_quest/06_dictaphone_done_1")));
    public static final DeferredHolder<SoundEvent, SoundEvent> QUEST_TEST_WORLD_QUEST_07_DICTAPHONE_DONE_2 =
            SOUND_EVENTS.register("quest/test_world_quest/07_dictaphone_done_2", () -> SoundEvent.createVariableRangeEvent(id("quest/test_world_quest/07_dictaphone_done_2")));
    public static final DeferredHolder<SoundEvent, SoundEvent> QUEST_TEST_WORLD_QUEST_08_TV_PROMPT =
            SOUND_EVENTS.register("quest/test_world_quest/08_tv_prompt", () -> SoundEvent.createVariableRangeEvent(id("quest/test_world_quest/08_tv_prompt")));
    public static final DeferredHolder<SoundEvent, SoundEvent> QUEST_TEST_WORLD_QUEST_09_TV_DONE_1 =
            SOUND_EVENTS.register("quest/test_world_quest/09_tv_done_1", () -> SoundEvent.createVariableRangeEvent(id("quest/test_world_quest/09_tv_done_1")));
    public static final DeferredHolder<SoundEvent, SoundEvent> QUEST_TEST_WORLD_QUEST_10_TV_DONE_2 =
            SOUND_EVENTS.register("quest/test_world_quest/10_tv_done_2", () -> SoundEvent.createVariableRangeEvent(id("quest/test_world_quest/10_tv_done_2")));

    private MarallyzenSounds() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Marallyzen.MODID, path);
    }
}
