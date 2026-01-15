package neutka.marallys.marallyzen.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Mixin(PlayerTabOverlay.class)
public class TabListNpcFilterMixin {
    @Inject(method = "getPlayerInfos()Ljava/util/List;", at = @At("RETURN"), cancellable = true)
    private void marallyzen$filterNpcEntries(CallbackInfoReturnable<List<PlayerInfo>> cir) {
        List<PlayerInfo> original = cir.getReturnValue();
        if (original == null || original.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        List<PlayerInfo> filtered = original.stream()
                .filter(info -> !isNpc(mc, info))
                .collect(Collectors.toList());
        cir.setReturnValue(filtered);
    }

    private boolean isNpc(Minecraft mc, PlayerInfo info) {
        if (info == null || info.getProfile() == null) {
            return false;
        }
        UUID uuid = info.getProfile().getId();
        if (uuid == null) {
            return false;
        }
        Player player = mc.level.getPlayerByUUID(uuid);
        if (player != null && player.getTags().contains("marallyzen_npc")) {
            return true;
        }
        String name = info.getProfile().getName();
        var scoreboard = mc.level.getScoreboard();
        if (name != null && !name.isEmpty()) {
            var team = scoreboard.getPlayersTeam(name);
            if (team != null && "marallyzen_npc_tab".equals(team.getName())) {
                return true;
            }
        }
        if (uuid != null) {
            var team = scoreboard.getPlayersTeam(uuid.toString());
            return team != null && "marallyzen_npc_tab".equals(team.getName());
        }
        return false;
    }
}
