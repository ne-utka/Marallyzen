package neutka.marallys.marallyzen.instance;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import neutka.marallys.marallyzen.Marallyzen;

public record PlayerSnapshot(
        ResourceKey<Level> dimension,
        Vec3 position,
        float yaw,
        float pitch,
        GameType gameMode,
        CompoundTag inventoryTag,
        boolean hasAbilities,
        boolean invulnerable,
        boolean flying,
        boolean mayfly,
        boolean instabuild,
        boolean mayBuild,
        float flyingSpeed,
        float walkingSpeed
) {
    private static final String PERSIST_TAG = "marallyzen_instance_snapshot";

    public static PlayerSnapshot capture(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        CompoundTag inventoryTag = new CompoundTag();
        ListTag items = new ListTag();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            CompoundTag itemTag = new CompoundTag();
            itemTag.putInt("Slot", i);
            stack.save(player.registryAccess(), itemTag);
            items.add(itemTag);
        }
        inventoryTag.put("Items", items);
        var abilities = player.getAbilities();
        Marallyzen.LOGGER.debug(
                "InstanceSnapshot: capture dim={} pos={} mode={} abilities=[invuln={},flying={},mayfly={},instabuild={},mayBuild={},flySpeed={},walkSpeed={}]",
                player.level().dimension().location(),
                player.position(),
                player.gameMode.getGameModeForPlayer(),
                abilities.invulnerable,
                abilities.flying,
                abilities.mayfly,
                abilities.instabuild,
                abilities.mayBuild,
                abilities.getFlyingSpeed(),
                abilities.getWalkingSpeed()
        );
        return new PlayerSnapshot(
                player.level().dimension(),
                player.position(),
                player.getYRot(),
                player.getXRot(),
                player.gameMode.getGameModeForPlayer(),
                inventoryTag,
                true,
                abilities.invulnerable,
                abilities.flying,
                abilities.mayfly,
                abilities.instabuild,
                abilities.mayBuild,
                abilities.getFlyingSpeed(),
                abilities.getWalkingSpeed()
        );
    }

    public void saveToPlayer(ServerPlayer player) {
        if (player == null) {
            return;
        }
        CompoundTag tag = new CompoundTag();
        if (dimension != null) {
            tag.putString("dim", dimension.location().toString());
        }
        if (position != null) {
            tag.putDouble("x", position.x);
            tag.putDouble("y", position.y);
            tag.putDouble("z", position.z);
        }
        tag.putFloat("yaw", yaw);
        tag.putFloat("pitch", pitch);
        if (gameMode != null) {
            tag.putString("mode", gameMode.getName());
        }
        if (inventoryTag != null) {
            tag.put("inv", inventoryTag.copy());
        }
        tag.putBoolean("restored", false);
        tag.putBoolean("hasAbilities", hasAbilities);
        if (hasAbilities) {
            tag.putBoolean("invulnerable", invulnerable);
            tag.putBoolean("flying", flying);
            tag.putBoolean("mayfly", mayfly);
            tag.putBoolean("instabuild", instabuild);
            tag.putBoolean("mayBuild", mayBuild);
            tag.putFloat("flySpeed", flyingSpeed);
            tag.putFloat("walkSpeed", walkingSpeed);
        }
        player.getPersistentData().put(PERSIST_TAG, tag);
    }

    public static PlayerSnapshot loadFromPlayer(ServerPlayer player) {
        if (player == null) {
            return null;
        }
        CompoundTag tag = player.getPersistentData().getCompound(PERSIST_TAG);
        if (tag == null || tag.isEmpty()) {
            return null;
        }
        if (tag.contains("restored") && tag.getBoolean("restored")) {
            return null;
        }
        ResourceKey<Level> dimKey = null;
        if (tag.contains("dim")) {
            ResourceLocation loc = ResourceLocation.tryParse(tag.getString("dim"));
            if (loc != null) {
                dimKey = ResourceKey.create(Registries.DIMENSION, loc);
            }
        }
        Vec3 pos = null;
        if (tag.contains("x") && tag.contains("y") && tag.contains("z")) {
            pos = new Vec3(tag.getDouble("x"), tag.getDouble("y"), tag.getDouble("z"));
        }
        float yaw = tag.contains("yaw") ? tag.getFloat("yaw") : 0.0f;
        float pitch = tag.contains("pitch") ? tag.getFloat("pitch") : 0.0f;
        GameType mode = null;
        if (tag.contains("mode")) {
            mode = GameType.byName(tag.getString("mode"), GameType.SURVIVAL);
        }
        CompoundTag inv = tag.contains("inv", Tag.TAG_COMPOUND) ? tag.getCompound("inv") : null;
        boolean hasAbilities = tag.contains("hasAbilities") && tag.getBoolean("hasAbilities");
        boolean invulnerable = tag.getBoolean("invulnerable");
        boolean flying = tag.getBoolean("flying");
        boolean mayfly = tag.getBoolean("mayfly");
        boolean instabuild = tag.getBoolean("instabuild");
        boolean mayBuild = tag.getBoolean("mayBuild");
        float flySpeed = tag.contains("flySpeed") ? tag.getFloat("flySpeed") : 0.05f;
        float walkSpeed = tag.contains("walkSpeed") ? tag.getFloat("walkSpeed") : 0.1f;
        return new PlayerSnapshot(
                dimKey,
                pos,
                yaw,
                pitch,
                mode,
                inv,
                hasAbilities,
                invulnerable,
                flying,
                mayfly,
                instabuild,
                mayBuild,
                flySpeed,
                walkSpeed
        );
    }

    public static void clearFromPlayer(ServerPlayer player) {
        if (player == null) {
            return;
        }
        player.getPersistentData().remove(PERSIST_TAG);
    }

    public boolean restore(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        if (player.getServer() == null || dimension == null || position == null) {
            Marallyzen.LOGGER.warn(
                    "InstanceSnapshot: restore skipped (missing server/dimension/position) dim={} pos={}",
                    dimension != null ? dimension.location() : null,
                    position
            );
            return false;
        }
        var target = player.getServer().getLevel(dimension);
        if (target == null) {
            Marallyzen.LOGGER.warn(
                    "InstanceSnapshot: restore skipped (target dimension missing) dim={}",
                    dimension.location()
            );
            return false;
        }
        CompoundTag tag = player.getPersistentData().getCompound(PERSIST_TAG);
        if (tag != null && !tag.isEmpty()) {
            if (tag.contains("restored") && tag.getBoolean("restored")) {
                Marallyzen.LOGGER.debug("InstanceSnapshot: restore skipped (already restored)");
                return false;
            }
            tag.putBoolean("restored", true);
            player.getPersistentData().put(PERSIST_TAG, tag);
        }
        Marallyzen.LOGGER.debug(
                "InstanceSnapshot: restore start dim={} pos={} mode={} abilities=[invuln={},flying={},mayfly={},instabuild={},mayBuild={},flySpeed={},walkSpeed={}] restricted={}",
                dimension.location(),
                position,
                gameMode,
                invulnerable,
                flying,
                mayfly,
                instabuild,
                mayBuild,
                flyingSpeed,
                walkingSpeed,
                InstanceSessionManager.getInstance().isPlayerRestricted(player)
        );
        Inventory inventory = player.getInventory();
        inventory.clearContent();
        if (inventoryTag != null) {
            ListTag items = inventoryTag.getList("Items", Tag.TAG_COMPOUND);
            for (int i = 0; i < items.size(); i++) {
                CompoundTag itemTag = items.getCompound(i);
                int slot = itemTag.getInt("Slot");
                ItemStack stack = ItemStack.parse(player.registryAccess(), itemTag).orElse(ItemStack.EMPTY);
                if (slot >= 0 && slot < inventory.getContainerSize()) {
                    inventory.setItem(slot, stack);
                }
            }
        }
        if (gameMode != null) {
            player.setGameMode(gameMode);
        }
        if (hasAbilities) {
            var abilities = player.getAbilities();
            abilities.invulnerable = invulnerable;
            abilities.flying = flying;
            abilities.mayfly = mayfly;
            abilities.instabuild = instabuild;
            abilities.mayBuild = mayBuild;
            abilities.setFlyingSpeed(flyingSpeed);
            abilities.setWalkingSpeed(walkingSpeed);
            player.onUpdateAbilities();
        }
        player.teleportTo(target, position.x, position.y, position.z, yaw, pitch);
        Marallyzen.LOGGER.debug(
                "InstanceSnapshot: restore done dim={} pos={} mode={} restricted={}",
                player.level().dimension().location(),
                player.position(),
                player.gameMode.getGameModeForPlayer(),
                InstanceSessionManager.getInstance().isPlayerRestricted(player)
        );
        return true;
    }
}
