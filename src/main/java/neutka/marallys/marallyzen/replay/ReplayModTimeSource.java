package neutka.marallys.marallyzen.replay;

import neutka.marallys.marallyzen.director.ReplayTimeSource;

public final class ReplayModTimeSource implements ReplayTimeSource {
    @Override
    public long getTimestamp() {
        return ReplayCompat.getReplayTimeMs();
    }

    @Override
    public float getSpeed() {
        double speed = ReplayCompat.getReplaySpeed();
        return speed > 0.0 ? (float) speed : 0.0f;
    }

    @Override
    public boolean isPlaying() {
        return ReplayCompat.isReplayActive() && getSpeed() > 0.0f;
    }
}
