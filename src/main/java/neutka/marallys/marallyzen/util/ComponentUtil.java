package neutka.marallys.marallyzen.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.RegistryOps;
import neutka.marallys.marallyzen.Marallyzen;

import java.util.Optional;

public final class ComponentUtil {
    private ComponentUtil() {
    }

    public static Optional<Component> fromJson(String json, RegistryAccess registryAccess) {
        if (json == null || json.isEmpty()) {
            return Optional.empty();
        }
        return ComponentSerialization.CODEC
            .parse(RegistryOps.create(JsonOps.INSTANCE, registryAccess), JsonParser.parseString(json))
            .resultOrPartial(error -> Marallyzen.LOGGER.warn("Component JSON parse error: {}", error));
    }

    public static Optional<String> toJson(Component component, RegistryAccess registryAccess) {
        if (component == null) {
            return Optional.empty();
        }
        return ComponentSerialization.CODEC
            .encodeStart(RegistryOps.create(JsonOps.INSTANCE, registryAccess), component)
            .resultOrPartial(error -> Marallyzen.LOGGER.warn("Component JSON encode error: {}", error))
            .map(JsonElement::toString);
    }
}
