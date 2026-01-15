package neutka.marallys.marallyzen.replay.playback;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class GhostEntity {
    private final Entity entity;
    private final ReplayEntityTrack track;

    public GhostEntity(Entity entity, ReplayEntityTrack track) {
        this.entity = entity;
        this.track = track;
    }

    public Entity getEntity() {
        return entity;
    }

    public ReplayEntityTrack getTrack() {
        return track;
    }

    public void apply(float time) {
        if (entity == null || track == null) {
            return;
        }
        ReplayEntityState prev = track.sample(time - 1.0f);
        ReplayEntityState cur = track.sample(time);
        if (cur == null || cur.position() == null) {
            return;
        }
        if (prev != null && prev.position() != null) {
            entity.xo = prev.position().x;
            entity.yo = prev.position().y;
            entity.zo = prev.position().z;
        } else {
            entity.xo = cur.position().x;
            entity.yo = cur.position().y;
            entity.zo = cur.position().z;
        }
        entity.setPos(cur.position().x, cur.position().y, cur.position().z);
        entity.setYRot(cur.yaw());
        entity.setXRot(cur.pitch());
        if (entity instanceof LivingEntity living) {
            living.yHeadRot = cur.headYaw();
            living.yBodyRot = cur.bodyYaw();
        }
        entity.setDeltaMovement(Vec3.ZERO);
    }
}
