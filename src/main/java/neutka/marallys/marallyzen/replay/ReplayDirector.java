package neutka.marallys.marallyzen.replay;

public class ReplayDirector implements CutsceneDirector {
    @Override
    public double getTime() {
        return ReplayCompat.getReplayTimeSeconds();
    }

    @Override
    public void play() {
        ReplayCompat.setReplaySpeed(1.0);
    }

    @Override
    public void pause() {
        ReplayCompat.setReplaySpeed(0.0);
    }

    @Override
    public void stop() {
        ReplayCompat.setReplaySpeed(0.0);
        ReplayCompat.seekReplayTicks(0);
    }

    @Override
    public void seek(double time) {
        ReplayCompat.seekReplaySeconds(time);
    }
}
