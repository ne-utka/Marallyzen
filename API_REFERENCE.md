# Marallyzen API Reference

This document provides a complete reference for Marallyzen's public API, allowing other mods to integrate with NPC, dialog, cutscene, and scripting systems.

## Getting Started

```java
// Get the main API instance
IMarallyzenAPI api = MarallyzenAPI.getInstance();

// Access individual managers
INpcManager npcManager = api.getNpcManager();
IDialogManager dialogManager = api.getDialogManager();
ICutsceneManager cutsceneManager = api.getCutsceneManager();
IScriptManager scriptManager = api.getScriptManager();
```

## NPC Manager API

### INpcManager

#### Methods

```java
// Create and spawn an NPC
Entity createNpc(String npcId, ServerLevel level, BlockPos spawnPos);

// Register an NPC programmatically
void registerNpc(String npcId, String name, ResourceLocation entityType,
                BlockPos spawnPos, List<BlockPos> waypoints, Map<String, String> metadata);

// Get an NPC by ID
Entity getNpc(String npcId);

// Check if entity is an NPC
boolean isNpc(Entity entity);

// Control NPC movement
void moveNpcToWaypoint(String npcId, int waypointIndex);
void setNpcWaypointLoop(String npcId, boolean loop);

// Manage NPC lifecycle
void despawnNpc(String npcId);

// Query NPCs
List<String> getAllNpcIds();
```

#### Example Usage

```java
// Register a custom NPC
Map<String, String> metadata = new HashMap<>();
metadata.put("profession", "merchant");
metadata.put("shop_type", "weapons");

List<BlockPos> waypoints = List.of(
    new BlockPos(100, 64, 100),
    new BlockPos(110, 64, 100),
    new BlockPos(110, 64, 110)
);

npcManager.registerNpc("weaponsmith", "Weaponsmith",
    new ResourceLocation("minecraft:villager"),
    new BlockPos(100, 64, 100), waypoints, metadata);

// Spawn the NPC
Entity npc = npcManager.createNpc("weaponsmith", level, new BlockPos(100, 64, 100));

// Control movement
npcManager.moveNpcToWaypoint("weaponsmith", 1);
npcManager.setNpcWaypointLoop("weaponsmith", true);
```

## Dialog Manager API

### IDialogManager

#### Methods

```java
// Open a dialog for a player
void openDialog(ServerPlayer player, String dialogId, String title, Map<String, String> buttons);

// Register a dialog programmatically
void registerDialog(String dialogId, String title, Map<String, String> buttons, String scriptName);

// Handle dialog button clicks
void handleDialogButtonClick(ServerPlayer player, String dialogId, String buttonId);

// Query dialogs
List<String> getAllDialogIds();
boolean hasDialog(String dialogId);
```

#### Example Usage

```java
// Create dialog buttons
Map<String, String> buttons = new HashMap<>();
buttons.put("greet", "Say hello");
buttons.put("trade", "Start trading");
buttons.put("quest", "Ask about quests");
buttons.put("bye", "Goodbye");

// Open dialog for player
dialogManager.openDialog(player, "villager_greeting", "Welcome, traveler!", buttons);

// Register a persistent dialog
dialogManager.registerDialog("shop_dialog", "Shop", buttons, "shop_script");
```

## Cutscene Manager API

### ICutsceneManager

#### Methods

```java
// Control scene playback
void playScene(String sceneName);
void stopScene();
boolean isScenePlaying();

// Register scenes programmatically
void registerScene(String sceneId, List<CameraKeyframe> keyframes, boolean loop, float interpolationSpeed);

// Query scenes
List<String> getAllSceneIds();
boolean hasScene(String sceneId);
```

#### CameraKeyframe Class

```java
public class CameraKeyframe {
    public final Vec3 position;
    public final float yaw;
    public final float pitch;
    public final float fov;
    public final long duration; // milliseconds

    public CameraKeyframe(Vec3 position, float yaw, float pitch, float fov, long duration);
}
```

#### Example Usage

```java
// Create camera keyframes
List<ICutsceneManager.CameraKeyframe> keyframes = new ArrayList<>();
keyframes.add(new ICutsceneManager.CameraKeyframe(
    new Vec3(0, 65, 0), 0, 0, 70, 3000
));
keyframes.add(new ICutsceneManager.CameraKeyframe(
    new Vec3(10, 70, 10), 90, -15, 60, 4000
));

// Register and play scene
cutsceneManager.registerScene("custom_scene", keyframes, false, 0.02f);
cutsceneManager.playScene("custom_scene");

// Check scene status
if (cutsceneManager.isScenePlaying()) {
    // Scene is active
}
```

## Script Manager API

### IScriptManager

#### Methods

```java
// Execute scripts
boolean executeScript(String scriptName);
boolean executeScript(String scriptName, Map<String, Object> context);

// Manage scripts
void reloadScripts();
boolean hasScript(String scriptName);

// Query scripts
List<String> getAllScriptNames();
String getDenizenVersion();
```

#### Example Usage

```java
// Execute a script
if (scriptManager.executeScript("npc_interaction")) {
    // Script executed successfully
}

// Execute with context
Map<String, Object> context = new HashMap<>();
context.put("player", player.getName().getString());
context.put("npc_id", "merchant");

scriptManager.executeScript("trade_script", context);

// Reload scripts
scriptManager.reloadScripts();

// Check script availability
if (scriptManager.hasScript("quest_handler")) {
    scriptManager.executeScript("quest_handler");
}
```

## Event System

Marallyzen integrates with NeoForge's event system. You can listen for NPC-related events:

```java
@SubscribeEvent
public static void onNpcInteract(PlayerInteractEntityEvent event) {
    if (MarallyzenAPI.getInstance().getNpcManager().isNpc(event.getTarget())) {
        // Handle NPC interaction
        event.setCanceled(true);
    }
}
```

## Configuration Files

While the API allows programmatic configuration, Marallyzen also supports external configuration files:

### NPC Configuration (JSON)
```json
{
  "id": "guard",
  "name": "City Guard",
  "entityType": "minecraft:villager",
  "spawnPos": { "x": 100, "y": 64, "z": 100 },
  "waypoints": [
    { "x": 100, "y": 64, "z": 100, "waitTicks": 200, "speed": 0.3 },
    { "x": 110, "y": 64, "z": 100, "waitTicks": 100, "speed": 0.3 }
  ],
  "waypointsLoop": true,
  "dialogScript": "guard_dialog"
}
```

### Scene Configuration (JSON)
```json
{
  "id": "dramatic_reveal",
  "loop": false,
  "interpolationSpeed": 0.01,
  "keyframes": [
    {
      "position": { "x": 0, "y": 65, "z": 0 },
      "yaw": 0, "pitch": 0, "fov": 70, "duration": 2000
    },
    {
      "position": { "x": 0, "y": 80, "z": 20 },
      "yaw": 180, "pitch": -30, "fov": 45, "duration": 5000
    }
  ]
}
```

### Script Configuration (Denizen .dsc)
```yaml
npc_event_handler:
    type: world
    events:
        on player clicks npc:
            - define npc:<context.entity>
            - narrate "You clicked on <[npc].name>!"
            - execute "npc_clicked" context:npc=<[npc]>
```

## Best Practices

### Error Handling
```java
try {
    Entity npc = npcManager.createNpc("my_npc", level, pos);
    if (npc == null) {
        // Handle creation failure
    }
} catch (Exception e) {
    // Handle exceptions
}
```

### Resource Management
```java
// Clean up when done
if (cutsceneManager.isScenePlaying()) {
    cutsceneManager.stopScene();
}

npcManager.despawnNpc("temporary_npc");
```

### Performance Considerations
- Register NPCs and scenes during mod initialization, not runtime
- Use external configuration files for complex setups
- Avoid creating NPCs in performance-critical code paths

## Version Compatibility

- **NeoForge**: 21.1.209+
- **Minecraft**: 1.21.1+
- **Java**: 21+

## Support

For API questions or issues:
- Check the examples in `src/main/resources/examples/`
- Review the main README.md
- Open an issue on the project repository

---

*This API provides comprehensive access to Marallyzen's features while maintaining clean separation between mods.*



