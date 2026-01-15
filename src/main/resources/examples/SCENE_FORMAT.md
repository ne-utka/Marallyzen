# Marallyzen Cutscene JSON Format

Server‑authoritative scenes are stored in `data/marallyzen/scenes/*.json`.
Client only renders; all timing/state lives on the server.

## Scene
```json
{
  "id": "late_train_scene",
  "duration": 200,                 // ticks
  "on_start": ["lock_player"],     // optional: lock_player, hide_hud
  "on_end": ["unlock_player"],     // optional: unlock_player, show_hud
  "participants": ["player"],      // optional, list of targets (future)
  "keyframes": [ /* see below */ ]
}
```

## Keyframe types

### CAMERA
```json
{
  "time": 0,
  "type": "CAMERA",
  "position": [12.5, 64.0, -8.3],
  "rotation": [30.0, 180.0],       // pitch, yaw
  "fov": 70.0,
  "smooth": true                   // lerp on client
}
```

### FADE
```json
{
  "time": 20,
  "type": "FADE",
  "fadeOut": 10,                   // ticks
  "black": 40,
  "fadeIn": 10,
  "title": "Путь к станции",
  "subtitle": "Глава 1",
  "blockInput": true,
  "sound": "minecraft:block.anvil.land"
}
```

### EYES_CLOSE
```json
{
  "time": 60,
  "type": "EYES_CLOSE",
  "close": 8,
  "black": 30,
  "open": 8,
  "blockInput": true
}
```

### OVERLAY (narration)
```json
{
  "time": 100,
  "type": "OVERLAY",
  "text": "Прибытие через 5 минут",
  "duration": 60,
  "fadeIn": 5,
  "fadeOut": 5
}
```

### NPC_LOOK
```json
{
  "time": 80,
  "type": "NPC_LOOK",
  "npcId": "guard_1",
  "target": "player"               // or [x,y,z] or target UUID
}
```

### NPC_EMOTION
```json
{
  "time": 90,
  "type": "NPC_EMOTION",
  "npcId": "guard_1",
  "emotion": "wave"
}
```

### ANIMATION (emote)
```json
{
  "time": 120,
  "type": "ANIMATION",
  "npcId": "guard_1",
  "emote": "SPE_Salute",
  "radius": 32
}
```

### PARTICLE
```json
{
  "time": 140,
  "type": "PARTICLE",
  "particle": "minecraft:smoke",
  "pos": [13.0, 65.2, -8.0],
  "count": 12,
  "spread": [0.2, 0.2, 0.2],
  "speed": 0.01
}
```

### BLOCK_FAKE
```json
{
  "time": 150,
  "type": "BLOCK_FAKE",
  "pos": [14,64,-7],
  "state": "minecraft:barrier",
  "duration": 40                  // client removes after duration
}
```

## Rules
- All `time`/durations are in ticks; server drives timing.
- Client never computes schedule, only executes events it receives.
- Unknown types should be rejected at load time on the server.
- `duration` on scene bounds EndCutscene and on_end flags.

