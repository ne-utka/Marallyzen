package neutka.marallys.marallyzen;

import net.neoforged.neoforge.common.ModConfigSpec;

public class MarallyzenConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue DEBUG_LOG = BUILDER
            .comment("Enable additional debug logging.")
            .define("debugLog", false);

    static final ModConfigSpec SPEC = BUILDER.build();
}


