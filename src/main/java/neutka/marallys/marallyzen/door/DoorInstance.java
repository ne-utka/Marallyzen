package neutka.marallys.marallyzen.door;

public final class DoorInstance {
    private final DoorDefinition definition;
    private DoorSavedData.DoorState state;

    public DoorInstance(DoorDefinition definition, DoorSavedData.DoorState state) {
        this.definition = definition;
        this.state = state;
    }

    public DoorDefinition definition() {
        return definition;
    }

    public DoorSavedData.DoorState state() {
        return state;
    }

    public void setState(DoorSavedData.DoorState state) {
        this.state = state;
    }
}
