package neutka.marallys.marallyzen.replay.client;

import net.minecraft.client.Minecraft;
import neutka.marallys.marallyzen.replay.ReplayCameraFrame;

public record ReplayClientCaptureContext(long tick, ReplayCameraFrame camera, Minecraft minecraft) {
}
