package neutka.marallys.marallyzen.door;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.PistonEvent;
import neutka.marallys.marallyzen.Marallyzen;

@EventBusSubscriber(modid = Marallyzen.MODID)
public final class DoorEventHandlers {
    private DoorEventHandlers() {
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (DoorEngine.getInstance().isProtected(level, event.getPos(), event.getPlayer())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (DoorEngine.getInstance().isProtected(level, event.getPos(), event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        var source = event.getExplosion().getDirectSourceEntity();
        event.getAffectedBlocks().removeIf(pos -> DoorEngine.getInstance().isProtected(level, pos, source));
    }

    @SubscribeEvent
    public static void onPistonPre(PistonEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        var pos = event.getPos();
        if (DoorEngine.getInstance().isProtected(level, pos, null)) {
            event.setCanceled(true);
            return;
        }
        var movedPos = pos.relative(event.getDirection());
        if (DoorEngine.getInstance().isProtected(level, movedPos, null)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMobGrief(EntityMobGriefingEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        if (DoorEngine.getInstance().isProtected(level, event.getEntity().blockPosition(), event.getEntity())) {
            event.setCanGrief(false);
        }
    }
}
