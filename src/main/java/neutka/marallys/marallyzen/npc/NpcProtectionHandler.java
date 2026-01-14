package neutka.marallys.marallyzen.npc;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import neutka.marallys.marallyzen.Marallyzen;

/**
 * Handles protection for NPCs (invulnerability, damage cancellation, death prevention).
 */
@EventBusSubscriber(modid = Marallyzen.MODID, bus = EventBusSubscriber.Bus.GAME)
public class NpcProtectionHandler {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) {
            return;
        }

        String npcId = NpcClickHandler.getRegistry().getNpcId(entity);
        if (npcId == null) {
            return; // Not an NPC
        }

        NpcData npcData = NpcClickHandler.getRegistry().getNpcData(npcId);
        if (npcData == null) {
            return;
        }

        // If NPC is invulnerable, cancel damage
        boolean shouldBeInvulnerable = npcData.getInvulnerable() != null ? npcData.getInvulnerable() : true;
        if (shouldBeInvulnerable) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) {
            return;
        }

        String npcId = NpcClickHandler.getRegistry().getNpcId(entity);
        if (npcId == null) {
            return; // Not an NPC
        }

        NpcData npcData = NpcClickHandler.getRegistry().getNpcData(npcId);
        if (npcData == null) {
            return;
        }

        // If NPC is invulnerable, prevent death
        boolean shouldBeInvulnerable = npcData.getInvulnerable() != null ? npcData.getInvulnerable() : true;
        if (shouldBeInvulnerable) {
            event.setCanceled(true);
            // Restore health to max
            entity.setHealth(entity.getMaxHealth());
        }
    }
}

