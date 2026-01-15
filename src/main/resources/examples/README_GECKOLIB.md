# GeckoLib Integration for Marallyzen NPCs

## Overview
Marallyzen supports custom NPC models and animations using GeckoLib. NPCs can use either vanilla Minecraft entities or custom GeckoLib models with animations.

## GeckoLib NPC Configuration

To create an NPC with custom GeckoLib model, add the `geckolib` section to your NPC JSON:

```json
{
  "id": "my_custom_npc",
  "name": "Custom NPC",
  "geckolib": {
    "model": "marallyzen:geo/my_model.geo.json",
    "animation": "marallyzen:animations/my_animation.animation.json"
  },
  "spawnPos": {
    "x": 0,
    "y": 64,
    "z": 0
  }
}
```

## File Structure

Place your GeckoLib resources in the mod's assets:

```
src/main/resources/assets/marallyzen/
├── geo/
│   └── my_model.geo.json
├── animations/
│   └── my_animation.animation.json
└── textures/
    └── entity/
        └── my_texture.png
```

## Commands

- `/marallyzen spawnnpc animated_guard` - Spawn an NPC with GeckoLib model
- `/marallyzen waypoint <npc_id> loop <true/false>` - Control waypoint looping
- `/marallyzen waypoint <npc_id> move <waypoint_index>` - Force NPC to specific waypoint

## Example NPCs

- `animated_guard.json` - Guard with custom model and patrol route
- `guard_patrol.json` - Regular villager with waypoints
- `merchant.json` - Static merchant NPC

## GeckoLib Resources

For creating custom models and animations, use:
- [Blockbench](https://blockbench.net/) - 3D modeling tool with GeckoLib support
- [GeckoLib Documentation](https://github.com/bernie-g/geckolib/wiki)
- [GeckoLib Examples](https://github.com/bernie-g/geckolib/tree/1.21/common/src/main/resources/assets/geckolib)



