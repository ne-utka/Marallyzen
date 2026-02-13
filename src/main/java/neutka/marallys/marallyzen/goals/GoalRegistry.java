package neutka.marallys.marallyzen.goals;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import neutka.marallys.marallyzen.Marallyzen;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for goal types and placeholder resolvers.
 */
public final class GoalRegistry {
    @FunctionalInterface
    public interface PlaceholderResolver {
        String resolve(GoalRuntimeContext context, GoalInstance instance);
    }

    private static final GoalRegistry INSTANCE = new GoalRegistry();

    private final Map<String, GoalType> goalTypes = new ConcurrentHashMap<>();
    private final Map<String, GoalType> customHandlers = new ConcurrentHashMap<>();
    private final Map<String, PlaceholderResolver> placeholders = new ConcurrentHashMap<>();

    private GoalRegistry() {
        registerDefaults();
    }

    public static GoalRegistry getInstance() {
        return INSTANCE;
    }

    public void registerGoalType(String id, GoalType goalType) {
        if (id == null || id.isBlank() || goalType == null) {
            return;
        }
        goalTypes.put(id.trim().toLowerCase(Locale.ROOT), goalType);
    }

    public GoalType getGoalType(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        return goalTypes.get(id.trim().toLowerCase(Locale.ROOT));
    }

    public void registerCustomHandler(String id, GoalType goalType) {
        if (id == null || id.isBlank() || goalType == null) {
            return;
        }
        customHandlers.put(id.trim().toLowerCase(Locale.ROOT), goalType);
    }

    public GoalType getCustomHandler(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        return customHandlers.get(id.trim().toLowerCase(Locale.ROOT));
    }

    public void registerPlaceholder(String id, PlaceholderResolver resolver) {
        if (id == null || id.isBlank() || resolver == null) {
            return;
        }
        placeholders.put(id.trim().toLowerCase(Locale.ROOT), resolver);
    }

    public Map<String, String> collectPlaceholderValues(GoalRuntimeContext context, GoalInstance instance) {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, PlaceholderResolver> entry : placeholders.entrySet()) {
            try {
                String value = entry.getValue().resolve(context, instance);
                if (value != null) {
                    result.put(entry.getKey(), value);
                }
            } catch (Exception e) {
                Marallyzen.LOGGER.debug("GoalRegistry placeholder '{}' failed for {}", entry.getKey(), instance.goalId(), e);
            }
        }
        return result;
    }

    private void registerDefaults() {
        registerGoalType("any_item", new AnyItemGoalType());
        registerGoalType("specific_item", new SpecificItemGoalType());
        registerGoalType("item_tag", new ItemTagGoalType());
        registerGoalType("timer", new TimerGoalType());
        registerGoalType("visual_only", new VisualOnlyGoalType());
        registerGoalType("custom", new CustomGoalType(this));

        registerPlaceholder("current_border", (ctx, instance) -> formatBorder(ctx.level().getWorldBorder().getSize()));
        registerPlaceholder("next_border", (ctx, instance) -> {
            double current = ctx.level().getWorldBorder().getSize();
            double next = current;
            for (String command : instance.script().onComplete().commands()) {
                String line = command.trim().toLowerCase(Locale.ROOT);
                if (line.startsWith("worldborder add ")) {
                    String raw = line.substring("worldborder add ".length()).trim();
                    try {
                        next = current + Double.parseDouble(raw);
                    } catch (Exception ignored) {
                    }
                    break;
                }
                if (line.startsWith("worldborder set ")) {
                    String raw = line.substring("worldborder set ".length()).trim();
                    try {
                        next = Double.parseDouble(raw);
                    } catch (Exception ignored) {
                    }
                    break;
                }
            }
            return formatBorder(next);
        });
    }

    private static String formatBorder(double size) {
        if (Math.abs(size - Math.rint(size)) < 0.001D) {
            return Long.toString(Math.round(size));
        }
        return String.format(Locale.ROOT, "%.2f", size);
    }

    private abstract static class AbstractItemCollectGoalType implements GoalType {
        @Override
        public void tick(GoalRuntimeContext context, GoalInstance instance) {
            if (instance.completed()) {
                return;
            }
            int radius = instance.progressData().zoneRadius();
            var center = instance.spawnPos();
            double cx = center.getX() + 0.5D;
            double cy = center.getY() + 0.5D;
            double cz = center.getZ() + 0.5D;
            AABB box = new AABB(cx - radius, cy - 3.0D, cz - radius, cx + radius, cy + 3.0D, cz + radius);
            for (ItemEntity itemEntity : context.level().getEntitiesOfClass(ItemEntity.class, box, ItemEntity::isAlive)) {
                if (!context.zoneManager().isInsideGoalZone(instance.goalId(), context.level(), itemEntity.position())) {
                    continue;
                }
                ItemStack stack = itemEntity.getItem();
                if (stack.isEmpty() || !accepts(stack, instance)) {
                    continue;
                }
                long delta = instance.script().goal().countMode() == GoalScript.CountMode.PER_STACK ? 1L : stack.getCount();
                if (delta <= 0L) {
                    continue;
                }
                instance.addProgress(delta);
                itemEntity.discard();
                if (instance.target() > 0L && instance.progress() >= instance.target()) {
                    break;
                }
            }
        }

        protected abstract boolean accepts(ItemStack stack, GoalInstance instance);
    }

    private static final class AnyItemGoalType extends AbstractItemCollectGoalType {
        @Override
        protected boolean accepts(ItemStack stack, GoalInstance instance) {
            return true;
        }
    }

    private static final class SpecificItemGoalType extends AbstractItemCollectGoalType {
        @Override
        protected boolean accepts(ItemStack stack, GoalInstance instance) {
            String required = instance.script().goal().itemId();
            if (required == null || required.isBlank()) {
                return false;
            }
            Identifier key = BuiltInRegistries.ITEM.getKey(stack.getItem());
            return key != null && required.equalsIgnoreCase(key.toString());
        }
    }

    private static final class ItemTagGoalType extends AbstractItemCollectGoalType {
        @Override
        protected boolean accepts(ItemStack stack, GoalInstance instance) {
            String tagRaw = instance.script().goal().itemTag();
            if (tagRaw == null || tagRaw.isBlank()) {
                return false;
            }
            try {
                TagKey<net.minecraft.world.item.Item> tag = TagKey.create(Registries.ITEM, Identifier.parse(tagRaw));
                return stack.is(tag);
            } catch (Exception e) {
                return false;
            }
        }
    }

    private static final class TimerGoalType implements GoalType {
        @Override
        public void tick(GoalRuntimeContext context, GoalInstance instance) {
            if (!instance.completed()) {
                instance.addProgress(1L);
            }
        }
    }

    private static final class VisualOnlyGoalType implements GoalType {
        @Override
        public void tick(GoalRuntimeContext context, GoalInstance instance) {
        }
    }

    private static final class CustomGoalType implements GoalType {
        private final GoalRegistry registry;

        private CustomGoalType(GoalRegistry registry) {
            this.registry = registry;
        }

        @Override
        public void tick(GoalRuntimeContext context, GoalInstance instance) {
            String handler = instance.script().goal().customHandler();
            if (handler == null || handler.isBlank()) {
                return;
            }
            GoalType delegate = registry.getCustomHandler(handler);
            if (delegate == null) {
                return;
            }
            delegate.tick(context, instance);
        }

        @Override
        public void collectDisplayVariables(GoalRuntimeContext context, GoalInstance instance, Map<String, String> output) {
            String handler = instance.script().goal().customHandler();
            if (handler == null || handler.isBlank()) {
                return;
            }
            GoalType delegate = registry.getCustomHandler(handler);
            if (delegate == null) {
                return;
            }
            delegate.collectDisplayVariables(context, instance, output);
        }
    }
}
