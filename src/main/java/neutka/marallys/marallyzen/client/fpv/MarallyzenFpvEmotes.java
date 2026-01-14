package neutka.marallys.marallyzen.client.fpv;

import net.minecraft.resources.ResourceLocation;

import java.util.Set;

/**
 * Registry of emotes allowed to use FPV rendering under Marallyzen control.
 */
public final class MarallyzenFpvEmotes {
    private static final Set<ResourceLocation> ALLOWED = Set.of(
            ResourceLocation.fromNamespaceAndPath("marallyzen", "spe_interactive_chain_jump"),
            ResourceLocation.fromNamespaceAndPath("marallyzen", "chain_hang_base")
    );

    private MarallyzenFpvEmotes() {}

    public static boolean isAllowed(ResourceLocation emoteId) {
        return ALLOWED.contains(emoteId);
    }
}
