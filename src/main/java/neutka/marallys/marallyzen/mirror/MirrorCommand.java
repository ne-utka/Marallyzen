package neutka.marallys.marallyzen.mirror;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

public final class MirrorCommand {
    private MirrorCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("mirror")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.literal("peer").executes(MirrorCommand::peer))
                .then(Commands.literal("unlink").executes(MirrorCommand::unlink))
                .then(Commands.literal("dispeer").executes(MirrorCommand::unlink));
    }

    private static int peer(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Component.literal("Only player can use this command."));
            return 0;
        }
        MirrorRegistry.armPeer(player);
        return 1;
    }

    private static int unlink(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Component.literal("Only player can use this command."));
            return 0;
        }
        MirrorRegistry.armUnlink(player);
        return 1;
    }
}

