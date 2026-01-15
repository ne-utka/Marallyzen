package neutka.marallys.marallyzen.mixin.client;

import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import neutka.marallys.marallyzen.blocks.DictaphoneSimpleBlock;
import neutka.marallys.marallyzen.client.ClientDictaphoneManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SectionCompiler.class)
public class DictaphoneRenderMixin {
    static {
        System.out.println("DictaphoneRenderMixin: LOADED");
    }

    @Redirect(
        method = "compile(Lnet/minecraft/core/SectionPos;Lnet/minecraft/client/renderer/chunk/RenderChunkRegion;Lcom/mojang/blaze3d/vertex/VertexSorting;Lnet/minecraft/client/renderer/SectionBufferBuilderPack;Ljava/util/List;)Lnet/minecraft/client/renderer/chunk/SectionCompiler$Results;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/chunk/RenderChunkRegion;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"
        ),
        require = 0
    )
    private BlockState marallyzen$hideDictaphone(RenderChunkRegion region, BlockPos pos) {
        BlockState state = region.getBlockState(pos);
        if (state.getBlock() instanceof DictaphoneSimpleBlock
            && ClientDictaphoneManager.hasClientDictaphone(pos)) {
            System.out.println("DictaphoneRenderMixin: HIDE " + pos);
            return Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    
}
