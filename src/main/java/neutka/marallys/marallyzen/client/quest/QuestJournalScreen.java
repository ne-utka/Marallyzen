package neutka.marallys.marallyzen.client.quest;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import neutka.marallys.marallyzen.network.NetworkHelper;
import neutka.marallys.marallyzen.network.QuestSelectPacket;
import neutka.marallys.marallyzen.quest.QuestCategory;
import neutka.marallys.marallyzen.quest.QuestCategoryColors;
import neutka.marallys.marallyzen.quest.QuestDefinition;
import neutka.marallys.marallyzen.quest.QuestInstance;
import neutka.marallys.marallyzen.quest.QuestPlayerData;
import neutka.marallys.marallyzen.quest.QuestStep;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class QuestJournalScreen extends Screen {
    private static final int OUTER_PADDING = 16;
    private static final int INNER_PADDING = 12;
    private static final int TAB_HEIGHT = 22;
    private static final int TAB_GAP = 6;
    private static final int LIST_ITEM_PADDING = 6;
    private static final int LIST_ITEM_GAP = 4;
    private static final int DIVIDER_WIDTH = 2;

    private static final int COLOR_PANEL = 0x96000000;
    private static final int COLOR_PANEL_BORDER = 0x24FFFFFF;
    private static final int COLOR_LIST_BG = 0x78000000;
    private static final int COLOR_DETAIL_BG = 0x78000000;
    private static final int COLOR_MUTED = 0xB3FFFFFF;
    private static final int COLOR_TITLE = 0xFFFFFFFF;
    private static final int COLOR_TAB_ACTIVE = 0x78000000;
    private static final int COLOR_TAB_INACTIVE = 0x52000000;
    private static final int COLOR_TAB_TEXT = 0xFFFFFFFF;
    private static final int COLOR_TAB_TEXT_INACTIVE = 0xB3FFFFFF;
    private static final int FADE_TICKS = 8;
    private static final int TOGGLE_BOX_SIZE = 10;
    private static final int TOGGLE_GAP = 6;

    private final List<QuestEntry> visibleEntries = new ArrayList<>();
    private final List<QuestHit> questHits = new ArrayList<>();
    private int listScrollOffset;
    private int listContentHeight;
    private long lastSyncMs;
    private Tab activeTab = Tab.ACTIVE;
    private String selectedQuestId;

    private Layout layout = new Layout();
    private float screenAlpha = 0.0f;
    private float previousAlpha = 0.0f;
    private boolean closing;
    private float renderAlpha = 1.0f;

    public QuestJournalScreen() {
        super(Component.translatable("screen.marallyzen.quest_journal"));
    }

    @Override
    protected void init() {
        screenAlpha = 0.0f;
        previousAlpha = 0.0f;
        closing = false;
        rebuildEntries();
    }

    private void rebuildEntries() {
        visibleEntries.clear();
        QuestClientState state = QuestClientState.getInstance();
        QuestPlayerData data = state.playerData();
        Map<String, QuestDefinition> definitions = state.definitions();
        lastSyncMs = state.lastSyncMs();

        List<QuestDefinition> candidates = new ArrayList<>();
        for (QuestDefinition definition : definitions.values()) {
            if (definition.flags().hidden()) {
                QuestInstance instance = data.quests().get(definition.id());
                if (instance == null || instance.state() == QuestInstance.State.NOT_STARTED) {
                    continue;
                }
            }
            candidates.add(definition);
        }

        candidates.sort(Comparator.comparing(definition -> QuestTextUtil.resolve(definition.title()).getString()));

        for (QuestDefinition definition : candidates) {
            QuestInstance instance = data.quests().get(definition.id());
            QuestInstance.State questState = instance != null ? instance.state() : QuestInstance.State.NOT_STARTED;
            if (!shouldInclude(questState)) {
                continue;
            }
            QuestCategory category = definition.resolvedCategory();
            Component title = QuestTextUtil.resolve(definition.title());
            Component description = QuestTextUtil.resolve(definition.description());
            visibleEntries.add(new QuestEntry(definition.id(), title, description, questState, category));
        }

        String activeQuestId = data.activeQuestId();
        if (activeTab == Tab.ACTIVE && activeQuestId != null) {
            boolean hasActive = visibleEntries.stream().anyMatch(entry -> entry.id.equals(activeQuestId));
            if (hasActive) {
                selectedQuestId = activeQuestId;
            }
        }
        if (selectedQuestId == null || visibleEntries.stream().noneMatch(entry -> entry.id.equals(selectedQuestId))) {
            selectedQuestId = visibleEntries.isEmpty() ? null : visibleEntries.get(0).id;
            if (activeTab == Tab.ACTIVE && selectedQuestId != null) {
                maybeSendActiveSelection(selectedQuestId);
            }
        }

        int itemHeight = font.lineHeight + (LIST_ITEM_PADDING * 2);
        listContentHeight = visibleEntries.size() * (itemHeight + LIST_ITEM_GAP);
        listScrollOffset = Mth.clamp(listScrollOffset, 0, Math.max(0, listContentHeight - getListAvailableHeight()));
    }

    private boolean shouldInclude(QuestInstance.State state) {
        return switch (activeTab) {
            case ACTIVE -> state == QuestInstance.State.ACTIVE;
            case COMPLETED -> state == QuestInstance.State.COMPLETED || state == QuestInstance.State.FAILED;
        };
    }

    private int getListAvailableHeight() {
        return layout.listBottom - layout.listTop - (INNER_PADDING * 2);
    }

    private void ensureSyncFresh() {
        QuestClientState state = QuestClientState.getInstance();
        if (state.lastSyncMs() != lastSyncMs) {
            rebuildEntries();
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        ensureSyncFresh();
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        renderAlpha = Mth.lerp(partialTick, previousAlpha, screenAlpha);

        layout = Layout.compute(width, height);
        int left = layout.outerLeft;
        int top = layout.outerTop;
        int right = layout.outerRight;
        int bottom = layout.outerBottom;

        guiGraphics.fill(left, top, right, bottom, applyAlpha(COLOR_PANEL, renderAlpha));

        renderTabs(guiGraphics, mouseX, mouseY);

        guiGraphics.fill(layout.listLeft, layout.listTop, layout.listRight, layout.listBottom, applyAlpha(COLOR_LIST_BG, renderAlpha));
        guiGraphics.fill(layout.detailLeft, layout.detailTop, layout.detailRight, layout.detailBottom, applyAlpha(COLOR_DETAIL_BG, renderAlpha));
        guiGraphics.fill(layout.dividerLeft, layout.listTop, layout.dividerLeft + DIVIDER_WIDTH, layout.listBottom, applyAlpha(COLOR_PANEL_BORDER, renderAlpha));

        renderList(guiGraphics, mouseX, mouseY);
        renderDetails(guiGraphics);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderTabs(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component activeLabel = Component.translatable("screen.marallyzen.quest_tab_active");
        Component completedLabel = Component.translatable("screen.marallyzen.quest_tab_completed");

        int activeWidth = font.width(activeLabel) + (INNER_PADDING * 2);
        int completedWidth = font.width(completedLabel) + (INNER_PADDING * 2);

        int tabsLeft = layout.outerLeft + INNER_PADDING;
        int tabsTop = layout.outerTop + INNER_PADDING / 2;
        layout.tabActive = new Rect(tabsLeft, tabsTop, activeWidth, TAB_HEIGHT);
        layout.tabCompleted = new Rect(tabsLeft + activeWidth + TAB_GAP, tabsTop, completedWidth, TAB_HEIGHT);

        drawTab(guiGraphics, layout.tabActive, activeLabel, activeTab == Tab.ACTIVE, mouseX, mouseY);
        drawTab(guiGraphics, layout.tabCompleted, completedLabel, activeTab == Tab.COMPLETED, mouseX, mouseY);
    }

    private void drawTab(GuiGraphics guiGraphics, Rect rect, Component label, boolean active, int mouseX, int mouseY) {
        boolean hovered = rect.contains(mouseX, mouseY);
        int bg = active ? COLOR_TAB_ACTIVE : COLOR_TAB_INACTIVE;
        if (hovered && !active) {
            bg = 0x66000000;
        }
        guiGraphics.fill(rect.left, rect.top, rect.right(), rect.bottom(), applyAlpha(bg, renderAlpha));
        int textColor = active ? COLOR_TAB_TEXT : COLOR_TAB_TEXT_INACTIVE;
        int textX = rect.left + (rect.width - font.width(label)) / 2;
        int textY = rect.top + (rect.height - font.lineHeight) / 2;
        guiGraphics.drawString(font, label, textX, textY, applyAlpha(textColor, renderAlpha), false);
    }

    private void renderList(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        questHits.clear();
        int listLeft = layout.listLeft + INNER_PADDING;
        int listTop = layout.listTop + INNER_PADDING;
        int listRight = layout.listRight - INNER_PADDING;
        int listBottom = layout.listBottom - INNER_PADDING;

        int itemHeight = font.lineHeight + (LIST_ITEM_PADDING * 2);
        int startY = listTop - listScrollOffset;

        if (visibleEntries.isEmpty()) {
            Component emptyLabel = Component.translatable(activeTab == Tab.ACTIVE
                ? "screen.marallyzen.quest_empty_active"
                : "screen.marallyzen.quest_empty_completed");
            guiGraphics.drawString(font, emptyLabel, listLeft, listTop, applyAlpha(COLOR_MUTED, renderAlpha), false);
            return;
        }

        for (int i = 0; i < visibleEntries.size(); i++) {
            QuestEntry entry = visibleEntries.get(i);
            int itemTop = startY + i * (itemHeight + LIST_ITEM_GAP);
            int itemBottom = itemTop + itemHeight;
            if (itemBottom < listTop || itemTop > listBottom) {
                continue;
            }

            Rect itemRect = new Rect(listLeft, itemTop, listRight - listLeft, itemHeight);
            boolean hovered = itemRect.contains(mouseX, mouseY);
            boolean selected = entry.id.equals(selectedQuestId);
            int rowColor = selected ? 0x66000000 : 0x52000000;
            if (hovered && !selected) {
                rowColor = 0x5A000000;
            }

            guiGraphics.fill(itemRect.left, itemRect.top, itemRect.right(), itemRect.bottom(), applyAlpha(rowColor, renderAlpha));
            int entryColor = getEntryColor(entry);
            guiGraphics.fill(itemRect.left, itemRect.top, itemRect.left + 3, itemRect.bottom(), applyAlpha(entryColor, renderAlpha));

            int textX = itemRect.left + 8;
            int textY = itemRect.top + LIST_ITEM_PADDING;
            guiGraphics.drawString(font, entry.title, textX, textY, applyAlpha(entryColor, renderAlpha), false);

            questHits.add(new QuestHit(entry.id, itemRect));
        }
    }

    private void renderDetails(GuiGraphics guiGraphics) {
        int detailLeft = layout.detailLeft + INNER_PADDING;
        int detailTop = layout.detailTop + INNER_PADDING;
        int detailRight = layout.detailRight - INNER_PADDING;

        renderHudToggle(guiGraphics, detailRight, detailTop);

        QuestEntry entry = getSelectedEntry();
        if (entry == null) {
            Component prompt = Component.translatable("screen.marallyzen.quest_select_prompt");
            guiGraphics.drawString(font, prompt, detailLeft, detailTop, applyAlpha(COLOR_MUTED, renderAlpha), false);
            return;
        }

        Component titleLabel = Component.translatable("screen.marallyzen.quest_detail_title");
        Component descriptionLabel = Component.translatable("screen.marallyzen.quest_detail_description");
        Component statusLabel = Component.translatable("screen.marallyzen.quest_status_label");

        int y = detailTop;
        guiGraphics.drawString(font, titleLabel, detailLeft, y, applyAlpha(COLOR_MUTED, renderAlpha), false);
        y += font.lineHeight + 4;
        int entryColor = getEntryColor(entry);
        guiGraphics.drawString(font, entry.title, detailLeft, y, applyAlpha(entryColor != COLOR_TITLE ? entryColor : COLOR_TITLE, renderAlpha), false);
        y += font.lineHeight + 10;

        guiGraphics.drawString(font, descriptionLabel, detailLeft, y, applyAlpha(COLOR_MUTED, renderAlpha), false);
        y += font.lineHeight + 4;

        int wrapWidth = Math.max(40, detailRight - detailLeft);
        List<FormattedCharSequence> wrapped = font.split(entry.description, wrapWidth);
        for (FormattedCharSequence line : wrapped) {
            guiGraphics.drawString(font, line, detailLeft, y, applyAlpha(0xE6FFFFFF, renderAlpha), false);
            y += font.lineHeight + 2;
        }

        y += 8;
        guiGraphics.drawString(font, statusLabel, detailLeft, y, applyAlpha(COLOR_MUTED, renderAlpha), false);
        y += font.lineHeight + 4;
        Component status = getStatusText(entry.state);
        int statusColor = entryColor != COLOR_TITLE ? entryColor : getStateColor(entry.state);
        guiGraphics.drawString(font, status, detailLeft, y, applyAlpha(statusColor, renderAlpha), false);

        QuestClientState state = QuestClientState.getInstance();
        QuestDefinition definition = state.definitions().get(entry.id);
        QuestInstance instance = state.playerData().quests().get(entry.id);
        if (definition == null || definition.steps().isEmpty()) {
            return;
        }

        y += font.lineHeight + 10;
        Component stepsLabel = Component.translatable("screen.marallyzen.quest_steps_label");
        guiGraphics.drawString(font, stepsLabel, detailLeft, y, applyAlpha(COLOR_MUTED, renderAlpha), false);
        y += font.lineHeight + 4;

        int activeIndex = instance != null ? instance.currentStepIndex() : -1;
        boolean asyncSteps = definition.flags().asyncSteps();
        for (int i = 0; i < definition.steps().size(); i++) {
            QuestStep step = definition.steps().get(i);
            if (step == null) {
                continue;
            }
            boolean completed = instance != null
                && (instance.state() == QuestInstance.State.COMPLETED
                || (asyncSteps && isStepComplete(instance, step))
                || (!asyncSteps && i < activeIndex));
            boolean active = instance != null
                && instance.state() == QuestInstance.State.ACTIVE
                && (asyncSteps ? !completed : i == activeIndex);

            String prefix = completed ? "[x] " : active ? "[>] " : "[ ] ";
            int color = completed ? getStateColor(QuestInstance.State.COMPLETED)
                : active ? getStateColor(QuestInstance.State.ACTIVE)
                : COLOR_MUTED;
            if (entryColor != COLOR_TITLE) {
                color = entryColor;
            }

            Component stepTitle = QuestTextUtil.resolve(step.title());
            Component line = Component.literal(prefix).append(stepTitle);

            if (instance != null) {
                int target = Math.max(1, step.target());
                int progress = instance.stepProgress().getOrDefault(step.id(), 0);
                if (target > 1 && (active || completed)) {
                    int clamped = Math.min(progress, target);
                    line = line.copy().append(Component.literal(" (" + clamped + "/" + target + ")"));
                }
                if (active && instance.isWaitingHandover() && step.id().equals(instance.handoverStepId())) {
                    line = line.copy().append(Component.literal(" "))
                        .append(Component.translatable("screen.marallyzen.quest_step_handover"));
                }
            }

            List<FormattedCharSequence> stepWrapped = font.split(line, wrapWidth);
            for (int w = 0; w < stepWrapped.size(); w++) {
                int offsetX = w == 0 ? 0 : font.width(prefix);
                guiGraphics.drawString(font, stepWrapped.get(w), detailLeft + offsetX, y, applyAlpha(color, renderAlpha), false);
                y += font.lineHeight + 2;
            }
        }
    }

    private void renderHudToggle(GuiGraphics guiGraphics, int detailRight, int detailTop) {
        QuestClientState state = QuestClientState.getInstance();
        boolean hudEnabled = state.isQuestHudEnabled();
        Component label = Component.translatable("screen.marallyzen.quest_hud_toggle");
        int labelWidth = font.width(label);
        int height = Math.max(TOGGLE_BOX_SIZE, font.lineHeight);
        int width = TOGGLE_BOX_SIZE + TOGGLE_GAP + labelWidth;
        int left = detailRight - width;
        int top = detailTop;
        layout.hudToggle = new Rect(left, top, width, height);

        int boxLeft = left;
        int boxTop = top + (height - TOGGLE_BOX_SIZE) / 2;
        int boxRight = boxLeft + TOGGLE_BOX_SIZE;
        int boxBottom = boxTop + TOGGLE_BOX_SIZE;
        guiGraphics.fill(boxLeft, boxTop, boxRight, boxBottom, applyAlpha(0x66000000, renderAlpha));

        if (hudEnabled) {
            int checkX = boxLeft + 2;
            int checkY = boxTop + 1;
            guiGraphics.drawString(font, Component.literal("x"), checkX, checkY, applyAlpha(COLOR_TAB_TEXT, renderAlpha), false);
        }

        int textX = boxRight + TOGGLE_GAP;
        int textY = top + (height - font.lineHeight) / 2;
        guiGraphics.drawString(font, label, textX, textY, applyAlpha(COLOR_TAB_TEXT_INACTIVE, renderAlpha), false);
    }

    @Override
    public void tick() {
        previousAlpha = screenAlpha;
        if (closing) {
            screenAlpha = Math.max(0.0f, screenAlpha - (1.0f / FADE_TICKS));
            if (screenAlpha <= 0.0f) {
                closing = false;
                Minecraft.getInstance().setScreen(null);
            }
        } else if (screenAlpha < 1.0f) {
            screenAlpha = Math.min(1.0f, screenAlpha + (1.0f / FADE_TICKS));
        }
    }

    @Override
    public void onClose() {
        if (closing) {
            return;
        }
        closing = true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (layout.listTop == 0) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        if (!layout.isInsideList((int) mouseX, (int) mouseY)) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        if (visibleEntries.isEmpty()) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        int itemHeight = font.lineHeight + (LIST_ITEM_PADDING * 2);
        int maxScroll = Math.max(0, listContentHeight - getListAvailableHeight());
        listScrollOffset = Mth.clamp(listScrollOffset - (int) (scrollY * (itemHeight + LIST_ITEM_GAP)), 0, maxScroll);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (layout.tabActive != null && layout.tabActive.contains((int) mouseX, (int) mouseY)) {
            if (activeTab != Tab.ACTIVE) {
                activeTab = Tab.ACTIVE;
                listScrollOffset = 0;
                rebuildEntries();
            }
            return true;
        }
        if (layout.tabCompleted != null && layout.tabCompleted.contains((int) mouseX, (int) mouseY)) {
            if (activeTab != Tab.COMPLETED) {
                activeTab = Tab.COMPLETED;
                listScrollOffset = 0;
                rebuildEntries();
            }
            return true;
        }
        if (layout.hudToggle != null && layout.hudToggle.contains((int) mouseX, (int) mouseY)) {
            QuestClientState state = QuestClientState.getInstance();
            state.setQuestHudEnabled(!state.isQuestHudEnabled());
            return true;
        }
        for (QuestHit hit : questHits) {
            if (hit.rect.contains((int) mouseX, (int) mouseY)) {
                selectedQuestId = hit.questId;
                if (activeTab == Tab.ACTIVE) {
                    maybeSendActiveSelection(selectedQuestId);
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private QuestEntry getSelectedEntry() {
        if (selectedQuestId == null) {
            return null;
        }
        for (QuestEntry entry : visibleEntries) {
            if (entry.id.equals(selectedQuestId)) {
                return entry;
            }
        }
        return null;
    }

    private static boolean isStepComplete(QuestInstance instance, QuestStep step) {
        int target = Math.max(1, step.target());
        int current = instance.stepProgress().getOrDefault(step.id(), 0);
        return current >= target;
    }

    private void maybeSendActiveSelection(String questId) {
        QuestClientState state = QuestClientState.getInstance();
        QuestInstance instance = state.playerData().quests().get(questId);
        if (instance == null || instance.state() != QuestInstance.State.ACTIVE) {
            return;
        }
        if (questId.equals(state.playerData().activeQuestId())) {
            return;
        }
        NetworkHelper.sendToServer(new QuestSelectPacket(questId));
    }

    private static int getStateColor(QuestInstance.State state) {
        return switch (state) {
            case ACTIVE -> 0xFFF2C94C;
            case COMPLETED -> 0xFF6EE28A;
            case FAILED -> 0xFFE06B6B;
            case NOT_STARTED -> 0xFFC0C0C0;
        };
    }

    private static int getEntryColor(QuestEntry entry) {
        if (entry.category != null) {
            return QuestCategoryColors.getColor(entry.category);
        }
        return getStateColor(entry.state);
    }

    private static Component getStatusText(QuestInstance.State state) {
        return switch (state) {
            case ACTIVE -> Component.translatable("screen.marallyzen.quest_status_active");
            case COMPLETED -> Component.translatable("screen.marallyzen.quest_status_completed");
            case FAILED -> Component.translatable("screen.marallyzen.quest_status_failed");
            case NOT_STARTED -> Component.translatable("screen.marallyzen.quest_status_not_started");
        };
    }

    private static int applyAlpha(int color, float alpha) {
        int baseAlpha = (color >>> 24) & 0xFF;
        if (baseAlpha == 0) {
            baseAlpha = 0xFF;
        }
        int scaled = Mth.clamp((int) (baseAlpha * alpha), 0, 255);
        return (scaled << 24) | (color & 0xFFFFFF);
    }

    private enum Tab {
        ACTIVE,
        COMPLETED
    }

    private static final class QuestEntry {
        private final String id;
        private final Component title;
        private final Component description;
        private final QuestInstance.State state;
        private final QuestCategory category;

        private QuestEntry(String id, Component title, Component description, QuestInstance.State state, QuestCategory category) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.state = state;
            this.category = category;
        }
    }

    private static final class QuestHit {
        private final String questId;
        private final Rect rect;

        private QuestHit(String questId, Rect rect) {
            this.questId = questId;
            this.rect = rect;
        }
    }

    private static final class Rect {
        private final int left;
        private final int top;
        private final int width;
        private final int height;

        private Rect(int left, int top, int width, int height) {
            this.left = left;
            this.top = top;
            this.width = width;
            this.height = height;
        }

        private int right() {
            return left + width;
        }

        private int bottom() {
            return top + height;
        }

        private boolean contains(int x, int y) {
            return x >= left && x <= right() && y >= top && y <= bottom();
        }
    }

    private static final class Layout {
        private int outerLeft;
        private int outerTop;
        private int outerRight;
        private int outerBottom;
        private int listLeft;
        private int listTop;
        private int listRight;
        private int listBottom;
        private int dividerLeft;
        private int detailLeft;
        private int detailTop;
        private int detailRight;
        private int detailBottom;
        private Rect tabActive;
        private Rect tabCompleted;
        private Rect hudToggle;

        private static Layout compute(int width, int height) {
            Layout layout = new Layout();
            int outerWidth = Math.min(width - OUTER_PADDING * 2, 760);
            int outerHeight = Math.min(height - OUTER_PADDING * 2, 420);
            layout.outerLeft = (width - outerWidth) / 2;
            layout.outerTop = (height - outerHeight) / 2;
            layout.outerRight = layout.outerLeft + outerWidth;
            layout.outerBottom = layout.outerTop + outerHeight;

            int contentTop = layout.outerTop + TAB_HEIGHT + INNER_PADDING + 6;
            layout.listTop = contentTop;
            layout.listBottom = layout.outerBottom - INNER_PADDING;

            int totalWidth = layout.outerRight - layout.outerLeft - INNER_PADDING * 2;
            int listWidth = (int) (totalWidth * 0.38f);
            layout.listLeft = layout.outerLeft + INNER_PADDING;
            layout.listRight = layout.listLeft + listWidth;

            layout.dividerLeft = layout.listRight + INNER_PADDING / 2;
            layout.detailLeft = layout.dividerLeft + DIVIDER_WIDTH + INNER_PADDING / 2;
            layout.detailRight = layout.outerRight - INNER_PADDING;
            layout.detailTop = contentTop;
            layout.detailBottom = layout.listBottom;

            return layout;
        }

        private boolean isInsideList(int x, int y) {
            return x >= listLeft && x <= listRight && y >= listTop && y <= listBottom;
        }
    }
}
