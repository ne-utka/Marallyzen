package neutka.marallys.marallyzen.npc;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import java.util.*;

/**
 * Data class representing an NPC configuration.
 * Loaded from JSON files in config/marallyzen/npcs/
 */
public class NpcData {
    private final String id;
    private String name;
    private EntityType<?> entityType;
    private BlockPos spawnPos;
    private ResourceLocation geckolibModel; // Optional: path to GeckoLib model
    private ResourceLocation geckolibAnimation; // Optional: path to GeckoLib animation
    private ResourceLocation geckolibTexture; // Optional: path to GeckoLib texture
    private String geckolibExpression; // Optional: default expression animation name
    private String geckolibTalkExpression; // Optional: talk expression animation name
    private String skinTexture; // Optional: player skin texture
    private String skinSignature; // Optional: player skin signature
    private String skinModel; // Optional: skin model type - "default" (Steve) or "slim" (Alex)
    private String dialogScript; // Optional: Denizen script to run on click
    private String cutscene; // Optional: cutscene name to play (can be triggered from scripts)
    private String defaultAnimation; // Optional: default emote animation to play when idle (e.g., "SPE_Idle")
    private List<Waypoint> waypoints; // Optional: movement waypoints
    private boolean waypointsLoop = true; // Whether waypoints should loop
    private Double maxHealth; // Optional: maximum health
    private Double health; // Optional: current health
    private Boolean invulnerable; // Optional: whether NPC is invulnerable (default: true)
    private String proximityText; // Optional: text to display when player approaches
    private Double proximityRange; // Optional: range for proximity detection (default: 8.0)
    private Boolean showNameTag; // Optional: whether to show name tag above NPC (default: true)
    private Boolean showProximityTextInActionBar; // Optional: whether to show proximity text in ActionBar (default: true)
    private Boolean showProximityTextInChat; // Optional: whether to show proximity text in chat (default: true)
    private Boolean valk; // Optional: enable random looped walking around spawn
    private Integer valkRadius; // Optional: walk radius from spawn (blocks)
    private String valkPattern; // Optional: random|circle|square
    private Boolean lookAtPlayers; // Optional: whether NPC tracks nearby players with head/body
    private AiSettings aiSettings; // Optional: AI dialog settings
    private WildfireSettings wildfireSettings; // Optional: Wildfire Gender settings
    private Map<String, String> metadata; // Additional metadata

    public NpcData(String id) {
        this.id = id;
        this.waypoints = new ArrayList<>();
        this.metadata = new HashMap<>();
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public EntityType<?> getEntityType() {
        return entityType;
    }

    public void setEntityType(EntityType<?> entityType) {
        this.entityType = entityType;
    }

    public BlockPos getSpawnPos() {
        return spawnPos;
    }

    public void setSpawnPos(BlockPos spawnPos) {
        this.spawnPos = spawnPos;
    }

    public ResourceLocation getGeckolibModel() {
        return geckolibModel;
    }

    public void setGeckolibModel(ResourceLocation geckolibModel) {
        this.geckolibModel = geckolibModel;
    }

    public ResourceLocation getGeckolibAnimation() {
        return geckolibAnimation;
    }

    public void setGeckolibAnimation(ResourceLocation geckolibAnimation) {
        this.geckolibAnimation = geckolibAnimation;
    }

    public ResourceLocation getGeckolibTexture() {
        return geckolibTexture;
    }

    public void setGeckolibTexture(ResourceLocation geckolibTexture) {
        this.geckolibTexture = geckolibTexture;
    }

    public String getGeckolibExpression() {
        return geckolibExpression;
    }

    public void setGeckolibExpression(String geckolibExpression) {
        this.geckolibExpression = geckolibExpression;
    }

    public String getGeckolibTalkExpression() {
        return geckolibTalkExpression;
    }

    public void setGeckolibTalkExpression(String geckolibTalkExpression) {
        this.geckolibTalkExpression = geckolibTalkExpression;
    }

    public String getSkinTexture() {
        return skinTexture;
    }

    public void setSkinTexture(String skinTexture) {
        this.skinTexture = skinTexture;
    }

    public String getSkinSignature() {
        return skinSignature;
    }

    public void setSkinSignature(String skinSignature) {
        this.skinSignature = skinSignature;
    }

    public String getSkinModel() {
        return skinModel;
    }

    public void setSkinModel(String skinModel) {
        this.skinModel = skinModel;
    }

    public String getDialogScript() {
        return dialogScript;
    }

    public void setDialogScript(String dialogScript) {
        this.dialogScript = dialogScript;
    }

    public String getCutscene() {
        return cutscene;
    }

    public void setCutscene(String cutscene) {
        this.cutscene = cutscene;
    }

    public String getDefaultAnimation() {
        return defaultAnimation;
    }

    public void setDefaultAnimation(String defaultAnimation) {
        this.defaultAnimation = defaultAnimation;
    }

    public List<Waypoint> getWaypoints() {
        return waypoints;
    }

    public void setWaypoints(List<Waypoint> waypoints) {
        this.waypoints = waypoints;
    }

    public boolean isWaypointsLoop() {
        return waypointsLoop;
    }

    public void setWaypointsLoop(boolean waypointsLoop) {
        this.waypointsLoop = waypointsLoop;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public Double getMaxHealth() {
        return maxHealth;
    }

    public void setMaxHealth(Double maxHealth) {
        this.maxHealth = maxHealth;
    }

    public Double getHealth() {
        return health;
    }

    public void setHealth(Double health) {
        this.health = health;
    }

    public Boolean getInvulnerable() {
        return invulnerable;
    }

    public void setInvulnerable(Boolean invulnerable) {
        this.invulnerable = invulnerable;
    }

    public String getProximityText() {
        return proximityText;
    }

    public void setProximityText(String proximityText) {
        this.proximityText = proximityText;
    }

    public Double getProximityRange() {
        return proximityRange;
    }

    public void setProximityRange(Double proximityRange) {
        this.proximityRange = proximityRange;
    }

    public Boolean getShowNameTag() {
        return showNameTag;
    }

    public void setShowNameTag(Boolean showNameTag) {
        this.showNameTag = showNameTag;
    }

    public Boolean getShowProximityTextInActionBar() {
        return showProximityTextInActionBar;
    }

    public void setShowProximityTextInActionBar(Boolean showProximityTextInActionBar) {
        this.showProximityTextInActionBar = showProximityTextInActionBar;
    }

    public Boolean getShowProximityTextInChat() {
        return showProximityTextInChat;
    }

    public void setShowProximityTextInChat(Boolean showProximityTextInChat) {
        this.showProximityTextInChat = showProximityTextInChat;
    }

    public Boolean getValk() {
        return valk;
    }

    public void setValk(Boolean valk) {
        this.valk = valk;
    }

    public Integer getValkRadius() {
        return valkRadius;
    }

    public void setValkRadius(Integer valkRadius) {
        this.valkRadius = valkRadius;
    }

    public String getValkPattern() {
        return valkPattern;
    }

    public void setValkPattern(String valkPattern) {
        this.valkPattern = valkPattern;
    }

    public Boolean getLookAtPlayers() {
        return lookAtPlayers;
    }

    public void setLookAtPlayers(Boolean lookAtPlayers) {
        this.lookAtPlayers = lookAtPlayers;
    }

    public AiSettings getAiSettings() {
        return aiSettings;
    }

    public void setAiSettings(AiSettings aiSettings) {
        this.aiSettings = aiSettings;
    }

    public WildfireSettings getWildfireSettings() {
        return wildfireSettings;
    }

    public void setWildfireSettings(WildfireSettings wildfireSettings) {
        this.wildfireSettings = wildfireSettings;
    }

    public static class AiSettings {
        private boolean enabled;
        private String systemPrompt;
        private String model;
        private Double temperature;
        private Integer maxTokens;
        private Integer optionCount;
        private Integer memoryTurns;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getSystemPrompt() {
            return systemPrompt;
        }

        public void setSystemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public Double getTemperature() {
            return temperature;
        }

        public void setTemperature(Double temperature) {
            this.temperature = temperature;
        }

        public Integer getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
        }

        public Integer getOptionCount() {
            return optionCount;
        }

        public void setOptionCount(Integer optionCount) {
            this.optionCount = optionCount;
        }

        public Integer getMemoryTurns() {
            return memoryTurns;
        }

        public void setMemoryTurns(Integer memoryTurns) {
            this.memoryTurns = memoryTurns;
        }
    }

    public static class WildfireSettings {
        private Boolean enabled;
        private String gender;
        private Float bustSize;
        private Boolean breastPhysics;
        private Float bounceMultiplier;
        private Float floppiness;
        private Boolean showBreastsInArmor;
        private Float cleavage;
        private Boolean uniboob;
        private Float xOffset;
        private Float yOffset;
        private Float zOffset;

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public String getGender() {
            return gender;
        }

        public void setGender(String gender) {
            this.gender = gender;
        }

        public Float getBustSize() {
            return bustSize;
        }

        public void setBustSize(Float bustSize) {
            this.bustSize = bustSize;
        }

        public Boolean getBreastPhysics() {
            return breastPhysics;
        }

        public void setBreastPhysics(Boolean breastPhysics) {
            this.breastPhysics = breastPhysics;
        }

        public Float getBounceMultiplier() {
            return bounceMultiplier;
        }

        public void setBounceMultiplier(Float bounceMultiplier) {
            this.bounceMultiplier = bounceMultiplier;
        }

        public Float getFloppiness() {
            return floppiness;
        }

        public void setFloppiness(Float floppiness) {
            this.floppiness = floppiness;
        }

        public Boolean getShowBreastsInArmor() {
            return showBreastsInArmor;
        }

        public void setShowBreastsInArmor(Boolean showBreastsInArmor) {
            this.showBreastsInArmor = showBreastsInArmor;
        }

        public Float getCleavage() {
            return cleavage;
        }

        public void setCleavage(Float cleavage) {
            this.cleavage = cleavage;
        }

        public Boolean getUniboob() {
            return uniboob;
        }

        public void setUniboob(Boolean uniboob) {
            this.uniboob = uniboob;
        }

        public Float getXOffset() {
            return xOffset;
        }

        public void setXOffset(Float xOffset) {
            this.xOffset = xOffset;
        }

        public Float getYOffset() {
            return yOffset;
        }

        public void setYOffset(Float yOffset) {
            this.yOffset = yOffset;
        }

        public Float getZOffset() {
            return zOffset;
        }

        public void setZOffset(Float zOffset) {
            this.zOffset = zOffset;
        }
    }

    /**
     * Waypoint for NPC movement paths.
     */
    public static class Waypoint {
        private final BlockPos pos;
        private final int waitTicks; // How long to wait at this waypoint
        private final double speed; // Movement speed multiplier

        public Waypoint(BlockPos pos, int waitTicks, double speed) {
            this.pos = pos;
            this.waitTicks = waitTicks;
            this.speed = speed;
        }

        public BlockPos getPos() {
            return pos;
        }

        public int getWaitTicks() {
            return waitTicks;
        }

        public double getSpeed() {
            return speed;
        }
    }
}
