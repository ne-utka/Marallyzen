package neutka.marallys.marallyzen.director;

public final class DirectorEvent {
    private final String label;
    private final Runnable action;
    private final boolean sticky;
    private final String group;

    public DirectorEvent(String label, Runnable action) {
        this(label, action, false, null);
    }

    public DirectorEvent(String label, Runnable action, boolean sticky, String group) {
        this.label = label;
        this.action = action;
        this.sticky = sticky;
        this.group = group;
    }

    public String label() {
        return label;
    }

    public Runnable action() {
        return action;
    }

    public boolean sticky() {
        return sticky;
    }

    public String group() {
        return group;
    }
}
