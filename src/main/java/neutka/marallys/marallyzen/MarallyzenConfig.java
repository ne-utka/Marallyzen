package neutka.marallys.marallyzen;

import net.neoforged.neoforge.common.ModConfigSpec;

public class MarallyzenConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue DEBUG_LOG = BUILDER
            .comment("Enable additional debug logging.")
            .translation("marallyzen.configuration.debugLog")
            .define("debugLog", false);

    public static final ModConfigSpec.DoubleValue PLASMO_AUDIO_VOLUME = BUILDER
            .comment("Volume multiplier for Marallyzen audio played via PlasmoVoice.")
            .translation("marallyzen.configuration.plasmovoiceAudioVolume")
            .defineInRange("plasmovoiceAudioVolume", 1.0, 0.0, 2.0);

    public static final ModConfigSpec SPEC = BUILDER.build();
}


