package neutka.marallys.marallyzen.client.cutscene.editor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
import net.minecraft.world.phys.Vec3;
import neutka.marallys.marallyzen.Marallyzen;

/**
 * Controls camera during cutscene recording and editing.
 * Handles switching between first and third person views.
 */
public class CutsceneCameraController {
    private static final Minecraft mc = Minecraft.getInstance();
    
    private boolean isBlockingInput = false;
    private CameraType originalCameraType = null;

    /**
     * Gets current camera position (eye position of player).
     */
    public Vec3 getCurrentPosition() {
        if (mc.player == null) {
            return Vec3.ZERO;
        }
        return mc.player.getEyePosition();
    }

    /**
     * Gets current camera rotation (yaw and pitch).
     */
    public float[] getCurrentRotation() {
        if (mc.player == null) {
            return new float[]{0.0f, 0.0f};
        }
        return new float[]{mc.player.getYRot(), mc.player.getXRot()};
    }

    /**
     * Gets current FOV.
     */
    public float getCurrentFov() {
        return mc.options.fov().get().floatValue();
    }

    /**
     * Gets current camera mode.
     */
    public CutsceneEditorData.CameraMode getCurrentCameraMode() {
        if (mc.options == null) {
            return CutsceneEditorData.CameraMode.FIRST_PERSON;
        }
        
        int cameraType = mc.options.getCameraType().ordinal();
        return cameraType == 0 
            ? CutsceneEditorData.CameraMode.FIRST_PERSON 
            : CutsceneEditorData.CameraMode.THIRD_PERSON;
    }

    /**
     * Switches camera mode between first and third person.
     */
    public void toggleCameraMode() {
        if (mc.options == null) {
            return;
        }

        CameraType currentType = mc.options.getCameraType();
        if (currentType == CameraType.FIRST_PERSON) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        } else {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        }
    }

    /**
     * Sets camera mode to first person.
     */
    public void setFirstPerson() {
        if (mc.options != null) {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        }
    }

    /**
     * Sets camera mode to third person.
     */
    public void setThirdPerson() {
        if (mc.options != null) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
    }

    /**
     * Sets camera mode.
     */
    public void setCameraMode(CutsceneEditorData.CameraMode mode) {
        if (mode == CutsceneEditorData.CameraMode.FIRST_PERSON) {
            setFirstPerson();
        } else {
            setThirdPerson();
        }
    }

    /**
     * Saves current camera type for later restoration.
     */
    public void saveCameraType() {
        if (mc.options != null) {
            originalCameraType = mc.options.getCameraType();
        }
    }

    /**
     * Restores saved camera type.
     */
    public void restoreCameraType() {
        if (mc.options != null && originalCameraType != null) {
            mc.options.setCameraType(originalCameraType);
            originalCameraType = null;
        }
    }

    /**
     * Blocks player input during recording/preview.
     */
    public void setBlockInput(boolean block) {
        this.isBlockingInput = block;
    }

    public boolean isBlockingInput() {
        return isBlockingInput;
    }
}


