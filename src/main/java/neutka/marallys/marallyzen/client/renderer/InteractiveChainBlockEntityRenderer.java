package neutka.marallys.marallyzen.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import neutka.marallys.marallyzen.blocks.InteractiveChainBlockEntity;
import neutka.marallys.marallyzen.blocks.MarallyzenBlocks;
import neutka.marallys.marallyzen.client.chain.InteractiveChainSwingVisuals;
import org.joml.Quaternionf;

@OnlyIn(Dist.CLIENT)
public class InteractiveChainBlockEntityRenderer implements BlockEntityRenderer<InteractiveChainBlockEntity> {
    private static long lastLogMillis = 0L;
    private static final double IDLE_PERIOD_SEC = 6.0;
    private static final double IDLE_ANGLE_X_DEG = 0.895;
    private static final double IDLE_ANGLE_Z_DEG = 0.364;
    private static final double IDLE_PHASE_OFFSET_RAD = Math.toRadians(60.0);
    private static final double SWING_PERIOD_SEC = 20.0;
    private static final double[] SWING_TIMES = {
            0.0, 0.54167, 2.0, 3.45833, 6.29167, 8.16667,
            10.0, 11.83333, 13.875, 15.75, 18.33333, 20.0
    };
    private static final double[] SWING_ANGLES_DEG = {
            0.0, 17.5, -27.5, 35.0, -27.5, 17.5,
            -10.0, 5.0, -2.5, 0.0, 0.0, 0.0
    };
    private static final double[] SWING_BONE2_TIMES = {
            0.0, 0.20833, 0.83333, 2.45833, 3.83333, 5.08333,
            6.83333, 8.58333, 10.58333, 12.29167, 14.125, 15.83333,
            17.45833, 19.95833, 20.0
    };
    private static final double[] SWING_BONE2_ANGLES_DEG = {
            0.0, -12.5, 17.5, -22.5, 22.5, 30.0,
            -25.0, 30.0, -12.5, 12.5, -12.5, 7.5,
            -2.5, 0.0, 0.0
    };
    private static final double[] SWING_BONE3_TIMES = {
            0.0, 0.20833, 0.83333, 2.45833, 3.83333, 5.08333,
            6.83333, 8.58333, 10.58333, 12.29167, 14.125, 15.83333,
            17.45833, 19.95833, 20.0
    };
    private static final double[] SWING_BONE3_ANGLES_DEG = {
            0.0, -12.5, 20.0, -40.0, 27.5, 30.0,
            -32.5, 37.5, -37.5, 17.5, -20.0, 15.0,
            2.5, 0.0, 0.0
    };

    public InteractiveChainBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public boolean shouldRenderOffScreen(InteractiveChainBlockEntity blockEntity) {
        return true;
    }

    @Override
    public void render(InteractiveChainBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Level level = blockEntity.getLevel();
        if (level == null) {
            return;
        }

        BlockPos pos = blockEntity.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() != MarallyzenBlocks.INTERACTIVE_CHAIN.get()) {
            return;
        }

        BlockPos above = pos.above();
        if (level.getBlockState(above).getBlock() == MarallyzenBlocks.INTERACTIVE_CHAIN.get()) {
            return;
        }

        int length = getChainLength(level, pos);
        if (length <= 0) {
            return;
        }

        long nowMillis = Util.getMillis();
        Vec3 anchor = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        boolean swingActive = InteractiveChainSwingVisuals.getInterpolatedOffset(pos, nowMillis) != null
                || InteractiveChainSwingVisuals.getLatestOffset(pos) != null;
        double idleXDeg = 0.0;
        double idleZDeg = 0.0;
        double mainSwingDeg = 0.0;
        double bone2SwingDeg = 0.0;
        double bone3SwingDeg = 0.0;
        if (swingActive) {
            mainSwingDeg = sampleCatmullRom(nowMillis / 1000.0 % SWING_PERIOD_SEC, SWING_TIMES, SWING_ANGLES_DEG);
            bone2SwingDeg = sampleCatmullRom(nowMillis / 1000.0 % SWING_PERIOD_SEC, SWING_BONE2_TIMES, SWING_BONE2_ANGLES_DEG);
            bone3SwingDeg = sampleCatmullRom(nowMillis / 1000.0 % SWING_PERIOD_SEC, SWING_BONE3_TIMES, SWING_BONE3_ANGLES_DEG);
        } else {
            double[] idle = idleAnglesDeg(nowMillis);
            idleXDeg = idle[0];
            idleZDeg = idle[1];
        }
        logRenderState(swingActive ? "swing" : "idle", pos, anchor, new Vec3(mainSwingDeg, bone2SwingDeg, bone3SwingDeg));

        BlockState chainState = Blocks.CHAIN.defaultBlockState().setValue(ChainBlock.AXIS, Direction.Axis.Y);
        poseStack.pushPose();
        poseStack.translate(anchor.x - pos.getX(), anchor.y - pos.getY(), anchor.z - pos.getZ());

        for (int i = 0; i < length; i++) {
            double frac = length == 1 ? 1.0 : (double) i / (double) (length - 1);
            double segmentWeight = 0.3 + (0.7 * frac);
            double swingWeight2 = smoothStep(0.15, 0.7, frac);
            double swingWeight3 = smoothStep(0.4, 1.0, frac);
            double bendXDeg;
            double bendZDeg;
            if (swingActive) {
                bendXDeg = (mainSwingDeg * segmentWeight)
                        + (bone2SwingDeg * swingWeight2)
                        + (bone3SwingDeg * swingWeight3);
                bendZDeg = 0.0;
            } else {
                bendXDeg = idleXDeg * segmentWeight;
                bendZDeg = idleZDeg * segmentWeight;
            }

            Quaternionf rot = new Quaternionf()
                    .rotateX((float) Math.toRadians(bendXDeg))
                    .rotateZ((float) Math.toRadians(bendZDeg));
            poseStack.mulPose(rot);

            poseStack.pushPose();
            poseStack.translate(-0.5, -1.0, -0.5);
            int light = LevelRenderer.getLightColor(level, pos.below(i));
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                    chainState,
                    poseStack,
                    bufferSource,
                    light,
                    packedOverlay
            );
            poseStack.popPose();

            poseStack.translate(0.0, -1.0, 0.0);
        }
        poseStack.popPose();
    }

    private static int getChainLength(Level level, BlockPos root) {
        int length = 0;
        BlockPos current = root;
        while (level.getBlockState(current).getBlock() == MarallyzenBlocks.INTERACTIVE_CHAIN.get()) {
            length++;
            current = current.below();
        }
        return length;
    }

    private static double[] idleAnglesDeg(long nowMillis) {
        double t = (nowMillis / 1000.0) % IDLE_PERIOD_SEC;
        double phase = (t / IDLE_PERIOD_SEC) * (Math.PI * 2.0);
        double xDeg = Math.sin(phase) * IDLE_ANGLE_X_DEG;
        double zDeg = Math.sin(phase + IDLE_PHASE_OFFSET_RAD) * IDLE_ANGLE_Z_DEG;
        return new double[] { xDeg, zDeg };
    }

    private static double sampleCatmullRom(double t, double[] times, double[] values) {
        if (times.length == 0) {
            return 0.0;
        }
        if (t <= times[0]) {
            return values[0];
        }
        for (int i = 0; i < times.length - 1; i++) {
            double t1 = times[i];
            double t2 = times[i + 1];
            if (t >= t1 && t <= t2) {
                double u = (t - t1) / (t2 - t1);
                double p0 = values[Math.max(0, i - 1)];
                double p1 = values[i];
                double p2 = values[i + 1];
                double p3 = values[Math.min(values.length - 1, i + 2)];
                return catmullRom(u, p0, p1, p2, p3);
            }
        }
        return values[values.length - 1];
    }

    private static double catmullRom(double t, double p0, double p1, double p2, double p3) {
        double t2 = t * t;
        double t3 = t2 * t;
        return 0.5 * ((2.0 * p1)
                + (-p0 + p2) * t
                + (2.0 * p0 - 5.0 * p1 + 4.0 * p2 - p3) * t2
                + (-p0 + 3.0 * p1 - 3.0 * p2 + p3) * t3);
    }

    private static void logRenderState(String label, BlockPos pos, Vec3 anchor, Vec3 offset) {
        long now = System.currentTimeMillis();
        if (now - lastLogMillis < 1000L) {
            return;
        }
        lastLogMillis = now;
        if (anchor == null || offset == null) {
            neutka.marallys.marallyzen.Marallyzen.LOGGER.info(
                "[InteractiveChain] renderer {} root={} anchor={} offset={}",
                label,
                pos,
                anchor,
                offset
            );
            return;
        }
        neutka.marallys.marallyzen.Marallyzen.LOGGER.info(
            "[InteractiveChain] renderer {} root={} anchor=({}, {}, {}) offset=({}, {}, {})",
            label,
            pos,
            anchor.x, anchor.y, anchor.z,
            offset.x, offset.y, offset.z
        );
    }

    private static double smoothStep(double edge0, double edge1, double x) {
        double t = Mth.clamp((x - edge0) / (edge1 - edge0), 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }
}
