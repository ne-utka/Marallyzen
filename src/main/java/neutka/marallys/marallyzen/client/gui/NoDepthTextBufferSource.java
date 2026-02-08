package neutka.marallys.marallyzen.client.gui;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import neutka.marallys.marallyzen.client.NoDepthTextRenderType;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.IdentityHashMap;
import java.util.Locale;

public final class NoDepthTextBufferSource implements MultiBufferSource {
    private final MultiBufferSource delegate;

    public NoDepthTextBufferSource(MultiBufferSource delegate) {
        this.delegate = delegate;
    }

    @Override
    public com.mojang.blaze3d.vertex.VertexConsumer getBuffer(RenderType type) {
        if (isTextRenderType(type)) {
            Identifier texture = extractTexture(type);
            if (texture != null) {
                return delegate.getBuffer(NoDepthTextRenderType.textNoDepth(texture));
            }
        }
        return delegate.getBuffer(type);
    }

    private static boolean isTextRenderType(RenderType type) {
        String name = type.toString().toLowerCase(Locale.ROOT);
        return name.contains("text");
    }

    private static Identifier extractTexture(RenderType type) {
        return findResourceLocation(type, new IdentityHashMap<>());
    }

    private static Identifier findResourceLocation(Object value, IdentityHashMap<Object, Boolean> seen) {
        if (value == null) {
            return null;
        }
        if (value instanceof Identifier rl) {
            return rl;
        }
        if (value.getClass().isPrimitive() || value.getClass().isEnum() || value instanceof String) {
            return null;
        }
        if (seen.put(value, Boolean.TRUE) != null) {
            return null;
        }
        for (Field field : value.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            field.setAccessible(true);
            try {
                Identifier found = findResourceLocation(field.get(value), seen);
                if (found != null) {
                    return found;
                }
            } catch (IllegalAccessException ignored) {
                // ignore inaccessible fields
            }
        }
        return null;
    }
}








