package neutka.marallys.marallyzen.npc;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

public class NpcEntity extends PathfinderMob {
    public static final EntityDataAccessor<String> NPC_ID =
            SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<String> APPEARANCE_ID =
            SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.STRING);

    public NpcEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setInvulnerable(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(NPC_ID, "");
        builder.define(APPEARANCE_ID, "");
    }

    public String getNpcId() {
        return entityData.get(NPC_ID);
    }

    public void setNpcId(String id) {
        entityData.set(NPC_ID, id != null ? id : "");
    }

    public String getAppearanceId() {
        return entityData.get(APPEARANCE_ID);
    }

    public void setAppearanceId(String id) {
        entityData.set(APPEARANCE_ID, id != null ? id : "");
    }
}
