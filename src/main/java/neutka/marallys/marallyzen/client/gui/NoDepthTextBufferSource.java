package neutka.marallys.marallyzen.client.gui;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.resources.ResourceLocation;
import neutka.marallys.marallyzen.client.NoDepthTextRenderType;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class NoDepthTextBufferSource implements MultiBufferSource {
    private final MultiBufferSource delegate;

    private static final Field TEXTURE_STATE_FIELD = findTextureStateField();
    private static final Field TEXTURE_FIELD = findTextureField();
    private static final Map<Class<?>, Field> COMPOSITE_STATE_FIELDS = new ConcurrentHashMap<>();

    public NoDepthTextBufferSource(MultiBufferSource delegate) {
        this.delegate = delegate;
    }

    @Override
    public com.mojang.blaze3d.vertex.VertexConsumer getBuffer(RenderType type) {
        if (isTextRenderType(type)) {
            ResourceLocation texture = extractTexture(type);
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

    private static ResourceLocation extractTexture(RenderType type) {
        if (TEXTURE_STATE_FIELD == null || TEXTURE_FIELD == null) {
            return null;
        }
        try {
            Field compositeField = COMPOSITE_STATE_FIELDS.computeIfAbsent(type.getClass(), NoDepthTextBufferSource::findCompositeStateField);
            if (compositeField == null) {
                return null;
            }
            Object compositeState = compositeField.get(type);
            if (compositeState == null) {
                return null;
            }
            Object textureState = TEXTURE_STATE_FIELD.get(compositeState);
            if (textureState == null) {
                return null;
            }
            Object value = TEXTURE_FIELD.get(textureState);
            return value instanceof ResourceLocation rl ? rl : null;
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    private static Field findCompositeStateField(Class<?> typeClass) {
        Class<?> current = typeClass;
        while (current != null && RenderType.class.isAssignableFrom(current)) {
            for (Field field : current.getDeclaredFields()) {
                if (field.getType().getName().contains("CompositeState")) {
                    field.setAccessible(true);
                    return field;
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private static Field findTextureStateField() {
        for (Field field : RenderType.CompositeState.class.getDeclaredFields()) {
            if (field.getType() == RenderStateShard.TextureStateShard.class) {
                field.setAccessible(true);
                return field;
            }
        }
        return null;
    }

    private static Field findTextureField() {
        for (Field field : RenderStateShard.TextureStateShard.class.getDeclaredFields()) {
            if (field.getType() == ResourceLocation.class) {
                field.setAccessible(true);
                return field;
            }
        }
        return null;
    }
}
