package neutka.marallys.marallyzen.client.quest;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import neutka.marallys.marallyzen.Marallyzen;

@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT)
public class QuestZoneVisualRenderer {
    private static boolean enabled = true;
    private static final double MAX_RENDER_DISTANCE = 64.0;

    // Minimal vanilla-like pulse rings.
    private static final int SPAWN_INTERVAL_TICKS = 3;
    private static final int CYCLE_TICKS = 44;
    private static final int POINTS_PER_RING = 10;
    private static final double BASE_RADIUS_INNER = 1.2;
    private static final double BASE_RADIUS_OUTER = 2.4;
    private static final double RADIUS_SWAY = 0.9;

    private static final DustParticleOptions WHITE_DUST = new DustParticleOptions(0xFFFFFF, 0.18f);

    private static long lastSpawnTick = Long.MIN_VALUE;
    private static boolean disabled;

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent.AfterEntities event) {
        if (!enabled || disabled) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }

        QuestZoneVisual zone = QuestClientState.getInstance().activeZone();
        if (zone == null) {
            return;
        }
        if (!zone.dimension().equals(mc.level.dimension())) {
            return;
        }
        if (zone.distanceTo(mc.player.position()) > MAX_RENDER_DISTANCE) {
            return;
        }
        if (!isPlayerInsideZone(mc, zone)) {
            return;
        }

        try {
            spawnMinimalPulse(mc, zone);
        } catch (Exception e) {
            disabled = true;
            Marallyzen.LOGGER.warn("QuestZoneVisualRenderer: disabled after render error", e);
        }
    }

    private static void spawnMinimalPulse(Minecraft mc, QuestZoneVisual zone) {
        long tick = mc.level.getGameTime();
        if (tick == lastSpawnTick || tick % SPAWN_INTERVAL_TICKS != 0) {
            return;
        }
        lastSpawnTick = tick;

        double progress = (tick % CYCLE_TICKS) / (double) CYCLE_TICKS;
        double pulse = (Math.sin(progress * Math.PI * 2.0) + 1.0) * 0.5;
        double innerR = BASE_RADIUS_INNER + pulse * RADIUS_SWAY;
        double outerR = BASE_RADIUS_OUTER + (1.0 - pulse) * RADIUS_SWAY;

        double centerX = zone.center().x;
        double centerZ = zone.center().z;
        int groundY = mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(centerX), Mth.floor(centerZ));
        double y = groundY + 0.16;

        // Spawn in sparse ring segments for subtle circular blinking.
        int segmentOffset = (int) (tick / SPAWN_INTERVAL_TICKS) % 3;
        spawnRingSegments(mc, centerX, centerZ, y, innerR, progress * 0.55, segmentOffset);
        spawnRingSegments(mc, centerX, centerZ, y, outerR, -progress * 0.45, (segmentOffset + 1) % 3);
    }

    private static void spawnRingSegments(Minecraft mc, double cx, double cz, double y,
                                          double radius, double phase, int segmentOffset) {
        for (int i = 0; i < POINTS_PER_RING; i++) {
            if ((i + segmentOffset) % 3 != 0) {
                continue;
            }
            double angle = ((Math.PI * 2.0) * i / POINTS_PER_RING) + phase;
            double px = cx + Math.cos(angle) * radius;
            double pz = cz + Math.sin(angle) * radius;
            mc.level.addParticle(WHITE_DUST, px, y, pz, 0.0, 0.0, 0.0);
        }
    }

    private static boolean isPlayerInsideZone(Minecraft mc, QuestZoneVisual zone) {
        Vec3 pos = mc.player.position();
        if (zone.ignoreHeight()) {
            return pos.x >= zone.bounds().minX && pos.x <= zone.bounds().maxX
                    && pos.z >= zone.bounds().minZ && pos.z <= zone.bounds().maxZ;
        }
        return zone.bounds().contains(pos);
    }
}
