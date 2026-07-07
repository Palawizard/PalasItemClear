# Compatibility and porting policy

## Supported matrix

Every row below is a separate release artifact. Install the JAR that matches both your Minecraft version band and server loader. Each line is built, unit-tested, then smoke-verified by installing the packaged JAR on a real dedicated server for that loader and confirming the item clearing scheduler starts and stops cleanly (see `scripts/prod-smoke-all.ps1`). Every row below passed this check.

| Minecraft band | Compile target | Loader | Java | Companion mods | Artifact suffix | Status |
|----------------|----------------|--------|------|----------------|-----------------|--------|
| 1.20.1 | 1.20.1 | Forge 47.4.x | 17 | Architectury API | `1.20.1-forge` | Supported |
| 1.20.1 | 1.20.1 | Fabric 0.19.x | 17 | Fabric API, Architectury API | `1.20.1-fabric` | Supported |
| 1.20.1 | 1.20.1 | NeoForge 47.1.x | 17 | none | `1.20.1-neoforge` | Supported |
| 1.20.2–1.20.4 | 1.20.4 | Forge 49.x | 17 | none | `1.20.4-forge` | Supported |
| 1.20.2–1.20.4 | 1.20.4 | Fabric 0.16.x | 17 | Fabric API | `1.20.4-fabric` | Supported |
| 1.20.2–1.20.4 | 1.20.4 | NeoForge 20.4.x | 17 | none | `1.20.4-neoforge` | Supported |
| 1.20.5–1.20.6 | 1.20.6 | Forge 50.x | 21 | none | `1.20.6-forge` | Supported |
| 1.20.5–1.20.6 | 1.20.6 | Fabric 0.16.x | 21 | Fabric API | `1.20.6-fabric` | Supported |
| 1.20.5–1.20.6 | 1.20.6 | NeoForge 20.6.x | 21 | none | `1.20.6-neoforge` | Supported |
| 1.21–1.21.4 | 1.21.1 | Forge 52.x | 21 | none | `1.21.1-forge` | Supported |
| 1.21–1.21.4 | 1.21.1 | Fabric 0.16.x | 21 | Fabric API | `1.21.1-fabric` | Supported |
| 1.21–1.21.4 | 1.21.1 | NeoForge 21.1.x | 21 | none | `1.21.1-neoforge` | Supported |
| 1.21.5–1.21.10 | 1.21.10 | Forge 60.x | 21 | none | `1.21.10-forge` | Supported |
| 1.21.5–1.21.10 | 1.21.10 | Fabric 0.19.x | 21 | Fabric API | `1.21.10-fabric` | Supported |
| 1.21.5–1.21.10 | 1.21.10 | NeoForge 21.10.x | 21 | none | `1.21.10-neoforge` | Supported |
| 1.21.11 | 1.21.11 | Forge 61.x | 21 | none | `1.21.11-forge` | Supported |
| 1.21.11 | 1.21.11 | Fabric 0.19.x | 21 | Fabric API | `1.21.11-fabric` | Supported |
| 1.21.11 | 1.21.11 | NeoForge 21.11.x | 21 | none | `1.21.11-neoforge` | Supported |
| 26.1–26.2 | 26.2 | Forge 65.x | 25 | none | `26.2-forge` | Supported |
| 26.1–26.2 | 26.2 | Fabric 0.19.x | 25 | Fabric API | `26.2-fabric` | Supported |
| 26.1–26.2 | 26.2 | NeoForge 26.2.x | 25 | none | `26.2-neoforge` | Supported |

Metadata version ranges cover every patch inside each band. A successful load outside these rows does not imply support.

## Artifact policy

- Every Minecraft band and loader combination receives a separate JAR.
- Artifact names use `palas-item-clear-<minecraft>-<loader>-<mod-version>.jar`.
- Source and development-shadow JARs are build outputs, not server release artifacts.
- Release JARs are built with stable file ordering and without source timestamps.
- A Git tag matching `v<mod-version>` triggers the release workflow after all required builds and dedicated-server smoke tests pass.

## Configuration reference

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

`excludedItems` and `excludedDimensions` require full `namespace:id` identifiers. Invalid configuration is backed up and safe defaults are loaded.

## Porting policy

1. Keep every supported line buildable while developing newer ports.
2. Add a new version directory when Minecraft introduces binary or API incompatibilities; do not hide differences behind a falsely broad metadata range.
3. Preserve configuration keys and command behavior when practical. Document unavoidable behavior changes before release.
4. Require compilation, automated tests, artifact inspection, and a dedicated-server smoke test for every supported line.
5. Promote a new line to supported only after feature parity for clearing, configuration, commands, filters, and recovery-bin behavior.

## Maintenance

- Fix correctness, duplication, data-loss, and server-start failures on every supported line where the affected API exists.
- Dependency updates stay within the declared Minecraft band unless a dedicated port is created.
- Removing a line from support requires a documented release note; existing binaries remain available but receive no implied future fixes.
- NeoForge lines for Minecraft 1.20.5 and newer require Java 21. Minecraft 26.x lines require Java 25.

Official references:

- [NeoForge getting started](https://docs.neoforged.net/docs/1.21.1/gettingstarted/)
- [NeoForge versioning](https://docs.neoforged.net/docs/1.21.1/gettingstarted/versioning/)
- [NeoForge Java requirements](https://docs.neoforged.net/user/docs/)
