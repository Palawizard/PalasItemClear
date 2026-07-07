[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [string]$BandId,
    [Parameter(Mandatory)]
    [ValidateSet('forge', 'fabric', 'neoforge')]
    [string]$Loader,
    [switch]$SkipBuild,
    [switch]$Fresh,
    [int]$ServerPort = 25566,
    [int]$RconPort = 25576,
    [int]$TimeoutMinutes = 8
)

# Production smoke test: install a real dedicated server for one version band and
# loader, drop the packaged release JAR (plus required companion mods) into mods/,
# boot the server, assert the item clearing scheduler starts, then stop it over RCON
# and assert a clean shutdown. Unlike dev-run (gradlew runServer) smoke, this exercises
# the artifact players actually install, so results are stable across loader toolchains.

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$matrix = Get-Content -LiteralPath (Join-Path $projectRoot 'versions\version-matrix.json') -Raw | ConvertFrom-Json
$band = $matrix.bands | Where-Object { $_.id -eq $BandId } | Select-Object -First 1
if (-not $band) { throw "Unknown version band '$BandId'." }
$loaderConfig = $band.loaders.$Loader
if (-not $loaderConfig) { throw "Loader '$Loader' is not defined for band '$BandId'." }

$modVersion = '0.1.0'
$architecturyVersion = '9.2.14'
$rconPassword = 'palasitemclear-smoke'
$cacheRoot = Join-Path $projectRoot 'build\prod-smoke'
$serverDir = Join-Path $cacheRoot "$BandId-$Loader"

function Resolve-JavaExe {
    param([int]$JavaVersion)
    # On CI (GitHub Actions setup-java) the JDKs live at JAVA_HOME_<ver>_X64.
    $ciHome = [Environment]::GetEnvironmentVariable("JAVA_HOME_${JavaVersion}_X64")
    if ($ciHome) {
        $exe = Join-Path $ciHome 'bin\java.exe'
        if (Test-Path -LiteralPath $exe) { return $exe }
        $exe = Join-Path $ciHome 'bin/java'
        if (Test-Path -LiteralPath $exe) { return $exe }
    }
    $candidates = switch ($JavaVersion) {
        17 { @('C:\Program Files\Microsoft\jdk-17.0.18.8-hotspot', 'C:\Program Files\Eclipse Adoptium\jdk-17*') }
        25 { @('C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot', 'C:\Program Files\Eclipse Adoptium\jdk-25*') }
        default { @('C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot', 'C:\Program Files\Eclipse Adoptium\jdk-21*') }
    }
    foreach ($candidate in $candidates) {
        $javaHome = $null
        if ($candidate -notmatch '[\*\?]' -and (Test-Path -LiteralPath $candidate)) {
            $javaHome = $candidate
        } else {
            $resolved = Get-ChildItem -Path $candidate -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
            if ($resolved) { $javaHome = $resolved.FullName }
        }
        if ($javaHome) {
            $exe = Join-Path $javaHome 'bin\java.exe'
            if (Test-Path -LiteralPath $exe) { return $exe }
        }
    }
    throw "No JDK $JavaVersion found for band '$BandId'."
}

function Get-File {
    param([string]$Url, [string]$Destination)
    if (Test-Path -LiteralPath $Destination) { return }
    New-Item -ItemType Directory -Path (Split-Path -Parent $Destination) -Force | Out-Null
    Write-Host "  download $([System.IO.Path]::GetFileName($Destination))"
    Invoke-WebRequest -Uri $Url -OutFile $Destination -UseBasicParsing -TimeoutSec 300
}

function Get-ModJar {
    $relative = $loaderConfig.path -replace '/', '\'
    $libs = Join-Path (Join-Path $projectRoot $relative) 'build\libs'
    $jar = Get-ChildItem -LiteralPath $libs -Filter "$($loaderConfig.artifact)-*.jar" -File -ErrorAction Stop |
        Where-Object { $_.Name -notmatch '-(sources|dev-shadow|shadow)\.jar$' } |
        Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if (-not $jar) { throw "No release JAR found in $libs for $($loaderConfig.artifact)." }
    return $jar.FullName
}

# Companion mods each build genuinely requires at runtime (from build.gradle deps).
function Get-Companions {
    $downloads = Join-Path $cacheRoot '_companions'
    $result = @()
    if ($Loader -eq 'fabric') {
        $api = $loaderConfig.fabricApi
        if (-not $api) { $api = '0.92.9+1.20.1' } # root 1.20.1 fabric
        $dest = Join-Path $downloads "fabric-api-$api.jar"
        Get-File "https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/$api/fabric-api-$api.jar" $dest
        $result += $dest
    }
    # Only the root 1.20.1 line is Architectury-based; version bands are standalone.
    if ($BandId -eq '1.20.1' -and $Loader -in @('forge', 'fabric')) {
        $dest = Join-Path $downloads "architectury-$Loader-$architecturyVersion.jar"
        Get-File "https://maven.architectury.dev/dev/architectury/architectury-$Loader/$architecturyVersion/architectury-$Loader-$architecturyVersion.jar" $dest
        $result += $dest
    }
    return $result
}

function Install-FabricServer {
    $marker = Join-Path $serverDir 'server.jar'
    if (Test-Path -LiteralPath $marker) { return }
    $installers = Invoke-RestMethod -Uri 'https://meta.fabricmc.net/v2/versions/installer' -TimeoutSec 120
    $installerVersion = ($installers | Where-Object { $_.stable } | Select-Object -First 1).version
    if (-not $installerVersion) { $installerVersion = $installers[0].version }
    $loaderVersion = $loaderConfig.loaderVersion
    if (-not $loaderVersion) { $loaderVersion = '0.19.3' } # root 1.20.1 line (see gradle.properties)
    $url = "https://meta.fabricmc.net/v2/versions/loader/$($band.compileTarget)/$loaderVersion/$installerVersion/server/jar"
    Get-File $url $marker
}

function Install-InstallerServer {
    param([string]$Url, [string]$JavaExe)
    $marker = Join-Path $serverDir 'run.bat'
    if (Test-Path -LiteralPath $marker) { return }
    New-Item -ItemType Directory -Path $serverDir -Force | Out-Null
    $installer = Join-Path $cacheRoot "_installers\$(Split-Path -Leaf $Url)"
    Get-File $Url $installer
    Write-Host "  installServer ($BandId/$Loader)..."
    & $JavaExe '-jar' $installer '--installServer' $serverDir *> (Join-Path $serverDir 'install.log')
    if ($LASTEXITCODE -ne 0) { throw "installServer failed (see install.log)." }
}

function Get-ArgsFile {
    $found = Get-ChildItem -LiteralPath (Join-Path $serverDir 'libraries') -Recurse -Filter 'win_args.txt' -File -ErrorAction SilentlyContinue |
        Select-Object -First 1
    if (-not $found) {
        $found = Get-ChildItem -LiteralPath (Join-Path $serverDir 'libraries') -Recurse -Filter 'unix_args.txt' -File -ErrorAction SilentlyContinue |
            Select-Object -First 1
    }
    return $found
}

function Write-ServerConfig {
    Set-Content -LiteralPath (Join-Path $serverDir 'eula.txt') -Value 'eula=true' -Encoding ascii
    Set-Content -LiteralPath (Join-Path $serverDir 'server.properties') -Encoding ascii -Value @(
        'online-mode=false'
        "server-port=$ServerPort"
        'enable-rcon=true'
        "rcon.port=$RconPort"
        "rcon.password=$rconPassword"
        'view-distance=4'
        'simulation-distance=4'
        'sync-chunk-writes=false'
        'level-name=smokeworld'
    )
}

function Set-Mods {
    param([string]$ModJar, [string[]]$Companions)
    $mods = Join-Path $serverDir 'mods'
    if (Test-Path -LiteralPath $mods) { Remove-Item -LiteralPath $mods -Recurse -Force }
    New-Item -ItemType Directory -Path $mods -Force | Out-Null
    Copy-Item -LiteralPath $ModJar -Destination $mods
    foreach ($c in $Companions) { Copy-Item -LiteralPath $c -Destination $mods }
    # Fresh world each run so startup is deterministic.
    Remove-Item -LiteralPath (Join-Path $serverDir 'smokeworld') -Recurse -Force -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath (Join-Path $serverDir 'logs\latest.log') -Force -ErrorAction SilentlyContinue
}

# --- RCON (minimal client) ---
function Read-ExactBytes { param([System.IO.Stream]$Stream, [int]$Count)
    $buffer = [byte[]]::new($Count); $offset = 0
    while ($offset -lt $Count) {
        $read = $Stream.Read($buffer, $offset, $Count - $offset)
        if ($read -le 0) { throw 'RCON connection closed.' }
        $offset += $read
    }
    return $buffer
}
function Send-RconPacket { param([System.IO.Stream]$Stream, [int]$RequestId, [int]$Type, [string]$Body)
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($Body)
    $len = 4 + 4 + $bytes.Length + 2
    $packet = [byte[]]::new(4 + $len)
    [Array]::Copy([BitConverter]::GetBytes($len), 0, $packet, 0, 4)
    [Array]::Copy([BitConverter]::GetBytes($RequestId), 0, $packet, 4, 4)
    [Array]::Copy([BitConverter]::GetBytes($Type), 0, $packet, 8, 4)
    [Array]::Copy($bytes, 0, $packet, 12, $bytes.Length)
    $Stream.Write($packet, 0, $packet.Length); $Stream.Flush()
}
function Read-RconPacketId { param([System.IO.Stream]$Stream)
    $lengthBytes = Read-ExactBytes -Stream $Stream -Count 4
    $payload = Read-ExactBytes -Stream $Stream -Count ([BitConverter]::ToInt32($lengthBytes, 0))
    return [BitConverter]::ToInt32($payload, 0)
}
function Stop-ServerWithRcon {
    $client = [System.Net.Sockets.TcpClient]::new()
    try {
        $client.Connect('127.0.0.1', $RconPort)
        $stream = $client.GetStream()
        Send-RconPacket -Stream $stream -RequestId 1 -Type 3 -Body $rconPassword
        if ((Read-RconPacketId -Stream $stream) -ne 1) { throw 'RCON auth failed.' }
        Send-RconPacket -Stream $stream -RequestId 2 -Type 2 -Body 'stop'
        Read-RconPacketId -Stream $stream | Out-Null
    } finally { $client.Dispose() }
}

function Invoke-Server {
    param([string]$JavaExe, [string[]]$LaunchArgs)
    $logFile = Join-Path $serverDir 'logs\latest.log'
    $stdout = Join-Path $serverDir 'smoke.stdout.log'
    $stderr = Join-Path $serverDir 'smoke.stderr.log'
    Remove-Item -LiteralPath $stdout, $stderr -Force -ErrorAction SilentlyContinue

    Write-Host "Starting $BandId/$Loader dedicated server (real JAR)..."
    $process = Start-Process -FilePath $JavaExe -ArgumentList $LaunchArgs -WorkingDirectory $serverDir `
        -RedirectStandardOutput $stdout -RedirectStandardError $stderr -WindowStyle Hidden -PassThru

    $deadline = (Get-Date).AddMinutes($TimeoutMinutes)
    $started = $false; $stopped = $false; $stopSent = $false; $rconTries = 0
    try {
        while ((Get-Date) -lt $deadline) {
            $active = if (Test-Path -LiteralPath $logFile) { $logFile } elseif (Test-Path -LiteralPath $stdout) { $stdout } else { $null }
            if ($active) {
                $log = Get-Content -LiteralPath $active -Raw -ErrorAction SilentlyContinue
                $started = $log -match 'Done \([^)]+\)! For help'
                $stopped = $log -match 'Item clearing scheduler stopped'
                if ($started -and -not $stopSent -and $rconTries -lt 20) {
                    # Retry: RCON listener may lag the "Done" line.
                    try { Stop-ServerWithRcon; $stopSent = $true }
                    catch { $rconTries++; Start-Sleep -Milliseconds 500 }
                }
                if ($started -and $stopped) { break }
            }
            if ($process.HasExited) { break }
            Start-Sleep -Seconds 1
        }
    } finally {
        if ($stopped) { $process.WaitForExit(30000) | Out-Null }
        if (-not $process.HasExited) { & taskkill.exe /PID $process.Id /T /F 2>$null | Out-Null }
    }

    $log = if (Test-Path -LiteralPath $logFile) { Get-Content -LiteralPath $logFile -Raw } else { Get-Content -LiteralPath $stdout -Raw -ErrorAction SilentlyContinue }
    if (-not $started) { throw "did not reach 'Done! For help'.`n$((Get-Content -LiteralPath $stderr -Tail 30 -ErrorAction SilentlyContinue) -join [Environment]::NewLine)" }
    if ($log -notmatch 'Item clearing scheduler started with intervalTicks=') { throw 'mod loaded but scheduler never started (mod not applied).' }
    if (-not $stopped) { throw 'server did not stop the scheduler cleanly.' }
    Write-Host "$BandId/$Loader OK: mod loaded, scheduler started and stopped on a real server."
}

# --- main ---
if (-not $SkipBuild) {
    $relative = $loaderConfig.path -replace '/', '\'
    $versionRoot = Join-Path $projectRoot $relative
    $javaHomePrev = $env:JAVA_HOME
    $env:JAVA_HOME = Split-Path -Parent (Split-Path -Parent (Resolve-JavaExe -JavaVersion $band.java))
    try {
        if ($loaderConfig.path -in @('forge', 'fabric')) {
            & (Join-Path $projectRoot 'gradlew.bat') ":$($loaderConfig.path):build" '--no-daemon'
        } else {
            & (Join-Path $projectRoot 'gradlew.bat') '-p' $versionRoot 'build' '--no-daemon'
        }
        if ($LASTEXITCODE -ne 0) { throw "Gradle build failed for $BandId/$Loader." }
    } finally { $env:JAVA_HOME = $javaHomePrev }
}

if ($Fresh -and (Test-Path -LiteralPath $serverDir)) { Remove-Item -LiteralPath $serverDir -Recurse -Force }
New-Item -ItemType Directory -Path $serverDir -Force | Out-Null

$javaExe = Resolve-JavaExe -JavaVersion $band.java
$modJar = Get-ModJar
$companions = Get-Companions

if ($Loader -eq 'fabric') {
    Install-FabricServer
    Write-ServerConfig
    Set-Mods -ModJar $modJar -Companions $companions
    Invoke-Server -JavaExe $javaExe -LaunchArgs @('-Xmx2G', '-jar', 'server.jar', 'nogui')
} else {
    if ($Loader -eq 'neoforge') {
        if ($BandId -eq '1.20.1') {
            $url = "https://maven.neoforged.net/releases/net/neoforged/forge/$($loaderConfig.neoVersion)/forge-$($loaderConfig.neoVersion)-installer.jar"
        } else {
            $url = "https://maven.neoforged.net/releases/net/neoforged/neoforge/$($loaderConfig.neoVersion)/neoforge-$($loaderConfig.neoVersion)-installer.jar"
        }
    } else {
        $forgeVersion = $loaderConfig.forgeVersion
        if (-not $forgeVersion) { $forgeVersion = '47.4.10' } # root 1.20.1 line (see gradle.properties)
        $fv = "$($band.compileTarget)-$forgeVersion"
        $url = "https://maven.minecraftforge.net/net/minecraftforge/forge/$fv/forge-$fv-installer.jar"
    }
    Install-InstallerServer -Url $url -JavaExe $javaExe
    Write-ServerConfig
    Set-Mods -ModJar $modJar -Companions $companions
    $argsFile = Get-ArgsFile
    if (-not $argsFile) { throw "No launch args file produced by installer for $BandId/$Loader." }
    $relArgs = $argsFile.FullName.Substring($serverDir.Length).TrimStart('\')
    $launch = @()
    if (Test-Path -LiteralPath (Join-Path $serverDir 'user_jvm_args.txt')) { $launch += '@user_jvm_args.txt' }
    $launch += "@$relArgs"
    $launch += 'nogui'
    Invoke-Server -JavaExe $javaExe -LaunchArgs $launch
}
