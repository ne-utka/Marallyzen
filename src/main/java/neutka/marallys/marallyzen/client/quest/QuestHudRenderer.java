package neutka.marallys.marallyzen.client.quest;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.quest.QuestCategory;
import neutka.marallys.marallyzen.quest.QuestCategoryColors;
import neutka.marallys.marallyzen.quest.QuestDefinition;
import neutka.marallys.marallyzen.quest.QuestInstance;
import neutka.marallys.marallyzen.quest.QuestStep;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class QuestHudRenderer {
    private static final int COLOR_WHITE = 0xFFFFFF;
    private static final int COLOR_GRAY = 0x9AA0A6;
    private static final int COLOR_YELLOW = 0xFFD166;
    private static final int COLOR_GREEN = 0x66FF66;
    private static final double ZONE_NEAR_DISTANCE = 64.0;
    private static final int HUD_FADE_TICKS = 8;

    private static float hudAlpha = 0.0f;
    private static float previousAlpha = 0.0f;
    private static List<HudLine> cachedLines = new ArrayList<>();

    private record HudLine(Component text, int color) {}

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.font == null) {
            return;
        }
        float partialTick = mc.getTimer().getGameTimeDeltaPartialTick(false);
        boolean blockHud = mc.screen instanceof QuestJournalScreen;

        if (neutka.marallys.marallyzen.client.cutscene.ScreenFadeManager.getInstance().isActive()) {
            return;
        }
        if (neutka.marallys.marallyzen.client.cutscene.EyesCloseManager.getInstance().isActive()) {
            return;
        }

        QuestClientState state = QuestClientState.getInstance();
        boolean hudEnabled = state.isQuestHudEnabled();
        List<HudLine> lines = new ArrayList<>();

        if (!blockHud && hudEnabled) {
            QuestDefinition definition = state.getActiveDefinition();
            QuestInstance instance = state.getActiveInstance();
            if (definition != null && instance != null && instance.state() == QuestInstance.State.ACTIVE) {
                int titleColor = getQuestTitleColor(definition);
                Component questTitle = QuestTextUtil.resolve(definition.title());
                lines.add(new HudLine(questTitle, titleColor));

                Component description = QuestTextUtil.resolve(definition.description());
                boolean hasDescription = description != null && !description.getString().isBlank();
                if (hasDescription) {
                    lines.add(new HudLine(description, COLOR_WHITE));
                }

                QuestStep step = definition.getStep(instance.currentStepIndex());
                if (step != null) {
                    int progress = instance.stepProgress().getOrDefault(step.id(), 0);
                    int target = Math.max(1, step.target());
                    Component stepTitle = QuestTextUtil.resolve(step.title());
                    Component stepLine = dotLine(stepTitle);
                    if (target > 1) {
                        stepLine = stepLine.copy().append(Component.literal(" " + progress + " / " + target));
                    }
                    lines.add(new HudLine(stepLine, COLOR_WHITE));
                }

                QuestZoneVisual zone = state.activeZone();
                if (zone != null && mc.level != null && mc.player != null) {
                    if (!zone.dimension().equals(mc.level.dimension())) {
                        // lines.add(new HudLine(dotLine(Component.literal("Зона задания: другое измерение")), COLOR_WHITE));
                    } else {
                        boolean inside = zone.contains(mc.player.getBoundingBox());
                        if (inside) {
                            // lines.add(new HudLine(dotLine(Component.literal("Зона задания: в зоне")), COLOR_WHITE));
                        } else {
                            double distance = zone.distanceTo(mc.player.position());
                            int meters = (int) Math.round(distance);
                            // lines.add(new HudLine(dotLine(Component.literal("Зона задания")), COLOR_WHITE));
                            lines.add(new HudLine(dotLine(Component.literal("\u0420\u0430\u0441\u0441\u0442\u043e\u044f\u043d\u0438\u0435: " + meters + " \u043c")), COLOR_WHITE));
                        }
                    }
                }
            }

            for (String questId : state.playerData().activeFarmQuests()) {
                QuestDefinition farmDef = state.definitions().get(questId);
                QuestInstance farmInstance = state.playerData().quests().get(questId);
                if (farmDef == null || farmInstance == null || farmInstance.state() != QuestInstance.State.ACTIVE) {
                    continue;
                }
            if (farmDef.resolvedCategory() != QuestCategory.FARM
                    && farmDef.resolvedCategory() != QuestCategory.DAILY) {
                continue;
            }
            Component questTitle = QuestTextUtil.resolve(farmDef.title());
            int farmColor = QuestCategoryColors.getColor(farmDef.resolvedCategory());
            if (farmDef.flags().asyncSteps()) {
                lines.add(new HudLine(questTitle, farmColor));
                for (QuestStep step : farmDef.steps()) {
                        if (step == null) {
                            continue;
                        }
                        int progress = farmInstance.stepProgress().getOrDefault(step.id(), 0);
                        int target = Math.max(1, step.target());
                        Component stepTitle = QuestTextUtil.resolve(step.title());
                        Component line = dotLine(stepTitle);
                        if (target > 1) {
                            int clamped = Math.min(progress, target);
                            line = line.copy().append(Component.literal(" " + clamped + " / " + target));
                        }
                        lines.add(new HudLine(line, farmColor));
                }
                } else {
                    QuestStep step = farmDef.getStep(farmInstance.currentStepIndex());
                    if (step == null) {
                        continue;
                    }
                    int progress = farmInstance.stepProgress().getOrDefault(step.id(), 0);
                    int target = Math.max(1, step.target());
                    Component line = dotLine(QuestTextUtil.resolve(step.title()))
                            .copy().append(Component.literal(" " + progress + " / " + target));
                    lines.add(new HudLine(line, farmColor));
                }
            }
        }

        boolean shouldShow = !blockHud && hudEnabled && !lines.isEmpty();
        if (shouldShow) {
            cachedLines = new ArrayList<>(lines);
        }

        previousAlpha = hudAlpha;
        float targetAlpha = shouldShow ? 1.0f : 0.0f;
        float step = 1.0f / HUD_FADE_TICKS;
        if (hudAlpha < targetAlpha) {
            hudAlpha = Math.min(targetAlpha, hudAlpha + step);
        } else if (hudAlpha > targetAlpha) {
            hudAlpha = Math.max(targetAlpha, hudAlpha - step);
        }
        float renderAlpha = net.minecraft.util.Mth.lerp(partialTick, previousAlpha, hudAlpha);
        if (renderAlpha <= 0.01f) {
            return;
        }

        List<HudLine> renderLines = shouldShow ? lines : cachedLines;
        if (renderLines.isEmpty()) {
            return;
        }

        GuiGraphics guiGraphics = event.getGuiGraphics();
        int x = 10;
        int y = 10;
        int padding = 4;
        int lineHeight = mc.font.lineHeight + 2;
        int maxWidth = 0;
        for (HudLine line : renderLines) {
            maxWidth = Math.max(maxWidth, mc.font.width(line.text()));
        }
        int boxWidth = maxWidth + padding * 2;
        int boxHeight = renderLines.size() * lineHeight + padding * 2;
        int bgColor = ((int) (150 * renderAlpha) << 24);
        guiGraphics.fill(x, y, x + boxWidth, y + boxHeight, bgColor);

        int textX = x + padding;
        int textY = y + padding;
        for (int i = 0; i < renderLines.size(); i++) {
            HudLine line = renderLines.get(i);
            guiGraphics.drawString(mc.font, line.text(), textX, textY + (i * lineHeight), applyAlpha(line.color(), renderAlpha), false);
        }
    }

    private static int applyAlpha(int color, float alpha) {
        int baseAlpha = (color >>> 24) & 0xFF;
        if (baseAlpha == 0) {
            baseAlpha = 0xFF;
        }
        int scaled = net.minecraft.util.Mth.clamp((int) (baseAlpha * alpha), 0, 255);
        return (scaled << 24) | (color & 0xFFFFFF);
    }

    private static Component dotLine(Component text) {
        return Component.literal("\u2022 ")
                .withStyle(style -> style.withColor(TextColor.fromRgb(COLOR_GRAY)))
                .append(text);
    }

    private static int getQuestTitleColor(QuestDefinition definition) {
        if (definition == null) {
            return COLOR_WHITE;
        }
        return QuestCategoryColors.getColor(definition.resolvedCategory());
    }
}
