package neutka.marallys.marallyzen.client.director;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.replay.ReplayCompat;
import neutka.marallys.marallyzen.replay.camera.ReplayCameraDirector;
import neutka.marallys.marallyzen.replay.timeline.TimelineScheduler;

import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DirectorReplayBrowserScreen extends Screen {
    private static final int LIST_X = 10;
    private static final int LIST_Y = 30;
    private static final int LIST_WIDTH = 280;
    private static final int LIST_HEIGHT = 170;
    private static final int LINE_HEIGHT = 14;

    private final List<ReplayEntry> entries = new ArrayList<>();
    private int selectedIndex = -1;
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private String statusMessage = "";

    private Button playReplayButton;
    private Button stopReplayButton;
    private Button playCameraButton;
    private Button playTimelineButton;
    private Button refreshButton;
    private Button openFolderButton;
    private EditBox cameraTrackBox;
    private EditBox timelineBox;

    public DirectorReplayBrowserScreen() {
        super(Component.translatable("screen.marallyzen.director.title"));
    }

    @Override
    protected void init() {
        super.init();
        disableBlurForThisScreen();

        int rightX = LIST_X + LIST_WIDTH + 20;
        int topY = LIST_Y;

        playReplayButton = Button.builder(Component.literal("Play Replay"), btn -> playSelectedReplay())
            .bounds(rightX, topY, 150, 20)
            .build();
        this.addRenderableWidget(playReplayButton);

        stopReplayButton = Button.builder(Component.literal("Stop Replay"), btn -> stopReplay())
            .bounds(rightX, topY + 25, 150, 20)
            .build();
        this.addRenderableWidget(stopReplayButton);

        cameraTrackBox = new EditBox(this.font, rightX, topY + 55, 150, 18, Component.literal("Camera Track"));
        cameraTrackBox.setValue("");
        this.addRenderableWidget(cameraTrackBox);

        playCameraButton = Button.builder(Component.literal("Play Camera"), btn -> playCameraTrack())
            .bounds(rightX, topY + 75, 150, 20)
            .build();
        this.addRenderableWidget(playCameraButton);

        timelineBox = new EditBox(this.font, rightX, topY + 105, 150, 18, Component.literal("Timeline"));
        timelineBox.setValue("");
        this.addRenderableWidget(timelineBox);

        playTimelineButton = Button.builder(Component.literal("Play Timeline"), btn -> playTimeline())
            .bounds(rightX, topY + 125, 150, 20)
            .build();
        this.addRenderableWidget(playTimelineButton);

        refreshButton = Button.builder(Component.literal("Refresh"), btn -> reloadReplays())
            .bounds(rightX, topY + 155, 150, 20)
            .build();
        this.addRenderableWidget(refreshButton);

        openFolderButton = Button.builder(Component.literal("Open Folder"), btn -> openReplayFolder())
            .bounds(rightX, topY + 180, 150, 20)
            .build();
        this.addRenderableWidget(openFolderButton);

        reloadReplays();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.drawString(this.font, Component.literal("Replays"), LIST_X, LIST_Y - 12, 0xFFFFFF);
        drawReplayList(guiGraphics, mouseX, mouseY);

        int statusY = LIST_Y + LIST_HEIGHT + 10;
        String replayStatus = ReplayCompat.isReplayAvailable()
            ? (ReplayCompat.isReplayActive() ? "ReplayMod: ACTIVE" : "ReplayMod: READY")
            : "ReplayMod: MISSING";
        guiGraphics.drawString(this.font, Component.literal(replayStatus), LIST_X, statusY, 0xAAAAAA);
        guiGraphics.drawString(this.font,
            Component.literal(String.format("Time: %.2fs", ReplayCompat.getReplayTimeSeconds())),
            LIST_X,
            statusY + 12,
            0xAAAAAA
        );
        if (statusMessage != null && !statusMessage.isBlank()) {
            guiGraphics.drawString(this.font, Component.literal(statusMessage), LIST_X, statusY + 24, 0xAAAAAA);
        }

        for (var renderable : this.renderables) {
            renderable.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Intentionally empty to keep the world visible behind the director UI.
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX >= LIST_X && mouseX <= LIST_X + LIST_WIDTH && mouseY >= LIST_Y && mouseY <= LIST_Y + LIST_HEIGHT) {
            int localY = (int) (mouseY - LIST_Y) + scrollOffset;
            int index = localY / LINE_HEIGHT;
            if (index >= 0 && index < entries.size()) {
                selectedIndex = index;
                fillDefaultsFromSelection();
                ReplayEntry entry = entries.get(index);
                statusMessage = "Selected: " + entry.displayName();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (mouseX >= LIST_X && mouseX <= LIST_X + LIST_WIDTH && mouseY >= LIST_Y && mouseY <= LIST_Y + LIST_HEIGHT) {
            int scrollDelta = (int) Math.signum(deltaY) * LINE_HEIGHT;
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - scrollDelta));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void drawReplayList(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.fill(LIST_X, LIST_Y, LIST_X + LIST_WIDTH, LIST_Y + LIST_HEIGHT, 0xFF1A1A1A);
        guiGraphics.renderOutline(LIST_X, LIST_Y, LIST_WIDTH, LIST_HEIGHT, 0xFF444444);

        int visibleLines = LIST_HEIGHT / LINE_HEIGHT;
        maxScroll = Math.max(0, entries.size() * LINE_HEIGHT - visibleLines * LINE_HEIGHT);
        if (scrollOffset > maxScroll) {
            scrollOffset = maxScroll;
        }

        int startIndex = scrollOffset / LINE_HEIGHT;
        int y = LIST_Y + (LINE_HEIGHT - (scrollOffset % LINE_HEIGHT));
        for (int i = startIndex; i < entries.size() && y < LIST_Y + LIST_HEIGHT; i++) {
            ReplayEntry entry = entries.get(i);
            int color = (i == selectedIndex) ? 0xFFD48E03 : 0xFFFFFF;
            guiGraphics.drawString(this.font, Component.literal(entry.displayName()), LIST_X + 6, y - 11, color);
            y += LINE_HEIGHT;
        }
    }

    private void reloadReplays() {
        entries.clear();
        selectedIndex = -1;
        scrollOffset = 0;
        statusMessage = "";

        Path recordingsDir = Minecraft.getInstance().gameDirectory.toPath().resolve("replay_recordings");
        if (!Files.exists(recordingsDir)) {
            return;
        }
        try {
            Files.list(recordingsDir)
                .filter(path -> Files.isRegularFile(path))
                .filter(this::isReplayFile)
                .sorted(Comparator.comparing(Path::toString).reversed())
                .forEach(path -> entries.add(new ReplayEntry(path.toFile(), path.getFileName().toString())));
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("Failed to scan replays in {}", recordingsDir);
        }
    }

    private void playSelectedReplay() {
        ReplayEntry entry = getSelected();
        if (entry == null) {
            statusMessage = "Select a replay first.";
            return;
        }
        File file = entry.file();
        if (!file.exists() || !file.isFile()) {
            statusMessage = "Replay path is not a file.";
            return;
        }
        statusMessage = "Starting replay...";
        Marallyzen.LOGGER.info("DirectorReplayBrowserScreen: playSelectedReplay {}", file.getAbsolutePath());
        Minecraft.getInstance().setScreen(null);
        Marallyzen.LOGGER.info("DirectorReplayBrowserScreen: calling ReplayCompat.startReplay");
        boolean ok = ReplayCompat.startReplay(file);
        Marallyzen.LOGGER.info("DirectorReplayBrowserScreen: ReplayCompat.startReplay result={}", ok);
        if (ok) {
            Minecraft.getInstance().setScreen(new ReplayInputScreen());
        }
        statusMessage = ok
            ? "Replay started."
            : "Replay start failed. Check log.";
    }

    private void stopReplay() {
        boolean ok = ReplayCompat.stopReplay();
        ReplayCameraDirector.getInstance().stop();
        TimelineScheduler.getInstance().stop();
        statusMessage = ok ? "Replay stopped." : "Replay stop failed. Check log.";
    }

    private void playCameraTrack() {
        String trackId = cameraTrackBox.getValue();
        if (trackId != null && !trackId.isBlank()) {
            boolean ok = ReplayCameraDirector.getInstance().playTrack(trackId.trim());
            statusMessage = ok ? "Camera track started." : "Camera track not found.";
        } else {
            statusMessage = "Camera track id is empty.";
        }
    }

    private void playTimeline() {
        String trackId = timelineBox.getValue();
        if (trackId != null && !trackId.isBlank()) {
            boolean ok = TimelineScheduler.getInstance().playTrack(trackId.trim());
            statusMessage = ok ? "Timeline started." : "Timeline not found.";
        } else {
            statusMessage = "Timeline id is empty.";
        }
    }

    private void fillDefaultsFromSelection() {
        ReplayEntry entry = getSelected();
        if (entry == null) {
            return;
        }
        String baseName = entry.baseName();
        if (cameraTrackBox.getValue().isBlank()) {
            cameraTrackBox.setValue(baseName);
        }
        if (timelineBox.getValue().isBlank()) {
            timelineBox.setValue(baseName);
        }
    }

    private void openReplayFolder() {
        Path recordingsDir = Minecraft.getInstance().gameDirectory.toPath().resolve("replay_recordings");
        if (!Files.exists(recordingsDir)) {
            return;
        }
        try {
            java.awt.Desktop.getDesktop().open(recordingsDir.toFile());
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("Failed to open replay folder: {}", recordingsDir);
        }
    }

    private ReplayEntry getSelected() {
        if (selectedIndex < 0 || selectedIndex >= entries.size()) {
            return null;
        }
        return entries.get(selectedIndex);
    }

    private void disableBlurForThisScreen() {
        try {
            Class<?> configClass = Class.forName("eu.midnightdust.blur.config.BlurConfig");
            var field = configClass.getField("forceDisabledScreens");
            Object value = field.get(null);
            if (value instanceof List<?> list) {
                String name = this.getClass().getCanonicalName();
                if (!list.contains(name)) {
                    @SuppressWarnings("unchecked")
                    List<String> mutable = (List<String>) list;
                    mutable.add(name);
                }
            }
        } catch (ClassNotFoundException e) {
            // Blur is not installed.
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("Failed to disable blur for director screen.", e);
        }
    }

    private boolean isReplayFile(Path path) {
        String name = path.getFileName().toString();
        String lower = name.toLowerCase();
        if (lower.endsWith(".mcpr.del")) {
            return false;
        }
        return lower.endsWith(".mcpr") || lower.endsWith("%2emcpr");
    }

    private static String decodeName(String name) {
        try {
            return URLDecoder.decode(name, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return name;
        }
    }

    private record ReplayEntry(File file, String displayName) {
        String baseName() {
            String name = decodeName(displayName);
            String lower = name.toLowerCase();
            if (lower.endsWith(".mcpr")) {
                return name.substring(0, name.length() - 5);
            }
            if (lower.endsWith("%2emcpr")) {
                return name.substring(0, name.length() - 7);
            }
            int dot = name.lastIndexOf('.');
            return dot > 0 ? name.substring(0, dot) : name;
        }

        public String displayName() {
            return decodeName(displayName);
        }
    }
}
