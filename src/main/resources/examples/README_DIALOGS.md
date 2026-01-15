# Dialog System for Marallyzen NPCs

## Overview
Marallyzen provides a complete dialog system for NPC interactions using native NeoForge GUI components. Dialogs are scripted using Denizen and can include interactive buttons for player choices.

## NPC Dialog Configuration

To add dialog to an NPC, specify a `dialogScript` in the NPC JSON:

```json
{
  "id": "merchant",
  "name": "Merchant",
  "dialogScript": "merchant_dialog",
  "spawnPos": {
    "x": 0,
    "y": 64,
    "z": 0
  }
}
```

## Dialog Scripts

Dialogs are written in Denizen script format (`.dsc` files) and placed in `config/marallyzen/scripts/`.

### Basic Dialog Structure

```yaml
# Assignment script - defines when NPC responds to interactions
merchant_dialog:
    type: assignment
    actions:
        on assignment:
            - trigger name:click state:true
    interact scripts:
    - merchant_interact

# Interact script - defines the actual dialog flow
merchant_interact:
    type: interact
    steps:
        1:
            click trigger:
                script:
                    - narrate "Hello, adventurer!"
                    - wait 1s
                    - define options:<list[buy|sell|leave]>
                    - choose <[options]>:
                        - case buy:
                            - narrate "What would you like to buy?"
                        - case sell:
                            - narrate "What would you like to sell?"
                        - case leave:
                            - narrate "Goodbye!"
    requirements:
        mode: all
```

## GUI Components

### DialogScreen
- Displays dialog text and interactive buttons
- Semi-transparent background with centered dialog box
- ESC key closes the dialog
- Buttons send click events to server

### Dialog Buttons
- Automatically generated from script options
- Each button corresponds to a dialog choice
- Clicks trigger server-side script execution

## Network Communication

### OpenDialogPacket (S2C)
Sent when player clicks on NPC with dialog script:
```java
new OpenDialogPacket(dialogId, title, buttonMap)
```

### DialogButtonClickPacket (C2S)
Sent when player clicks dialog button:
```java
new DialogButtonClickPacket(dialogId, buttonId)
```

## Commands

- `/marallyzen reload` - Reload scripts and NPC configurations
- `/marallyzen spawnnpc <id>` - Spawn NPC at your location

## Examples

### Simple Guard Dialog
```yaml
guard_dialog:
    type: assignment
    actions:
        on assignment:
            - trigger name:click state:true
    interact scripts:
    - guard_interact

guard_interact:
    type: interact
    steps:
        1:
            click trigger:
                script:
                    - narrate "Halt! Who goes there?"
                    - define options:<list[friend|enemy]>
                    - choose <[options]>:
                        - case friend:
                            - narrate "Welcome, friend!"
                        - case enemy:
                            - narrate "Begone, villain!"
```

### Merchant with Multiple Options
```yaml
merchant_interact:
    type: interact
    steps:
        1:
            click trigger:
                script:
                    - narrate "Welcome to my shop!"
                    - define options:<list[buy|sell|info|leave]>
                    - choose <[options]>:
                        - case buy:
                            - narrate "Here's what I have..."
                        - case sell:
                            - narrate "Show me what you have..."
                        - case info:
                            - narrate "I trade in rare items..."
                        - case leave:
                            - narrate "Come back soon!"
```

## File Structure

```
config/marallyzen/
├── scripts/
│   ├── guard_dialog.dsc
│   └── merchant_dialog.dsc
└── npcs/
    ├── guard_patrol.json
    └── merchant.json
```

## Integration with Denizen

The dialog system integrates with Denizen's event system:
- Player clicks trigger events
- Script execution with conditional logic
- Support for variables and data storage
- Complex dialog trees and branching narratives

## Future Enhancements

- Rich text formatting in dialogs
- Custom dialog backgrounds/textures
- Sound effects for dialog interactions
- Multi-page dialogs with scrolling
- Conditional dialog options based on player state



