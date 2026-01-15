package neutka.marallys.marallyzen.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.Method;
import java.util.Objects;

public class OldTvBlockEntity extends BlockEntity {
    private Object loopHandle;
    private String currentSoundId;

    public OldTvBlockEntity(BlockPos pos, BlockState state) {
        super(MarallyzenBlockEntities.OLD_TV_BE.get(), pos, state);
    }

    public void clientTick() {
        BlockState state = getBlockState();
        boolean isOn = state.hasProperty(OldTvBlock.ON) && state.getValue(OldTvBlock.ON);
        if (isOn) {
            String desiredSoundId = resolveSoundId();
            if (loopHandle == null || !Objects.equals(currentSoundId, desiredSoundId)) {
                stopLoopSound();
                loopHandle = invokeSoundMethod("startLoop", worldPosition, desiredSoundId);
                currentSoundId = desiredSoundId;
            }
        } else {
            stopLoopSound();
        }
    }

    @Override
    public void setRemoved() {
        stopLoopSound();
        super.setRemoved();
    }

    private void stopLoopSound() {
        if (loopHandle != null) {
            invokeSoundMethod("stopLoop", loopHandle);
            loopHandle = null;
        }
        currentSoundId = null;
    }

    private String resolveSoundId() {
        if (level == null) {
            return null;
        }
        var state = neutka.marallys.marallyzen.client.OldTvMediaManager.getMediaState(worldPosition, level.dimension());
        if (state == null || !state.animated()) {
            return null;
        }
        return state.soundId();
    }

    private static Object invokeSoundMethod(String methodName, Object... args) {
        try {
            Class<?> controller = Class.forName("neutka.marallys.marallyzen.client.OldTvSoundController");
            if (args.length == 1) {
                Object arg = args[0];
                Method method = controller.getDeclaredMethod(methodName, arg instanceof BlockPos ? BlockPos.class : Object.class);
                return method.invoke(null, arg);
            }
            Method method = controller.getDeclaredMethod(methodName, BlockPos.class, String.class);
            return method.invoke(null, args);
        } catch (Exception ignored) {
            return null;
        }
    }
}
