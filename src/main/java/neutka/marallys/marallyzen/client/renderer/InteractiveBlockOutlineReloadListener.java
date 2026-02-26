package neutka.marallys.marallyzen.client.renderer;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.resources.VanillaClientListeners;
import neutka.marallys.marallyzen.Marallyzen;

public final class InteractiveBlockOutlineReloadListener {
    private static final Identifier LISTENER_ID =
        Identifier.fromNamespaceAndPath(Marallyzen.MODID, "interactive_block_outline_cache_clear");

    private InteractiveBlockOutlineReloadListener() {}

    public static void onAddClientReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(LISTENER_ID, new SimplePreparableReloadListener<Void>() {
            @Override
            protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
                return null;
            }

            @Override
            protected void apply(Void ignored, ResourceManager resourceManager, ProfilerFiller profiler) {
                InteractiveBlockOutlineRenderer.clearCaches();
            }
        });
        event.addDependency(VanillaClientListeners.MODELS, LISTENER_ID);
    }
}
