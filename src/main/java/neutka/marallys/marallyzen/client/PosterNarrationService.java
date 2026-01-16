package neutka.marallys.marallyzen.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import neutka.marallys.marallyzen.blocks.InteractiveBlockNarrations;
import neutka.marallys.marallyzen.blocks.PosterBlock;
import neutka.marallys.marallyzen.client.narration.NarrationManager;
import neutka.marallys.marallyzen.entity.PosterEntity;

public final class PosterNarrationService {
    private static boolean wasShowing = false;

    private PosterNarrationService() {
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            clearIfNeeded();
            return;
        }

        Component message = null;
        HitResult hit = mc.hitResult;
        if (hit instanceof BlockHitResult blockHit) {
            message = resolveBlockMessage(mc, blockHit);
        } else if (hit instanceof EntityHitResult entityHit) {
            message = resolveEntityMessage(mc, entityHit);
        }
        if (message == null) {
            PosterEntity posterEntity = findPosterEntityInFront(mc);
            if (posterEntity != null) {
                message = resolvePosterEntityMessage(mc, posterEntity);
            }
        }

        if (message != null) {
            NarrationManager.getInstance().updateProximity(message, null, 1.0f);
            wasShowing = true;
        } else {
            clearIfNeeded();
        }
    }

    private static Component resolveBlockMessage(Minecraft mc, BlockHitResult hit) {
        BlockPos pos = hit.getBlockPos();
        BlockState state = mc.level.getBlockState(pos);
        if (!(state.getBlock() instanceof PosterBlock)) {
            return null;
        }
        if (ClientPosterManager.isPosterHidden(pos)) {
            return null;
        }
        if (ClientPosterManager.hasClientPoster(pos)) {
            return null;
        }
        return InteractiveBlockNarrations.posterBlockMessage();
    }

    private static Component resolveEntityMessage(Minecraft mc, EntityHitResult hit) {
        Entity entity = hit.getEntity();
        if (!(entity instanceof PosterEntity posterEntity)) {
            return null;
        }
        return resolvePosterEntityMessage(mc, posterEntity);
    }

    private static Component resolvePosterEntityMessage(Minecraft mc, PosterEntity posterEntity) {
        if (posterEntity.getCurrentState() != PosterEntity.State.VIEWING) {
            return null;
        }
        if (posterEntity.getPosterNumber() == 11) {
            return null;
        }
        boolean viewingFront = isViewingFrontSide(mc.player, posterEntity);
        return viewingFront
            ? InteractiveBlockNarrations.posterFlipToBackMessage()
            : InteractiveBlockNarrations.posterFlipToFrontMessage();
    }

    private static PosterEntity findPosterEntityInFront(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            return null;
        }
        Vec3 eyePos = mc.player.getEyePosition();
        Vec3 lookVec = mc.player.getLookAngle();
        double reachDistance = 5.0;
        Vec3 endPos = eyePos.add(lookVec.scale(reachDistance));
        PosterEntity closestPoster = null;
        double closestDistance = Double.MAX_VALUE;
        for (PosterEntity posterEntity : mc.level.getEntitiesOfClass(
                PosterEntity.class,
                new AABB(
                        eyePos.x - reachDistance,
                        eyePos.y - reachDistance,
                        eyePos.z - reachDistance,
                        eyePos.x + reachDistance,
                        eyePos.y + reachDistance,
                        eyePos.z + reachDistance
                )
        )) {
            if (posterEntity.getCurrentState() != PosterEntity.State.VIEWING) {
                continue;
            }
            Vec3 posterPos = posterEntity.position();
            Vec3 toPoster = posterPos.subtract(eyePos);
            double distance = toPoster.length();
            if (distance > reachDistance) {
                continue;
            }
            Vec3 hitVec = getPosterHitBox(posterEntity).clip(eyePos, endPos).orElse(null);
            if (hitVec == null) {
                continue;
            }
            double hitDistance = hitVec.distanceTo(eyePos);
            if (hitDistance < closestDistance) {
                closestDistance = hitDistance;
                closestPoster = posterEntity;
            }
        }
        return closestPoster;
    }

    private static AABB getPosterHitBox(PosterEntity posterEntity) {
        Vec3 pos = posterEntity.position();
        return new AABB(
            pos.x - 0.5,
            pos.y - 0.5,
            pos.z - 0.5,
            pos.x + 0.5,
            pos.y + 0.5,
            pos.z + 0.5
        );
    }

    private static boolean isViewingFrontSide(LocalPlayer player, PosterEntity posterEntity) {
        Direction facing = posterEntity.getFacing();
        if (facing == null) {
            return !posterEntity.isFlipped();
        }
        Vec3 posterPos = posterEntity.position();
        Vec3 toPlayer = player.getEyePosition().subtract(posterPos);
        Vec3 frontNormal = Vec3.atLowerCornerOf(facing.getNormal()).normalize();
        boolean playerInFront = frontNormal.dot(toPlayer) >= 0.0;
        return playerInFront ^ posterEntity.isFlipped();
    }

    private static void clearIfNeeded() {
        if (!wasShowing) {
            return;
        }
        NarrationManager.getInstance().startProximityFadeOut();
        wasShowing = false;
    }
}
