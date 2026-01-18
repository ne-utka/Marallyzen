package neutka.marallys.marallyzen.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import neutka.marallys.marallyzen.blocks.MarallyzenBlocks;
import neutka.marallys.marallyzen.network.NetworkHelper;
import neutka.marallys.marallyzen.network.RadioInteractPacket;

@EventBusSubscriber(value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class RadioInteractionHandler {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        if (event.getButton() != 1 || event.getAction() != 1) {
            return;
        }
        HitResult hitResult = mc.hitResult;
        if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockHitResult blockHit = (BlockHitResult) hitResult;
        BlockState state = mc.level.getBlockState(blockHit.getBlockPos());
        Block block = state.getBlock();
        if (block != MarallyzenBlocks.RADIO.get()) {
            return;
        }

        boolean shift = Screen.hasShiftDown();
        if (shift) {
            boolean nowPlaying = ClientRadioManager.toggle(blockHit.getBlockPos());
            String stationName = ClientRadioManager.getSelectedStationName();
            NetworkHelper.sendToServer(new RadioInteractPacket(blockHit.getBlockPos(), RadioInteractPacket.ACTION_TOGGLE, stationName));
            if (!nowPlaying) {
                ClientRadioManager.stop(blockHit.getBlockPos());
            }
        } else {
            ClientRadioManager.switchStation(blockHit.getBlockPos());
            String stationName = ClientRadioManager.getSelectedStationName();
            NetworkHelper.sendToServer(new RadioInteractPacket(blockHit.getBlockPos(), RadioInteractPacket.ACTION_SWITCH, stationName));
        }
        event.setCanceled(true);
    }
}
