package neutka.marallys.marallyzen.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class PosterBlockEntity extends BlockEntity {
    private String posterText = "";
    private String posterTitle = "";
    private String posterAuthor = "";
    private long posterCreatedAt = 0;
    private String oldposterVariant = "default"; // For oldposter (ID 11): "default", "alive", "band", "dead"
    private String targetPlayerName = ""; // Player name for head display on oldposter variants (single name for alive/dead)
    private java.util.List<String> targetPlayerNames = new java.util.ArrayList<>(); // List of player names for band variant (up to 3)

    public PosterBlockEntity(BlockPos pos, BlockState blockState) {
        super(MarallyzenBlockEntities.POSTER_BE.get(), pos, blockState);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (posterText != null && !posterText.isEmpty()) {
            tag.putString("PosterText", posterText);
        }
        if (posterTitle != null && !posterTitle.isEmpty()) {
            tag.putString("PosterTitle", posterTitle);
        }
        if (posterAuthor != null && !posterAuthor.isEmpty()) {
            tag.putString("PosterAuthor", posterAuthor);
        }
        if (posterCreatedAt > 0) {
            tag.putLong("PosterCreatedAt", posterCreatedAt);
        }
        // Always save variant, even if "default", to ensure it persists
        String variantToSave = oldposterVariant != null ? oldposterVariant : "default";
        tag.putString("OldposterVariant", variantToSave);
        if (targetPlayerName != null && !targetPlayerName.isEmpty()) {
            tag.putString("TargetPlayerName", targetPlayerName);
        }
        // Save list of names for band variant
        if (targetPlayerNames != null && !targetPlayerNames.isEmpty()) {
            net.minecraft.nbt.ListTag namesList = new net.minecraft.nbt.ListTag();
            for (String name : targetPlayerNames) {
                if (name != null && !name.isEmpty()) {
                    namesList.add(net.minecraft.nbt.StringTag.valueOf(name));
                }
            }
            if (!namesList.isEmpty()) {
                tag.put("TargetPlayerNames", namesList);
            }
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("PosterText")) {
            posterText = tag.getString("PosterText");
        }
        if (tag.contains("PosterTitle")) {
            posterTitle = tag.getString("PosterTitle");
        }
        if (tag.contains("PosterAuthor")) {
            posterAuthor = tag.getString("PosterAuthor");
        }
        if (tag.contains("PosterCreatedAt")) {
            posterCreatedAt = tag.getLong("PosterCreatedAt");
        }
        if (tag.contains("OldposterVariant")) {
            oldposterVariant = tag.getString("OldposterVariant");
        } else {
            oldposterVariant = "default";
        }
        if (tag.contains("TargetPlayerName")) {
            targetPlayerName = tag.getString("TargetPlayerName");
        } else {
            targetPlayerName = "";
        }
        // Load list of names for band variant
        targetPlayerNames.clear();
        if (tag.contains("TargetPlayerNames", net.minecraft.nbt.Tag.TAG_LIST)) {
            net.minecraft.nbt.ListTag namesList = tag.getList("TargetPlayerNames", net.minecraft.nbt.Tag.TAG_STRING);
            for (int i = 0; i < namesList.size(); i++) {
                String name = namesList.getString(i);
                if (name != null && !name.isEmpty()) {
                    targetPlayerNames.add(name);
                }
            }
        }
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

