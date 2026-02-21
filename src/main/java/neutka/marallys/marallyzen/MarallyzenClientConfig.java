package neutka.marallys.marallyzen;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class MarallyzenClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue VANILLA_ITEM_TEXTURE_OVERRIDES = BUILDER
            .comment("Replace vanilla item textures with Marallyzen overrides.")
            .translation("marallyzen.configuration.vanillaItemTextureOverrides")
            .define("vanillaItemTextureOverrides", false);

    public static final ModConfigSpec.BooleanValue INTERACTIVE_BLOCK_OUTLINE = BUILDER
            .comment("Show interactive block outline highlights.")
            .translation("marallyzen.configuration.interactiveBlockOutline")
            .define("interactiveBlockOutline", false);

    public static final ModConfigSpec.BooleanValue INTERACTIVE_PROMPT_BACKGROUND = BUILDER
            .comment("Show dark background for interactive 3D prompts.")
            .translation("marallyzen.configuration.interactivePromptBackground")
            .define("interactivePromptBackground", true);

    public static final ModConfigSpec.BooleanValue NARRATION_HUD_BACKGROUND = BUILDER
            .comment("Show dark background for narration HUD text.")
            .translation("marallyzen.configuration.narrationHudBackground")
            .define("narrationHudBackground", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private MarallyzenClientConfig() {
    }
}
