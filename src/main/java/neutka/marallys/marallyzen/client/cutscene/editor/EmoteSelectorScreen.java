package neutka.marallys.marallyzen.client.cutscene.editor;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import neutka.marallys.marallyzen.Marallyzen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Screen for selecting an emote from available Emotecraft emotes.
 */
public class EmoteSelectorScreen extends Screen {
    private final Screen parent;
    private final Consumer<String> onEmoteSelected;
    private EditBox searchBox;
    private List<String> availableEmotes = new ArrayList<>();
    private List<String> filteredEmotes = new ArrayList<>();
    private int scrollOffset = 0;
    private static final int ITEMS_PER_PAGE = 10;

    public EmoteSelectorScreen(Screen parent, Consumer<String> onEmoteSelected) {
        super(Component.literal("Select Emote"));
        this.parent = parent;
        this.onEmoteSelected = onEmoteSelected;
        loadAvailableEmotes();
    }

    private void loadAvailableEmotes() {
        availableEmotes.clear();
        
        // Try to get emotes from Emotecraft
        try {
            Class<?> emoteHolderClass = Class.forName("io.github.kosmx.emotes.main.EmoteHolder");
            java.lang.reflect.Field listField = emoteHolderClass.getField("list");
            Object emoteList = listField.get(null);
            java.lang.reflect.Method valuesMethod = emoteList.getClass().getMethod("values");
            java.util.Collection<?> emoteHolders = (java.util.Collection<?>) valuesMethod.invoke(emoteList);
            
            for (Object emoteHolder : emoteHolders) {
                try {
                    java.lang.reflect.Field nameField = emoteHolderClass.getField("name");
                    Object nameComponent = nameField.get(emoteHolder);
                    java.lang.reflect.Method getStringMethod = nameComponent.getClass().getMethod("getString");
                    String name = (String) getStringMethod.invoke(nameComponent);
                    if (name != null && !name.isEmpty()) {
                        availableEmotes.add(name);
                    }
                } catch (Exception e) {
                    // Skip this emote
                }
            }
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("Failed to load emotes from Emotecraft", e);
            // Add some default emotes as fallback
            availableEmotes.add("Waving");
            availableEmotes.add("Clap");
            availableEmotes.add("Point");
        }
        
        filteredEmotes = new ArrayList<>(availableEmotes);
    }

    @Override
    protected void init() {
        super.init();
        
        int searchY = 20;
        searchBox = new EditBox(this.font, this.width / 2 - 150, searchY, 300, 20, Component.literal("Search"));
        searchBox.setResponder(this::onSearchChanged);
        this.addRenderableWidget(searchBox);
        
        int buttonY = this.height - 30;
        Button cancelButton = Button.builder(
            Component.literal("Cancel"),
            btn -> this.minecraft.setScreen(parent)
        ).bounds(this.width / 2 - 100, buttonY, 200, 20).build();
        this.addRenderableWidget(cancelButton);
    }

    private void onSearchChanged(String query) {
        filteredEmotes.clear();
        if (query == null || query.isEmpty()) {
            filteredEmotes.addAll(availableEmotes);
        } else {
            String lowerQuery = query.toLowerCase();
            for (String emote : availableEmotes) {
                if (emote.toLowerCase().contains(lowerQuery)) {
                    filteredEmotes.add(emote);
                }
            }
        }
        scrollOffset = 0;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        
        // Title
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 5, 0xFFFFFF);
        
        // Render emote list
        int startY = 50;
        int itemHeight = 20;
        int visibleItems = Math.min(ITEMS_PER_PAGE, filteredEmotes.size() - scrollOffset);
        
        for (int i = 0; i < visibleItems; i++) {
            int index = scrollOffset + i;
            if (index >= filteredEmotes.size()) break;
            
            String emote = filteredEmotes.get(index);
            int y = startY + i * itemHeight;
            
            // Highlight on hover
            boolean hovered = mouseX >= 50 && mouseX <= this.width - 50 &&
                             mouseY >= y && mouseY < y + itemHeight;
            int bgColor = hovered ? 0xFF555555 : 0xFF2C2C2C;
            guiGraphics.fill(50, y, this.width - 50, y + itemHeight, bgColor);
            
            guiGraphics.drawString(this.font, emote, 55, y + 5, 0xFFFFFF);
        }
        
        // Scroll indicators
        if (scrollOffset > 0) {
            guiGraphics.drawString(this.font, "^", this.width / 2, startY - 15, 0xFFFFFF);
        }
        if (scrollOffset + ITEMS_PER_PAGE < filteredEmotes.size()) {
            guiGraphics.drawString(this.font, "v", this.width / 2, startY + visibleItems * itemHeight + 5, 0xFFFFFF);
        }
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Left click
            int startY = 50;
            int itemHeight = 20;
            
            for (int i = 0; i < ITEMS_PER_PAGE; i++) {
                int index = scrollOffset + i;
                if (index >= filteredEmotes.size()) break;
                
                int y = startY + i * itemHeight;
                if (mouseX >= 50 && mouseX <= this.width - 50 &&
                    mouseY >= y && mouseY < y + itemHeight) {
                    String selectedEmote = filteredEmotes.get(index);
                    onEmoteSelected.accept(selectedEmote);
                    return true;
                }
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        double delta = scrollY;
        if (delta > 0 && scrollOffset > 0) {
            scrollOffset--;
            return true;
        } else if (delta < 0 && scrollOffset + ITEMS_PER_PAGE < filteredEmotes.size()) {
            scrollOffset++;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void removed() {
        super.removed();
    }
}


