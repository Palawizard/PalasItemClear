# Contributing

## Development setup

Palas Item Clear builds with the Gradle wrapper. Install JDK 17, 21, and 25 (for the Minecraft 1.20.x, 1.21.x, and 26.x lines). The build runs on Java 21 and compiles each band with the matching toolchain.

```powershell
.\gradlew.bat clean build
.\scripts\build-all-version-bands.ps1 -SkipSmoke
```

## Verification

Run these checks before opening a pull request:

```powershell
.\gradlew.bat :common:test
.\scripts\prod-smoke-all.ps1
.\scripts\check-release-artifacts.ps1 -SkipBuild
```

`prod-smoke-all.ps1` installs each loader's real dedicated server, drops the packaged JAR plus companions, and asserts the item clearing scheduler starts and stops.

## Workflow

- Open feature work against the `dev` branch.
- Use `type(scope): thing done` commit subjects in English.
- Keep player-facing text, configuration defaults, and documentation in English; no emoji.
- Keep commits focused and do not include generated or local editor files.
- Explain user-visible changes and verification in the pull request.

By contributing, you agree that your work is licensed under the MIT License.
