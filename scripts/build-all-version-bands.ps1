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

function Get-JdkHome {
    # Resolve a JDK home by major version: GitHub Actions (JAVA_HOME_<v>_X64) first,
    # then common local install locations.
    param([int]$Version, [switch]$Optional)
    $ci = [Environment]::GetEnvironmentVariable("JAVA_HOME_${Version}_X64")
    if ($ci -and (Test-Path -LiteralPath $ci)) { return $ci }
    $candidates = switch ($Version) {
        17 { @('C:\Program Files\Microsoft\jdk-17.0.18.8-hotspot', 'C:\Program Files\Eclipse Adoptium\jdk-17*') }
        25 { @('C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot', 'C:\Program Files\Eclipse Adoptium\jdk-25*') }
        default { @('C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot', 'C:\Program Files\Eclipse Adoptium\jdk-21*') }
    }
    foreach ($c in $candidates) {
        if ($c -notmatch '[\*\?]' -and (Test-Path -LiteralPath $c)) { return $c }
        $r = Get-ChildItem -Path $c -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($r) { return $r.FullName }
    }
    if ($Optional) { return $null }
    throw "No JDK $Version found."
}

function Invoke-GradleBuild {
    param(
        [string]$WorkingDirectory,
        [int]$JavaVersion,
        [string[]]$ExtraArgs = @('build', '--no-daemon')
    )

    $previousJavaHome = $env:JAVA_HOME
    try {
        # Gradle 8.12 cannot run on Java 25; run the build on <=21 and let the toolchain
        # compile the band's real release. Toolchain paths keep Java 25 discoverable.
        $runJava = if ($JavaVersion -ge 25) { 21 } else { $JavaVersion }
        $env:JAVA_HOME = Get-JdkHome -Version $runJava

        $installPaths = @(17, 21, 25 | ForEach-Object { Get-JdkHome -Version $_ -Optional } | Where-Object { $_ })
        $toolchainArg = "-Dorg.gradle.java.installations.paths=$($installPaths -join ',')"

        Push-Location $WorkingDirectory
        if (Test-Path (Join-Path $WorkingDirectory 'gradlew.bat')) {
            Push-Location $WorkingDirectory
            & .\gradlew.bat @ExtraArgs $toolchainArg
        } else {
            Push-Location $projectRoot
            & .\gradlew.bat -p $WorkingDirectory @ExtraArgs $toolchainArg
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
    & (Join-Path $projectRoot 'scripts\smoke-test-all-version-bands.ps1')
    if ($LASTEXITCODE -ne 0) { throw 'smoke-test-all-version-bands failed' }
    Add-Result -Band 'all' -Loader 'smoke' -Status 'PASS' -Detail 'every supported band and loader'
}

Write-Host ''
$results | Format-Table -AutoSize
$failures = @($results | Where-Object Status -eq 'FAIL')
if ($failures.Count -gt 0) {
    throw "$($failures.Count) build step(s) failed."
}
