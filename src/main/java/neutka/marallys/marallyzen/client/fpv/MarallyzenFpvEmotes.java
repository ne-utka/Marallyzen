package neutka.marallys.marallyzen.client.fpv;

import net.minecraft.resources.ResourceLocation;

import java.util.Set;

/**
 * Registry of emotes allowed to use FPV rendering under Marallyzen control.
 */
public final class MarallyzenFpvEmotes {
    private static final Set<ResourceLocation> ALLOWED = Set.of(
            ResourceLocation.fromNamespaceAndPath("marallyzen", "spe_interactive_chain_jump"),
            ResourceLocation.fromNamespaceAndPath("marallyzen", "chain_hang_base"),
            ResourceLocation.fromNamespaceAndPath("marallyzen", "lever_grab_shake"),
            ResourceLocation.fromNamespaceAndPath("marallyzen", "lever_down")
    );

    private MarallyzenFpvEmotes() {}

    public static boolean isAllowed(ResourceLocation emoteId) {
        if (emoteId == null) {
            return false;
        }
        if (ALLOWED.contains(emoteId)) {
            return true;
        }
        String path = emoteId.getPath();
        if (path == null) {
            return false;
        }
        // Allow config-sourced emotes without strict namespace match
        return path.equalsIgnoreCase("lever_grab_shake")
            || path.equalsIgnoreCase("lever_down");
    }
}
