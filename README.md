<p align="center">
  <img src="docs/assets/logo.svg" alt="Palas Item Clear" width="128" height="128">
</p>

# Palas Item Clear

[![Build and release](https://github.com/Palawizard/PalasItemClear/actions/workflows/ci.yml/badge.svg)](https://github.com/Palawizard/PalasItemClear/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

Server-side Minecraft mod that periodically removes dropped items after configurable, colored chat warnings. Administrators can tune scheduling, filters, and messages from a JSON file or in-game commands. A short-lived recovery bin lets operators restore recently cleared stacks through a vanilla chest interface.

## Requirements

- A dedicated Minecraft server (Forge, Fabric, or NeoForge depending on the artifact)
- Java 17 for Minecraft 1.20.x lines; Java 21 for 1.21.x lines; Java 25 for Minecraft 26.x lines
- Fabric lines also require Fabric API where noted in [docs/SUPPORT.md](docs/SUPPORT.md)

The mod is server-side only. Players do not need it installed on their clients.

## Installation

1. Download the JAR that matches both your Minecraft version band and server loader from [GitHub Releases](https://github.com/Palawizard/PalasItemClear/releases).
2. Place it in the server `mods` folder.
3. Install any required companion mods listed for that line in [docs/SUPPORT.md](docs/SUPPORT.md).
4. Start the server once to generate `config/palasitemclear.json`.

Never reuse a JAR across Minecraft version bands.

## Stack

- Java 17, 21, and 25 depending on the Minecraft line
- Gradle 8.12
- Architectury Loom for the 1.20.1 Forge/Fabric line
- Standalone Fabric, Forge, and NeoForge projects for every supported version band from 1.20.1 through 26.2

## Run

Build and test the canonical 1.20.1 Forge/Fabric line:

```powershell
.\gradlew.bat clean build
.\scripts\smoke-test-servers.ps1
```

Build every supported version band:

```powershell
.\scripts\build-all-version-bands.ps1 -SkipSmoke
.\scripts\check-release-artifacts.ps1 -SkipBuild
```

Run dedicated-server smoke tests for every supported band and loader:

```powershell
.\scripts\smoke-test-all-version-bands.ps1
```

Development servers:

```powershell
.\gradlew.bat :forge:runServer
.\gradlew.bat :fabric:runServer
```

## Configuration

The server creates `config/palasitemclear.json` on first startup. Defaults:

- Clear interval: 300 seconds
- Warnings: 60, 30, and 5 seconds before each clear
- Recovery-bin retention: 120 seconds in memory

Run `/palasitemclear reload` after editing the file. See the example in [docs/SUPPORT.md](docs/SUPPORT.md#configuration-reference) for the full schema.

## Commands

All commands require permission level 2.

| Command | Purpose |
|---------|---------|
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

## Supported versions

Palas Item Clear ships separate artifacts for every Minecraft version band from 1.20.1 through 26.2. The full compatibility matrix, Java requirements, companion mods, and porting policy live in [docs/SUPPORT.md](docs/SUPPORT.md).

## Verification

```powershell
.\gradlew.bat clean build
.\scripts\build-all-version-bands.ps1
.\scripts\check-release-artifacts.ps1 -SkipBuild
```

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).
