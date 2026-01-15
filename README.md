# Marallyzen

[![Lines of Code](https://img.shields.io/badge/Lines%20of%20Code-51798-blue)](https://github.com/ne-utka/Marallyzen)

Marallyzen is a NeoForge Minecraft mod (MC 1.21.1+) focused on roleplay, NPCs, scripted interactions, and cinematic presentation. It bundles a full NPC stack, dialog UI, cutscene system, and DenizenCore scripting so content creators can build quests, scenes, and interactive storytelling without hardcoding logic.

![Uploading 202601151207 (1) (2).gif…]()

## Key Features and Components

- **NPC system**: create NPCs from any entity type; define them via JSON; spawn/despawn and manage them at runtime; attach patrol routes and behaviors; integrate with external configs for rapid iteration.
- **Dialog system**: scripted conversations with branching choices; custom in-game GUI screens for dialog and buttons; server-client synchronization so dialog state stays consistent for players; extensible structure for new dialog types.
- **Cutscene/camera system**: keyframed camera paths; JSON-defined scenes with timing and interpolation; runtime controls to play/stop scenes; input locking for smooth cinematic playback.
- **Scripting (DenizenCore)**: advanced script execution for NPC behavior, events, and dialog logic; external script files for easy edits; event system to react to player actions/world triggers; API access for programmatic script control.
- **Developer API**: public interfaces for NPCs, dialogs, cutscenes, and scripts so other mods can spawn NPCs, open dialogs, and play scenes programmatically.
- **Example content**: sample NPC configs, scripts, and scene definitions under `src/main/resources/examples/` to show patterns for real content.
- **Build/setup**: Gradle-based project with modding dependencies and configuration folders for NPCs, scripts, and scenes, ready for server or client usage.

![01JW1HH4JDAZ4MSFP4PHKTQCWP (1) (2)](https://github.com/user-attachments/assets/e7985354-a91c-4ffd-9a9d-b312ae0ee261)

![01JW1HGBMPE3E89AWBG237M3QQ (1)](https://github.com/user-attachments/assets/ca25fd64-2b38-4350-b84e-15e2e19b4866)

## Project Structure Highlights

- `src/main/resources/examples/`: ready-to-copy samples for NPCs, dialogs/scripts, and cutscenes.
- `config/marallyzen/`: runtime data folders for NPC definitions, scripts, scenes, and waypoints.
- Mod components and integrations: modules and assets for client UI, runtime management, and scripting integration.

## Install

1. Download the mod JAR.
2. Place it in your `mods` folder.
3. Start your NeoForge server or client.

## Usage

### NPC config (JSON)

```json
{
  "id": "trader",
  "name": "Village Trader",
  "entityType": "minecraft:villager",
  "spawnPos": { "x": 100, "y": 64, "z": 100 },
  "dialogScript": "trader_dialog"
}
```

### Scripted dialog (Denizen)

```yaml
trader_dialog:
    type: assignment
    actions:
        on assignment:
            - trigger name:click state:true
    interact scripts:
    - trader_interact

trader_interact:
    type: interact
    steps:
        1:
            click trigger:
                script:
                    - narrate "Welcome to my shop!"
                    - define options:<list[buy|sell|leave]>
                    - choose <[options]>:
                        - case buy:
                            - narrate "What would you like to buy?"
                        - case leave:
                            - narrate "Come back soon!"
```

### Cutscene (JSON)

```json
{
  "id": "intro",
  "loop": false,
  "interpolationSpeed": 0.02,
  "keyframes": [
    {
      "position": { "x": 0, "y": 65, "z": 0 },
      "yaw": 0,
      "pitch": 0,
      "fov": 70,
      "duration": 3000
    },
    {
      "position": { "x": 10, "y": 70, "z": 10 },
      "yaw": 90,
      "pitch": -15,
      "fov": 60,
      "duration": 4000
    }
  ]
}
```

### Commands

```
/marallyzen spawnnpc <id>
/marallyzen playscene <scene_id>
/marallyzen reload
```

## API

### Getting started

```java
IMarallyzenAPI api = MarallyzenAPI.getInstance();
```

### NPC management

```java
INpcManager npcManager = api.getNpcManager();

Entity npc = npcManager.createNpc("my_npc", level, new BlockPos(100, 64, 100));
npcManager.moveNpcToWaypoint("my_npc", 2);
npcManager.setNpcWaypointLoop("my_npc", true);
```

### Dialog management

```java
IDialogManager dialogManager = api.getDialogManager();

Map<String, String> buttons = Map.of(
    "option1", "Talk about weather",
    "option2", "Ask for directions"
);
dialogManager.openDialog(player, "greeting", "Hello!", buttons);
```

### Cutscene management

```java
ICutsceneManager cutsceneManager = api.getCutsceneManager();

cutsceneManager.playScene("intro_scene");
if (cutsceneManager.isScenePlaying()) {
    // Handle scene state.
}
```

### Script management

```java
IScriptManager scriptManager = api.getScriptManager();

scriptManager.executeScript("my_script");
scriptManager.reloadScripts();
```
