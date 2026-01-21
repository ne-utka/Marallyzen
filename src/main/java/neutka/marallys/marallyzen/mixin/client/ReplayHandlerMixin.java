package neutka.marallys.marallyzen.mixin.client;

import neutka.marallys.marallyzen.client.director.DirectorReplayOverlayBridge;
import neutka.marallys.marallyzen.replay.ReplayCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.replaymod.replay.ReplayHandler", remap = false)
public abstract class ReplayHandlerMixin {
    @Inject(method = "<init>(Lcom/replaymod/replaystudio/replay/ReplayFile;Z)V", at = @At("TAIL"))
    private void marallyzen$onReplayHandlerInit(CallbackInfo ci) {
        DirectorReplayOverlayBridge.onReplayHandlerCreated(this);
    }

    @Inject(method = "doJump", at = @At("HEAD"))
    private void marallyzen$onReplayJump(CallbackInfo ci) {
        ReplayCompat.markSeek(1500L);
    }
}
