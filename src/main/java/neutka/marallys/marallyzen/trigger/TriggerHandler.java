package neutka.marallys.marallyzen.trigger;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

@FunctionalInterface
public interface TriggerHandler {
    boolean shouldActivate(ServerPlayer player, BlockPos pos, Level level);
}
