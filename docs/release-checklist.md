# Release checklist

1. Move the pending `CHANGELOG.md` entries under the new version and date.
2. Write the release notes at `.github/release-notes/v<version>.md` (the release job requires this file and uses it as the GitHub release body and the mod-store changelog).
3. Confirm the working tree is clean on the release branch.
4. Run full verification:
   ```powershell
   .\gradlew.bat clean build
   .\scripts\build-all-version-bands.ps1 -SkipSmoke
   .\scripts\prod-smoke-all.ps1
   .\scripts\check-release-artifacts.ps1 -SkipBuild
   ```
5. Confirm `docs/SUPPORT.md` matches the shipped bands and loaders.
6. Bump `mod_version` in `gradle.properties` if needed (the release job checks the tag matches it).
7. Tag the release: `git tag v<version>` and push the tag.

Pushing a `v*` tag builds, packages, and attaches every artifact to a GitHub release,
then (once configured) publishes each Minecraft line to Modrinth and CurseForge.

## Mod-store publishing (one-time setup)

Publishing to Modrinth and CurseForge is automated by the `publish-mods` job, but it
stays dormant until you set it up once:

1. Create the project on each site by hand (name, description, logo, categories). Reuse
   the copy in [store-listing.md](store-listing.md). A project cannot be created over the
   API, and CurseForge requires a one-time approval.
2. Add the identifiers as **repository variables**: `MODRINTH_ID` (project slug or id) and
   `CURSEFORGE_ID` (numeric project id).
3. Add the credentials as **repository secrets**: `MODRINTH_TOKEN` and `CURSEFORGE_TOKEN`.

After that, every `v*` tag publishes one version per Minecraft line (each bundling its
Forge, Fabric, and NeoForge files) with the release notes as the changelog. Configure
only one platform and the other is skipped automatically.
