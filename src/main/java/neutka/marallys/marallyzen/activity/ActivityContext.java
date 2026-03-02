package neutka.marallys.marallyzen.activity;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import neutka.marallys.marallyzen.trigger.blueprint.TriggerBlueprint;
import neutka.marallys.marallyzen.trigger.instance.TriggerInstance;

public record ActivityContext(
        MinecraftServer server,
        ServerLevel level,
        ServerPlayer player,
        TriggerInstance instance,
        TriggerBlueprint blueprint
) {
}
