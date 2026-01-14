package neutka.marallys.marallyzen.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import neutka.marallys.marallyzen.audio.MarallyzenSounds;

@OnlyIn(Dist.CLIENT)
public final class OldTvSoundController {
    private OldTvSoundController() {
    }

    public static Object startLoop(BlockPos pos, String soundId) {
        SoundEvent soundEvent = resolveSound(soundId);
        OldTvLoopSound sound = new OldTvLoopSound(pos, soundEvent);
        Minecraft.getInstance().getSoundManager().play(sound);
        return sound;
    }

    public static void stopLoop(Object handle) {
        if (handle instanceof OldTvLoopSound sound) {
            sound.stopSound();
        }
    }

    private static final class OldTvLoopSound extends AbstractTickableSoundInstance {
        private boolean stopped;

        OldTvLoopSound(BlockPos pos, SoundEvent soundEvent) {
            super(soundEvent, SoundSource.BLOCKS, RandomSource.create());
            this.looping = true;
            this.delay = 0;
            this.volume = 1.0f;
            this.pitch = 1.0f;
            this.attenuation = Attenuation.LINEAR;
            this.x = pos.getX() + 0.5;
            this.y = pos.getY() + 0.5;
            this.z = pos.getZ() + 0.5;
        }

        @Override
        public void tick() {
            var player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }
            double dx = player.getX() - this.x;
            double dy = player.getY() - this.y;
            double dz = player.getZ() - this.z;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            float maxDistance = 16.0f;
            float falloff = (float) (1.0 - (distance / maxDistance));
            if (falloff < 0.0f) {
                falloff = 0.0f;
            }
            this.volume = falloff;
        }

        void stopSound() {
            this.stopped = true;
            this.stop();
        }

        public boolean isStopped() {
            return stopped;
        }
    }

    private static SoundEvent resolveSound(String soundId) {
        if (soundId == null || soundId.isBlank()) {
            return MarallyzenSounds.TV.get();
        }
        try {
            ResourceLocation id = ResourceLocation.parse(soundId);
            return BuiltInRegistries.SOUND_EVENT.getOptional(id).orElse(MarallyzenSounds.TV.get());
        } catch (Exception e) {
            return MarallyzenSounds.TV.get();
        }
    }
}
