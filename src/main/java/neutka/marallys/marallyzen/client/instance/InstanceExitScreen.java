package neutka.marallys.marallyzen.client.instance;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import neutka.marallys.marallyzen.network.InstanceLeaveRequestPacket;
import neutka.marallys.marallyzen.network.NetworkHelper;

public class InstanceExitScreen extends Screen {
    public InstanceExitScreen() {
        super(Component.translatable("screen.marallyzen.instance_exit.title"));
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int buttonWidth = 240;
        int buttonHeight = 20;

        Button leaveButton = Button.builder(
                Component.translatable("screen.marallyzen.instance_exit.leave"),
                btn -> {
                    NetworkHelper.sendToServer(new InstanceLeaveRequestPacket());
                    onClose();
                })
            .bounds(centerX - buttonWidth / 2, centerY, buttonWidth, buttonHeight)
            .build();
        this.addRenderableWidget(leaveButton);

        Button closeButton = Button.builder(
                Component.translatable("screen.marallyzen.instance_exit.close"),
                btn -> onClose())
            .bounds(centerX - buttonWidth / 2, centerY + 25, buttonWidth, buttonHeight)
            .build();
        this.addRenderableWidget(closeButton);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0x80000000);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 30, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
