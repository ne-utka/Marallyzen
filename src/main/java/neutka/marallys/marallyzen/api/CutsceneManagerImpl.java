package neutka.marallys.marallyzen.api;

import neutka.marallys.marallyzen.client.camera.CameraManager;
import neutka.marallys.marallyzen.client.camera.SceneData;
import neutka.marallys.marallyzen.client.camera.SceneLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the Cutscene Manager API.
 */
class CutsceneManagerImpl implements ICutsceneManager {

    @Override
    public void playScene(String sceneName) {
        CameraManager.getInstance().playScene(sceneName);
    }

    @Override
    public void stopScene() {
        CameraManager.getInstance().stopScene();
    }

    @Override
    public boolean isScenePlaying() {
        return CameraManager.getInstance().isScenePlaying();
    }

    @Override
    public void registerScene(String sceneId, List<CameraKeyframe> keyframes, boolean loop, float interpolationSpeed) {
        SceneData sceneData = new SceneData(sceneId);
        sceneData.setLoop(loop);
        sceneData.setInterpolationSpeed(interpolationSpeed);

        for (CameraKeyframe kf : keyframes) {
            sceneData.addKeyframe(kf.position, kf.yaw, kf.pitch, kf.fov, kf.duration);
        }

        // Note: This adds to the SceneLoader's registry, but doesn't persist to disk
        // For persistence, scenes should be created as JSON files
        SceneLoader.getAllScenes().put(sceneId, sceneData);
    }

    @Override
    public List<String> getAllSceneIds() {
        return new ArrayList<>(SceneLoader.getAllScenes().keySet());
    }

    @Override
    public boolean hasScene(String sceneId) {
        return SceneLoader.getScene(sceneId) != null;
    }
}



