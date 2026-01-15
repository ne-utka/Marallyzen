package neutka.marallys.marallyzen.client.camera;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Central manager for camera control and cutscenes.
 * Coordinates CameraController and CutscenePlayer.
 */
@EventBusSubscriber(modid = neutka.marallys.marallyzen.Marallyzen.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class CameraManager {
    private static final CameraManager instance = new CameraManager();

    private final CameraController cameraController;
    private final CutscenePlayer cutscenePlayer;

    private CameraManager() {
        this.cameraController = new CameraController();
        this.cutscenePlayer = new CutscenePlayer(cameraController);
    }

    public static CameraManager getInstance() {
        return instance;
    }

    /**
     * Plays a cutscene by name.
     */
    public void playScene(String sceneName) {
        if (cutscenePlayer.isPlaying()) {
            cutscenePlayer.stopScene();
        }
        cutscenePlayer.playScene(sceneName);
    }

    /**
     * Stops the current cutscene.
     */
    public void stopScene() {
        cutscenePlayer.stopScene();
    }

    /**
     * Checks if a cutscene is playing.
     */
    public boolean isScenePlaying() {
        return cutscenePlayer.isPlaying();
    }

    /**
     * Gets the camera controller.
     */
    public CameraController getCameraController() {
        return cameraController;
    }

    /**
     * Gets the cutscene player.
     */
    public CutscenePlayer getCutscenePlayer() {
        return cutscenePlayer;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        instance.cameraController.tick();
        instance.cutscenePlayer.tick();
    }
}
