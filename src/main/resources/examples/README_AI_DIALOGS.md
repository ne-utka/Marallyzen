# AI Dialogs for Marallyzen NPCs

## Overview
Marallyzen can generate NPC dialog with an external AI service. AI dialogs are server-side and use the same dialog HUD and narration flow as scripted dialogs.

## Global Config
Create or edit `config/marallyzen/ai.json`:

```json
{
  "enabled": true,
  "baseUrl": "https://api.openai.com/v1/chat/completions",
  "apiKey": "YOUR_API_KEY",
  "model": "gpt-4o-mini",
  "temperature": 0.7,
  "maxTokens": 200,
  "timeoutSeconds": 20,
  "defaultOptionCount": 3,
  "defaultMemoryTurns": 8,
  "systemPrompt": "You are an NPC in a game. Reply in the same language as the player. Return a JSON object only: {\"reply\":\"...\",\"options\":[\"...\",\"...\"]}."
}
```

## NPC Config
Enable AI on an NPC by adding an `ai` section to its JSON:

```json
{
  "id": "sage",
  "name": "Sage",
  "ai": {
    "enabled": true,
    "systemPrompt": "You are a wise forest spirit. Keep responses short and calm.",
    "model": "gpt-4o-mini",
    "temperature": 0.6,
    "maxTokens": 160,
    "optionCount": 4,
    "memoryTurns": 6
  }
}
```

## Notes
- AI dialogs do not require `dialogScript`.
- Conversation history is kept per player + NPC (limited by `memoryTurns`).
- Use `/marallyzen reload` after editing configs.
