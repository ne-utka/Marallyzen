package neutka.marallys.marallyzen.npc.replay;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Single-tick player movement snapshot for NPC replay.
 */
public record NpcReplayFrame(
        double x,
        double y,
        double z,
        double dx,
        double dy,
        double dz,
        float yaw,
        float pitch,
        float bodyYaw,
        float headYaw,
        boolean onGround,
        boolean sprint,
        boolean crouch,
        String mainHandItemId,
        int mainHandCount,
        String offHandItemId,
        int offHandCount,
        String emoteId,
        boolean usingItem,
        boolean attack,
        float swingProgress,
        boolean jumping,
        float fallDistance,
        boolean fallFlying,
        boolean swimming,
        boolean flying,
        float movementSpeed,
        int repeat
) {
    private static final double POS_EPS = 1.0e-6;
    private static final double VEL_EPS = 1.0e-6;
    private static final float ROT_EPS = 1.0e-4f;
    private static final float SWING_EPS = 1.0e-3f;
    private static final float SPEED_EPS = 1.0e-4f;

    public static NpcReplayFrame fromPlayer(ServerPlayer player, boolean forcedAttack, String emoteId) {
        if (player == null) {
            return empty();
        }
        var velocity = player.getDeltaMovement();
        float body = player.yBodyRot;
        float head = player.yHeadRot;
        float swing = player.getAttackAnim(0.0f);
        boolean jump = !player.onGround() && velocity.y > 0.08D;
        String mainHandItemId = "";
        int mainHandCount = 0;
        if (!player.getMainHandItem().isEmpty()) {
            mainHandItemId = BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).toString();
            mainHandCount = player.getMainHandItem().getCount();
        }
        String offHandItemId = "";
        int offHandCount = 0;
        if (!player.getOffhandItem().isEmpty()) {
            offHandItemId = BuiltInRegistries.ITEM.getKey(player.getOffhandItem().getItem()).toString();
            offHandCount = player.getOffhandItem().getCount();
        }
        boolean attack = forcedAttack || swing > 0.02F;
        float moveSpeed = 0.1F;
        var movementAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementAttr != null) {
            moveSpeed = (float) movementAttr.getBaseValue();
        }
        return new NpcReplayFrame(
                player.getX(),
                player.getY(),
                player.getZ(),
                velocity.x,
                velocity.y,
                velocity.z,
                player.getYRot(),
                player.getXRot(),
                body,
                head,
                player.onGround(),
                player.isSprinting(),
                player.isCrouching(),
                mainHandItemId,
                mainHandCount,
                offHandItemId,
                offHandCount,
                emoteId == null ? "" : emoteId,
                player.isUsingItem(),
                attack,
                swing,
                jump,
                (float) player.fallDistance,
                player.isFallFlying(),
                player.isSwimming(),
                player.getAbilities().flying,
                moveSpeed,
                1
        );
    }

    public static NpcReplayFrame empty() {
        return new NpcReplayFrame(
                0.0D, 0.0D, 0.0D,
                0.0D, 0.0D, 0.0D,
                0.0F, 0.0F, 0.0F, 0.0F,
                true, false, false, "", 0, "", 0, "", false, false, 0.0F, false, 0.0F,
                false, false, false, 0.1F,
                1
        );
    }

    public NpcReplayFrame withRepeat(int repeatTicks) {
        return new NpcReplayFrame(
                x, y, z,
                dx, dy, dz,
                yaw, pitch, bodyYaw, headYaw,
                onGround, sprint, crouch, mainHandItemId, mainHandCount, offHandItemId, offHandCount, emoteId, usingItem, attack, swingProgress, jumping, fallDistance,
                fallFlying, swimming, flying, movementSpeed,
                Math.max(1, repeatTicks)
        );
    }

    public boolean nearlyEquals(NpcReplayFrame other) {
        if (other == null) {
            return false;
        }
        return abs(x - other.x) <= POS_EPS
                && abs(y - other.y) <= POS_EPS
                && abs(z - other.z) <= POS_EPS
                && abs(dx - other.dx) <= VEL_EPS
                && abs(dy - other.dy) <= VEL_EPS
                && abs(dz - other.dz) <= VEL_EPS
                && angleDiff(yaw, other.yaw) <= ROT_EPS
                && abs(pitch - other.pitch) <= ROT_EPS
                && angleDiff(bodyYaw, other.bodyYaw) <= ROT_EPS
                && angleDiff(headYaw, other.headYaw) <= ROT_EPS
                && onGround == other.onGround
                && sprint == other.sprint
                && crouch == other.crouch
                && equals(mainHandItemId, other.mainHandItemId)
                && mainHandCount == other.mainHandCount
                && equals(offHandItemId, other.offHandItemId)
                && offHandCount == other.offHandCount
                && equals(emoteId, other.emoteId)
                && usingItem == other.usingItem
                && attack == other.attack
                && abs(swingProgress - other.swingProgress) <= SWING_EPS
                && jumping == other.jumping
                && abs(fallDistance - other.fallDistance) <= ROT_EPS
                && fallFlying == other.fallFlying
                && swimming == other.swimming
                && flying == other.flying
                && abs(movementSpeed - other.movementSpeed) <= SPEED_EPS;
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("x", x);
        obj.addProperty("y", y);
        obj.addProperty("z", z);
        obj.addProperty("yaw", yaw);
        obj.addProperty("pitch", pitch);
        obj.addProperty("bodyYaw", bodyYaw);
        obj.addProperty("headYaw", headYaw);
        obj.addProperty("dx", dx);
        obj.addProperty("dy", dy);
        obj.addProperty("dz", dz);
        obj.addProperty("sprint", sprint);
        obj.addProperty("crouch", crouch);
        if (mainHandItemId != null && !mainHandItemId.isBlank()) {
            obj.addProperty("mainHandItem", mainHandItemId);
            obj.addProperty("mainHandCount", Math.max(1, mainHandCount));
        }
        if (offHandItemId != null && !offHandItemId.isBlank()) {
            obj.addProperty("offHandItem", offHandItemId);
            obj.addProperty("offHandCount", Math.max(1, offHandCount));
        }
        if (emoteId != null && !emoteId.isBlank()) {
            obj.addProperty("emote", emoteId);
        }
        obj.addProperty("onGround", onGround);
        obj.addProperty("usingItem", usingItem);
        obj.addProperty("attack", attack);
        obj.addProperty("swingProgress", swingProgress);
        obj.addProperty("jumping", jumping);
        obj.addProperty("fallDistance", fallDistance);
        obj.addProperty("fallFlying", fallFlying);
        obj.addProperty("swimming", swimming);
        obj.addProperty("flying", flying);
        obj.addProperty("movementSpeed", movementSpeed);
        if (repeat > 1) {
            obj.addProperty("repeat", repeat);
        }
        return obj;
    }

    public static NpcReplayFrame fromJson(JsonObject obj) {
        if (obj == null) {
            return empty();
        }
        double x = readDouble(obj, "x", 0.0D);
        double y = readDouble(obj, "y", 0.0D);
        double z = readDouble(obj, "z", 0.0D);
        double dx = readDouble(obj, "dx", 0.0D);
        double dy = readDouble(obj, "dy", 0.0D);
        double dz = readDouble(obj, "dz", 0.0D);
        float yaw = readFloat(obj, "yaw", 0.0F);
        float pitch = readFloat(obj, "pitch", 0.0F);
        float bodyYaw = readFloat(obj, "bodyYaw", yaw);
        float headYaw = readFloat(obj, "headYaw", yaw);
        boolean onGround = readBool(obj, "onGround", true);
        boolean sprint = readBool(obj, "sprint", false);
        boolean crouch = readBool(obj, "crouch", false);
        String mainHandItem = readString(obj, "mainHandItem", "");
        int mainHandCount = Math.max(0, readInt(obj, "mainHandCount", mainHandItem.isBlank() ? 0 : 1));
        String offHandItem = readString(obj, "offHandItem", "");
        int offHandCount = Math.max(0, readInt(obj, "offHandCount", offHandItem.isBlank() ? 0 : 1));
        String emoteId = readString(obj, "emote", "");
        boolean usingItem = readBool(obj, "usingItem", false);
        boolean attack = readBool(obj, "attack", false);
        float swing = readFloat(obj, "swingProgress", 0.0F);
        boolean jumping = readBool(obj, "jumping", false);
        float fallDistance = readFloat(obj, "fallDistance", 0.0F);
        boolean fallFlying = readBool(obj, "fallFlying", false);
        boolean swimming = readBool(obj, "swimming", false);
        boolean flying = readBool(obj, "flying", false);
        float movementSpeed = readFloat(obj, "movementSpeed", 0.1F);
        int repeat = Math.max(1, readInt(obj, "repeat", 1));
        return new NpcReplayFrame(
                x, y, z,
                dx, dy, dz,
                yaw, pitch, bodyYaw, headYaw,
                onGround, sprint, crouch, mainHandItem, mainHandCount, offHandItem, offHandCount, emoteId, usingItem, attack, swing, jumping, fallDistance,
                fallFlying, swimming, flying, movementSpeed,
                repeat
        );
    }

    public static NpcReplayFrame interpolate(NpcReplayFrame from, NpcReplayFrame to, double alpha) {
        if (from == null) {
            return to == null ? empty() : to;
        }
        if (to == null) {
            return from;
        }
        double t = Mth.clamp(alpha, 0.0D, 1.0D);
        return new NpcReplayFrame(
                Mth.lerp(t, from.x, to.x),
                Mth.lerp(t, from.y, to.y),
                Mth.lerp(t, from.z, to.z),
                Mth.lerp(t, from.dx, to.dx),
                Mth.lerp(t, from.dy, to.dy),
                Mth.lerp(t, from.dz, to.dz),
                Mth.rotLerp((float) t, from.yaw, to.yaw),
                (float) Mth.lerp(t, from.pitch, to.pitch),
                Mth.rotLerp((float) t, from.bodyYaw, to.bodyYaw),
                Mth.rotLerp((float) t, from.headYaw, to.headYaw),
                t < 0.5D ? from.onGround : to.onGround,
                t < 0.5D ? from.sprint : to.sprint,
                t < 0.5D ? from.crouch : to.crouch,
                t < 0.5D ? from.mainHandItemId : to.mainHandItemId,
                t < 0.5D ? from.mainHandCount : to.mainHandCount,
                t < 0.5D ? from.offHandItemId : to.offHandItemId,
                t < 0.5D ? from.offHandCount : to.offHandCount,
                t < 0.5D ? from.emoteId : to.emoteId,
                t < 0.5D ? from.usingItem : to.usingItem,
                from.attack || to.attack,
                (float) Mth.lerp(t, from.swingProgress, to.swingProgress),
                t < 0.5D ? from.jumping : to.jumping,
                (float) Mth.lerp(t, from.fallDistance, to.fallDistance),
                t < 0.5D ? from.fallFlying : to.fallFlying,
                t < 0.5D ? from.swimming : to.swimming,
                t < 0.5D ? from.flying : to.flying,
                (float) Mth.lerp(t, from.movementSpeed, to.movementSpeed),
                1
        );
    }

    private static float angleDiff(float a, float b) {
        return Math.abs(Mth.wrapDegrees(a - b));
    }

    private static double abs(double value) {
        return value < 0.0D ? -value : value;
    }

    private static float abs(float value) {
        return value < 0.0F ? -value : value;
    }

    private static boolean equals(String a, String b) {
        if (a == null || a.isBlank()) {
            return b == null || b.isBlank();
        }
        return a.equals(b);
    }

    private static double readDouble(JsonObject obj, String key, double fallback) {
        if (obj == null || !obj.has(key)) {
            return fallback;
        }
        try {
            return obj.get(key).getAsDouble();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static float readFloat(JsonObject obj, String key, float fallback) {
        if (obj == null || !obj.has(key)) {
            return fallback;
        }
        try {
            return obj.get(key).getAsFloat();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static int readInt(JsonObject obj, String key, int fallback) {
        if (obj == null || !obj.has(key)) {
            return fallback;
        }
        try {
            return obj.get(key).getAsInt();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static boolean readBool(JsonObject obj, String key, boolean fallback) {
        if (obj == null || !obj.has(key)) {
            return fallback;
        }
        try {
            return obj.get(key).getAsBoolean();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String readString(JsonObject obj, String key, String fallback) {
        if (obj == null || !obj.has(key)) {
            return fallback;
        }
        try {
            return obj.get(key).getAsString();
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
