package neutka.marallys.marallyzen.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Map;

/**
 * Screen for NPC dialog interactions.
 * Displays dialog text and interactive choice buttons.
 */
public class DialogScreen extends Screen {
    private final String dialogId;
    private final String dialogTitle;
    private final Map<String, String> dialogButtons;

    public DialogScreen(String dialogId, String dialogTitle, Map<String, String> dialogButtons) {
        super(Component.literal(dialogTitle));
        this.dialogId = dialogId;
        this.dialogTitle = dialogTitle;
        this.dialogButtons = dialogButtons;
    }

    @Override
    protected void init() {
        super.init();

        // Calculate button positions
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int buttonWidth = 200;
        int buttonHeight = 20;
        int buttonSpacing = 25;

        // Create buttons for each dialog option
        int buttonIndex = 0;
        for (Map.Entry<String, String> entry : dialogButtons.entrySet()) {
            String buttonId = entry.getKey();
            String buttonText = entry.getValue();

            Button button = Button.builder(
                    Component.literal(buttonText),
                    btn -> onButtonClicked(buttonId)
            )
            .bounds(centerX - buttonWidth / 2, centerY + buttonIndex * buttonSpacing, buttonWidth, buttonHeight)
            .build();

            this.addRenderableWidget(button);
            buttonIndex++;
        }

        // Add close button at the bottom
        Button closeButton = Button.builder(
                Component.literal("Close"),
                btn -> onClose()
        )
        .bounds(centerX - buttonWidth / 2, centerY + buttonIndex * buttonSpacing + 10, buttonWidth, buttonHeight)
        .build();

        this.addRenderableWidget(closeButton);
    }

    private void onButtonClicked(String buttonId) {
        // Send button click to server
        // TODO: Implement network sending
        // NetworkHelper.sendToServer(
        //         new DialogButtonClickPacket(dialogId, buttonId)
        // );

        // For now, just log and close dialog
        neutka.marallys.marallyzen.Marallyzen.LOGGER.info("Dialog button clicked: {} -> {}", dialogId, buttonId);
        onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Render semi-transparent background
        guiGraphics.fill(0, 0, this.width, this.height, 0x80000000);

        // Render dialog background
        int dialogWidth = 300;
        int dialogHeight = 200;
        int dialogX = (this.width - dialogWidth) / 2;
        int dialogY = (this.height - dialogHeight) / 2;

        guiGraphics.fill(dialogX, dialogY, dialogX + dialogWidth, dialogY + dialogHeight, 0xFF404040);
        guiGraphics.renderOutline(dialogX, dialogY, dialogWidth, dialogHeight, 0xFFFFFFFF);

        // Render title
        int titleY = dialogY + 20;
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, titleY, 0xFFFFFF);

        // Render buttons
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false; // Allow the game to continue running
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // ESC key
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}

