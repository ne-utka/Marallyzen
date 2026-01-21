package neutka.marallys.marallyzen.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import neutka.marallys.marallyzen.replay.ReplayCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenRenderMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void marallyzen$skipReplayScreenRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (ReplayCompat.shouldSuppressReplayScreen((Screen) (Object) this)) {
            ci.cancel();
        }
    }
}
