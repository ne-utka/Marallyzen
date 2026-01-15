package neutka.marallys.marallyzen.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Рендерит значок разговора над NPC, у которых есть диалог.
 */
public class NpcTalkIconRenderer {
    private static final Map<UUID, TalkIcon> ICONS = new ConcurrentHashMap<>();
    private static final float ICON_SCALE = 0.01875f; // Reduced by 25%: 0.025 * 0.75
    private static final float BOB_AMPLITUDE = 0.08f;
    private static final float BOB_SPEED = 0.05f;
    private static final float HEIGHT_OFFSET = 0.9f;
    private static final float PIXEL_SIZE = 2.0f; // размер одного пикселя фигуры
    private static final int ICON_WIDTH = 5;
    private static final int ICON_HEIGHT = 7;
    // 5x7 маска по образцу
    private static final boolean[][] ICON_MASK = new boolean[][]{
            {true,  false, false, false, true },
            {true,  true,  true,  true,  true },
            {true,  false, false, false, true },
            {false, true,  true,  true,  false},
            {false, false, true,  false, false},
            {false, false, true,  false, false},
            {false, false, true,  false, false},
    };

    public static void showIcon(UUID npcUuid, int argbColor) {
        ICONS.put(npcUuid, new TalkIcon(argbColor));
    }

    public static void hideIcon(UUID npcUuid) {
        ICONS.remove(npcUuid);
    }

    public static void render(PoseStack poseStack, Camera camera, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || ICONS.isEmpty()) {
            return;
        }

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        long gameTime = mc.level.getGameTime();

        for (Map.Entry<UUID, TalkIcon> entry : ICONS.entrySet()) {
            Entity entity = findEntityByUuid(entry.getKey(), mc.level);
            if (entity == null || !entity.isAlive()) {
                continue;
            }

            double x = Mth.lerp(partialTick, entity.xo, entity.getX());
            double y = Mth.lerp(partialTick, entity.yo, entity.getY());
            double z = Mth.lerp(partialTick, entity.zo, entity.getZ());

            // Высота над головой
            float yOffset = entity.getBbHeight() + HEIGHT_OFFSET;
            // Плавное покачивание
            float bob = (float) Math.sin((gameTime + partialTick) * BOB_SPEED) * BOB_AMPLITUDE;

            poseStack.pushPose();
            poseStack.translate(x - camera.getPosition().x, y - camera.getPosition().y + yOffset + bob, z - camera.getPosition().z);

            // Поворачиваем к камере (billboard)
            float cameraYaw = camera.getYRot();
            float cameraPitch = camera.getXRot();
            poseStack.mulPose(Axis.YP.rotationDegrees(-cameraYaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(cameraPitch));
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0f));

            poseStack.scale(ICON_SCALE, ICON_SCALE, ICON_SCALE);

            Matrix4f matrix = poseStack.last().pose();

            int color = entry.getValue().argbColor | 0xFF000000; // гарантируем альфу
            renderPixelIcon(matrix, bufferSource, color);

            bufferSource.endBatch();
            poseStack.popPose();
        }
    }

    private static Entity findEntityByUuid(UUID uuid, ClientLevel level) {
        if (level == null) {
            return null;
        }
        for (Entity e : level.entitiesForRendering()) {
            if (uuid.equals(e.getUUID())) {
                return e;
            }
        }
        return null;
    }

    private static void renderPixelIcon(Matrix4f matrix, MultiBufferSource.BufferSource bufferSource, int color) {
        VertexConsumer vc = bufferSource.getBuffer(net.minecraft.client.renderer.RenderType.gui());

        float widthPx = ICON_WIDTH * PIXEL_SIZE;
        float heightPx = ICON_HEIGHT * PIXEL_SIZE;
        float originX = -widthPx / 2.0f;
        float originY = 0.0f;

        for (int y = 0; y < ICON_HEIGHT; y++) {
            for (int x = 0; x < ICON_WIDTH; x++) {
                if (!ICON_MASK[y][x]) continue;
                float left = originX + x * PIXEL_SIZE;
                float top = originY + y * PIXEL_SIZE;
                float right = left + PIXEL_SIZE;
                float bottom = top + PIXEL_SIZE;
                addQuad(vc, matrix, left, top, right, bottom, color, -0.01f);
            }
        }
    }

    private static void addQuad(VertexConsumer vc, Matrix4f matrix,
                                float left, float top, float right, float bottom,
                                int color, float z) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        // Two triangles forming a quad
        vc.addVertex(matrix, left, top, z).setColor(r, g, b, a).setUv(0, 0);
        vc.addVertex(matrix, right, top, z).setColor(r, g, b, a).setUv(1, 0);
        vc.addVertex(matrix, right, bottom, z).setColor(r, g, b, a).setUv(1, 1);

        vc.addVertex(matrix, left, top, z).setColor(r, g, b, a).setUv(0, 0);
        vc.addVertex(matrix, right, bottom, z).setColor(r, g, b, a).setUv(1, 1);
        vc.addVertex(matrix, left, bottom, z).setColor(r, g, b, a).setUv(0, 1);
    }

    private record TalkIcon(int argbColor) {
    }
}

