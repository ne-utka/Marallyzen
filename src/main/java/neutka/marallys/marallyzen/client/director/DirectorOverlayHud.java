package neutka.marallys.marallyzen.client.director;

import java.lang.reflect.Method;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.client.emote.ClientEmoteHandler;
import neutka.marallys.marallyzen.director.CameraState;
import neutka.marallys.marallyzen.director.CameraTrack;
import neutka.marallys.marallyzen.director.DirectorEvent;
import neutka.marallys.marallyzen.director.DirectorEventRunner;
import neutka.marallys.marallyzen.director.DirectorProject;
import neutka.marallys.marallyzen.director.DirectorRuntime;
import neutka.marallys.marallyzen.director.EventTrack;
import neutka.marallys.marallyzen.director.Keyframe;
import neutka.marallys.marallyzen.director.ReplayTimeSource;
import neutka.marallys.marallyzen.director.ReplayTimeSourceHolder;
import neutka.marallys.marallyzen.npc.GeckoNpcEntity;
import neutka.marallys.marallyzen.npc.NpcEntity;
import neutka.marallys.marallyzen.replay.ReplayCompat;

public final class DirectorOverlayHud {
    private static final int PANEL_X = 10;
    private static final int PANEL_Y = 150;
    private static final int PANEL_WIDTH = 440;
    private static final int PANEL_HEIGHT = 250;
    private static final int TOP_BAR_HEIGHT = 28;
    private static final int MODE_PANEL_WIDTH = 130;
    private static final int TIMELINE_HEIGHT = 64;
    private static final int BUTTON_WIDTH = 190;
    private static final int BUTTON_HEIGHT = 18;
    private static final int BUTTON_PAD = 6;
    private static final int CONTROL_BUTTON_WIDTH = 28;
    private static final int PLAY_BUTTON_WIDTH = 58;
    private static final long FLIGHT_SAMPLE_MS = 250L;

    private static final String[] EMOTE_IDS = { "waving", "clap", "point", "here" };
    private static final String[] EMOTE_LABELS = { "Махание", "Аплодисменты", "Указать", "Сюда" };
    private static final String[] DIALOG_TEXTS = {
        "Добро пожаловать!",
        "Камера, мотор!",
        "Съемка началась."
    };
    private static final String[] SOUND_IDS = {
        "minecraft:block.note_block.pling",
        "minecraft:entity.player.levelup",
        "minecraft:block.amethyst_block.chime"
    };
    private static final String[] SOUND_LABELS = {
        "Pling",
        "Level Up",
        "Amethyst"
    };
    private static final float[] SOUND_VOLUMES = { 0.5f, 1.0f, 1.5f };
    private static final float[] SOUND_PITCHES = { 0.8f, 1.0f, 1.2f };
    private static final String[] WEATHER_LABELS = { "Ясно", "Дождь", "Гроза" };
    private static final float[] WEATHER_RAIN = { 0.0f, 1.0f, 1.0f };
    private static final float[] WEATHER_THUNDER = { 0.0f, 0.0f, 1.0f };
    private static final String[] TIME_LABELS = { "День", "Вечер", "Ночь" };
    private static final long[] TIME_VALUES = { 1000L, 6000L, 13000L };
    private static final String[] FLAG_IDS = { "start", "beat1", "beat2", "final" };

    private static boolean visible;
    private static DirectorProject project;
    private static Mode mode = Mode.CAMERA;
    private static boolean recordingFlight;
    private static long nextFlightSampleAtMs;
    private static String lastStatus;
    private static long lastStatusAtMs;

    private static boolean eventMenuOpen;
    private static EventType eventType = EventType.EMOTE;
    private static TargetMode emoteTarget = TargetMode.NEAREST_NPC;
    private static int emoteIndex;
    private static int dialogIndex;
    private static int soundIndex;
    private static int volumeIndex;
    private static int pitchIndex;
    private static int flagIndex;
    private static int contextScroll;
    private static int contextScrollMax;

    private enum Mode {
        CAMERA("Камера"),
        EVENTS("События"),
        WORLD("Мир"),
        VIEW("Просмотр");

        private final String label;

        Mode(String label) {
            this.label = label;
        }
    }

    private enum EventType {
        EMOTE("Эмоция NPC"),
        DIALOG("Диалог"),
        SOUND("Звук"),
        FLAG("Флаг");

        private final String label;

        EventType(String label) {
            this.label = label;
        }
    }

    private enum TargetMode {
        NEAREST_NPC("Ближайший NPC"),
        NEAREST_PLAYER("Ближайший игрок"),
        PLAYER("Игрок");

        private final String label;

        TargetMode(String label) {
            this.label = label;
        }
    }

    private DirectorOverlayHud() {
    }

    public static boolean isVisible() {
        return visible;
    }

    public static void toggle() {
        setVisible(!visible);
    }

    public static void hide() {
        stopPreview();
        setVisible(false);
    }

    private static void setVisible(boolean value) {
        visible = value;
    }

    public static void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.font == null) {
            return;
        }

        ensureProject();

        graphics.fill(PANEL_X, PANEL_Y, PANEL_X + PANEL_WIDTH, PANEL_Y + PANEL_HEIGHT, 0xAA0B0E14);
        renderTopBar(graphics, mc);
        renderModePanel(graphics, mc);
        renderContextPanel(graphics, mc);
        renderTimeline(graphics, mc);
    }

    public static void tick() {
        if (!recordingFlight) {
            return;
        }
        ReplayTimeSource timeSource = ReplayTimeSourceHolder.get();
        if (timeSource == null) {
            return;
        }
        long timeMs = timeSource.getTimestamp();
        if (timeMs < nextFlightSampleAtMs) {
            return;
        }
        nextFlightSampleAtMs = timeMs + FLIGHT_SAMPLE_MS;
        captureCameraKey(timeMs);
        setStatus("Запись пролета: ключ добавлен");
    }

    public static boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (!visible || button != 0) {
            return false;
        }
        if (handleTopBarClick(mouseX, mouseY)) {
            return true;
        }
        if (handleModeClick(mouseX, mouseY)) {
            return true;
        }
        if (handleContextClick(mouseX, mouseY)) {
            return true;
        }
        if (handleTimelineClick(mouseX, mouseY)) {
            return true;
        }
        return false;
    }

    public static int getScaledMouseX(Minecraft mc) {
        return scaleMouse(mc, mc.mouseHandler.xpos(), true);
    }

    public static int getScaledMouseY(Minecraft mc) {
        return scaleMouse(mc, mc.mouseHandler.ypos(), false);
    }

    private static int scaleMouse(Minecraft mc, double raw, boolean xAxis) {
        var window = mc.getWindow();
        int scaled = xAxis ? window.getGuiScaledWidth() : window.getGuiScaledHeight();
        int real = getWindowSize(window, xAxis);
        if (real <= 0) {
            return (int) raw;
        }
        return (int) Math.round(raw * scaled / (double) real);
    }

    private static int getWindowSize(Object window, boolean xAxis) {
        String[] methods = xAxis
            ? new String[] { "getScreenWidth", "getWidth" }
            : new String[] { "getScreenHeight", "getHeight" };
        for (String name : methods) {
            try {
                Object value = window.getClass().getMethod(name).invoke(window);
                if (value instanceof Integer i) {
                    return i;
                }
            } catch (ReflectiveOperationException e) {
                // Try next.
            }
        }
        return 0;
    }

    private static void drawButton(GuiGraphics graphics, Minecraft mc, int x, int y, String label) {
        graphics.fill(x, y, x + BUTTON_WIDTH, y + BUTTON_HEIGHT, 0xFF2C3442);
        graphics.drawString(mc.font, label, x + 8, y + 5, 0xFFFFFF, false);
    }

    private static void drawSmallButton(GuiGraphics graphics, Minecraft mc, int x, int y, int width, String label) {
        graphics.fill(x, y, x + width, y + BUTTON_HEIGHT, 0xFF2C3442);
        graphics.drawString(mc.font, label, x + 6, y + 5, 0xFFFFFF, false);
    }

    private static void drawButtonClipped(
        GuiGraphics graphics,
        Minecraft mc,
        int x,
        int y,
        String label,
        int visibleTop,
        int visibleBottom
    ) {
        int bottom = y + BUTTON_HEIGHT;
        if (bottom < visibleTop || y > visibleBottom) {
            return;
        }
        drawButton(graphics, mc, x, y, label);
    }

    private static boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    public static boolean handleMouseScroll(double delta) {
        if (!visible || contextScrollMax <= 0) {
            return false;
        }
        int direction = delta > 0 ? -1 : delta < 0 ? 1 : 0;
        if (direction == 0) {
            return false;
        }
        int step = BUTTON_HEIGHT + BUTTON_PAD;
        int next = clamp(contextScroll + direction * step, 0, contextScrollMax);
        if (next == contextScroll) {
            return false;
        }
        contextScroll = next;
        return true;
    }

    private static int getContextContentHeight() {
        int rows = 0;
        if (mode == Mode.CAMERA) {
            rows = 3;
        } else if (mode == Mode.EVENTS) {
            if (eventMenuOpen) {
                rows = EventType.values().length;
            } else {
                rows = 1;
                if (eventType == EventType.EMOTE) {
                    rows += 3;
                } else if (eventType == EventType.DIALOG) {
                    rows += 2;
                } else if (eventType == EventType.SOUND) {
                    rows += 4;
                } else if (eventType == EventType.FLAG) {
                    rows += 2;
                }
            }
        } else if (mode == Mode.WORLD) {
            rows = 5;
        } else {
            rows = 2;
        }
        return rows * (BUTTON_HEIGHT + BUTTON_PAD);
    }

    private static void ensureProject() {
        if (project == null) {
            project = new DirectorProject();
            project.tracks.add(new CameraTrack());
            project.tracks.add(new EventTrack());
        }
        if (!DirectorRuntime.isActive()) {
            DirectorRuntime.start(project);
        }
    }

    private static CameraTrack getCameraTrack() {
        if (project == null) {
            return null;
        }
        for (var track : project.tracks) {
            if (track instanceof CameraTrack cameraTrack) {
                return cameraTrack;
            }
        }
        CameraTrack cameraTrack = new CameraTrack();
        project.tracks.add(cameraTrack);
        return cameraTrack;
    }

    private static EventTrack getEventTrack() {
        if (project == null) {
            return null;
        }
        for (var track : project.tracks) {
            if (track instanceof EventTrack eventTrack) {
                return eventTrack;
            }
        }
        EventTrack eventTrack = new EventTrack();
        project.tracks.add(eventTrack);
        return eventTrack;
    }

    private static void addCameraKey() {
        ReplayTimeSource timeSource = ReplayTimeSourceHolder.get();
        if (timeSource == null) {
            setStatus("Нет источника времени");
            return;
        }
        long timeMs = timeSource.getTimestamp();
        captureCameraKey(timeMs);
        setStatus("Камера сохранена");
    }

    private static void captureCameraKey(long timeMs) {
        ensureProject();
        CameraTrack track = getCameraTrack();
        if (track == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        var camera = mc.gameRenderer.getMainCamera();
        Vec3 position = camera.getPosition();
        Vec3 rotation = new Vec3(camera.getXRot(), camera.getYRot(), 0.0);
        float fov = mc.options != null ? mc.options.fov().get().floatValue() : 70.0f;
        track.keyframes().add(new Keyframe<>(timeMs, new CameraState(position, rotation, fov)));
    }

    private static void addEvent(String label, Runnable action) {
        ensureProject();
        EventTrack track = getEventTrack();
        if (track == null) {
            return;
        }
        ReplayTimeSource timeSource = ReplayTimeSourceHolder.get();
        if (timeSource == null) {
            setStatus("Нет источника времени");
            return;
        }
        long timeMs = timeSource.getTimestamp();
        track.keyframes().add(new Keyframe<>(timeMs, new DirectorEvent(label, wrapEventAction(label, action))));
        setStatus("Событие добавлено: " + label);
        if (DirectorRuntime.isPreviewing()) {
            DirectorEventRunner.tick(timeMs, project);
        }
    }

    private static void addStickyEvent(String label, String group, Runnable action) {
        ensureProject();
        EventTrack track = getEventTrack();
        if (track == null) {
            return;
        }
        ReplayTimeSource timeSource = ReplayTimeSourceHolder.get();
        if (timeSource == null) {
            setStatus("Нет источника времени");
            return;
        }
        long timeMs = timeSource.getTimestamp();
        track.keyframes().add(new Keyframe<>(timeMs, new DirectorEvent(label, action, true, group)));
        setStatus("Событие добавлено: " + label);
        if (action != null) {
            action.run();
        }
    }

    private static void renderTopBar(GuiGraphics graphics, Minecraft mc) {
        int topX = PANEL_X;
        int topY = PANEL_Y;
        graphics.fill(topX, topY, topX + PANEL_WIDTH, topY + TOP_BAR_HEIGHT, 0xFF1B2230);

        int cursorX = topX + 8;
        drawSmallButton(graphics, mc, cursorX, topY + 5, CONTROL_BUTTON_WIDTH, "<<");
        cursorX += CONTROL_BUTTON_WIDTH + 4;
        drawSmallButton(graphics, mc, cursorX, topY + 5, CONTROL_BUTTON_WIDTH, "<");
        cursorX += CONTROL_BUTTON_WIDTH + 4;
        drawSmallButton(graphics, mc, cursorX, topY + 5, PLAY_BUTTON_WIDTH, isPlaying() ? "Пауза" : "Пуск");
        cursorX += PLAY_BUTTON_WIDTH + 4;
        drawSmallButton(graphics, mc, cursorX, topY + 5, CONTROL_BUTTON_WIDTH, ">");
        cursorX += CONTROL_BUTTON_WIDTH + 4;
        drawSmallButton(graphics, mc, cursorX, topY + 5, CONTROL_BUTTON_WIDTH, ">>");
        int controlsEndX = cursorX + CONTROL_BUTTON_WIDTH;

        ReplayTimeSource timeSource = ReplayTimeSourceHolder.get();
        long timeMs = timeSource != null ? timeSource.getTimestamp() : 0L;
        long totalMs = getReplayDurationMs();
        String timeText = formatTime(timeMs) + " / " + formatTime(totalMs);
        int timeWidth = mc.font.width(timeText);
        graphics.drawString(
            mc.font,
            timeText,
            topX + PANEL_WIDTH - timeWidth - 8,
            topY + 9,
            0xFFFFFF,
            false
        );

        double speed = timeSource != null ? timeSource.getSpeed() : 0.0;
        String speedText = "Скорость x" + String.format(Locale.ROOT, "%.1f", speed);
        int speedWidth = mc.font.width(speedText);
        int speedX = topX + PANEL_WIDTH - timeWidth - speedWidth - 16;
        if (speedX < controlsEndX + 12) {
            speedX = controlsEndX + 12;
        }
        graphics.drawString(mc.font, speedText, speedX, topY + 9, 0xFF9FB0C8, false);

        if (recordingFlight) {
            int recX = controlsEndX + 10;
            if (recX + mc.font.width("REC") < speedX) {
                graphics.drawString(mc.font, "REC", recX, topY + 9, 0xFFE15C54, false);
            }
        }
    }

    private static void renderModePanel(GuiGraphics graphics, Minecraft mc) {
        int panelX = PANEL_X + 8;
        int panelY = PANEL_Y + TOP_BAR_HEIGHT + 10;
        int buttonY = panelY + 10;
        for (Mode entry : Mode.values()) {
            int color = entry == mode ? 0xFF365C83 : 0xFF2A2F3A;
            graphics.fill(panelX, buttonY, panelX + MODE_PANEL_WIDTH, buttonY + BUTTON_HEIGHT, color);
            graphics.drawString(mc.font, entry.label, panelX + 6, buttonY + 4, 0xFFFFFF, false);
            buttonY += BUTTON_HEIGHT + 4;
        }
    }

    private static void renderContextPanel(GuiGraphics graphics, Minecraft mc) {
        int panelX = PANEL_X + MODE_PANEL_WIDTH + 20;
        int panelY = PANEL_Y + TOP_BAR_HEIGHT + 10;
        int visibleTop = panelY + 10;
        int visibleBottom = PANEL_Y + PANEL_HEIGHT - TIMELINE_HEIGHT - 12;
        contextScrollMax = Math.max(0, getContextContentHeight() - (visibleBottom - visibleTop));
        contextScroll = clamp(contextScroll, 0, contextScrollMax);

        int buttonY = panelY + 12 - contextScroll;
        if (mode == Mode.CAMERA) {
            drawButtonClipped(graphics, mc, panelX, buttonY, "Поставить камеру", visibleTop, visibleBottom);
            buttonY += BUTTON_HEIGHT + BUTTON_PAD;
            if (recordingFlight) {
                drawButtonClipped(
                    graphics,
                    mc,
                    panelX,
                    buttonY,
                    "Остановить запись",
                    visibleTop,
                    visibleBottom
                );
            } else {
                drawButtonClipped(graphics, mc, panelX, buttonY, "Записать пролет", visibleTop, visibleBottom);
            }
            buttonY += BUTTON_HEIGHT + BUTTON_PAD;
            drawButtonClipped(graphics, mc, panelX, buttonY, "Просмотр камеры", visibleTop, visibleBottom);
        } else if (mode == Mode.EVENTS) {
            buttonY = renderEventPanel(graphics, mc, panelX, buttonY, visibleTop, visibleBottom);
        } else if (mode == Mode.WORLD) {
            drawButtonClipped(graphics, mc, panelX, buttonY, "Погода: ясно", visibleTop, visibleBottom);
            buttonY += BUTTON_HEIGHT + BUTTON_PAD;
            drawButtonClipped(graphics, mc, panelX, buttonY, "Погода: дождь", visibleTop, visibleBottom);
            buttonY += BUTTON_HEIGHT + BUTTON_PAD;
            drawButtonClipped(graphics, mc, panelX, buttonY, "Погода: гроза", visibleTop, visibleBottom);
            buttonY += BUTTON_HEIGHT + BUTTON_PAD;
            drawButtonClipped(graphics, mc, panelX, buttonY, "Время: день", visibleTop, visibleBottom);
            buttonY += BUTTON_HEIGHT + BUTTON_PAD;
            drawButtonClipped(graphics, mc, panelX, buttonY, "Время: ночь", visibleTop, visibleBottom);
        } else {
            if (DirectorRuntime.isPreviewing()) {
                drawButtonClipped(graphics, mc, panelX, buttonY, "Остановить сцену", visibleTop, visibleBottom);
            } else {
                drawButtonClipped(
                    graphics,
                    mc,
                    panelX,
                    buttonY,
                    "Воспроизвести сцену",
                    visibleTop,
                    visibleBottom
                );
            }
            buttonY += BUTTON_HEIGHT + BUTTON_PAD;
            drawButtonClipped(graphics, mc, panelX, buttonY, "Скрыть Marallyzen", visibleTop, visibleBottom);
        }

        int infoY = Math.max(visibleTop, visibleBottom - 14);
        renderStatusLine(graphics, mc, panelX, infoY);
    }

    private static int renderEventPanel(
        GuiGraphics graphics,
        Minecraft mc,
        int panelX,
        int buttonY,
        int visibleTop,
        int visibleBottom
    ) {
        if (eventMenuOpen) {
            graphics.drawString(mc.font, "Выбор типа события", panelX, buttonY - 2, 0xFF8CA0B8, false);
            for (EventType type : EventType.values()) {
                drawButtonClipped(graphics, mc, panelX, buttonY, type.label, visibleTop, visibleBottom);
                buttonY += BUTTON_HEIGHT + BUTTON_PAD;
            }
            return buttonY;
        }

        drawButtonClipped(graphics, mc, panelX, buttonY, "Тип события: " + eventType.label, visibleTop, visibleBottom);
        buttonY += BUTTON_HEIGHT + BUTTON_PAD;
        if (eventType == EventType.EMOTE) {
            drawButtonClipped(graphics, mc, panelX, buttonY, "Эмоция: " + getEmoteLabel(), visibleTop, visibleBottom);
            buttonY += BUTTON_HEIGHT + BUTTON_PAD;
            drawButtonClipped(graphics, mc, panelX, buttonY, "Цель: " + emoteTarget.label, visibleTop, visibleBottom);
            buttonY += BUTTON_HEIGHT + BUTTON_PAD;
            drawButtonClipped(graphics, mc, panelX, buttonY, "Добавить событие", visibleTop, visibleBottom);
        } else if (eventType == EventType.DIALOG) {
            drawButtonClipped(graphics, mc, panelX, buttonY, "Текст: " + getDialogText(), visibleTop, visibleBottom);
            buttonY += BUTTON_HEIGHT + BUTTON_PAD;
            drawButtonClipped(graphics, mc, panelX, buttonY, "Добавить событие", visibleTop, visibleBottom);
        } else if (eventType == EventType.SOUND) {
            drawButtonClipped(graphics, mc, panelX, buttonY, "Звук: " + getSoundLabel(), visibleTop, visibleBottom);
            buttonY += BUTTON_HEIGHT + BUTTON_PAD;
            drawButtonClipped(
                graphics,
                mc,
                panelX,
                buttonY,
                "Громкость: " + SOUND_VOLUMES[volumeIndex],
                visibleTop,
                visibleBottom
            );
            buttonY += BUTTON_HEIGHT + BUTTON_PAD;
            drawButtonClipped(
                graphics,
                mc,
                panelX,
                buttonY,
                "Высота: " + SOUND_PITCHES[pitchIndex],
                visibleTop,
                visibleBottom
            );
            buttonY += BUTTON_HEIGHT + BUTTON_PAD;
            drawButtonClipped(graphics, mc, panelX, buttonY, "Добавить событие", visibleTop, visibleBottom);
        } else if (eventType == EventType.FLAG) {
            drawButtonClipped(graphics, mc, panelX, buttonY, "Флаг: " + FLAG_IDS[flagIndex], visibleTop, visibleBottom);
            buttonY += BUTTON_HEIGHT + BUTTON_PAD;
            drawButtonClipped(graphics, mc, panelX, buttonY, "Добавить событие", visibleTop, visibleBottom);
        }
        return buttonY;
    }

    private static void renderTimeline(GuiGraphics graphics, Minecraft mc) {
        int timelineX = PANEL_X + 8;
        int timelineY = PANEL_Y + PANEL_HEIGHT - TIMELINE_HEIGHT - 8;
        int timelineWidth = PANEL_WIDTH - 16;
        int lineY = timelineY + 24;
        graphics.fill(timelineX, timelineY, timelineX + timelineWidth, timelineY + TIMELINE_HEIGHT, 0xFF161B24);
        graphics.fill(timelineX + 4, lineY, timelineX + timelineWidth - 4, lineY + 1, 0xFF6D7B8D);

        long totalMs = Math.max(1L, getReplayDurationMs());
        int tickCount = 6;
        for (int i = 0; i <= tickCount; i++) {
            int x = timelineX + 4 + (timelineWidth - 8) * i / tickCount;
            graphics.fill(x, lineY - 6, x + 1, lineY + 6, 0xFF556173);
            long t = totalMs * i / tickCount;
            graphics.drawString(mc.font, formatTime(t), x - 10, lineY + 26, 0xFF909BAC, false);
        }

        renderTrackDots(graphics, timelineX, lineY - 2, timelineWidth, totalMs);
        renderCurrentTimeMarker(graphics, timelineX, lineY, timelineWidth, totalMs);
    }

    private static void renderTrackDots(GuiGraphics graphics, int timelineX, int lineY, int timelineWidth, long totalMs) {
        CameraTrack cameraTrack = getCameraTrack();
        EventTrack eventTrack = getEventTrack();
        if (cameraTrack != null) {
            graphics.drawString(
                Minecraft.getInstance().font,
                "Камера",
                timelineX + 6,
                lineY - 14,
                0xFF7FA8D9,
                false
            );
            for (Keyframe<CameraState> key : cameraTrack.keyframes()) {
                int x = timelineX + 4 + (int) ((timelineWidth - 8) * (key.timeMs() / (double) totalMs));
                graphics.fill(x - 2, lineY - 2, x + 2, lineY + 2, 0xFF4DA3E4);
            }
        }
        if (eventTrack != null) {
            int eventLineY = lineY + 18;
            graphics.drawString(
                Minecraft.getInstance().font,
                "События",
                timelineX + 6,
                eventLineY - 10,
                0xFFE5A24A,
                false
            );
            for (Keyframe<DirectorEvent> key : eventTrack.keyframes()) {
                int x = timelineX + 4 + (int) ((timelineWidth - 8) * (key.timeMs() / (double) totalMs));
                graphics.fill(x - 2, eventLineY - 2, x + 2, eventLineY + 2, 0xFFF0A64E);
            }
        }
    }

    private static void renderCurrentTimeMarker(
        GuiGraphics graphics,
        int timelineX,
        int lineY,
        int timelineWidth,
        long totalMs
    ) {
        ReplayTimeSource timeSource = ReplayTimeSourceHolder.get();
        long timeMs = timeSource != null ? timeSource.getTimestamp() : 0L;
        int x = timelineX + 4 + (int) ((timelineWidth - 8) * (timeMs / (double) totalMs));
        graphics.fill(x, lineY - 18, x + 1, lineY + 26, 0xFFB5E2FF);
    }

    private static boolean handleTopBarClick(int mouseX, int mouseY) {
        int topX = PANEL_X + 8;
        int topY = PANEL_Y + 5;
        if (isInside(mouseX, mouseY, topX, topY, CONTROL_BUTTON_WIDTH, BUTTON_HEIGHT)) {
            seekByMs(-5000);
            return true;
        }
        topX += CONTROL_BUTTON_WIDTH + 4;
        if (isInside(mouseX, mouseY, topX, topY, CONTROL_BUTTON_WIDTH, BUTTON_HEIGHT)) {
            seekByMs(-1000);
            return true;
        }
        topX += CONTROL_BUTTON_WIDTH + 4;
        if (isInside(mouseX, mouseY, topX, topY, PLAY_BUTTON_WIDTH, BUTTON_HEIGHT)) {
            togglePlay();
            return true;
        }
        topX += PLAY_BUTTON_WIDTH + 4;
        if (isInside(mouseX, mouseY, topX, topY, CONTROL_BUTTON_WIDTH, BUTTON_HEIGHT)) {
            seekByMs(1000);
            return true;
        }
        topX += CONTROL_BUTTON_WIDTH + 4;
        if (isInside(mouseX, mouseY, topX, topY, CONTROL_BUTTON_WIDTH, BUTTON_HEIGHT)) {
            seekByMs(5000);
            return true;
        }
        return false;
    }

    private static boolean handleModeClick(int mouseX, int mouseY) {
        int panelX = PANEL_X + 8;
        int panelY = PANEL_Y + TOP_BAR_HEIGHT + 20;
        int buttonY = panelY;
        for (Mode entry : Mode.values()) {
            if (isInside(mouseX, mouseY, panelX, buttonY, MODE_PANEL_WIDTH, BUTTON_HEIGHT)) {
                mode = entry;
                eventMenuOpen = false;
                contextScroll = 0;
                return true;
            }
            buttonY += BUTTON_HEIGHT + 4;
        }
        return false;
    }

    private static boolean handleContextClick(int mouseX, int mouseY) {
        int panelX = PANEL_X + MODE_PANEL_WIDTH + 20;
        int panelY = PANEL_Y + TOP_BAR_HEIGHT + 22 - contextScroll;
        int visibleTop = PANEL_Y + TOP_BAR_HEIGHT + 20;
        int visibleBottom = PANEL_Y + PANEL_HEIGHT - TIMELINE_HEIGHT - 12;
        if (mouseY < visibleTop || mouseY > visibleBottom) {
            return false;
        }
        if (mode == Mode.CAMERA) {
            if (isInside(mouseX, mouseY, panelX, panelY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
                addCameraKey();
                return true;
            }
            if (isInside(mouseX, mouseY, panelX, panelY + BUTTON_HEIGHT + BUTTON_PAD, BUTTON_WIDTH, BUTTON_HEIGHT)) {
                toggleFlightRecording();
                return true;
            }
            if (isInside(mouseX, mouseY, panelX, panelY + (BUTTON_HEIGHT + BUTTON_PAD) * 2, BUTTON_WIDTH, BUTTON_HEIGHT)) {
                togglePreview();
                return true;
            }
        } else if (mode == Mode.EVENTS) {
            return handleEventClick(mouseX, mouseY, panelX, panelY);
        } else if (mode == Mode.WORLD) {
            int cursorY = panelY;
            if (isInside(mouseX, mouseY, panelX, cursorY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
                addWeatherEvent(0.0f, 0.0f, "Погода: ясно");
                return true;
            }
            cursorY += BUTTON_HEIGHT + BUTTON_PAD;
            if (isInside(mouseX, mouseY, panelX, cursorY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
                addWeatherEvent(1.0f, 0.0f, "Погода: дождь");
                return true;
            }
            cursorY += BUTTON_HEIGHT + BUTTON_PAD;
            if (isInside(mouseX, mouseY, panelX, cursorY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
                addWeatherEvent(1.0f, 1.0f, "Погода: гроза");
                return true;
            }
            cursorY += BUTTON_HEIGHT + BUTTON_PAD;
            if (isInside(mouseX, mouseY, panelX, cursorY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
                addTimeEvent(1000L, "Время: день");
                return true;
            }
            cursorY += BUTTON_HEIGHT + BUTTON_PAD;
            if (isInside(mouseX, mouseY, panelX, cursorY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
                addTimeEvent(13000L, "Время: ночь");
                return true;
            }
        } else {
            if (isInside(mouseX, mouseY, panelX, panelY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
                togglePreview();
                return true;
            }
            if (isInside(mouseX, mouseY, panelX, panelY + BUTTON_HEIGHT + BUTTON_PAD, BUTTON_WIDTH, BUTTON_HEIGHT)) {
                hide();
                return true;
            }
        }
        return false;
    }

    private static boolean handleEventClick(int mouseX, int mouseY, int panelX, int panelY) {
        int cursorY = panelY;
        if (eventMenuOpen) {
            for (EventType type : EventType.values()) {
                if (isInside(mouseX, mouseY, panelX, cursorY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
                    eventType = type;
                    eventMenuOpen = false;
                    contextScroll = 0;
                    return true;
                }
                cursorY += BUTTON_HEIGHT + BUTTON_PAD;
            }
            return false;
        }

        if (isInside(mouseX, mouseY, panelX, cursorY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
            eventMenuOpen = true;
            contextScroll = 0;
            return true;
        }
        cursorY += BUTTON_HEIGHT + BUTTON_PAD;
        if (eventType == EventType.EMOTE) {
            if (isInside(mouseX, mouseY, panelX, cursorY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
                emoteIndex = cycleIndex(emoteIndex, EMOTE_IDS.length);
                return true;
            }
            cursorY += BUTTON_HEIGHT + BUTTON_PAD;
            if (isInside(mouseX, mouseY, panelX, cursorY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
                emoteTarget = cycleTarget(emoteTarget);
                return true;
            }
            cursorY += BUTTON_HEIGHT + BUTTON_PAD;
            if (isInside(mouseX, mouseY, panelX, cursorY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
                addEmoteEvent();
                return true;
            }
        } else if (eventType == EventType.DIALOG) {
            if (isInside(mouseX, mouseY, panelX, cursorY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
                dialogIndex = cycleIndex(dialogIndex, DIALOG_TEXTS.length);
                return true;
            }
            cursorY += BUTTON_HEIGHT + BUTTON_PAD;
            if (isInside(mouseX, mouseY, panelX, cursorY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
                addDialogEvent();
                return true;
            }
        } else if (eventType == EventType.SOUND) {
            if (isInside(mouseX, mouseY, panelX, cursorY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
                soundIndex = cycleIndex(soundIndex, SOUND_IDS.length);
                return true;
            }
            cursorY += BUTTON_HEIGHT + BUTTON_PAD;
            if (isInside(mouseX, mouseY, panelX, cursorY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
                volumeIndex = cycleIndex(volumeIndex, SOUND_VOLUMES.length);
                return true;
            }
            cursorY += BUTTON_HEIGHT + BUTTON_PAD;
            if (isInside(mouseX, mouseY, panelX, cursorY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
                pitchIndex = cycleIndex(pitchIndex, SOUND_PITCHES.length);
                return true;
            }
            cursorY += BUTTON_HEIGHT + BUTTON_PAD;
            if (isInside(mouseX, mouseY, panelX, cursorY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
                addSoundEvent();
                return true;
            }
        } else if (eventType == EventType.FLAG) {
            if (isInside(mouseX, mouseY, panelX, cursorY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
                flagIndex = cycleIndex(flagIndex, FLAG_IDS.length);
                return true;
            }
            cursorY += BUTTON_HEIGHT + BUTTON_PAD;
            if (isInside(mouseX, mouseY, panelX, cursorY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
                addFlagEvent(FLAG_IDS[flagIndex]);
                return true;
            }
        }
        return false;
    }

    private static boolean handleTimelineClick(int mouseX, int mouseY) {
        int timelineX = PANEL_X + 12;
        int timelineY = PANEL_Y + PANEL_HEIGHT - TIMELINE_HEIGHT;
        int timelineWidth = PANEL_WIDTH - 24;
        int timelineHeight = TIMELINE_HEIGHT - 8;
        if (!isInside(mouseX, mouseY, timelineX, timelineY, timelineWidth, timelineHeight)) {
            return false;
        }
        long totalMs = Math.max(1L, getReplayDurationMs());
        double ratio = (mouseX - timelineX) / (double) timelineWidth;
        long targetMs = (long) (totalMs * ratio);
        seekToMs(targetMs);
        return true;
    }

    private static boolean isPlaying() {
        ReplayTimeSource timeSource = ReplayTimeSourceHolder.get();
        return timeSource != null && timeSource.getSpeed() > 0.0f;
    }

    private static void togglePlay() {
        if (isPlaying()) {
            ReplayCompat.setReplaySpeed(0.0f);
            return;
        }
        ReplayCompat.setReplaySpeed(1.0f);
    }

    private static void togglePreview() {
        if (DirectorRuntime.isPreviewing()) {
            stopPreview();
            setStatus("Просмотр остановлен");
            return;
        }
        startPreview();
        setStatus("Просмотр запущен");
    }

    private static void startPreview() {
        ensureProject();
        DirectorRuntime.startPreview();
    }

    private static void stopPreview() {
        DirectorRuntime.stopPreview();
    }

    private static void toggleFlightRecording() {
        recordingFlight = !recordingFlight;
        nextFlightSampleAtMs = 0L;
        setStatus(recordingFlight ? "Запись пролета начата" : "Запись пролета остановлена");
    }

    private static void renderStatusLine(GuiGraphics graphics, Minecraft mc, int x, int y) {
        if (lastStatus == null) {
            return;
        }
        long timeMs = System.currentTimeMillis();
        if (timeMs - lastStatusAtMs > 5000L) {
            return;
        }
        graphics.drawString(mc.font, lastStatus, x, y, 0xFF95C0E8, false);
    }

    private static void setStatus(String text) {
        lastStatus = text;
        lastStatusAtMs = System.currentTimeMillis();
    }

    private static Runnable wrapEventAction(String label, Runnable action) {
        return () -> {
            try {
                action.run();
            } finally {
                setStatus("Применено: " + label);
            }
        };
    }

    private static void addEmoteEvent() {
        String emoteId = EMOTE_IDS[emoteIndex];
        String label = "Эмоция: " + getEmoteLabel();
        addEvent(label, () -> playEmote(emoteId, emoteTarget));
    }

    private static void addDialogEvent() {
        String text = getDialogText();
        addEvent("Диалог", () -> showDialog(text));
    }

    private static void addSoundEvent() {
        String label = "Звук: " + getSoundLabel();
        float volume = SOUND_VOLUMES[volumeIndex];
        float pitch = SOUND_PITCHES[pitchIndex];
        String soundId = SOUND_IDS[soundIndex];
        addEvent(label, () -> playSound(soundId, volume, pitch));
    }

    private static void addWeatherEvent(float rain, float thunder, String label) {
        addStickyEvent(label, "weather", () -> setWeather(rain, thunder));
    }

    private static void addTimeEvent(long time, String label) {
        addStickyEvent(label, "time", () -> setTimeOfDay(time));
    }

    private static void addFlagEvent(String flag) {
        addEvent("Флаг: " + flag, () -> Marallyzen.LOGGER.info("Director flag set: {}", flag));
    }

    private static void playEmote(String emoteId, TargetMode targetMode) {
        Entity target = findEmoteTarget(targetMode);
        if (target == null) {
            setStatus("Цель эмоции не найдена");
            return;
        }
        Object emote = ClientEmoteHandler.loadEmoteFromRegistry(emoteId);
        if (emote == null) {
            setStatus("Эмоция не найдена: " + emoteId);
            return;
        }
        ClientEmoteHandler.playEmoteOnNpc(target, emote);
    }

    private static Entity findEmoteTarget(TargetMode targetMode) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return null;
        }
        if (targetMode == TargetMode.PLAYER) {
            return mc.player;
        }
        Vec3 origin = mc.gameRenderer.getMainCamera().getPosition();
        double radius = 24.0;
        AABB box = new AABB(
            origin.x - radius,
            origin.y - radius,
            origin.z - radius,
            origin.x + radius,
            origin.y + radius,
            origin.z + radius
        );
        if (targetMode == TargetMode.NEAREST_NPC) {
            Entity npc = findNearestEntity(box, origin, NpcEntity.class, GeckoNpcEntity.class);
            if (npc != null) {
                return npc;
            }
        }
        Player nearestPlayer = findNearestPlayer(box, origin);
        return nearestPlayer != null ? nearestPlayer : mc.player;
    }

    private static Player findNearestPlayer(AABB box, Vec3 origin) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return null;
        }
        Player nearest = null;
        double bestDist = Double.MAX_VALUE;
        for (Player player : mc.level.getEntitiesOfClass(Player.class, box)) {
            double dist = player.position().distanceToSqr(origin);
            if (dist < bestDist) {
                bestDist = dist;
                nearest = player;
            }
        }
        return nearest;
    }

    @SafeVarargs
    private static Entity findNearestEntity(AABB box, Vec3 origin, Class<? extends Entity>... types) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return null;
        }
        Entity nearest = null;
        double bestDist = Double.MAX_VALUE;
        for (Class<? extends Entity> type : types) {
            for (Entity entity : mc.level.getEntitiesOfClass(type, box)) {
                double dist = entity.position().distanceToSqr(origin);
                if (dist < bestDist) {
                    bestDist = dist;
                    nearest = entity;
                }
            }
        }
        return nearest;
    }

    private static void showDialog(String text) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal(text), false);
        }
    }

    private static void playSound(String soundId, float volume, float pitch) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        ResourceLocation key;
        try {
            key = ResourceLocation.parse(soundId);
        } catch (IllegalArgumentException e) {
            setStatus("Неизвестный звук: " + soundId);
            return;
        }
        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.getOptional(key).orElse(null);
        if (sound == null) {
            setStatus("Неизвестный звук: " + soundId);
            return;
        }
        Vec3 pos = mc.gameRenderer.getMainCamera().getPosition();
        mc.level.playLocalSound(pos.x, pos.y, pos.z, sound, SoundSource.MASTER, volume, pitch, false);
    }

    private static void setWeather(float rain, float thunder) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        boolean applied = false;
        try {
            Method setRain = mc.level.getClass().getMethod("setRainLevel", float.class);
            setRain.invoke(mc.level, rain);
            applied = true;
        } catch (ReflectiveOperationException e) {
            // Ignore.
        }
        try {
            Method setThunder = mc.level.getClass().getMethod("setThunderLevel", float.class);
            setThunder.invoke(mc.level, thunder);
            applied = true;
        } catch (ReflectiveOperationException e) {
            // Ignore.
        }
        if (!applied) {
            applyWeatherOnLevelData(mc, rain, thunder);
        }
    }

    private static void setTimeOfDay(long time) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        try {
            Method setDayTime = mc.level.getClass().getMethod("setDayTime", long.class);
            setDayTime.invoke(mc.level, time);
            return;
        } catch (ReflectiveOperationException e) {
            // Ignore.
        }
        applyTimeOnLevelData(mc, time);
    }    private static void applyWeatherOnLevelData(Minecraft mc, float rain, float thunder) {
        try {
            Method getLevelData = mc.level.getClass().getMethod("getLevelData");
            Object levelData = getLevelData.invoke(mc.level);
            if (levelData == null) {
                return;
            }
            boolean raining = rain > 0.0f;
            boolean thundering = thunder > 0.0f;
            tryInvoke(levelData, "setRaining", boolean.class, raining);
            tryInvoke(levelData, "setThundering", boolean.class, thundering);
        } catch (ReflectiveOperationException e) {
            // Ignore.
        }
    }

    private static void applyTimeOnLevelData(Minecraft mc, long time) {
        try {
            Method getLevelData = mc.level.getClass().getMethod("getLevelData");
            Object levelData = getLevelData.invoke(mc.level);
            if (levelData == null) {
                return;
            }
            tryInvoke(levelData, "setDayTime", long.class, time);
        } catch (ReflectiveOperationException e) {
            // Ignore.
        }
    }

    private static void tryInvoke(Object target, String name, Class<?> param, Object value) {
        try {
            Method method = target.getClass().getMethod(name, param);
            method.invoke(target, value);
        } catch (ReflectiveOperationException e) {
            // Ignore.
        }
    }

    private static void seekByMs(long deltaMs) {
        ReplayTimeSource timeSource = ReplayTimeSourceHolder.get();
        long timeMs = timeSource != null ? timeSource.getTimestamp() : 0L;
        seekToMs(timeMs + deltaMs);
    }

    private static void seekToMs(long timeMs) {
        long totalMs = getReplayDurationMs();
        long clamped = Math.max(0L, Math.min(timeMs, totalMs > 0 ? totalMs : timeMs));
        int ticks = (int) Math.round(clamped / 50.0);
        ReplayCompat.seekReplayTicks(ticks);
    }

    private static long getReplayDurationMs() {
        long duration = ReplayCompat.getReplayDurationMs();
        return duration > 0 ? duration : 10000L;
    }

    private static String formatTime(long timeMs) {
        if (timeMs < 0) {
            timeMs = 0;
        }
        long totalSeconds = timeMs / 1000;
        long minutes = totalSeconds / 60;
        double seconds = totalSeconds % 60 + (timeMs % 1000) / 1000.0;
        return String.format(Locale.ROOT, "%02d:%05.2f", minutes, seconds);
    }

    private static String getEmoteLabel() {
        return EMOTE_LABELS[emoteIndex];
    }

    private static String getDialogText() {
        return DIALOG_TEXTS[dialogIndex];
    }

    private static String getSoundLabel() {
        return SOUND_LABELS[soundIndex];
    }

    private static int cycleIndex(int index, int size) {
        if (size <= 0) {
            return 0;
        }
        return (index + 1) % size;
    }

    private static TargetMode cycleTarget(TargetMode value) {
        int next = value.ordinal() + 1;
        TargetMode[] values = TargetMode.values();
        return values[next % values.length];
    }
}
