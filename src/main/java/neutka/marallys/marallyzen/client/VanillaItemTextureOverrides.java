package neutka.marallys.marallyzen.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.MarallyzenClientConfig;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT)
public final class VanillaItemTextureOverrides {
    private static final Identifier PACK_LOCATION =
            Identifier.fromNamespaceAndPath(Marallyzen.MODID, "resourcepacks/vanilla_item_overrides");
    private static final String PACK_ID = "mod/" + PACK_LOCATION;
    private static int pendingDebugCheckTicks = -1;

    private VanillaItemTextureOverrides() {
    }

    @SubscribeEvent
    public static void onAddPackFinders(AddPackFindersEvent event) {
        boolean enabled = false;
        try {
            enabled = MarallyzenClientConfig.VANILLA_ITEM_TEXTURE_OVERRIDES.get();
        } catch (IllegalStateException ignored) {
            // Config may not be loaded yet during AddPackFindersEvent.
        }
        event.addPackFinders(
                PACK_LOCATION,
                PackType.CLIENT_RESOURCES,
                Component.translatable("pack.marallyzen.vanilla_item_overrides"),
                PackSource.BUILT_IN,
                enabled,
                Pack.Position.TOP
        );
    }

    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() != MarallyzenClientConfig.SPEC) {
            return;
        }
        applyConfigSelection();
    }

    @SubscribeEvent
    public static void onConfigLoading(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() != MarallyzenClientConfig.SPEC) {
            return;
        }
        applyConfigSelection();
    }

    public static void applyConfigSelection() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return;
        }
        mc.execute(() -> {
            PackRepository repository = mc.getResourcePackRepository();
            List<String> selected = new ArrayList<>(repository.getSelectedIds());
            boolean enabled = MarallyzenClientConfig.VANILLA_ITEM_TEXTURE_OVERRIDES.get();
            boolean changed = false;
            if (enabled) {
                if (!selected.contains(PACK_ID)) {
                    selected.add(PACK_ID);
                    changed = true;
                }
            } else if (selected.remove(PACK_ID)) {
                changed = true;
            }
            if (changed) {
                repository.setSelected(selected);
                mc.reloadResourcePacks();
                pendingDebugCheckTicks = 40;
                Marallyzen.LOGGER.info("[VanillaItemOverrides] Selected packs: {}", repository.getSelectedIds());
            } else if (enabled) {
                pendingDebugCheckTicks = 40;
            }
        });
    }

    public static void clientTick() {
        if (pendingDebugCheckTicks < 0) {
            return;
        }
        pendingDebugCheckTicks--;
        if (pendingDebugCheckTicks == 0) {
            logOverrideState();
        }
    }

    private static void logOverrideState() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getResourceManager() == null) {
            return;
        }
        String beetrootHash = hashResource(mc, Identifier.withDefaultNamespace("textures/item/beetroot.png"));
        Marallyzen.LOGGER.info(
                "[VanillaItemOverrides] beetroot hash={}",
                beetrootHash
        );
    }

    private static String hashResource(Minecraft mc, Identifier location) {
        var resource = mc.getResourceManager().getResource(location);
        if (resource.isEmpty()) {
            return "missing";
        }
        try (InputStream stream = resource.get().open()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = stream.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("[VanillaItemOverrides] Failed to hash resource {}", location, e);
            return "error";
        }
    }
}


