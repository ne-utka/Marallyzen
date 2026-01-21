package neutka.marallys.marallyzen.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import neutka.marallys.marallyzen.replay.ReplayCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftScreenMixin {
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void marallyzen$skipReplayScreens(Screen screen, CallbackInfo ci) {
        if (ReplayCompat.shouldSuppressReplayScreen(screen)) {
            ci.cancel();
        }
    }
}
