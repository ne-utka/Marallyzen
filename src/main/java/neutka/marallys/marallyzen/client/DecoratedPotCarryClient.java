package neutka.marallys.marallyzen.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.client.narration.NarrationManager;
import neutka.marallys.marallyzen.entity.DecoratedPotCarryEntity;
import neutka.marallys.marallyzen.network.DecoratedPotCarryActionPacket;
import neutka.marallys.marallyzen.network.NetworkHelper;
import neutka.marallys.marallyzen.server.DecoratedPotCarryManager;
import neutka.marallys.marallyzen.util.NarrationIcons;

import java.util.List;

@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class DecoratedPotCarryClient {
    private static boolean wasCarrying = false;

    private DecoratedPotCarryClient() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        boolean carrying = isCarrying(mc);
        NarrationManager manager = NarrationManager.getInstance();
        if (carrying) {
            manager.startNarration(buildNarration(), null, 5, 999999, 3, false);
        } else if (!carrying && wasCarrying) {
            if (isCarryNarration(manager.getActive() != null ? manager.getActive().getText() : null)) {
                manager.startNarrationFadeOut();
            }
        }
        wasCarrying = carrying;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        if (mc.screen != null) {
            return;
        }
        if (event.getAction() != 1) {
            return;
        }
        DecoratedPotCarryEntity carried = getCarriedEntity(mc);
        if (carried == null) {
            return;
        }
        if (event.getButton() == 1) {
            NetworkHelper.sendToServer(new DecoratedPotCarryActionPacket(DecoratedPotCarryManager.ACTION_PLACE));
            event.setCanceled(true);
        } else if (event.getButton() == 0) {
            NetworkHelper.sendToServer(new DecoratedPotCarryActionPacket(DecoratedPotCarryManager.ACTION_THROW));
            event.setCanceled(true);
        }
    }

    private static DecoratedPotCarryEntity getCarriedEntity(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            return null;
        }
        List<DecoratedPotCarryEntity> entities = mc.level.getEntitiesOfClass(
            DecoratedPotCarryEntity.class,
            mc.player.getBoundingBox().inflate(64.0)
        );
        for (DecoratedPotCarryEntity entity : entities) {
            if (entity.isCarriedBy(mc.player.getUUID())) {
                return entity;
            }
        }
        return null;
    }

    public static boolean isCarrying(Minecraft mc) {
        return getCarriedEntity(mc) != null;
    }

    private static Component buildNarration() {
        Component rmb = NarrationIcons.rmb();
        Component lmb = NarrationIcons.lmb();
        return Component.translatable("narration.marallyzen.decorated_pot_drag", rmb, lmb);
    }

    private static boolean isCarryNarration(Component text) {
        if (text == null) {
            return false;
        }
        return normalizeGlyphs(text.getString()).equals(normalizeGlyphs(buildNarration().getString()));
    }

    private static String normalizeGlyphs(String value) {
        if (value == null) {
            return null;
        }
        return value.replace('\ue901', '\ue900')
            .replace('\ue903', '\ue902');
    }
}
