package neutka.marallys.marallyzen.replay.playback;

import net.minecraft.client.Camera;

public interface ReplayCamera {
    void apply(Camera camera, float time);

    float getFov(float time);
}
