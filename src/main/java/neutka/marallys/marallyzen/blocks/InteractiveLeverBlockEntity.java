package neutka.marallys.marallyzen.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.util.GeckoLibUtil;

public class InteractiveLeverBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final RawAnimation PULL_ANIM =
        RawAnimation.begin().thenPlay("animation.interactive_lever");
    private static final RawAnimation IDLE_UP =
        RawAnimation.begin().thenLoop("animation.interactive_lever_idle_up");
    private static final RawAnimation IDLE_DOWN =
        RawAnimation.begin().thenLoop("animation.interactive_lever_idle_down");
    private static final int PULL_ANIM_TICKS = 100;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public InteractiveLeverBlockEntity(BlockPos pos, BlockState state) {
        super(MarallyzenBlockEntities.INTERACTIVE_LEVER_BE.get(), pos, state);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate)
            .triggerableAnim("pull", PULL_ANIM)
            .receiveTriggeredAnimations());
    }

    private PlayState predicate(AnimationState<InteractiveLeverBlockEntity> state) {
        if (state.getController().isPlayingTriggeredAnimation()) {
            Integer useTicks = getAnimData(DataTickets.USE_TICKS);
            if (useTicks != null && useTicks > 0) {
                state.getController().setAnimationSpeed((double) PULL_ANIM_TICKS / (double) useTicks);
            } else {
                state.getController().setAnimationSpeed(1.0);
            }
            return PlayState.CONTINUE;
        }
        state.getController().setAnimationSpeed(1.0);
        boolean powered = getBlockState().getValue(net.minecraft.world.level.block.LeverBlock.POWERED);
        state.getController().setAnimation(powered ? IDLE_DOWN : IDLE_UP);
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
