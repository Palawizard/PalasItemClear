[CmdletBinding()]
param(
    [switch]$SkipRoot,
    [switch]$SkipSmoke,
    [string[]]$OnlyBands
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$matrixPath = Join-Path $projectRoot 'versions\version-matrix.json'
$matrix = Get-Content -LiteralPath $matrixPath -Raw | ConvertFrom-Json
$results = New-Object System.Collections.Generic.List[object]

function Add-Result {
    param([string]$Band, [string]$Loader, [string]$Status, [string]$Detail)
    $results.Add([pscustomobject]@{ Band = $Band; Loader = $Loader; Status = $Status; Detail = $Detail })
    $color = switch ($Status) { 'PASS' { 'Green' } 'FAIL' { 'Red' } default { 'Yellow' } }
    Write-Host "[$Status] $Band / $Loader - $Detail" -ForegroundColor $color
}

function Invoke-GradleBuild {
    param(
        [string]$WorkingDirectory,
        [int]$JavaVersion,
        [string[]]$ExtraArgs = @('build', '--no-daemon')
    )

    $previousJavaHome = $env:JAVA_HOME
    try {
        if ($JavaVersion -eq 17) {
            $candidates = @(
                'C:\Program Files\Microsoft\jdk-17.0.18.8-hotspot',
                'C:\Program Files\Eclipse Adoptium\jdk-17*'
            )
        } elseif ($JavaVersion -ge 25) {
            $candidates = @('C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot')
        } else {
            $candidates = @('C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot')
        }

        foreach ($candidate in $candidates) {
            if ($candidate -notmatch '[\*\?]' -and (Test-Path -LiteralPath $candidate)) {
                $env:JAVA_HOME = $candidate
                break
            }

            $resolved = Get-ChildItem -Path $candidate -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
            if ($resolved) {
                $env:JAVA_HOME = $resolved.FullName
                break
            }
        }

        Push-Location $WorkingDirectory
        if (Test-Path '.\gradlew.bat') {
            & .\gradlew.bat @ExtraArgs
        } else {
            & (Join-Path $projectRoot 'gradlew.bat') -p $WorkingDirectory @ExtraArgs
        }
        if ($LASTEXITCODE -ne 0) { throw "Gradle failed with exit code $LASTEXITCODE" }
    } finally {
        Pop-Location
        $env:JAVA_HOME = $previousJavaHome
    }
}

if (-not $SkipRoot) {
    try {
        Invoke-GradleBuild -WorkingDirectory $projectRoot -JavaVersion 17
        Add-Result -Band '1.20.1' -Loader 'forge+fabric' -Status 'PASS' -Detail 'root multiloader build'
    } catch {
        Add-Result -Band '1.20.1' -Loader 'forge+fabric' -Status 'FAIL' -Detail $_.Exception.Message
    }
}

foreach ($band in $matrix.bands) {
    if ($OnlyBands -and ($band.id -notin $OnlyBands)) { continue }

    foreach ($loaderName in @('neoforge', 'fabric', 'forge')) {
        $loader = $band.loaders.$loaderName
        if (-not $loader) { continue }

        $path = $loader.path
        if ($path -in @('forge', 'fabric')) {
            continue
        }

        $fullPath = Join-Path $projectRoot ($path -replace '/', '\')
        if (-not (Test-Path $fullPath)) {
            Add-Result -Band $band.id -Loader $loaderName -Status 'SKIP' -Detail "missing $path"
            continue
        }

        try {
            Invoke-GradleBuild -WorkingDirectory $fullPath -JavaVersion $band.java
            Add-Result -Band $band.id -Loader $loaderName -Status 'PASS' -Detail $loader.artifact
        } catch {
            Add-Result -Band $band.id -Loader $loaderName -Status 'FAIL' -Detail $_.Exception.Message
        }
    }
}

if (-not $SkipSmoke) {
    try {
        & (Join-Path $projectRoot 'scripts\smoke-test-servers.ps1')
        if ($LASTEXITCODE -ne 0) { throw "smoke-test-servers failed" }
        Add-Result -Band '1.20.1' -Loader 'smoke' -Status 'PASS' -Detail 'Forge/Fabric dedicated servers'
    } catch {
        Add-Result -Band '1.20.1' -Loader 'smoke' -Status 'FAIL' -Detail $_.Exception.Message
    }

    try {
        & (Join-Path $projectRoot 'scripts\smoke-test-neoforge.ps1')
        if ($LASTEXITCODE -ne 0) { throw "smoke-test-neoforge failed" }
        Add-Result -Band '1.21-1.21.4' -Loader 'smoke' -Status 'PASS' -Detail 'NeoForge 1.21.1 dedicated server'
    } catch {
        Add-Result -Band '1.21-1.21.4' -Loader 'smoke' -Status 'FAIL' -Detail $_.Exception.Message
    }
}

Write-Host ''
$results | Format-Table -AutoSize
$failures = @($results | Where-Object Status -eq 'FAIL')
if ($failures.Count -gt 0) {
    throw "$($failures.Count) build step(s) failed."
}
