package neutka.marallys.marallyzen.mixin.common;

import io.netty.buffer.Unpooled;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import neutka.marallys.marallyzen.cutscene.world.server.CutsceneWorldRecorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public class ServerLevelParticlesMixin {
    @Inject(
        method = "sendParticles(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I",
        at = @At("HEAD")
    )
    private void marallyzen$cutsceneParticles(ParticleOptions options, double x, double y, double z, int count,
                                              double dx, double dy, double dz, double speed,
                                              CallbackInfoReturnable<Integer> cir) {
        if (options == null) {
            return;
        }
        ParticleType<?> type = options.getType();
        if (type == null) {
            return;
        }
        String typeId = net.minecraft.core.registries.BuiltInRegistries.PARTICLE_TYPE
            .getKey(type)
            .toString();
        RegistryFriendlyByteBuf buffer =
            new RegistryFriendlyByteBuf(Unpooled.buffer(), ((ServerLevel) (Object) this).registryAccess());
        @SuppressWarnings("unchecked")
        net.minecraft.network.codec.StreamCodec<RegistryFriendlyByteBuf, ParticleOptions> codec =
            (net.minecraft.network.codec.StreamCodec<RegistryFriendlyByteBuf, ParticleOptions>) type.streamCodec();
        try {
            codec.encode(buffer, options);
        } catch (Exception ignored) {
            return;
        }
        byte[] data = new byte[buffer.readableBytes()];
        buffer.readBytes(data);
        CutsceneWorldRecorder.onParticle((ServerLevel) (Object) this, typeId, x, y, z, dx, dy, dz, count, data);
    }
}
