package neutka.marallys.marallyzen.trigger.command;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SelectionManager {
    private static final String WAND_KEY = "marallyzen_trigger_wand";
    private static final int MAX_SIZE = 20;

    private final Map<UUID, Selection> selections = new ConcurrentHashMap<>();

    public ItemStack createWand() {
        ItemStack stack = new ItemStack(Items.BLAZE_ROD);
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = data.copyTag();
        tag.putBoolean(WAND_KEY, true);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Trigger Wand"));
        return stack;
    }

    public boolean isWand(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem() != Items.BLAZE_ROD) {
            return false;
        }
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return false;
        }
        return data.copyTag().getBoolean(WAND_KEY).orElse(false);
    }

    public SelectionResult setPos1(ServerPlayer player, String dimensionId, BlockPos pos) {
        if (player == null || pos == null) {
            return SelectionResult.invalid("Invalid selection input.");
        }
        Selection selection = selections.computeIfAbsent(player.getUUID(), id -> new Selection());
        selection.setPos1(dimensionId, pos);
        return validateSelection(selection);
    }

    public SelectionResult setPos2(ServerPlayer player, String dimensionId, BlockPos pos) {
        if (player == null || pos == null) {
            return SelectionResult.invalid("Invalid selection input.");
        }
        Selection selection = selections.computeIfAbsent(player.getUUID(), id -> new Selection());
        selection.setPos2(dimensionId, pos);
        return validateSelection(selection);
    }

    public Selection getSelection(UUID playerId) {
        return selections.get(playerId);
    }

    public Selection consumeSelection(UUID playerId) {
        Selection selection = selections.remove(playerId);
        if (selection == null || !selection.isComplete()) {
            return null;
        }
        return selection;
    }

    public void clearSelection(UUID playerId) {
        selections.remove(playerId);
    }

    private SelectionResult validateSelection(Selection selection) {
        if (selection == null || !selection.isComplete()) {
            return SelectionResult.ok("Selection updated.");
        }
        int sizeX = selection.sizeX();
        int sizeY = selection.sizeY();
        int sizeZ = selection.sizeZ();
        if (sizeX > MAX_SIZE || sizeY > MAX_SIZE || sizeZ > MAX_SIZE) {
            return SelectionResult.invalid("Selection too large: " + sizeX + "x" + sizeY + "x" + sizeZ + " (max 20x20x20).");
        }
        return SelectionResult.ok("Selection: " + sizeX + "x" + sizeY + "x" + sizeZ + ".");
    }

    public static final class Selection {
        private String dimensionId;
        private BlockPos pos1;
        private BlockPos pos2;

        private void setPos1(String dimensionId, BlockPos pos) {
            if (this.dimensionId != null && dimensionId != null && !this.dimensionId.equals(dimensionId)) {
                this.pos2 = null;
            }
            this.dimensionId = dimensionId;
            this.pos1 = pos.immutable();
        }

        private void setPos2(String dimensionId, BlockPos pos) {
            if (this.dimensionId != null && dimensionId != null && !this.dimensionId.equals(dimensionId)) {
                this.pos1 = null;
            }
            this.dimensionId = dimensionId;
            this.pos2 = pos.immutable();
        }

        public String dimensionId() {
            return dimensionId;
        }

        public BlockPos pos1() {
            return pos1;
        }

        public BlockPos pos2() {
            return pos2;
        }

        public boolean isComplete() {
            return pos1 != null && pos2 != null && dimensionId != null && !dimensionId.isBlank();
        }

        public BlockPos min() {
            if (!isComplete()) {
                return BlockPos.ZERO;
            }
            return new BlockPos(
                    Math.min(pos1.getX(), pos2.getX()),
                    Math.min(pos1.getY(), pos2.getY()),
                    Math.min(pos1.getZ(), pos2.getZ())
            );
        }

        public BlockPos max() {
            if (!isComplete()) {
                return BlockPos.ZERO;
            }
            return new BlockPos(
                    Math.max(pos1.getX(), pos2.getX()),
                    Math.max(pos1.getY(), pos2.getY()),
                    Math.max(pos1.getZ(), pos2.getZ())
            );
        }

        public int sizeX() {
            if (!isComplete()) {
                return 0;
            }
            return Math.abs(pos1.getX() - pos2.getX()) + 1;
        }

        public int sizeY() {
            if (!isComplete()) {
                return 0;
            }
            return Math.abs(pos1.getY() - pos2.getY()) + 1;
        }

        public int sizeZ() {
            if (!isComplete()) {
                return 0;
            }
            return Math.abs(pos1.getZ() - pos2.getZ()) + 1;
        }
    }

    public record SelectionResult(boolean valid, String message) {
        public static SelectionResult ok(String message) {
            return new SelectionResult(true, message);
        }

        public static SelectionResult invalid(String message) {
            return new SelectionResult(false, message);
        }
    }
}
