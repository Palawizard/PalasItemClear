[CmdletBinding()]
param(
    [switch]$SkipBuild,
    [switch]$IncludeNeoForge,
    [switch]$NeoForgeOnly
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot

if (-not $SkipBuild) {
    & (Join-Path $projectRoot 'gradlew.bat') clean build
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle build failed with exit code $LASTEXITCODE."
    }
}

$expected = if ($NeoForgeOnly) { @() } else { @(
    @{
        Loader = 'fabric'
        Directory = 'fabric\build\libs'
        Pattern = 'palas-item-clear-1.20.1-fabric-*.jar'
        Metadata = 'fabric.mod.json'
    },
    @{
        Loader = 'forge'
        Directory = 'forge\build\libs'
        Pattern = 'palas-item-clear-1.20.1-forge-*.jar'
        Metadata = 'META-INF/mods.toml'
    }
) }

if ($IncludeNeoForge -or $NeoForgeOnly) {
    $expected += @{
        Loader = 'neoforge'
        Directory = 'versions\1.21.1-neoforge\build\libs'
        Pattern = 'palas-item-clear-1.21.1-neoforge-*.jar'
        Metadata = 'META-INF/neoforge.mods.toml'
    }
}

foreach ($artifact in $expected) {
    $libs = Join-Path $projectRoot $artifact.Directory
    $jars = @(Get-ChildItem -LiteralPath $libs -Filter $artifact.Pattern -File |
        Where-Object { $_.Name -notmatch '-(sources|dev-shadow)\.jar$' })

    if ($jars.Count -ne 1) {
        throw "Expected one release JAR for $($artifact.Loader), found $($jars.Count)."
    }

    $entries = & jar tf $jars[0].FullName
    if ($LASTEXITCODE -ne 0) {
        throw "Could not inspect $($jars[0].FullName)."
    }

    foreach ($requiredEntry in @(
        $artifact.Metadata,
        'net/palasitemclear/PalasItemClear.class',
        'net/palasitemclear/server/ServerClearController.class'
    )) {
        if ($entries -notcontains $requiredEntry) {
            throw "$($jars[0].Name) is missing $requiredEntry."
        }
    }

    if ($jars[0].Length -lt 10KB) {
        throw "$($jars[0].Name) is unexpectedly small."
    }

    Write-Host "$($artifact.Loader) artifact verified: $($jars[0].Name) ($($jars[0].Length) bytes)"
}
