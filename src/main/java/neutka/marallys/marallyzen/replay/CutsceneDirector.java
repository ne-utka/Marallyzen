package neutka.marallys.marallyzen.replay;

public interface CutsceneDirector {
    double getTime();

    void play();

    void pause();

    void stop();

    void seek(double time);
}
