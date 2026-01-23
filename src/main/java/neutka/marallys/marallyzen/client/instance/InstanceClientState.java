package neutka.marallys.marallyzen.client.instance;

import net.minecraft.client.Minecraft;

public class InstanceClientState {
    private static final InstanceClientState INSTANCE = new InstanceClientState();

    private boolean inInstance;
    private String questId;

    public static InstanceClientState getInstance() {
        return INSTANCE;
    }

    public boolean isInInstance() {
        return inInstance;
    }

    public String questId() {
        return questId;
    }

    public void applyStatus(boolean inInstance, String questId) {
        this.inInstance = inInstance;
        this.questId = questId;
        if (!inInstance) {
            var mc = Minecraft.getInstance();
            if (mc.screen instanceof InstanceExitScreen) {
                mc.setScreen(null);
            }
        }
    }
}
