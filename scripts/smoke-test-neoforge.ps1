[CmdletBinding()]
param()

& (Join-Path $PSScriptRoot 'smoke-test-version-band.ps1') -BandId '1.21-1.21.4' -Loader neoforge
