## Summary

<!-- What does this change do? -->

## Verification

<!-- Commands you ran and whether they passed -->

- [ ] `.\gradlew.bat clean build`
- [ ] `.\scripts\build-all-version-bands.ps1 -SkipSmoke` (if build or shared code changed)
- [ ] `.\scripts\check-release-artifacts.ps1 -SkipBuild` (if artifacts or packaging changed)
- [ ] `.\scripts\smoke-test-all-version-bands.ps1` (if server lifecycle or loader adapters changed)

## Notes

<!-- Anything reviewers should know -->
