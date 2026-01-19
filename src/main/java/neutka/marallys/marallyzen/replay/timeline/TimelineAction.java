package neutka.marallys.marallyzen.replay.timeline;

@FunctionalInterface
public interface TimelineAction {
    void execute(TimelineEvent event, TimelineScheduler scheduler);
}
