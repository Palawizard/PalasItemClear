# Changelog

All notable changes to Pala's Item Clear are documented in this file.

## [0.1.0] - 2026-07-08

### Added

- Configurable item clearing on the server thread, with a five-minute default interval and item, dimension, age, custom-name, and player-ownership filters.
- Colored chat warnings at 60, 30, and 5 seconds before each clear, all configurable.
- Permission-gated `/palasitemclear` administration commands for status, manual clear, pause, resume, reset, reload, and live settings.
- In-memory recovery bin with a vanilla chest interface (`/palasitemclear bin [player]`).
- Separate, real-server-verified artifacts for Forge, Fabric, and NeoForge across every Minecraft band from 1.20.1 to 26.2.
- MIT licensing.
