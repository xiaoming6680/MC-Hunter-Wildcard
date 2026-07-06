# Changelog

English | [Simplified Chinese](CHANGELOG.md)

## 1.2.0 - Modrinth Initial Release

This is the first Manhunt Wildcard version published on Modrinth. It is not an update from an older Modrinth release; it is the initial public release with the core gameplay experience included.

### Initial Features

- Adds the hunter vs runner Manhunt game framework.
- Supports `/hw` commands for operators to start and stop games, manage teams, and test wildcards.
- Adds a preparation phase to reduce chaotic starts and give players time to join teams and position themselves.
- Provides hunters with a tracking compass, with targets refreshed according to server configuration.
- Periodically triggers random wildcard events, shown through the HUD, BossBar, and chat messages.
- Supports multiple runner and hunter win conditions.
- Supports configurable respawn modes, lives, death drops, preparation boundary, wildcard interval, and wildcard duration.
- Provides an in-game config screen where OP players can adjust major rules while the game is waiting.
- Includes English and Simplified Chinese language files using Minecraft's native language system.

### Wildcards

This version includes 16 wildcards:

- Speed Rush
- Featherweight
- Glowing
- Night Hunt
- Explosive Death
- Supply Drop
- Hunter Radar
- Compass Chaos
- Hunger Chase
- Weapon Overheat
- Light Load
- Block Decay
- Pearl Frenzy
- Wind Charge Brawl
- Blood Rage
- Disabled Wildcard

### Compatibility

- Minecraft `1.21.11`
- Fabric Loader `0.16.0+`
- Fabric API
- Java `21`

Installing the mod on both client and server is recommended. The server runs the game logic; the client provides the config screen, HUD, and local language display.
