# Marallyzen

Marallyzen is a NeoForge Minecraft mod (MC 1.21.1+) focused on roleplay, NPCs, scripted interactions, and cinematic presentation. It bundles a full NPC stack, dialog UI, cutscene system, and DenizenCore scripting so content creators can build quests, scenes, and interactive storytelling without hardcoding logic.

## Key Features and Components

- **NPC system**: create NPCs from any entity type; define them via JSON; spawn/despawn and manage them at runtime; attach patrol routes and behaviors; integrate with external configs for rapid iteration.
- **Dialog system**: scripted conversations with branching choices; custom in-game GUI screens for dialog and buttons; server-client synchronization so dialog state stays consistent for players; extensible structure for new dialog types.
- **Cutscene/camera system**: keyframed camera paths; JSON-defined scenes with timing and interpolation; runtime controls to play/stop scenes; input locking for smooth cinematic playback.
- **Scripting (DenizenCore)**: advanced script execution for NPC behavior, events, and dialog logic; external script files for easy edits; event system to react to player actions/world triggers; API access for programmatic script control.
- **Developer API**: public interfaces for NPCs, dialogs, cutscenes, and scripts so other mods can spawn NPCs, open dialogs, and play scenes programmatically.
- **Example content**: sample NPC configs, scripts, and scene definitions under `src/main/resources/examples/` to show patterns for real content.
- **Build/setup**: Gradle-based project with modding dependencies and configuration folders for NPCs, scripts, and scenes, ready for server or client usage.

## Project Structure Highlights

- `src/main/resources/examples/`: ready-to-copy samples for NPCs, dialogs/scripts, and cutscenes.
- `config/marallyzen/`: runtime data folders for NPC definitions, scripts, scenes, and waypoints.
- Mod components and integrations: modules and assets for client UI, runtime management, and scripting integration.
