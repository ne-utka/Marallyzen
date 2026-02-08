package neutka.marallys.marallyzen.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.object.PlayState;
import software.bernie.geckolib.animation.state.AnimationTest;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.util.GeckoLibUtil;

public class InteractiveValveBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final RawAnimation SPIN_ANIM =
        RawAnimation.begin().thenPlay("animation");
    private static final RawAnimation WOBBLE_LEFT_ANIM =
        RawAnimation.begin().thenPlay("wobble_left");
    private static final RawAnimation WOBBLE_RIGHT_ANIM =
        RawAnimation.begin().thenPlay("wobble_right");
    private static final int SPIN_ANIM_TICKS = 90;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public InteractiveValveBlockEntity(BlockPos pos, BlockState state) {
        super(MarallyzenBlockEntities.INTERACTIVE_VALVE_BE.get(), pos, state);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("controller", 0, this::predicate)
            .triggerableAnim("spin", SPIN_ANIM)
            .triggerableAnim("wobble_left", WOBBLE_LEFT_ANIM)
            .triggerableAnim("wobble_right", WOBBLE_RIGHT_ANIM)
            .receiveTriggeredAnimations());
    }

    private PlayState predicate(AnimationTest<InteractiveValveBlockEntity> state) {
        if (state.controller().isPlayingTriggeredAnimation()) {
            Integer useTicks = state.getData(DataTickets.USE_TICKS);
            if (useTicks != null && useTicks > 0) {
                state.controller().setAnimationSpeed((double) SPIN_ANIM_TICKS / (double) useTicks);
            } else {
                state.controller().setAnimationSpeed(1.0);
            }
            return PlayState.CONTINUE;
        }
        state.controller().setAnimationSpeed(1.0);
        return PlayState.STOP;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}

