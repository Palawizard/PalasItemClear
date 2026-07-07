# Contributing

Thanks for helping improve Palas Item Clear.

## Before you start

- Read [README.md](README.md) and [docs/SUPPORT.md](docs/SUPPORT.md) for supported Minecraft bands and loader requirements.
- Open an issue first for large changes so we can agree on scope.
- Keep player-facing text, configuration defaults, and documentation in English.

## Development setup

```powershell
.\gradlew.bat build
.\scripts\build-all-version-bands.ps1 -SkipSmoke
```

Run targeted checks while iterating:

```powershell
.\gradlew.bat :common:test
.\scripts\prod-smoke.ps1 -BandId 1.20.1 -Loader forge
```

## Pull requests

- Target the `dev` branch unless you are preparing a release merge into `main`.
- Keep commits focused. Prefer the format `type(scope): thing done`.
- Include automated verification results in the PR description.
- Do not commit local-only directories such as `.cursor/`, `.vscode/`, or `.idea/`.

## What to verify

Before requesting review, run the checks that match your change:

```powershell
.\gradlew.bat clean build
.\scripts\build-all-version-bands.ps1 -SkipSmoke
.\scripts\check-release-artifacts.ps1 -SkipBuild
```

If you touch server lifecycle or loader adapters, also run the real-server smoke check:

```powershell
.\scripts\prod-smoke-all.ps1
```

## Code style

- Match the surrounding module and loader conventions.
- Put shared domain logic in `common/` or `versions/shared/` instead of duplicating it per loader.
- Add JUnit tests in `common/src/test` for pure logic changes.
