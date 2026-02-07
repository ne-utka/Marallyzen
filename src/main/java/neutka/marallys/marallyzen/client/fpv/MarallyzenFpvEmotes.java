package neutka.marallys.marallyzen.client.fpv;

import net.minecraft.resources.Identifier;

import java.util.Set;

/**
 * Registry of emotes allowed to use FPV rendering under Marallyzen control.
 */
public final class MarallyzenFpvEmotes {
    private static final Set<Identifier> ALLOWED = Set.of(
            Identifier.fromNamespaceAndPath("marallyzen", "lever_grab_shake"),
            Identifier.fromNamespaceAndPath("marallyzen", "lever_shake"),
            Identifier.fromNamespaceAndPath("marallyzen", "lever_down"),
            Identifier.fromNamespaceAndPath("marallyzen", "valve_grab"),
            Identifier.fromNamespaceAndPath("marallyzen", "valve_spin"),
            Identifier.fromNamespaceAndPath("marallyzen", "valve_done")
    );

    private MarallyzenFpvEmotes() {}

    public static boolean isAllowed(Identifier emoteId) {
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
            || path.equalsIgnoreCase("lever_shake")
            || path.equalsIgnoreCase("lever_down")
            || path.equalsIgnoreCase("valve_grab")
            || path.equalsIgnoreCase("valve_spin")
            || path.equalsIgnoreCase("valve_done");
    }
}


