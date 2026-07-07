# Release checklist

1. Move the pending `CHANGELOG.md` entries under the new version and date.
2. Confirm the working tree is clean on the release branch.
3. Run full verification:
   ```powershell
   .\gradlew.bat clean build
   .\scripts\build-all-version-bands.ps1 -SkipSmoke
   .\scripts\prod-smoke-all.ps1
   .\scripts\check-release-artifacts.ps1 -SkipBuild
   ```
4. Confirm `docs/SUPPORT.md` matches the shipped bands and loaders.
5. Tag the release: `git tag v<version>` and push the tag.
6. The `v*` tag triggers the CI release job, which builds, smoke-tests, packages, and attaches every artifact.
7. Write English release notes describing user-visible changes.
