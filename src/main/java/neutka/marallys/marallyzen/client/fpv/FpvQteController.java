package neutka.marallys.marallyzen.client.fpv;

import net.minecraft.resources.Identifier;

public final class FpvQteController {
    private final FpvPhaseController phases = new FpvPhaseController();

    public void tick(Identifier emoteId, boolean fpvActive) {
        phases.update(emoteId, fpvActive);
        FpvQteState.endFrame();
    }

    public FpvPhaseController.Sample sample(float timeSeconds) {
        return phases.sample(timeSeconds);
    }
}
