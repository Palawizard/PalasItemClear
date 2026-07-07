# Compatibility and porting policy

## Supported matrix

| Minecraft | Loader | Java | Status | Artifact suffix |
|-----------|--------|------|--------|-----------------|
| 1.20.1 | Forge 47.4.x | 17 | Supported, primary acceptance target | `1.20.1-forge` |
| 1.20.1 | Fabric Loader 0.19.x | 17 | Supported | `1.20.1-fabric` |
| 1.21.1 | NeoForge 21.1.x | 21 | Supported, separate version line | `1.21.1-neoforge` |

Only combinations listed as supported are release targets. A successful load on another Minecraft patch or loader does not imply support.

## Artifact policy

- Every Minecraft and loader combination receives a separate JAR.
- Artifact names use `palas-item-clear-<minecraft>-<loader>-<mod-version>.jar`.
- Source and development-shadow JARs are build outputs, not server release artifacts.
- Release JARs are built with stable file ordering and without source timestamps.
- A Git tag matching `v<mod-version>` triggers the release workflow after all required builds and dedicated-server smoke tests pass.

## Porting policy

1. Keep the 1.20.1 Forge/Fabric line buildable while developing newer ports.
2. Add a new version directory when Minecraft introduces binary or API incompatibilities; do not hide differences behind a falsely broad metadata range.
3. Preserve configuration keys and command behavior when practical. Document unavoidable behavior changes before release.
4. Require compilation, automated tests, artifact inspection, and a dedicated-server smoke test for every supported line.
5. Promote a new line to supported only after feature parity for clearing, configuration, commands, filters, and recovery-bin behavior.

## Maintenance

- Fix correctness, duplication, data-loss, and server-start failures on every supported line where the affected API exists.
- Dependency updates stay within the declared Minecraft line unless a dedicated port is created.
- Removing a line from support requires a documented release note; existing binaries remain available but receive no implied future fixes.
- NeoForge lines for Minecraft 1.20.5 and newer require Java 21, following the NeoForge runtime requirements.

Official references:

- [NeoForge getting started](https://docs.neoforged.net/docs/1.21.1/gettingstarted/)
- [NeoForge versioning](https://docs.neoforged.net/docs/1.21.1/gettingstarted/versioning/)
- [NeoForge Java requirements](https://docs.neoforged.net/user/docs/)
