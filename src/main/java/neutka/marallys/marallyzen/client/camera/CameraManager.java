package neutka.marallys.marallyzen.client.camera;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Central manager for camera control.
 */
@EventBusSubscriber(modid = neutka.marallys.marallyzen.Marallyzen.MODID, value = Dist.CLIENT)
public class CameraManager {
    private static final CameraManager instance = new CameraManager();

    private final CameraController cameraController;

    private CameraManager() {
        this.cameraController = new CameraController();
    }

    public static CameraManager getInstance() {
        return instance;
    }

    /**
     * Gets the camera controller.
     */
    public CameraController getCameraController() {
        return cameraController;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        instance.cameraController.tick();
    }
}
