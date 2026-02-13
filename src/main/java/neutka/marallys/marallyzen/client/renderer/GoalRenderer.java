package neutka.marallys.marallyzen.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
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
import net.minecraft.util.Mth;
import neutka.marallys.marallyzen.entity.GoalDisplayEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Billboard world-space text renderer for goal entities.
 */
public class GoalRenderer extends EntityRenderer<GoalDisplayEntity, GoalRenderer.RenderState> {
    private static final float TEXT_SCALE = 0.025F;

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

        String title = applyPlaceholders(entity.displayTitle(), entity);
        String[] rawLines = entity.displayLinesRaw().isBlank() ? new String[0] : entity.displayLinesRaw().split("\n");
        List<String> lines = new ArrayList<>(rawLines.length);
        int maxWidth = 0;

        if (!title.isBlank()) {
            maxWidth = Math.max(maxWidth, font.width(title));
        }
        for (String rawLine : rawLines) {
            String line = applyPlaceholders(rawLine, entity);
            lines.add(line);
            maxWidth = Math.max(maxWidth, font.width(line));
        }

        int leftX = -maxWidth / 2;
        int y = 0;
        if (!title.isBlank()) {
            drawLeft(font, bufferSource, poseStack, title, leftX, y, titleColor);
            y += font.lineHeight + 2;
        }

        for (String line : lines) {
            drawLeft(font, bufferSource, poseStack, line, leftX, y, linesColor);
            y += font.lineHeight + 1;
        }
        poseStack.popPose();
    }

    private void drawLeft(Font font, MultiBufferSource bufferSource, PoseStack poseStack, String text, int x, int y, int rgb) {
        font.drawInBatch(
                text,
                x,
                y,
                (0xFF << 24) | (rgb & 0xFFFFFF),
                false,
                poseStack.last().pose(),
                bufferSource,
                Font.DisplayMode.SEE_THROUGH,
                0,
                LightTexture.FULL_BRIGHT
        );
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
}
