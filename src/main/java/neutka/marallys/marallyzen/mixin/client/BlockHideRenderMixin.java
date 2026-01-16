package neutka.marallys.marallyzen.mixin.client;

import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import neutka.marallys.marallyzen.blocks.DictaphoneSimpleBlock;
import neutka.marallys.marallyzen.blocks.PosterBlock;
import neutka.marallys.marallyzen.client.ClientDictaphoneManager;
import neutka.marallys.marallyzen.client.ClientPosterManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SectionCompiler.class)
public class BlockHideRenderMixin {
    @Redirect(
        method = "compile(Lnet/minecraft/core/SectionPos;Lnet/minecraft/client/renderer/chunk/RenderChunkRegion;Lcom/mojang/blaze3d/vertex/VertexSorting;Lnet/minecraft/client/renderer/SectionBufferBuilderPack;Ljava/util/List;)Lnet/minecraft/client/renderer/chunk/SectionCompiler$Results;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/chunk/RenderChunkRegion;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"
        ),
        require = 0
    )
    private BlockState marallyzen$hideBlocks(RenderChunkRegion region, BlockPos pos) {
        BlockState state = region.getBlockState(pos);
        if (state.getBlock() instanceof DictaphoneSimpleBlock
            && ClientDictaphoneManager.hasClientDictaphone(pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        if (state.getBlock() instanceof PosterBlock
            && ClientPosterManager.isPosterHidden(pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return state;
    }
}
