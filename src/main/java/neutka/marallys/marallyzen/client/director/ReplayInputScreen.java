package neutka.marallys.marallyzen.client.director;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import neutka.marallys.marallyzen.Marallyzen;

public class ReplayInputScreen extends Screen {
    private boolean passEventsLogged = false;

    public ReplayInputScreen() {
        super(Component.literal("Replay Input"));
        enablePassEvents();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Intentionally empty: keep the replay view visible.
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void tick() {
        enablePassEvents();
    }

    private void enablePassEvents() {
        boolean enabled = false;
        try {
            Class<?> ext = Class.forName("de.johni0702.minecraft.gui.versions.ScreenExt");
            if (ext.isInstance(this)) {
                ext.getMethod("setPassEvents", boolean.class).invoke(this, true);
                enabled = true;
                try {
                    Object value = ext.getMethod("doesPassEvents").invoke(this);
                    if (value instanceof Boolean b) {
                        enabled = b;
                    }
                } catch (ReflectiveOperationException e) {
                    // Ignore: we only use this for diagnostics.
                }
            }
        } catch (ClassNotFoundException e) {
            // JGui not present.
        } catch (ReflectiveOperationException e) {
            // Fall through to the field-based fallback.
        }
        try {
            var field = Screen.class.getDeclaredField("passEvents");
            field.setAccessible(true);
            field.setBoolean(this, true);
            enabled = true;
        } catch (ReflectiveOperationException e) {
            // If we can't enable passEvents, the replay HUD may not receive input.
        }
        if (!passEventsLogged) {
            Marallyzen.LOGGER.info("ReplayInputScreen passEvents enabled={}", enabled);
            passEventsLogged = true;
        }
    }
}
