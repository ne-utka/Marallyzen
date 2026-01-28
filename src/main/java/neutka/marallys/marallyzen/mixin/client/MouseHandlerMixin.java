package neutka.marallys.marallyzen.mixin.client;

import net.minecraft.client.MouseHandler;
import neutka.marallys.marallyzen.client.lever.LeverInteractionClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @Shadow private double accumulatedDX;
    @Shadow private double accumulatedDY;

    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void marallyzen$blockMouseTurn(CallbackInfo ci) {
        if (LeverInteractionClient.isBlockingInput()
            && net.minecraft.client.Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            // Prevent camera drift and avoid backlog jumps.
            accumulatedDX = 0.0;
            accumulatedDY = 0.0;
            ci.cancel();
        }
    }
}
