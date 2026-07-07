[CmdletBinding()]
param()

& (Join-Path $PSScriptRoot 'smoke-test-version-band.ps1') -BandId '1.20.1' -Loader forge
& (Join-Path $PSScriptRoot 'smoke-test-version-band.ps1') -BandId '1.20.1' -Loader fabric
