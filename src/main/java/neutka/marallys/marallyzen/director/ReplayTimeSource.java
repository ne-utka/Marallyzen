package neutka.marallys.marallyzen.director;

public interface ReplayTimeSource {
    long getTimestamp();
    float getSpeed();
    boolean isPlaying();
}
