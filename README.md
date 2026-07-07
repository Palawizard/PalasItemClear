# Palas Item Clear

Server-side Minecraft mod that periodically removes dropped items after configurable, colored chat warnings.

## Installation

Use the artifact matching both the Minecraft version and server loader. Never reuse a JAR across Minecraft versions.

- Minecraft 1.20.1 Fabric: install Fabric API and Architectury API.
- Minecraft 1.20.1 Forge: install Architectury API.
- Minecraft 1.21.1 NeoForge: no additional runtime library is required.

The mod is server-side only; players do not need it on their clients.

## Stack

- Minecraft 1.20.1
- Forge and Fabric via Architectury
- Minecraft 1.21.1 on NeoForge
- Java 17 for 1.20.1; Java 21 for 1.21.1
- Gradle

## Run

Use a Java 17 or newer JDK.

```powershell
.\gradlew.bat :forge:runServer
.\gradlew.bat :fabric:runServer
```

Build the 1.20.1 loader artifacts:

```powershell
.\gradlew.bat build
```

Build the 1.21.1 NeoForge artifact:

```powershell
.\gradlew.bat -p versions/1.21.1-neoforge build
```

Smoke-test dedicated server startup and shutdown for both loaders:

```powershell
.\scripts\smoke-test-servers.ps1
```

## Configuration

The server creates `config/palasitemclear.json` on first startup. It controls the interval, warning times and messages, exclusion filters, and recovery-bin retention. Run `/palasitemclear reload` after editing it.

Defaults:

- Clear interval: 300 seconds
- Warnings: 60, 30, and 5 seconds
- Recovery-bin retention: 120 seconds

Complete example:

```json
{
  "configVersion": 1,
  "schedule": {
    "intervalSeconds": 300,
    "warningSeconds": [60, 30, 5]
  },
  "messages": {
    "warning": "<yellow>Items on the ground will be cleared in <gold>{seconds}</gold> seconds.",
    "cleared": "<gray>Cleared <white>{count}</white> dropped items."
  },
  "filters": {
    "excludedItems": ["minecraft:nether_star"],
    "excludedDimensions": ["minecraft:the_end"],
    "minAgeTicks": 0,
    "excludeNamedItems": false,
    "excludePlayerOwnedItems": false
  },
  "bin": {
    "retentionSeconds": 120
  }
}
```

`excludedItems` and `excludedDimensions` require full `namespace:id` identifiers. `minAgeTicks` protects recently spawned drops. Named and player-owned exclusions are disabled by default. Invalid configuration is backed up and safe defaults are loaded.

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

Commands work from the server console except `bin`, which reports the available count because a console cannot open a chest interface. The optional player argument filters entries attributed to that player's owner or thrower metadata.

## Recovery bin

Every cleared stack is held in memory for `bin.retentionSeconds`. Taking an item removes it atomically from the bin, including when multiple administrators have the interface open. Data is intentionally discarded when the server stops and is never written to disk.

## Verification

```powershell
.\gradlew.bat clean build
.\scripts\smoke-test-servers.ps1
.\scripts\check-release-artifacts.ps1 -SkipBuild
```

The release check accepts only the loader JARs without `-sources` or `-dev-shadow` in their names.
