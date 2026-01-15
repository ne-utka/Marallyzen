package neutka.marallys.marallyzen.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility codecs for network serialization.
 * Based on Clientizen's Codecs but adapted for NeoForge.
 */
public class NetworkCodecs {

    public static final StreamCodec<RegistryFriendlyByteBuf, String> STRING = StreamCodec.of(
            NetworkCodecs::writeString,
            NetworkCodecs::readString
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, Map<String, String>> STRING_MAP = StreamCodec.of(
            NetworkCodecs::writeStringMap,
            NetworkCodecs::readStringMap
    );

    public static <T> StreamCodec<RegistryFriendlyByteBuf, T> nullable(StreamCodec<RegistryFriendlyByteBuf, T> codec) {
        return StreamCodec.of(
                (buf, value) -> {
                    buf.writeBoolean(value != null);
                    if (value != null) {
                        codec.encode(buf, value);
                    }
                },
                buf -> buf.readBoolean() ? codec.decode(buf) : null
        );
    }

    private static void writeString(RegistryFriendlyByteBuf buf, String str) {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        buf.writeInt(bytes.length);
        buf.writeBytes(bytes);
    }

    private static String readString(RegistryFriendlyByteBuf buf) {
        byte[] bytes = new byte[buf.readInt()];
        buf.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static void writeStringMap(RegistryFriendlyByteBuf buf, Map<String, String> map) {
        buf.writeInt(map.size());
        for (Map.Entry<String, String> entry : map.entrySet()) {
            writeString(buf, entry.getKey());
            writeString(buf, entry.getValue());
        }
    }

    private static Map<String, String> readStringMap(RegistryFriendlyByteBuf buf) {
        int size = buf.readInt();
        Map<String, String> map = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            String key = readString(buf);
            String value = readString(buf);
            map.put(key, value);
        }
        return map;
    }
}




