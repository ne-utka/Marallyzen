package neutka.marallys.marallyzen.replay;

import java.util.List;
import net.minecraft.nbt.CompoundTag;

public record ReplayChunkSnapshot(long tick, int chunkX, int chunkZ,
                                  List<ReplayChunkSectionData> sections,
                                  List<CompoundTag> blockEntities) {
}
