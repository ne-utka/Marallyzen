package neutka.marallys.marallyzen.network;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.instance.InstanceWorldManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public record InstanceRegistryPacket(String worldName) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<InstanceRegistryPacket> TYPE =
            new CustomPacketPayload.Type<>(MarallyzenNetwork.id("instance_registry"));

    public static final StreamCodec<RegistryFriendlyByteBuf, InstanceRegistryPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> NetworkCodecs.STRING.encode(buf, packet.worldName()),
            buf -> new InstanceRegistryPacket(NetworkCodecs.STRING.decode(buf))
    );

    @Override
    public CustomPacketPayload.Type<InstanceRegistryPacket> type() {
        return TYPE;
    }

    public static void handle(InstanceRegistryPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist != Dist.CLIENT) {
                return;
            }
            registerClientStem(packet.worldName());
        });
    }

    public static void registerClientStem(String worldName) {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }
        if (worldName == null || worldName.isBlank()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        var access = mc.level != null ? mc.level.registryAccess()
                : (mc.getConnection() != null ? mc.getConnection().registryAccess() : null);
        if (access == null) {
            return;
        }
        Registry<LevelStem> stemRegistry = access.registry(Registries.LEVEL_STEM).orElse(null);
        if (stemRegistry == null) {
            return;
        }
        ResourceKey<Level> levelKey = InstanceWorldManager.buildInstanceKeyFromWorldName(worldName);
        ResourceKey<LevelStem> stemKey = ResourceKey.create(Registries.LEVEL_STEM, levelKey.location());
        if (stemRegistry.get(stemKey) != null) {
            return;
        }
        ResourceLocation overworldId = ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");
        ResourceKey<LevelStem> overworldKey = ResourceKey.create(Registries.LEVEL_STEM, overworldId);
        LevelStem overworldStem = stemRegistry.get(overworldKey);
        if (overworldStem == null) {
            return;
        }
        try {
            if (isRegistryFrozen(stemRegistry) && !setRegistryFrozen(stemRegistry, false)) {
                return;
            }
            try {
                Registry.register(stemRegistry, stemKey.location(), overworldStem);
                Marallyzen.LOGGER.info("InstanceRegistryPacket: registered client level stem {}", stemKey.location());
            } finally {
                setRegistryFrozen(stemRegistry, true);
            }
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("InstanceRegistryPacket: failed to register client level stem {}", stemKey.location(), e);
        }
    }

    private static boolean isRegistryFrozen(Registry<?> registry) {
        try {
            Method method = registry.getClass().getMethod("isFrozen");
            Object result = method.invoke(registry);
            if (result instanceof Boolean frozen) {
                return frozen;
            }
        } catch (Exception ignored) {
        }
        try {
            Field field = findBooleanField(registry.getClass(), "frozen");
            if (field != null) {
                field.setAccessible(true);
                return field.getBoolean(registry);
            }
        } catch (Exception ignored) {
        }
        return true;
    }

    private static boolean setRegistryFrozen(Registry<?> registry, boolean frozen) {
        try {
            Field field = findBooleanField(registry.getClass(), "frozen");
            if (field == null) {
                return false;
            }
            field.setAccessible(true);
            field.setBoolean(registry, frozen);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static Field findBooleanField(Class<?> cls, String name) {
        for (Class<?> current = cls; current != null; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(name);
                if (field.getType() == boolean.class) {
                    return field;
                }
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }
}
