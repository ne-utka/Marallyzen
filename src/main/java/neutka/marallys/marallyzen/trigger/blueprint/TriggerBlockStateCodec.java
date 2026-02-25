package neutka.marallys.marallyzen.trigger.blueprint;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class TriggerBlockStateCodec {
    private TriggerBlockStateCodec() {
    }

    public static String encode(BlockState state) {
        if (state == null) {
            return "minecraft:air";
        }
        Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (id == null) {
            return "minecraft:air";
        }
        StringBuilder builder = new StringBuilder(id.toString());
        List<Property<?>> properties = new ArrayList<>(state.getProperties());
        if (!properties.isEmpty()) {
            properties.sort(Comparator.comparing(Property::getName));
            builder.append('[');
            for (int i = 0; i < properties.size(); i++) {
                Property<?> property = properties.get(i);
                if (i > 0) {
                    builder.append(',');
                }
                builder.append(property.getName()).append('=').append(getValueName(state, property));
            }
            builder.append(']');
        }
        return builder.toString();
    }

    public static BlockState decode(String raw) {
        if (raw == null || raw.isBlank()) {
            return Blocks.AIR.defaultBlockState();
        }
        String text = raw.trim();
        String blockIdText = text;
        String propsText = "";

        int propsStart = text.indexOf('[');
        if (propsStart >= 0) {
            int propsEnd = text.lastIndexOf(']');
            if (propsEnd <= propsStart) {
                return Blocks.AIR.defaultBlockState();
            }
            blockIdText = text.substring(0, propsStart).trim();
            propsText = text.substring(propsStart + 1, propsEnd).trim();
        }

        Identifier blockId;
        try {
            blockId = Identifier.parse(blockIdText.toLowerCase(Locale.ROOT));
        } catch (Exception e) {
            return Blocks.AIR.defaultBlockState();
        }

        Block block = BuiltInRegistries.BLOCK.get(blockId).map(Holder.Reference::value).orElse(null);
        if (block == null) {
            return Blocks.AIR.defaultBlockState();
        }

        BlockState state = block.defaultBlockState();
        if (propsText.isBlank()) {
            return state;
        }

        String[] pairs = propsText.split(",");
        for (String pair : pairs) {
            int eq = pair.indexOf('=');
            if (eq <= 0 || eq >= pair.length() - 1) {
                continue;
            }
            String key = pair.substring(0, eq).trim();
            String value = pair.substring(eq + 1).trim();
            Property<?> property = block.getStateDefinition().getProperty(key);
            if (property == null) {
                continue;
            }
            state = applyProperty(state, property, value);
        }
        return state;
    }

    private static <T extends Comparable<T>> String getValueName(BlockState state, Property<T> property) {
        T value = state.getValue(property);
        return property.getName(value);
    }

    private static <T extends Comparable<T>> BlockState applyProperty(BlockState state, Property<T> property, String raw) {
        return property.getValue(raw).map(value -> state.setValue(property, value)).orElse(state);
    }
}
