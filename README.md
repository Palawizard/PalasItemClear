# Palas Item Clear

Server-side Minecraft mod that periodically removes dropped items after configurable, colored chat warnings.

## Installation

Use the artifact matching your Minecraft 1.20.1 server loader.

- Fabric: install Fabric API and Architectury API.
- Forge: install Architectury API.

The mod is server-side only; players do not need it on their clients.

## Stack

- Minecraft 1.20.1
- Forge and Fabric via Architectury
- Java 17
- Gradle

## Run

Use a Java 17 or newer JDK.

```powershell
.\gradlew.bat :forge:runServer
.\gradlew.bat :fabric:runServer
```

Build both loader artifacts:

```powershell
.\gradlew.bat build
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
