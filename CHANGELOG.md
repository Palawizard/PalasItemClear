# Changelog

All notable changes to Pala's Item Clear are documented in this file.

## [0.1.2] - 2026-10-30

### Fixed

- Lag spikes during clears on Minecraft 1.21.9 and newer: the recovery bin no longer asks Mojang's profile service for owner names on the server thread. Names now come from online players and the local user cache only.
- Items thrown by mobs (piglin bartering, villagers, allays) are no longer treated as player-owned. `excludePlayerOwnedItems` now protects only items dropped by players, and mob-thrown items are stored in the recovery bin without an owner.
- Minecraft 1.21.5-1.21.8: the former `1.21.10-fabric` JAR loaded there but called APIs that only exist from 1.21.9, so clearing an item dropped by a player or a mob could crash the server with `NoSuchMethodError`. The former `1.21.10-forge` and `1.21.10-neoforge` JARs required Forge 60 and NeoForge 21.10, so they never loaded on these versions. All three loaders now have dedicated JARs.
- Recovery-bin deposits no longer slow down with the number of stored stacks; large farm clears (thousands of non-stackable items) stay fast.

### Added

- Item tags in `filters.excludedItems`, written as `#namespace:path` (for example `#minecraft:logs`).
- `filters.minAgeTicksOverrides`: per-item or per-tag minimum age that replaces `minAgeTicks` for matching items. Existing configuration files gain an empty `{}` entry on the next load.
- Separate artifacts for Minecraft 1.21.5-1.21.8: `1.21.8-fabric` and `1.21.8-neoforge` (1.21.5-1.21.8), `1.21.5-forge` (Forge 55), and `1.21.8-forge` (1.21.6-1.21.8, Forge 56-58).

### Changed

- The `1.21.10-*` artifacts now cover Minecraft 1.21.9-1.21.10, including Forge 59 and NeoForge 21.9, which previously had no loadable JAR.
- Documented how to keep only some warnings (`"warningSeconds": [30]`) or disable them (`[]`).

### Removed

- README claim of a guardrail against runaway passes, which the mod never implemented.

## [0.1.1] - 2026-07-08

### Added

- Configurable item clearing on the server thread, with a five-minute default interval and item, dimension, age, custom-name, and player-ownership filters.
- Colored chat warnings at 60, 30, and 5 seconds before each clear, all configurable.
- Permission-gated `/palasitemclear` administration commands for status, manual clear, pause, resume, reset, reload, and live settings.
- In-memory recovery bin with a vanilla chest interface (`/palasitemclear bin [player]`).
- Separate, real-server-verified artifacts for Forge, Fabric, and NeoForge across every Minecraft band from 1.20.1 to 26.2.
- MIT licensing.
