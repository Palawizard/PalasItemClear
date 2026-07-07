## Summary

Describe the change and its user-visible effect.

## Verification

- [ ] `.\gradlew.bat :common:test`
- [ ] `.\scripts\prod-smoke-all.ps1` (or the affected bands)
- [ ] `.\scripts\check-release-artifacts.ps1 -SkipBuild`
- [ ] Relevant manual checks completed

## Release impact

- [ ] Changelog updated when needed
- [ ] No generated or local editor files included
