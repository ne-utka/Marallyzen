package neutka.marallys.marallyzen.blocks;

import net.minecraft.core.BlockPos;
import com.mojang.serialization.Codec;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

public class PosterBlockEntity extends BlockEntity {
    private String posterText = "";
    private String posterTitle = "";
    private String posterAuthor = "";
    private String posterBackText = "";
    private long posterCreatedAt = 0;
    private boolean protectedByOp = false;
    private String oldposterVariant = "default"; // For oldposter (ID 11): "default", "alive", "band", "dead"
    private String targetPlayerName = ""; // Player name for head display on oldposter variants (single name for alive/dead)
    private java.util.List<String> targetPlayerNames = new java.util.ArrayList<>(); // List of player names for band variant (up to 3)

    public PosterBlockEntity(BlockPos pos, BlockState blockState) {
        super(MarallyzenBlockEntities.POSTER_BE.get(), pos, blockState);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (posterText != null && !posterText.isEmpty()) {
            output.putString("PosterText", posterText);
        }
        if (posterTitle != null && !posterTitle.isEmpty()) {
            output.putString("PosterTitle", posterTitle);
        }
        if (posterAuthor != null && !posterAuthor.isEmpty()) {
            output.putString("PosterAuthor", posterAuthor);
        }
        if (posterBackText != null && !posterBackText.isEmpty()) {
            output.putString("PosterBackText", posterBackText);
        }
        if (posterCreatedAt > 0) {
            output.putLong("PosterCreatedAt", posterCreatedAt);
        }
        output.putBoolean("ProtectedByOp", protectedByOp);
        // Always save variant, even if "default", to ensure it persists
        String variantToSave = oldposterVariant != null ? oldposterVariant : "default";
        output.putString("OldposterVariant", variantToSave);
        if (targetPlayerName != null && !targetPlayerName.isEmpty()) {
            output.putString("TargetPlayerName", targetPlayerName);
        }
        // Save list of names for band variant
        if (targetPlayerNames != null && !targetPlayerNames.isEmpty()) {
            var namesList = output.list("TargetPlayerNames", Codec.STRING);
            for (String name : targetPlayerNames) {
                if (name != null && !name.isEmpty()) {
                    namesList.add(name);
                }
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        posterText = input.getString("PosterText").orElse("");
        posterTitle = input.getString("PosterTitle").orElse("");
        posterAuthor = input.getString("PosterAuthor").orElse("");
        posterBackText = input.getString("PosterBackText").orElse("");
        posterCreatedAt = input.getLong("PosterCreatedAt").orElse(0L);
        protectedByOp = input.getBooleanOr("ProtectedByOp", false);
        oldposterVariant = input.getString("OldposterVariant").orElse("default");
        targetPlayerName = input.getString("TargetPlayerName").orElse("");
        // Load list of names for band variant
        targetPlayerNames.clear();
        var namesList = input.listOrEmpty("TargetPlayerNames", Codec.STRING);
        namesList.stream()
            .filter(name -> name != null && !name.isEmpty())
            .forEach(targetPlayerNames::add);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    public String getPosterText() {
        return posterText != null ? posterText : "";
    }

    public void setPosterText(String posterText) {
        this.posterText = posterText;
        setChanged();
    }

    public String getPosterTitle() {
        return posterTitle != null ? posterTitle : "";
    }

    public void setPosterTitle(String posterTitle) {
        this.posterTitle = posterTitle;
        setChanged();
    }

    public String getPosterAuthor() {
        return posterAuthor != null ? posterAuthor : "";
    }

    public void setPosterAuthor(String posterAuthor) {
        this.posterAuthor = posterAuthor;
        setChanged();
    }

    public String getPosterBackText() {
        return posterBackText != null ? posterBackText : "";
    }

    public void setPosterBackText(String posterBackText) {
        this.posterBackText = posterBackText != null ? posterBackText : "";
        setChanged();
    }

    public boolean hasBoundText() {
        return (posterText != null && !posterText.isEmpty())
            || (posterTitle != null && !posterTitle.isEmpty())
            || (posterAuthor != null && !posterAuthor.isEmpty())
            || (posterBackText != null && !posterBackText.isEmpty());
    }

    public boolean isProtectedByOp() {
        return protectedByOp;
    }

    public void setProtectedByOp(boolean protectedByOp) {
        this.protectedByOp = protectedByOp;
        setChanged();
    }

    public long getPosterCreatedAt() {
        return posterCreatedAt;
    }

    public void setPosterCreatedAt(long posterCreatedAt) {
        this.posterCreatedAt = posterCreatedAt;
        setChanged();
    }

    public String getOldposterVariant() {
        return oldposterVariant != null ? oldposterVariant : "default";
    }

    public void setOldposterVariant(String variant) {
        this.oldposterVariant = variant != null ? variant : "default";
        setChanged();
    }
    
    public String getTargetPlayerName() {
        return targetPlayerName != null ? targetPlayerName : "";
    }
    
    public void setTargetPlayerName(String targetPlayerName) {
        this.targetPlayerName = targetPlayerName != null ? targetPlayerName : "";
        setChanged();
    }
    
    /**
     * Gets the list of player names for band variant.
     * Returns up to 3 names.
     */
    public java.util.List<String> getTargetPlayerNames() {
        if (targetPlayerNames == null) {
            targetPlayerNames = new java.util.ArrayList<>();
        }
        return targetPlayerNames;
    }
    
    /**
     * Sets the list of player names for band variant.
     * Only first 3 names are stored.
     */
    public void setTargetPlayerNames(java.util.List<String> names) {
        if (targetPlayerNames == null) {
            targetPlayerNames = new java.util.ArrayList<>();
        } else {
            targetPlayerNames.clear();
        }
        if (names != null) {
            // Only store up to 3 names
            for (int i = 0; i < Math.min(3, names.size()); i++) {
                String name = names.get(i);
                if (name != null && !name.isEmpty()) {
                    targetPlayerNames.add(name);
                }
            }
        }
        setChanged();
    }
}
