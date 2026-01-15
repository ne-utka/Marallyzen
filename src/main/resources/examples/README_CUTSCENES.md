# Cutscene System for Marallyzen

## Overview
Marallyzen provides a powerful cutscene system for cinematic camera movements. Cutscenes are defined as sequences of camera keyframes with smooth interpolation between positions, rotations, and FOV changes.

## Scene Configuration

Cutscenes are defined in JSON files placed in `config/marallyzen/scenes/`. Each scene consists of keyframes that define camera positions over time.

### Basic Scene Structure

```json
{
  "id": "my_scene",
  "loop": false,
  "interpolationSpeed": 0.02,
  "keyframes": [
    {
      "position": { "x": 0.0, "y": 65.0, "z": 0.0 },
      "yaw": 0.0,
      "pitch": 0.0,
      "fov": 70.0,
      "duration": 3000
    },
    {
      "position": { "x": 10.0, "y": 70.0, "z": 10.0 },
      "yaw": 90.0,
      "pitch": -15.0,
      "fov": 60.0,
      "duration": 4000
    }
  ]
}
```

## Scene Properties

- **id**: Unique identifier for the scene
- **loop**: Whether the scene should loop infinitely (default: false)
- **interpolationSpeed**: How fast the camera interpolates between keyframes (0.0-1.0, default: 0.05)
- **keyframes**: Array of camera positions

## Keyframe Properties

Each keyframe defines a camera state:

- **position**: Camera position as {x, y, z} coordinates
- **yaw**: Horizontal rotation in degrees (0 = north, 90 = east, etc.)
- **pitch**: Vertical rotation in degrees (-90 = up, 0 = horizontal, 90 = down)
- **fov**: Field of view in degrees (default: 70.0)
- **duration**: How long this keyframe lasts in milliseconds

## Commands

- `/marallyzen playscene <scene_id>` - Start playing a cutscene
- `/marallyzen reload` - Reload scenes from configuration files

## Network Communication

### PlayScenePacket (S2C)
Sent when a cutscene should start playing:
```java
new PlayScenePacket(sceneName)
```

## Camera Control Features

### Smooth Interpolation
- Linear interpolation between positions
- Angle wrapping for seamless rotations
- Configurable interpolation speed

### Player Input Locking
- During cutscenes, player input is locked
- Camera movement is controlled by the scene
- Players cannot interrupt the cinematic experience

### Scene Management
- Scenes are loaded automatically on client startup
- Multiple scenes can be defined
- Scenes can loop or play once

## Examples

### Intro Cinematic
```json
{
  "id": "intro_scene",
  "loop": false,
  "interpolationSpeed": 0.02,
  "keyframes": [
    {
      "position": { "x": 0.0, "y": 65.0, "z": 0.0 },
      "yaw": 0.0,
      "pitch": 0.0,
      "fov": 70.0,
      "duration": 3000
    },
    {
      "position": { "x": 0.0, "y": 75.0, "z": 10.0 },
      "yaw": 90.0,
      "pitch": -15.0,
      "fov": 60.0,
      "duration": 4000
    }
  ]
}
```

### Patrol Overview Camera
```json
{
  "id": "patrol_overview",
  "loop": true,
  "interpolationSpeed": 0.03,
  "keyframes": [
    {
      "position": { "x": 95.0, "y": 68.0, "z": 95.0 },
      "yaw": 45.0,
      "pitch": -20.0,
      "fov": 75.0,
      "duration": 5000
    }
  ]
}
```

## File Structure

```
config/marallyzen/
├── scenes/
│   ├── intro_scene.json
│   ├── patrol_overview.json
│   └── boss_fight_intro.json
```

## Integration with NPCs

Cutscenes can be triggered by:
- Player commands
- NPC interactions (future feature)
- Script events (future feature)
- Server events (future feature)

## Future Enhancements

- Entity attachment (camera follows NPCs)
- Script-based scene definition
- Advanced easing functions (ease-in, ease-out)
- Sound synchronization
- Multiple camera layers
- Scene branching based on conditions



