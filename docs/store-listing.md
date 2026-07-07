# Store listing and SEO

Copy-paste source for Modrinth and CurseForge project pages, plus discoverability
metadata for search engines and AI assistants. Keep this in sync with the README.

## Summary (short description)

Use this for the Modrinth "summary" and the CurseForge "summary" fields (both are short,
single-line). Under 256 characters.

```text
Server-side item cleaner that removes dropped items on a timer with colored warnings, powerful filters, and a recovery bin to undo mistakes. A modern, configurable ClearLag alternative for Forge, Fabric, and NeoForge from Minecraft 1.20.1 to 26.2.
```

## Full description (Modrinth / CurseForge body)

Modrinth accepts Markdown directly. On CurseForge, paste into the rich-text editor and
apply headings/lists. This body leads with the primary keywords for search ranking.

```markdown
# Palas Item Clear

**Palas Item Clear** is a lightweight, **server-side** mod that automatically **clears dropped items** on your Minecraft server to **reduce lag** and keep the ground clean. It is a modern, fully configurable alternative to classic **clear-lag / ClearLagg** mods, and it runs on **Forge, Fabric, and NeoForge** across every Minecraft version from **1.20.1 to 26.2**.

Players do **not** need to install anything: the mod is server-side only, and vanilla clients can connect normally.

## Why Palas Item Clear

- **Reduce server lag** caused by piles of dropped item entities on the ground.
- **Fair warnings** so players can pick up loot before a clear: colored chat messages at 60, 30, and 5 seconds (all configurable).
- **Recovery bin** to undo mistakes: recently cleared items are kept briefly in memory and can be restored through a vanilla chest interface, with optional per-player filtering.
- **Precise control** with filters by item, dimension, item age, custom name, and player ownership.
- **No client mod required.** One less thing for your players to install.

## Features

- Automatic item clearing on a configurable timer (five minutes by default)
- Colored, configurable warning messages (MiniMessage-style formatting)
- Filters: excluded items, excluded dimensions, minimum age, named items, player-owned items
- In-game admin commands: status, next clear, clear now, pause, resume, reset, reload, live settings
- Recovery bin: `/palasitemclear bin [player]` opens a chest to restore recently cleared items
- Safe by design: runs on the server thread, only touches loaded levels, and never removes inventories, blocks, or non-item entities

## Commands

`/palasitemclear` (permission level 2) — help, `status`, `next`, `clear`, `bin [player]`, `pause`, `resume`, `reset`, `reload`, `set interval <seconds>`.

## Compatibility

Separate, individually tested builds for Forge, Fabric, and NeoForge, covering every Minecraft band from 1.20.1 to 26.2 with no gaps. Download the file that matches your Minecraft version and loader. Fabric builds require Fabric API.

## Keywords

clear items, clear lag, clearlag, clearlagg, item cleaner, dropped item removal, ground item cleanup, reduce lag, server performance, entity cleanup, auto clear, server-side, recovery bin.
```

## Platform metadata

### Modrinth

- Project type: **Mod**
- Environment: **Server** (client: unsupported / not required)
- Loaders: **Forge, Fabric, NeoForge**
- Categories: **Management**, **Utility**, **Optimization**
- Game versions: 1.20.1 through 26.2 (tag each supported band)
- License: **MIT**
- Links: Source and Issues -> this GitHub repository

### CurseForge

- Categories: **Server Utility**, **Miscellaneous**
- Environment / Mod loader: **Forge, Fabric, NeoForge**
- Game versions: 1.20.1 through 26.2
- License: **MIT**

## GitHub discoverability

Repository description:

```text
Server-side Minecraft mod that clears dropped items to reduce lag, with warnings, filters, and a recovery bin. Forge, Fabric, and NeoForge, 1.20.1 to 26.2.
```

Repository topics:

```text
minecraft, minecraft-mod, forge, fabric, neoforge, server-side, clearlag, clear-lag,
item-cleaner, anti-lag, performance, server-utility, dropped-items, architectury, java
```

## Search-engine and AI notes

- The README, this listing, and the store pages all use the same primary phrasing
  ("server-side item cleaner", "reduce lag", "ClearLag alternative", explicit version
  and loader coverage) so search engines and AI assistants get one consistent answer.
- Prefer concrete, factual claims (exact versions, loaders, "no client mod required")
  over marketing adjectives; they are easier for models to cite accurately.
