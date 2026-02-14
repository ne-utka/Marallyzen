package neutka.marallys.marallyzen.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import neutka.marallys.marallyzen.entity.GoalDisplayEntity;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Billboard world-space text renderer for goal entities.
 */
public class GoalRenderer extends EntityRenderer<GoalDisplayEntity, GoalRenderer.RenderState> {
    private static final float TEXT_SCALE = 0.025F;
    private static final int SHADOW_OFFSET_X = 1;
    private static final int SHADOW_OFFSET_Y = 1;
    private static final int SHADOW_ALPHA = 0x96;
    private static final float SHADOW_SHADE_FACTOR = 0.24F;
    private static final float MAIN_Z_BIAS = -0.01F;

    public GoalRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public RenderState createRenderState() {
        return new RenderState();
    }

    @Override
    public void extractRenderState(GoalDisplayEntity entity, RenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.entity = entity;
        state.partialTick = partialTick;
    }

    @Override
    public void submit(RenderState state, PoseStack poseStack, SubmitNodeCollector renderTasks, CameraRenderState cameraState) {
        GoalDisplayEntity entity = state.entity;
        if (entity == null) {
            return;
        }
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        renderGoal(entity, state.partialTick, poseStack, bufferSource);
        bufferSource.endBatch();
        super.submit(state, poseStack, renderTasks, cameraState);
    }

    private void renderGoal(GoalDisplayEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource) {
        double baseX = Mth.lerp(partialTick, entity.xo, entity.getX());
        double baseY = Mth.lerp(partialTick, entity.yo, entity.getY());
        double baseZ = Mth.lerp(partialTick, entity.zo, entity.getZ());

        long gameTime = entity.level().getGameTime();
        double amplitude = entity.floatAmplitude();
        double speed = entity.floatSpeed();
        double height = entity.floatHeight();
        double wavePrimary = Math.sin((gameTime + partialTick) * speed) * amplitude;
        double waveSecondary = Math.sin((gameTime + partialTick) * speed * 0.35D) * amplitude * 0.5D;
        double offsetY = height + wavePrimary + waveSecondary;

        poseStack.pushPose();
        poseStack.translate(entity.getX() - baseX, entity.getY() - baseY + offsetY, entity.getZ() - baseZ);
        poseStack.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(-TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);

        Font font = Minecraft.getInstance().font;
        int titleColor = entity.titleColor();
        int linesColor = entity.linesColor();
        FontDescription titleFont = parseFont(entity.titleFont());
        FontDescription linesFont = parseFont(entity.linesFont());

        String title = applyPlaceholders(entity.displayTitle(), entity);
        String[] rawLines = entity.displayLinesRaw().isBlank() ? new String[0] : entity.displayLinesRaw().split("\n");
        StyledLine titleLine = buildStyledLine(title, titleFont, titleColor, font);
        List<StyledLine> lines = new ArrayList<>(rawLines.length);
        int maxWidth = 0;

        if (!title.isBlank()) {
            maxWidth = Math.max(maxWidth, titleLine.width());
        }
        for (String rawLine : rawLines) {
            String line = applyPlaceholders(rawLine, entity);
            StyledLine lineComponent = buildStyledLine(line, linesFont, linesColor, font);
            lines.add(lineComponent);
            maxWidth = Math.max(maxWidth, lineComponent.width());
        }

        int leftX = -maxWidth / 2;
        int y = 0;
        if (!title.isBlank()) {
            drawLeft(font, bufferSource, poseStack, titleLine, leftX, y);
            y += font.lineHeight + 2;
        }

        for (StyledLine line : lines) {
            drawLeft(font, bufferSource, poseStack, line, leftX, y);
            y += font.lineHeight + 1;
        }
        poseStack.popPose();
    }

    private void drawLeft(Font font, MultiBufferSource bufferSource, PoseStack poseStack, StyledLine line, int x, int y) {
        poseStack.pushPose();
        drawLine(font, bufferSource, poseStack, line, x + SHADOW_OFFSET_X, y + SHADOW_OFFSET_Y, true);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0D, 0.0D, MAIN_Z_BIAS);
        drawLine(font, bufferSource, poseStack, line, x, y, false);
        poseStack.popPose();
    }

    private void drawLine(Font font, MultiBufferSource bufferSource, PoseStack poseStack, StyledLine line, int x, int y, boolean shadow) {
        int cursorX = x;
        for (StyledSegment segment : line.segments()) {
            font.drawInBatch(
                    segment.component(),
                    cursorX,
                    y,
                    shadow ? shadeForShadow(segment.rgb()) : ((0xFF << 24) | (segment.rgb() & 0xFFFFFF)),
                    false,
                    poseStack.last().pose(),
                    bufferSource,
                    Font.DisplayMode.NORMAL,
                    0,
                    LightTexture.FULL_BRIGHT
            );
            cursorX += segment.width();
        }
    }

    private int shadeForShadow(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        int sr = clamp((int) Math.round(r * SHADOW_SHADE_FACTOR));
        int sg = clamp((int) Math.round(g * SHADOW_SHADE_FACTOR));
        int sb = clamp((int) Math.round(b * SHADOW_SHADE_FACTOR));
        return (SHADOW_ALPHA << 24) | (sr << 16) | (sg << 8) | sb;
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private StyledLine buildStyledLine(String text, FontDescription font, int defaultRgb, Font fontRenderer) {
        String raw = text == null ? "" : text;
        Deque<Integer> colorStack = new ArrayDeque<>();
        colorStack.push(defaultRgb & 0xFFFFFF);
        List<StyledSegment> segments = new ArrayList<>();

        StringBuilder buffer = new StringBuilder(raw.length());
        int i = 0;
        while (i < raw.length()) {
            char c = raw.charAt(i);
            if (c == '\\' && i + 1 < raw.length() && raw.charAt(i + 1) == '[') {
                buffer.append('[');
                i += 2;
                continue;
            }
            if (c == '[') {
                int close = raw.indexOf(']', i + 1);
                if (close > i) {
                    String token = raw.substring(i + 1, close).trim();
                    if ("/".equals(token)) {
                        appendSegment(segments, buffer, font, colorStack.peek(), fontRenderer);
                        if (colorStack.size() > 1) {
                            colorStack.pop();
                        }
                        i = close + 1;
                        continue;
                    }
                    if (token.startsWith("#")) {
                        Integer parsed = parseHexColorToken(token);
                        if (parsed != null) {
                            appendSegment(segments, buffer, font, colorStack.peek(), fontRenderer);
                            colorStack.push(parsed);
                            i = close + 1;
                            continue;
                        }
                    }
                }
            }
            buffer.append(c);
            i++;
        }
        appendSegment(segments, buffer, font, colorStack.peek(), fontRenderer);

        if (segments.isEmpty()) {
            MutableComponent emptyComponent = withFont(Component.literal(""), font);
            return new StyledLine(List.of(new StyledSegment(emptyComponent, defaultRgb & 0xFFFFFF, 0)), 0);
        }
        int width = 0;
        for (StyledSegment segment : segments) {
            width += segment.width();
        }
        return new StyledLine(List.copyOf(segments), width);
    }

    private void appendSegment(List<StyledSegment> target, StringBuilder buffer, FontDescription font, int rgb, Font fontRenderer) {
        if (buffer.isEmpty()) {
            return;
        }
        String segment = buffer.toString();
        buffer.setLength(0);

        MutableComponent component = withFont(Component.literal(segment), font);
        int width = fontRenderer.width(component);
        target.add(new StyledSegment(component, rgb & 0xFFFFFF, width));
    }

    private MutableComponent withFont(MutableComponent component, FontDescription font) {
        if (font == null) {
            return component;
        }
        Style style = Style.EMPTY.withFont(font);
        return component.withStyle(style);
    }

    private Integer parseHexColorToken(String token) {
        String hex = token.substring(1).trim();
        if (hex.length() == 3) {
            char r = hex.charAt(0);
            char g = hex.charAt(1);
            char b = hex.charAt(2);
            hex = "" + r + r + g + g + b + b;
        }
        if (hex.length() != 6) {
            return null;
        }
        try {
            return Integer.parseInt(hex, 16) & 0xFFFFFF;
        } catch (Exception ignored) {
            return null;
        }
    }

    private FontDescription parseFont(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return new FontDescription.Resource(Identifier.parse(raw.trim()));
        } catch (Exception ignored) {
            return null;
        }
    }

    private String applyPlaceholders(String raw, GoalDisplayEntity entity) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        long progress = entity.progress();
        long target = entity.target();
        long percent = target <= 0L ? 0L : Math.round((progress * 100.0D) / Math.max(1L, target));

        String result = raw
                .replace("{progress}", Long.toString(progress))
                .replace("{target}", Long.toString(target))
                .replace("{percent}", Long.toString(percent));

        for (Map.Entry<String, String> entry : parseVars(entity.serializedVars()).entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    private Map<String, String> parseVars(String encoded) {
        Map<String, String> result = new HashMap<>();
        if (encoded == null || encoded.isBlank()) {
            return result;
        }
        String[] lines = encoded.split("\n");
        for (String line : lines) {
            int sep = findSeparator(line);
            if (sep <= 0) {
                continue;
            }
            String key = unescape(line.substring(0, sep));
            String value = unescape(line.substring(sep + 1));
            if (!key.isBlank()) {
                result.put(key, value);
            }
        }
        return result;
    }

    private int findSeparator(String line) {
        boolean escaped = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            if (c == '=') {
                return i;
            }
        }
        return -1;
    }

    private String unescape(String raw) {
        StringBuilder out = new StringBuilder(raw.length());
        boolean escaped = false;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (escaped) {
                if (c == 'n') {
                    out.append('\n');
                } else {
                    out.append(c);
                }
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            out.append(c);
        }
        if (escaped) {
            out.append('\\');
        }
        return out.toString();
    }

    public static final class RenderState extends EntityRenderState {
        private GoalDisplayEntity entity;
        private float partialTick;
    }

    private record StyledLine(List<StyledSegment> segments, int width) {
    }

    private record StyledSegment(MutableComponent component, int rgb, int width) {
    }
}
