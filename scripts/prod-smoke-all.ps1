[CmdletBinding()]
param(
    [string[]]$OnlyBands,
    [ValidateSet('forge', 'fabric', 'neoforge')]
    [string[]]$OnlyLoaders,
    [switch]$SkipBuild,
    [switch]$Fresh
)

# Run the production-JAR dedicated-server smoke test for every supported band and
# loader. Installs are cached under build/prod-smoke, and passing combinations are
# recorded in results.json so an interrupted run resumes instead of restarting.

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$matrix = Get-Content -LiteralPath (Join-Path $projectRoot 'versions\version-matrix.json') -Raw | ConvertFrom-Json
$bandScript = Join-Path $PSScriptRoot 'prod-smoke.ps1'
$resultsPath = Join-Path $projectRoot 'build\prod-smoke\results.json'

$prior = @{}
if ((Test-Path -LiteralPath $resultsPath) -and -not $Fresh) {
    foreach ($r in (Get-Content -LiteralPath $resultsPath -Raw | ConvertFrom-Json)) {
        $prior["$($r.Band)/$($r.Loader)"] = $r.Status
    }
}

$results = New-Object System.Collections.Generic.List[object]
function Save-Results {
    New-Item -ItemType Directory -Path (Split-Path -Parent $resultsPath) -Force | Out-Null
    $results | ConvertTo-Json | Set-Content -LiteralPath $resultsPath -Encoding utf8
}
function Add-Result {
    param([string]$Band, [string]$Loader, [string]$Status, [string]$Detail)
    $results.Add([pscustomobject]@{ Band = $Band; Loader = $Loader; Status = $Status; Detail = $Detail })
    $color = switch ($Status) { 'PASS' { 'Green' } 'FAIL' { 'Red' } default { 'Yellow' } }
    Write-Host "[$Status] $Band / $Loader - $Detail" -ForegroundColor $color
    Save-Results
}

foreach ($band in $matrix.bands) {
    if ($OnlyBands -and ($band.id -notin $OnlyBands)) { continue }
    foreach ($loaderName in @('fabric', 'neoforge', 'forge')) {
        if ($OnlyLoaders -and ($loaderName -notin $OnlyLoaders)) { continue }
        if (-not $band.loaders.$loaderName) { continue }

        $key = "$($band.id)/$loaderName"
        if ($prior[$key] -eq 'PASS' -and -not $Fresh) {
            Add-Result -Band $band.id -Loader $loaderName -Status 'PASS' -Detail 'cached (previous run)'
            continue
        }

        $smokeArgs = @{ BandId = $band.id; Loader = $loaderName }
        if ($SkipBuild) { $smokeArgs.SkipBuild = $true }
        if ($Fresh) { $smokeArgs.Fresh = $true }
        try {
            & $bandScript @smokeArgs
            Add-Result -Band $band.id -Loader $loaderName -Status 'PASS' -Detail 'real dedicated server'
        } catch {
            Add-Result -Band $band.id -Loader $loaderName -Status 'FAIL' -Detail $_.Exception.Message
        }
    }
}

Write-Host ''
$results | Format-Table -AutoSize
$failures = @($results | Where-Object Status -eq 'FAIL')
if ($failures.Count -gt 0) { throw "$($failures.Count) production smoke test(s) failed." }
Write-Host 'All production smoke tests passed.' -ForegroundColor Green
