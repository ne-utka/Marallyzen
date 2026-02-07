package neutka.marallys.marallyzen.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import neutka.marallys.marallyzen.util.PermissionHelper;

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

            var level = player.level();
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
            boolean isWritten = stack.getItem() instanceof net.minecraft.world.item.WrittenBookItem;
            boolean isWritable = stack.getItem() instanceof net.minecraft.world.item.WritableBookItem;
            if (!isWritten && !isWritable) {
                return;
            }

            var writtenContent = isWritten
                ? stack.get(net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT)
                : null;
            var writableContent = isWritable
                ? stack.get(net.minecraft.core.component.DataComponents.WRITABLE_BOOK_CONTENT)
                : null;
            if (writtenContent == null && writableContent == null) {
                return;
            }

            String title = "";
            if (writtenContent != null && writtenContent.title() != null && writtenContent.title().raw() != null) {
                title = writtenContent.title().raw();
            }

            net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(pos);
            if (be == null) {
                be = posterBlock.newBlockEntity(pos, level.getBlockState(pos));
                if (be != null) {
                    level.setBlockEntity(be);
                }
            }

            if (be instanceof neutka.marallys.marallyzen.blocks.PosterBlockEntity posterBe) {
                if (posterBe.isProtectedByOp() && !PermissionHelper.isOp(player)) {
                    return;
                }
                java.util.List<String> pageTexts = new java.util.ArrayList<>();
                var bookPages = writtenContent != null ? writtenContent.pages() : writableContent.pages();
                if (bookPages != null && !bookPages.isEmpty()) {
                    for (var page : bookPages) {
                        if (page == null) {
                            continue;
                        }
                        Object raw = page.raw();
                        if (raw == null) {
                            continue;
                        }
                        String pageText = raw instanceof net.minecraft.network.chat.Component component
                            ? component.getString()
                            : raw.toString();
                        if (pageText != null && !pageText.isEmpty()) {
                            pageTexts.add(pageText);
                        }
                    }
                }
                String frontText = pageTexts.isEmpty() ? "" : pageTexts.get(0).trim();
                String backText = pageTexts.size() > 1 ? pageTexts.get(1).trim() : "";

                if (PermissionHelper.isOp(player)) {
                    posterBe.setProtectedByOp(true);
                }

                if (!frontText.isEmpty()) {
                    posterBe.setPosterText(frontText);
                    posterBe.setPosterTitle(title);
                    posterBe.setPosterCreatedAt(level.getGameTime());
                }
                if (!backText.isEmpty()) {
                    posterBe.setPosterBackText(backText);
                } else {
                    posterBe.setPosterBackText("");
                }

                if (posterBlock.getPosterNumber() == 11 && !title.isBlank()) {
                    String bookTitle = title.toLowerCase().trim();
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
                        var pages = writtenContent != null ? writtenContent.pages() : writableContent.pages();
                        if (pages != null && !pages.isEmpty()) {
                            var firstPage = pages.get(0);
                            if (firstPage != null && firstPage.raw() != null) {
                                Object rawPage = firstPage.raw();
                                String firstPageText = rawPage instanceof net.minecraft.network.chat.Component component
                                    ? component.getString()
                                    : rawPage.toString();
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
                level.getChunkAt(pos).markUnsaved();
            }
        });
    }
}

