package neutka.marallys.marallyzen.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PosterBookBindPacket(BlockPos blockPos, boolean mainHand) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PosterBookBindPacket> TYPE =
        new CustomPacketPayload.Type<>(MarallyzenNetwork.id("poster_book_bind"));

    private static final StreamCodec<RegistryFriendlyByteBuf, BlockPos> BLOCK_POS_CODEC = StreamCodec.of(
        (buf, pos) -> buf.writeLong(pos.asLong()),
        buf -> BlockPos.of(buf.readLong())
    );

    private static final StreamCodec<RegistryFriendlyByteBuf, Boolean> BOOL_CODEC = StreamCodec.of(
        (buf, value) -> buf.writeBoolean(value),
        RegistryFriendlyByteBuf::readBoolean
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, PosterBookBindPacket> STREAM_CODEC =
        StreamCodec.composite(
            BLOCK_POS_CODEC,
            PosterBookBindPacket::blockPos,
            BOOL_CODEC,
            PosterBookBindPacket::mainHand,
            PosterBookBindPacket::new
        );

    @Override
    public CustomPacketPayload.Type<PosterBookBindPacket> type() {
        return TYPE;
    }

    public static void handle(PosterBookBindPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof net.minecraft.server.level.ServerPlayer player)) {
                return;
            }

            var level = player.serverLevel();
            BlockPos pos = packet.blockPos();
            if (!level.isLoaded(pos)) {
                return;
            }

            if (!(level.getBlockState(pos).getBlock() instanceof neutka.marallys.marallyzen.blocks.PosterBlock posterBlock)) {
                return;
            }

            if (player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos)) > 36.0) {
                return;
            }

            net.minecraft.world.InteractionHand hand = packet.mainHand()
                ? net.minecraft.world.InteractionHand.MAIN_HAND
                : net.minecraft.world.InteractionHand.OFF_HAND;
            var stack = player.getItemInHand(hand);
            if (!(stack.getItem() instanceof net.minecraft.world.item.WrittenBookItem)) {
                return;
            }

            var bookContent = stack.get(net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT);
            if (bookContent == null || bookContent.title() == null) {
                return;
            }

            var titleFilterable = bookContent.title();
            if (titleFilterable == null || titleFilterable.raw() == null) {
                return;
            }

            net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(pos);
            if (be == null) {
                be = posterBlock.newBlockEntity(pos, level.getBlockState(pos));
                if (be != null) {
                    level.setBlockEntity(be);
                }
            }

            if (be instanceof neutka.marallys.marallyzen.blocks.PosterBlockEntity posterBe) {
                if (player.hasPermissions(2)) {
                    posterBe.setProtectedByOp(true);
                }

                if (posterBlock.getPosterNumber() == 11) {
                    String bookTitle = titleFilterable.raw().toLowerCase().trim();
                    String variant = null;
                    if (bookTitle.equals("dead")) {
                        variant = "dead";
                    } else if (bookTitle.equals("alive")) {
                        variant = "alive";
                    } else if (bookTitle.equals("band")) {
                        variant = "band";
                    }

                    if (variant != null) {
                        java.util.List<String> playerNames = new java.util.ArrayList<>();
                        var pages = bookContent.pages();
                        if (pages != null && !pages.isEmpty()) {
                            var firstPage = pages.get(0);
                            if (firstPage != null && firstPage.raw() != null) {
                                String firstPageText = firstPage.raw().getString();
                                if (firstPageText != null && !firstPageText.isEmpty()) {
                                    String[] lines = firstPageText.split("\\n");
                                    if (variant.equals("band")) {
                                        for (int i = 0; i < Math.min(3, lines.length); i++) {
                                            String name = lines[i].trim();
                                            if (!name.isEmpty()) {
                                                playerNames.add(name);
                                            }
                                        }
                                    } else if (lines.length > 0) {
                                        String name = lines[0].trim();
                                        if (!name.isEmpty()) {
                                            playerNames.add(name);
                                        }
                                    }
                                }
                            }
                        }

                        posterBe.setOldposterVariant(variant);
                        if (!playerNames.isEmpty()) {
                            if (variant.equals("band")) {
                                posterBe.setTargetPlayerNames(playerNames);
                            } else {
                                posterBe.setTargetPlayerName(playerNames.get(0));
                            }
                        }
                    }
                }

                posterBe.setChanged();
                level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
                level.getChunkAt(pos).setUnsaved(true);
            }
        });
    }
}
