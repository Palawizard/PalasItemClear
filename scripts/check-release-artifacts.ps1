[CmdletBinding()]
param(
    [switch]$SkipBuild,
    [string[]]$OnlyBands,
    # When set, verify already-built JARs found (recursively) under this directory
    # instead of the per-band build/libs output. Used by CI to check downloaded artifacts.
    [string]$ArtifactsRoot
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$matrixPath = Join-Path $projectRoot 'versions\version-matrix.json'
$matrix = Get-Content -LiteralPath $matrixPath -Raw | ConvertFrom-Json

function Get-ReleaseJar {
    param(
        [string]$Directory,
        [string]$Pattern
    )

    if ($ArtifactsRoot) {
        return @(Get-ChildItem -LiteralPath $ArtifactsRoot -Recurse -Filter $Pattern -File -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -notmatch '-(sources|dev-shadow)\.jar$' })
    }

    $libs = Join-Path $projectRoot $Directory
    return @(Get-ChildItem -LiteralPath $libs -Filter $Pattern -File -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notmatch '-(sources|dev-shadow)\.jar$' })
}

function Assert-ReleaseJar {
    param(
        [string]$Label,
        [string]$Directory,
        [string]$Pattern,
        [string[]]$RequiredEntries
    )

    $jars = Get-ReleaseJar -Directory $Directory -Pattern $Pattern
    if ($jars.Count -ne 1) {
        throw "Expected one release JAR for $Label in $Directory ($Pattern), found $($jars.Count)."
    }

    $jar = $jars[0]
    $entries = & jar tf $jar.FullName
    if ($LASTEXITCODE -ne 0) {
        throw "Could not inspect $($jar.FullName)."
    }

    foreach ($requiredEntry in $RequiredEntries) {
        if ($entries -notcontains $requiredEntry) {
            throw "$($jar.Name) is missing $requiredEntry."
        }
    }

    if ($jar.Length -lt 10KB) {
        throw "$($jar.Name) is unexpectedly small."
    }

    Write-Host "$Label artifact verified: $($jar.Name) ($($jar.Length) bytes)"
}

if (-not $SkipBuild -and -not $ArtifactsRoot) {
  & (Join-Path $projectRoot 'scripts\build-all-version-bands.ps1') -SkipSmoke
  if ($LASTEXITCODE -ne 0) {
    throw 'Version-band build failed before artifact verification.'
  }
}

foreach ($band in $matrix.bands) {
    if ($OnlyBands -and ($band.id -notin $OnlyBands)) { continue }

    foreach ($loaderName in @('forge', 'fabric', 'neoforge')) {
        $loader = $band.loaders.$loaderName
        if (-not $loader) { continue }

        $artifactBase = $loader.artifact
        $modVersion = (Get-Content -LiteralPath (Join-Path $projectRoot 'gradle.properties') -Raw |
            Select-String -Pattern 'mod_version=(.+)' -AllMatches).Matches[0].Groups[1].Value.Trim()

        if ($loader.path -eq 'forge') {
            $directory = 'forge\build\libs'
            $pattern = "$artifactBase-$modVersion.jar"
            $metadata = 'META-INF/mods.toml'
        } elseif ($loader.path -eq 'fabric') {
            $directory = 'fabric\build\libs'
            $pattern = "$artifactBase-$modVersion.jar"
            $metadata = 'fabric.mod.json'
        } else {
            $directory = ($loader.path -replace '/', '\') + '\build\libs'
            $pattern = "$artifactBase-$modVersion.jar"
            $metadata = switch ($loaderName) {
                'fabric' { 'fabric.mod.json' }
                'neoforge' {
                    # NeoForge only adopted neoforge.mods.toml in 20.5; 1.20.1 (legacy) and
                    # 1.20.4 (20.4) still ship META-INF/mods.toml.
                    if ($band.id -in @('1.20.1', '1.20.2-1.20.4')) { 'META-INF/mods.toml' } else { 'META-INF/neoforge.mods.toml' }
                }
                default { 'META-INF/mods.toml' }
            }
        }

        Assert-ReleaseJar -Label "$($band.id)/$loaderName" -Directory $directory -Pattern $pattern -RequiredEntries @(
            $metadata,
            'net/palasitemclear/PalasItemClear.class',
            'net/palasitemclear/server/ServerClearController.class'
        )
    }
}
