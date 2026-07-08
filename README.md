<p align="center">
  <img src="docs/assets/logo.svg" alt="Pala's Item Clear" width="120" height="120">
</p>

# Pala's Item Clear

### Periodically clear dropped items on your Minecraft server, with warnings and a recovery bin

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1%20to%2026.2-62B47A?logo=minecraft&logoColor=white)](docs/SUPPORT.md)
[![Loaders](https://img.shields.io/badge/Loaders-Forge%20%7C%20Fabric%20%7C%20NeoForge-2d6a4f)](docs/SUPPORT.md)
[![Java](https://img.shields.io/badge/Java-17%20%7C%2021%20%7C%2025-orange?logo=openjdk&logoColor=white)](docs/SUPPORT.md)
[![CI](https://github.com/Palawizard/PalasItemClear/actions/workflows/ci.yml/badge.svg)](https://github.com/Palawizard/PalasItemClear/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

[Features](#features) | [Installation](#installation) | [Configuration](#configuration) | [Commands](#commands) | [Compatibility](#compatibility) | [Verification](#verification)

---

## Features

### Clearing

- Reduces server lag caused by accumulating dropped items, like a modern ClearLag alternative
- Removes only dropped item entities, on the server thread, across loaded server levels
- Configurable interval with a five-minute default
- Item, dimension, age, custom-name, and player-ownership filters
- Guardrail against runaway passes with very large item counts

### Warnings and messages

- Colored chat warnings at 60, 30, and 5 seconds before each clear, all configurable
- MiniMessage-style color formatting
- English defaults; every message is editable

### Administration

- Permission-gated `/palasitemclear` commands for status, manual clear, pause, resume, reset, reload, and live settings
- Atomic configuration loading with validation, backup, and safe recovery

### Recovery bin

- Recently cleared stacks kept in memory for a configurable window (120 seconds by default)
- `/palasitemclear bin [player]` opens a vanilla chest interface to restore items
- Works without installing the mod on clients

### Compatibility

- Separate, real-server-verified artifacts for Forge, Fabric, and NeoForge
- Every Minecraft band from 1.20.1 to 26.2, with no gaps or overlaps
- Server-side only; vanilla clients can still connect

---

## Installation

1. Download the JAR that matches both your Minecraft version band and server loader from [GitHub Releases](https://github.com/Palawizard/PalasItemClear/releases).
2. Place it in the server `mods` folder.
3. Install any companion mods listed for that line in [docs/SUPPORT.md](docs/SUPPORT.md) (Fabric lines require Fabric API).
4. Start the server once to generate `config/palasitemclear.json`.

Never reuse a JAR across Minecraft version bands.

---

## Configuration

The server creates `config/palasitemclear.json` on first startup. Defaults:

- Clear interval: 300 seconds
- Warnings: 60, 30, and 5 seconds before each clear
- Recovery-bin retention: 120 seconds in memory

Run `/palasitemclear reload` after editing the file. The full schema and an example live in [docs/SUPPORT.md](docs/SUPPORT.md#configuration-reference).

---

## Commands

All commands require permission level 2.

| Command | Purpose |
| --- | --- |
| `/palasitemclear` | Show command help |
| `/palasitemclear status` | Show scheduler status and the last result |
| `/palasitemclear next` | Show the time until the next clear |
| `/palasitemclear clear` | Clear eligible dropped items now |
| `/palasitemclear bin [player]` | Open the recovery bin, optionally filtered by player |
| `/palasitemclear pause` | Pause automatic clearing |
| `/palasitemclear resume` | Resume automatic clearing |
| `/palasitemclear reset` | Reset the countdown |
| `/palasitemclear reload` | Reload the configuration file |
| `/palasitemclear set interval <seconds>` | Change and save the clear interval |

---

## Compatibility

Pala's Item Clear ships a separate artifact for every Minecraft version band and loader, from 1.20.1 through 26.2. Metadata ranges cover every patch with no gaps or overlaps, so exactly one artifact matches any supported server.

The full matrix, Java requirements, companion mods, and porting policy live in [docs/SUPPORT.md](docs/SUPPORT.md).

---

## Architecture

```text
palasitemclear/
├── common/     Shared clearing, scheduling, filters, config, messages, commands, and bin
├── forge/      Forge entry point for the 1.20.1 line
├── fabric/     Fabric entry point for the 1.20.1 line
├── versions/   Standalone Forge, Fabric, and NeoForge projects, one per version band
├── scripts/    Build, real-server smoke, and release-artifact tooling
└── .github/    CI matrix build, smoke, and tagged-release automation
```

The `common` module owns all behavior; loader modules only adapt lifecycle events,
commands, and server access. `versions/version-matrix.json` defines the compatibility
bands and metadata ranges shared by the build, smoke, and release tooling.

### Clearing flow

```text
server tick  →  scheduler countdown  →  warnings at 60s, 30s, 5s
                                     →  clear dropped items (filtered)  →  recovery bin (timed retention)
```

Every clear runs on the server thread and only iterates loaded server levels.

---

## Tech stack

| Layer | Technology |
| --- | --- |
| Language | Java 17, 21, and 25 depending on the Minecraft line |
| Build | Gradle 8.12 with per-band toolchains |
| Loaders | Forge, Fabric, and NeoForge |
| 1.20.1 line | Architectury Loom over a shared core |
| Version bands | Standalone per-loader projects from 1.20.1 to 26.2 |
| Verification | JUnit unit tests and real-server smoke |
| CI and releases | GitHub Actions matrix; artifacts published on `v*` tags |

---

## Verification

```powershell
.\gradlew.bat clean build
.\scripts\build-all-version-bands.ps1 -SkipSmoke
.\scripts\prod-smoke-all.ps1
.\scripts\check-release-artifacts.ps1 -SkipBuild
```

`prod-smoke-all.ps1` installs each loader's real dedicated server, drops the packaged JAR plus companions, and asserts the item clearing scheduler starts and stops on every supported band.

---

## Contributing and releases

- Integration branch: `dev`
- Commit format: `type(scope): thing done`
- Tags matching `v*` publish a GitHub release with every artifact
- Contribution guide: [CONTRIBUTING.md](CONTRIBUTING.md)
- Compatibility and porting policy: [docs/SUPPORT.md](docs/SUPPORT.md)
- Changelog: [CHANGELOG.md](CHANGELOG.md)

---

## License

Distributed under the [MIT License](LICENSE).

---

Pala's Item Clear | Server-side | Minecraft 1.20.1 to 26.2
