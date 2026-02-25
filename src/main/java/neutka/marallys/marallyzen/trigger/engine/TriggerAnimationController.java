package neutka.marallys.marallyzen.trigger.engine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import neutka.marallys.marallyzen.network.NetworkHelper;
import neutka.marallys.marallyzen.network.TriggerScreenShakePacket;
import neutka.marallys.marallyzen.trigger.blueprint.TriggerBlueprint;
import neutka.marallys.marallyzen.trigger.blueprint.TriggerBlockStateCodec;
import neutka.marallys.marallyzen.trigger.instance.TriggerInstance;

import java.util.List;

public final class TriggerAnimationController {
    public boolean tick(ServerLevel level, TriggerInstance instance, TriggerBlueprint blueprint) {
        if (level == null || instance == null || blueprint == null) {
            return true;
        }
        TriggerBlueprint.Settings.AnimationSettings animation = blueprint.settings().animation();
        int duration = Math.max(1, animation.duration());
        instance.setAnimationTick(instance.animationTick() + 1);
        switch (normalize(animation.type())) {
            case "rise_from_ground" -> emitParticles(level, instance, blueprint);
            default -> emitParticles(level, instance, blueprint);
        }
        return instance.animationTick() >= duration;
    }

    public void onAnimationStart(ServerLevel level, TriggerInstance instance, TriggerBlueprint blueprint) {
        if (level == null || instance == null || blueprint == null) {
            return;
        }
        playStartSound(level, instance, blueprint);
        emitParticles(level, instance, blueprint);
        broadcastScreenShake(level, instance, blueprint);
    }

    private void playStartSound(ServerLevel level, TriggerInstance instance, TriggerBlueprint blueprint) {
        SoundEvent event = resolveSound(blueprint.settings().sound());
        BlockPos center = instance.centerPos();
        level.playSound(
                null,
                center,
                event,
                SoundSource.BLOCKS,
                0.6F,
                0.85F + level.random.nextFloat() * 0.25F
        );
    }

    private SoundEvent resolveSound(String soundId) {
        if (soundId == null || soundId.isBlank()) {
            return SoundEvents.GENERIC_EXPLODE.value();
        }
        Identifier id = Identifier.tryParse(soundId);
        if (id == null) {
            return SoundEvents.GENERIC_EXPLODE.value();
        }
        SoundEvent event = BuiltInRegistries.SOUND_EVENT.getValue(id);
        return event == null ? SoundEvents.GENERIC_EXPLODE.value() : event;
    }

    private void emitParticles(ServerLevel level, TriggerInstance instance, TriggerBlueprint blueprint) {
        List<TriggerBlueprint.BlockEntry> blocks = blueprint.blocks();
        if (blocks.isEmpty()) {
            return;
        }
        String particleProfile = normalize(blueprint.settings().particle());
        int samples = Math.min(12, blocks.size());
        for (int i = 0; i < samples; i++) {
            TriggerBlueprint.BlockEntry entry = blocks.get((instance.animationTick() * 7 + i * 11) % blocks.size());
            BlockPos target = instance.worldPos().offset(entry.localPos());
            var blockState = TriggerBlockStateCodec.decode(entry.state());
            if (blockState.isAir()) {
                continue;
            }
            if ("cloud".equals(particleProfile)) {
                level.sendParticles(ParticleTypes.CLOUD, target.getX() + 0.5D, target.getY() + 0.2D, target.getZ() + 0.5D, 2, 0.12D, 0.12D, 0.12D, 0.01D);
            } else if ("poof".equals(particleProfile)) {
                level.sendParticles(ParticleTypes.POOF, target.getX() + 0.5D, target.getY() + 0.2D, target.getZ() + 0.5D, 2, 0.10D, 0.10D, 0.10D, 0.01D);
            } else if ("smoke".equals(particleProfile)) {
                level.sendParticles(ParticleTypes.SMOKE, target.getX() + 0.5D, target.getY() + 0.2D, target.getZ() + 0.5D, 2, 0.10D, 0.10D, 0.10D, 0.01D);
            } else {
                level.sendParticles(
                        new BlockParticleOption(ParticleTypes.BLOCK, blockState),
                        target.getX() + 0.5D,
                        target.getY() + 0.2D,
                        target.getZ() + 0.5D,
                        3,
                        0.18D,
                        0.18D,
                        0.18D,
                        0.02D
                );
            }
        }
    }

    private void broadcastScreenShake(ServerLevel level, TriggerInstance instance, TriggerBlueprint blueprint) {
        TriggerBlueprint.Settings.ShakeSettings shake = blueprint.settings().animation().screenShake();
        int radius = Math.max(1, shake.radius());
        float maxIntensity = Math.max(0.0F, shake.maxIntensity());
        if (maxIntensity <= 0.0F) {
            return;
        }

        Vec3 center = Vec3.atCenterOf(instance.centerPos());
        int duration = Math.max(1, shake.durationTicks());
        String falloff = shake.falloff() == null ? "linear" : shake.falloff().trim().toLowerCase();

        for (ServerPlayer player : level.players()) {
            double distance = player.position().distanceTo(center);
            if (distance > radius) {
                continue;
            }
            float normalized = 1.0F - (float) (distance / radius);
            float factor = switch (falloff) {
                case "smooth", "quadratic" -> normalized * normalized;
                default -> normalized;
            };
            float intensity = maxIntensity * factor;
            if (intensity <= 0.01F) {
                continue;
            }
            NetworkHelper.sendToPlayer(player, new TriggerScreenShakePacket(intensity, duration));
        }
    }

    private String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase();
    }
}
