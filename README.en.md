# Manhunt Wildcard

English | [简体中文](README.md)

Manhunt Wildcard is a Fabric mod for Minecraft Manhunt. Players split into hunters and runners, while the game periodically triggers random wildcard events that shift the pace of the chase.

## Inspiration

This mod is partly inspired by a recent-season Apex Legends Wildcard event. The idea is to add temporary rule changes on top of the normal match structure, creating new risks, openings, and tactical decisions each round. Manhunt Wildcard brings that style of rotating match modifier into Minecraft Manhunt.

## Features

- Hunter vs runner team gameplay
- Operator-controlled game start, stop, and wildcard testing
- Preparation phase to prevent messy starts
- Tracking compass for hunters, refreshed by server config
- Periodic random wildcards shown through HUD, BossBar, and chat messages
- Multiple runner and hunter win conditions
- Configurable respawn modes, lives, death drops, and preparation boundary
- English and Simplified Chinese language files using Minecraft's native lang system

## Wildcards

| Wildcard | Effect |
| --- | --- |
| Speed Rush | Everyone gains speed |
| Featherweight | Jump boost and slow falling |
| Glowing | All players glow |
| Night Hunt | Forces night and gives hunters night vision |
| Explosive Death | Deaths or kills trigger explosions |
| Supply Drop | Spawns random supply chests |
| Hunter Radar | Hunters receive nearest-runner distance hints |
| Compass Chaos | Tracking direction is offset |
| Hunger Chase | Hunger drains faster and food changes the tempo |
| Weapon Overheat | Repeated attacks trigger overheat penalties |
| Light Load | Light armor grants speed; heavy armor slows the wearer |
| Block Decay | Newly placed blocks disappear after a delay |
| Pearl Frenzy | Periodically grants ender pearls, with possible side effects |
| Wind Charge Brawl | Periodically grants wind charges |
| Blood Rage | Low health grants buffs |
| Disabled Wildcard | No extra effect for this round |

## Requirements

- Minecraft `1.21.11`
- Fabric Loader `0.19.0+`
- Fabric API
- Java `21`

Installing the mod on both client and server is recommended. The server runs the game logic; the client provides the config screen, HUD, and local language display.

## Installation

1. Install Fabric Loader.
2. Install Fabric API.
3. Download `MC-Manhunt-Wildcard-<version>.jar`.
4. Put the jar into the `mods/` folder on both client and server.
5. Start the game or server.

On first launch, the mod creates:

```text
config/hunterwildcard.json
```

The internal mod id and config file name remain `hunterwildcard` to preserve compatibility with existing configs, language keys, and network payloads.

## Player Commands

| Command | Description |
| --- | --- |
| `/hw join hunter` | Join the hunters |
| `/hw join runner` | Join the runners |
| `/hw leave` | Leave your team |
| `/hw status` | Show current game status |
| `/hw wildcard list` | Show wildcard enable status |

## Operator Commands

These commands require OP permission:

| Command | Description |
| --- | --- |
| `/hw start` | Start a game |
| `/hw stop` | Stop the game |
| `/hw wildcard roll` | Roll a wildcard immediately |
| `/hw wildcard stop` | Stop the active wildcard |
| `/hw wildcard test <id>` | Test a specific wildcard |
| `/hw config reload` | Reload config |
| `/hw config save` | Save config |
| `/hw debug true` | Enable the debug screen |
| `/hw debug false` | Disable the debug screen |

## Config Screen

Press `H` by default to open the config screen. Operators can edit rules while the game is waiting, including:

- Game and team status
- Core timers, preparation boundary, and death drops
- Runner and hunter win conditions
- Respawn modes, lives, and respawn timers
- Wildcard interval, duration, enabled states, and per-wildcard settings
- Debug actions and wildcard testing

## Design Goals

Manhunt Wildcard aims to make Manhunt rounds more varied:

- Keep the chase from settling into one fixed rhythm
- Add randomness, adaptation, and counterplay
- Preserve the core objective-driven feel of Minecraft Manhunt
- Give server owners a configurable, testable, localized gameplay extension

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE).
