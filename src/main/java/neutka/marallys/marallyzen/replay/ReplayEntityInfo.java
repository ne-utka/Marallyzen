package neutka.marallys.marallyzen.replay;

import java.util.UUID;

public record ReplayEntityInfo(UUID id, String entityTypeId, boolean player, String name,
                               String skinValue, String skinSignature) {
}
