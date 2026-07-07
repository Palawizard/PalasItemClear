[CmdletBinding()]
param(
    [switch]$Force,
    [string]$MatrixPath
)

$ErrorActionPreference = 'Stop'
$scriptRoot = if ($PSScriptRoot) { $PSScriptRoot } else { Split-Path -Parent $MyInvocation.MyCommand.Path }
$projectRoot = Split-Path -Parent $scriptRoot
$matrixPath = if ($MatrixPath) { $MatrixPath } else { Join-Path $projectRoot 'versions\version-matrix.json' }
$matrix = Get-Content -LiteralPath $matrixPath -Raw | ConvertFrom-Json
$sharedRoot = Join-Path $projectRoot 'versions\shared'
$templateRoot = Join-Path $sharedRoot 'templates'

$skipPaths = @(
    'forge',
    'fabric',
    'versions/1.21.1-neoforge',
    'versions/26.2-neoforge',
    'versions/26.2-fabric',
    'versions/26.2-forge'
)

function Write-Utf8NoBom {
    param([string]$Path, [string]$Content)
    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($Path, $Content, $utf8NoBom)
}

function Get-FilterTier {
    param([string]$CompileTarget)
    if ($CompileTarget -match '^26\.') { return 'modern' }
    if ($CompileTarget -eq '1.21.11') { return 'premodern' }
    $parts = $CompileTarget.Split('.')
    if ($parts.Length -lt 2) { return 'components' }
    $minor = [int]$parts[1]
    $patch = if ($parts.Length -gt 2) { [int]$parts[2] } else { 0 }
    if ($minor -eq 20 -and $patch -le 4) { return 'legacy' }
    if ($minor -eq 20 -and $patch -ge 5) { return 'components-legacy-rl' }
    if ($minor -eq 21 -and $patch -ge 5) { return 'components-2' }
    return 'components'
}

function Copy-GradleWrapper {
    param(
        [string]$TargetDir,
        [int]$JavaVersion,
        [string]$LoaderName,
        [bool]$UseModernWrapper = $false
    )
    $source = if ($JavaVersion -ge 25 -or $UseModernWrapper) {
        Join-Path $projectRoot 'versions\26.2-neoforge'
    } else {
        $projectRoot
    }
    foreach ($file in @('gradlew', 'gradlew.bat')) {
        Copy-Item (Join-Path $source $file) (Join-Path $TargetDir $file) -Force
    }
    $wrapperDir = Join-Path $TargetDir 'gradle\wrapper'
    New-Item -ItemType Directory -Path $wrapperDir -Force | Out-Null
    Copy-Item (Join-Path $source 'gradle\wrapper\*') $wrapperDir -Force
}

function Ensure-EmptyDir {
    param([string]$Path)
    if (Test-Path $Path) {
        if (-not $Force) { throw "Path exists (use -Force): $Path" }
        Remove-Item $Path -Recurse -Force
    }
    New-Item -ItemType Directory -Path $Path -Force | Out-Null
}

function Fix-Components2Commands {
    param([string]$CommandsFile)
    if (-not (Test-Path $CommandsFile)) { return }
    $content = Get-Content -LiteralPath $CommandsFile -Raw
    $fixed = $content -replace 'player\.getGameProfile\(\)\.getName\(\)', 'player.getGameProfile().name()'
    Write-Utf8NoBom -Path $CommandsFile -Content $fixed
}

function Copy-LoaderSources {
    param(
        [string]$TargetDir,
        [string]$LoaderName,
        [bool]$LegacyTick = $false,
        [string]$NeoTier = 'standard',
        [string]$FilterTier = 'components',
        [string]$FabricApiVersion = '',
        [string]$MinecraftVersionSpec = ''
    )
    $srcBase = Join-Path $TargetDir 'src\main\java\net\palasitemclear'
    New-Item -ItemType Directory -Path $srcBase -Force | Out-Null

    if ($FilterTier -eq 'modern' -or $FilterTier -eq 'premodern') {
        $canonicalSrc = Join-Path $projectRoot "versions\26.2-$LoaderName\src\main"
        Copy-Item (Join-Path $canonicalSrc 'java\net\palasitemclear\PalasItemClear.java') (Join-Path $srcBase 'PalasItemClear.java') -Force
        Copy-Item (Join-Path $canonicalSrc 'java\net\palasitemclear\PlatformPaths.java') (Join-Path $srcBase 'PlatformPaths.java') -Force
        Copy-Item (Join-Path $canonicalSrc 'java\net\palasitemclear\compat') (Join-Path $srcBase 'compat') -Recurse -Force
        New-Item -ItemType Directory -Path (Join-Path $srcBase 'command') -Force | Out-Null
        Copy-Item (Join-Path $canonicalSrc 'java\net\palasitemclear\command\PalasItemClearCommands.java') (Join-Path $srcBase 'command\PalasItemClearCommands.java') -Force
        $entryClass = switch ($LoaderName) {
            'fabric' { 'PalasItemClearFabric' }
            'forge' { 'PalasItemClearForge' }
            'neoforge' { 'PalasItemClearNeoForge' }
            default { throw "Unsupported loader $LoaderName" }
        }
        New-Item -ItemType Directory -Path (Join-Path $srcBase $LoaderName) -Force | Out-Null
        Copy-Item (Join-Path $canonicalSrc "java\net\palasitemclear\$LoaderName\$entryClass.java") (Join-Path $srcBase "$LoaderName\$entryClass.java") -Force

        $resBase = Join-Path $TargetDir 'src\main\resources'
        New-Item -ItemType Directory -Path (Join-Path $resBase 'META-INF') -Force | Out-Null
        if ($LoaderName -eq 'fabric') {
            Copy-Item (Join-Path $canonicalSrc 'resources\fabric.mod.json') (Join-Path $resBase 'fabric.mod.json') -Force
            $fabricJson = Get-Content -LiteralPath (Join-Path $resBase 'fabric.mod.json') -Raw
            if ($FilterTier -notin @('components-2', 'premodern', 'modern') -and $fabricJson -notmatch '"accessWidener"') {
                Copy-Item (Join-Path $projectRoot 'common\src\main\resources\palasitemclear.accesswidener') (Join-Path $resBase 'palasitemclear.accesswidener') -Force
                $fabricJson = $fabricJson -replace '("entrypoints"\s*:\s*\{[\s\S]*?\n  \},)', "`$1`n  `"accessWidener`": `"palasitemclear.accesswidener`","
            }
            $fabricJson = $fabricJson -replace '"fabric-api": ">=[\d.+A-Za-z]+"', "`"fabric-api`": `">=$FabricApiVersion`""
            $fabricJson = $fabricJson -replace '"minecraft": ">=[^"]+"', "`"minecraft`": `"$MinecraftVersionSpec`""
            Write-Utf8NoBom -Path (Join-Path $resBase 'fabric.mod.json') -Content $fabricJson
        } elseif ($LoaderName -eq 'forge') {
            Copy-Item (Join-Path $canonicalSrc 'resources\META-INF\mods.toml') (Join-Path $resBase 'META-INF\mods.toml') -Force
            $modsToml = Get-Content -LiteralPath (Join-Path $resBase 'META-INF\mods.toml') -Raw
            $modsToml = $modsToml -replace 'loaderVersion = "\[\d+,\)"', 'loaderVersion = "${loader_version_range}"'
            Write-Utf8NoBom -Path (Join-Path $resBase 'META-INF\mods.toml') -Content $modsToml
        } else {
            Copy-Item (Join-Path $canonicalSrc 'resources\META-INF\*') (Join-Path $resBase 'META-INF') -Force
        }
        return
    }

    if ($LoaderName -eq 'neoforge') {
        if ($NeoTier -eq '1201') {
            $tpl = Join-Path $templateRoot 'neoforge-1201'
            Copy-Item (Join-Path $tpl 'PalasItemClearNeoForge.java') (Join-Path $srcBase 'PalasItemClearNeoForge.java.tmp') -Force
            New-Item -ItemType Directory -Path (Join-Path $srcBase 'neoforge') -Force | Out-Null
            Move-Item (Join-Path $srcBase 'PalasItemClearNeoForge.java.tmp') (Join-Path $srcBase 'neoforge\PalasItemClearNeoForge.java') -Force
            Copy-Item (Join-Path (Join-Path $projectRoot 'versions\1.21.1-neoforge\src\main\java\net\palasitemclear') 'PalasItemClear.java') (Join-Path $srcBase 'PalasItemClear.java') -Force
            Copy-Item (Join-Path $tpl 'PlatformPaths.java') (Join-Path $srcBase 'PlatformPaths.java') -Force
            New-Item -ItemType Directory -Path (Join-Path $srcBase 'command') -Force | Out-Null
            Copy-Item (Join-Path $tpl 'PalasItemClearCommands.java') (Join-Path $srcBase 'command\PalasItemClearCommands.java') -Force
            $resBase = Join-Path $TargetDir 'src\main\resources'
            New-Item -ItemType Directory -Path (Join-Path $resBase 'META-INF') -Force | Out-Null
            Copy-Item (Join-Path $tpl 'mods.toml') (Join-Path $resBase 'META-INF\mods.toml') -Force
            return
        }

        $neoSrc = Join-Path $projectRoot 'versions\1.21.1-neoforge\src\main'
        Copy-Item (Join-Path $neoSrc 'java\net\palasitemclear\neoforge') (Join-Path $srcBase 'neoforge') -Recurse -Force
        Copy-Item (Join-Path $neoSrc 'java\net\palasitemclear\PalasItemClear.java') (Join-Path $srcBase 'PalasItemClear.java') -Force
        Copy-Item (Join-Path $neoSrc 'java\net\palasitemclear\PlatformPaths.java') (Join-Path $srcBase 'PlatformPaths.java') -Force
        New-Item -ItemType Directory -Path (Join-Path $srcBase 'command') -Force | Out-Null
        Copy-Item (Join-Path $neoSrc 'java\net\palasitemclear\command\PalasItemClearCommands.java') (Join-Path $srcBase 'command\PalasItemClearCommands.java') -Force
        if ($LegacyTick) {
            Copy-Item (Join-Path $templateRoot 'neoforge-legacy-tick\PalasItemClearNeoForge.java') (Join-Path $srcBase 'neoforge\PalasItemClearNeoForge.java') -Force
        }
        if ($FilterTier -eq 'components-2') {
            Fix-Components2Commands -CommandsFile (Join-Path $srcBase 'command\PalasItemClearCommands.java')
        }
        $resBase = Join-Path $TargetDir 'src\main\resources'
        New-Item -ItemType Directory -Path (Join-Path $resBase 'META-INF') -Force | Out-Null
        Copy-Item (Join-Path $neoSrc 'resources\META-INF\*') (Join-Path $resBase 'META-INF') -Force
        return
    }

    $template = Join-Path $templateRoot "$LoaderName-components"
    New-Item -ItemType Directory -Path (Join-Path $srcBase 'command') -Force | Out-Null
    Copy-Item (Join-Path $template 'PalasItemClear.java') (Join-Path $srcBase 'PalasItemClear.java') -Force
    Copy-Item (Join-Path $template 'PlatformPaths.java') (Join-Path $srcBase 'PlatformPaths.java') -Force
    Copy-Item (Join-Path $template 'PalasItemClearCommands.java') (Join-Path $srcBase 'command\PalasItemClearCommands.java') -Force
    if ($FilterTier -eq 'components-2') {
        Fix-Components2Commands -CommandsFile (Join-Path $srcBase 'command\PalasItemClearCommands.java')
    }
    $entryClass = switch ($LoaderName) {
        'fabric' { 'PalasItemClearFabric' }
        'forge' { 'PalasItemClearForge' }
        default { throw "Unsupported loader $LoaderName" }
    }
    New-Item -ItemType Directory -Path (Join-Path $srcBase $LoaderName) -Force | Out-Null
    Copy-Item (Join-Path $template "$entryClass.java") (Join-Path $srcBase "$LoaderName\$entryClass.java") -Force
    if ($LoaderName -eq 'forge' -and $FilterTier -eq 'components-2') {
        Copy-Item (Join-Path $projectRoot 'versions\26.2-forge\src\main\java\net\palasitemclear\forge\PalasItemClearForge.java') (Join-Path $srcBase 'forge\PalasItemClearForge.java') -Force
    }

    $resBase = Join-Path $TargetDir 'src\main\resources'
    New-Item -ItemType Directory -Path (Join-Path $resBase 'META-INF') -Force | Out-Null
    if ($LoaderName -eq 'fabric') {
        Copy-Item (Join-Path $template 'fabric.mod.json') (Join-Path $resBase 'fabric.mod.json') -Force
        if ($FilterTier -notin @('components-2', 'premodern', 'modern')) {
            Copy-Item (Join-Path $projectRoot 'common\src\main\resources\palasitemclear.accesswidener') (Join-Path $resBase 'palasitemclear.accesswidener') -Force
            $fabricJsonPath = Join-Path $resBase 'fabric.mod.json'
            $fabricJson = Get-Content -LiteralPath $fabricJsonPath -Raw
            $fabricJson = $fabricJson -replace '("entrypoints"\s*:\s*\{[\s\S]*?\n  \},)', "`$1`n  `"accessWidener`": `"palasitemclear.accesswidener`","
            Write-Utf8NoBom -Path $fabricJsonPath -Content $fabricJson
        }
    } else {
        Copy-Item (Join-Path $template 'mods.toml') (Join-Path $resBase 'META-INF\mods.toml') -Force
    }
}

function Scaffold-StandaloneProject {
    param(
        [string]$RelativePath,
        [string]$LoaderName,
        [object]$Band,
        [object]$Loader
    )

    if ($RelativePath -in $skipPaths) {
        Write-Host "Skip canonical $RelativePath"
        return
    }

    $target = Join-Path $projectRoot ($RelativePath -replace '/', '\')
    if ((Test-Path $target) -and -not $Force) {
        Write-Host "Skip existing $RelativePath"
        return
    }

    $slug = ($RelativePath -split '/')[-1]
    $filterTier = Get-FilterTier -CompileTarget $Band.compileTarget
    Ensure-EmptyDir -Path $target
    $useModernWrapper = ($LoaderName -eq 'forge' -and $filterTier -in @('premodern', 'modern'))
    Copy-GradleWrapper -TargetDir $target -JavaVersion $Band.java -LoaderName $LoaderName -UseModernWrapper $useModernWrapper

    $fabricRepoLine = ''
    if ($LoaderName -eq 'fabric') {
        $fabricRepoLine = "        maven { name = 'Fabric'; url = 'https://maven.fabricmc.net/' }`n        maven { url = 'https://maven.architectury.dev/' }`n"
    } elseif ($LoaderName -eq 'forge') {
        $fabricRepoLine = "        maven { url = 'https://maven.minecraftforge.net/' }`n"
    }
    $settings = @"
pluginManagement {
    repositories {
$fabricRepoLine        gradlePluginPortal()
        maven { url = 'https://maven.neoforged.net/releases' }
    }
}

plugins {
    id 'org.gradle.toolchains.foojay-resolver-convention' version '1.0.0'
}

rootProject.name = "palas-item-clear-$slug"
"@
    Write-Utf8NoBom -Path (Join-Path $target 'settings.gradle') -Content $settings

    if ($LoaderName -eq 'neoforge') {
        $legacyTick = $false
        $neoTier = 'standard'
        switch ($Band.id) {
            '1.20.1' {
                $buildTemplate = Join-Path $templateRoot 'neoforge-legacy\build.gradle'
                $neoTier = '1201'
            }
            '1.20.2-1.20.4' {
                $buildTemplate = Join-Path $templateRoot 'neoforge-userdev\build.gradle'
                $legacyTick = $true
            }
            default {
                $buildTemplate = Join-Path $templateRoot 'neoforge-standalone\build.gradle'
            }
        }
        Copy-Item $buildTemplate (Join-Path $target 'build.gradle') -Force
        $props = @"
org.gradle.jvmargs=-Xmx2G
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true

minecraft_version=$($Band.compileTarget)
minecraft_version_range=$($Band.minecraftRange)
neo_version=$($Loader.neoVersion)
loader_version_range=[1,)
filter_tier=$filterTier
java_release=$($Band.java)

mod_id=palasitemclear
mod_name=Palas Item Clear
mod_version=0.1.0
mod_group_id=net.palasitemclear
mod_license=All Rights Reserved
"@
        Write-Utf8NoBom -Path (Join-Path $target 'gradle.properties') -Content $props
        Copy-LoaderSources -TargetDir $target -LoaderName 'neoforge' -LegacyTick $legacyTick -NeoTier $neoTier -FilterTier $filterTier

        if ($neoTier -ne '1201') {
            $modsToml = Get-Content -LiteralPath (Join-Path $target 'src\main\resources\META-INF\neoforge.mods.toml') -Raw
            $modsToml = $modsToml -replace 'javaVersion = "\[\d+,\)"', "javaVersion = `"[$($Band.java),)`""
            Write-Utf8NoBom -Path (Join-Path $target 'src\main\resources\META-INF\neoforge.mods.toml') -Content $modsToml
        } else {
            Write-Utf8NoBom -Path (Join-Path $target 'src\main\resources\META-INF\accesstransformer.cfg') -Content "public net.minecraft.world.entity.item.ItemEntity f_31988_ # thrower`n"
        }
    } elseif ($LoaderName -eq 'fabric') {
        Copy-Item (Join-Path $templateRoot 'fabric-components\build.gradle') (Join-Path $target 'build.gradle') -Force
        $props = @"
org.gradle.jvmargs=-Xmx2G
org.gradle.parallel=true
org.gradle.configuration-cache=false

minecraft_version=$($Band.compileTarget)
minecraft_version_range=$($Band.minecraftRange)
loader_version=$($Loader.loaderVersion)
loom_version=$($Loader.loomVersion)
filter_tier=$filterTier
java_release=$($Band.java)

mod_version=0.1.0
maven_group=net.palasitemclear
fabric_api_version=$($Loader.fabricApi)
yarn_mappings=$($Loader.yarnMappings)
"@
        Write-Utf8NoBom -Path (Join-Path $target 'gradle.properties') -Content $props
        $mcSpec = ($Band.minecraftRange -replace '^\[', '>=') -replace ',', ' <'
        $mcSpec = $mcSpec.TrimEnd(')', ']')
        Copy-LoaderSources -TargetDir $target -LoaderName 'fabric' -FilterTier $filterTier -FabricApiVersion $Loader.fabricApi -MinecraftVersionSpec $mcSpec
    } else {
        $forgeBuildTemplate = if ($filterTier -in @('premodern', 'modern')) {
            Join-Path $templateRoot 'forge-modern\build.gradle'
        } else {
            Join-Path $templateRoot 'forge-components\build.gradle'
        }
        Copy-Item $forgeBuildTemplate (Join-Path $target 'build.gradle') -Force
        $loaderRange = if ($Loader.loaderVersionRange) { $Loader.loaderVersionRange } else { '[47,)' }
        $props = @"
org.gradle.jvmargs=-Xmx2G
org.gradle.caching=true
org.gradle.parallel=true
org.gradle.configuration-cache=false

minecraft_version=$($Band.compileTarget)
minecraft_version_range=$($Band.minecraftRange)
forge_version=$($Loader.forgeVersion)
loader_version_range=$loaderRange
filter_tier=$filterTier
null_thrower=true
java_release=$($Band.java)

mod_version=0.1.0
mod_group_id=net.palasitemclear
mod_id=palasitemclear
mod_name=Palas Item Clear
mod_license=All Rights Reserved
"@
        Write-Utf8NoBom -Path (Join-Path $target 'gradle.properties') -Content $props
        Copy-LoaderSources -TargetDir $target -LoaderName 'forge' -FilterTier $filterTier
    }

    Write-Host "Scaffolded $LoaderName band at $RelativePath (tier=$filterTier, range=$($Band.minecraftRange))"
}

foreach ($band in $matrix.bands) {
    foreach ($loaderName in @('neoforge', 'fabric', 'forge')) {
        $loader = $band.loaders.$loaderName
        if (-not $loader) { continue }
        Scaffold-StandaloneProject -RelativePath $loader.path -LoaderName $loaderName -Band $band -Loader $loader
    }
}

Write-Host 'Scaffold complete.'
