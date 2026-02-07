package neutka.marallys.marallyzen.client;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class NoDepthTextRenderType {
    private static final Map<Identifier, RenderType> CACHE = new ConcurrentHashMap<>();

    private NoDepthTextRenderType() {
    }

    public static RenderType textNoDepth(Identifier texture) {
        return CACHE.computeIfAbsent(texture, RenderTypes::textSeeThrough);
    }
}







