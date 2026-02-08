package neutka.marallys.marallyzen.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import neutka.marallys.marallyzen.Marallyzen;

@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT)
public class PosterEntityPromptHudRenderer {
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        PosterEntityPromptHud.getInstance().tick();
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent.AfterParticles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        Camera camera = mc.gameRenderer.getMainCamera();
        PoseStack poseStack = event.getPoseStack();
        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        PosterEntityPromptHud.getInstance().renderInWorld(poseStack, camera, partialTick);
    }
}
