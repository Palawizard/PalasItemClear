[CmdletBinding()]
param(
    [string[]]$OnlyBands,
    [ValidateSet('forge', 'fabric', 'neoforge')]
    [string[]]$OnlyLoaders
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$matrixPath = Join-Path $projectRoot 'versions\version-matrix.json'
$matrix = Get-Content -LiteralPath $matrixPath -Raw | ConvertFrom-Json
$results = New-Object System.Collections.Generic.List[object]
$bandScript = Join-Path $PSScriptRoot 'smoke-test-version-band.ps1'

function Add-Result {
    param([string]$Band, [string]$Loader, [string]$Status, [string]$Detail)
    $results.Add([pscustomobject]@{ Band = $Band; Loader = $Loader; Status = $Status; Detail = $Detail })
    $color = switch ($Status) { 'PASS' { 'Green' } 'FAIL' { 'Red' } default { 'Yellow' } }
    Write-Host "[$Status] $Band / $Loader - $Detail" -ForegroundColor $color
}

foreach ($band in $matrix.bands) {
    if ($OnlyBands -and ($band.id -notin $OnlyBands)) { continue }

    foreach ($loaderName in @('forge', 'fabric', 'neoforge')) {
        if ($OnlyLoaders -and ($loaderName -notin $OnlyLoaders)) { continue }
        if (-not $band.loaders.$loaderName) { continue }

        try {
            & $bandScript -BandId $band.id -Loader $loaderName
            Add-Result -Band $band.id -Loader $loaderName -Status 'PASS' -Detail 'dedicated server startup/shutdown'
        } catch {
            Add-Result -Band $band.id -Loader $loaderName -Status 'FAIL' -Detail $_.Exception.Message
        }
    }
}

Write-Host ''
$results | Format-Table -AutoSize
$failures = @($results | Where-Object Status -eq 'FAIL')
if ($failures.Count -gt 0) {
    throw "$($failures.Count) dedicated-server smoke test(s) failed."
}
