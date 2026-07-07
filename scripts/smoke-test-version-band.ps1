[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [string]$BandId,
    [Parameter(Mandatory)]
    [ValidateSet('forge', 'fabric', 'neoforge')]
    [string]$Loader,
    [int]$ServerPort = 25566,
    [int]$RconPort = 25576,
    [int]$TimeoutMinutes = 5
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$matrixPath = Join-Path $projectRoot 'versions\version-matrix.json'
$matrix = Get-Content -LiteralPath $matrixPath -Raw | ConvertFrom-Json
$band = $matrix.bands | Where-Object { $_.id -eq $BandId } | Select-Object -First 1
if (-not $band) {
    throw "Unknown version band '$BandId'."
}

$loaderConfig = $band.loaders.$Loader
if (-not $loaderConfig) {
    throw "Loader '$Loader' is not defined for band '$BandId'."
}

function Set-JavaHome {
    param([int]$JavaVersion)

    $previous = $env:JAVA_HOME
    $candidates = switch ($JavaVersion) {
        17 { @('C:\Program Files\Microsoft\jdk-17.0.18.8-hotspot', 'C:\Program Files\Eclipse Adoptium\jdk-17*') }
        25 { @('C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot') }
        default { @('C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot') }
    }

    foreach ($candidate in $candidates) {
        if ($candidate -notmatch '[\*\?]' -and (Test-Path -LiteralPath $candidate)) {
            $env:JAVA_HOME = $candidate
            return $previous
        }
        $resolved = Get-ChildItem -Path $candidate -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($resolved) {
            $env:JAVA_HOME = $resolved.FullName
            return $previous
        }
    }

    return $previous
}

function Read-ExactBytes {
    param([System.IO.Stream]$Stream, [int]$Count)

    $buffer = [byte[]]::new($Count)
    $offset = 0
    while ($offset -lt $Count) {
        $read = $Stream.Read($buffer, $offset, $Count - $offset)
        if ($read -le 0) {
            throw 'RCON connection closed unexpectedly.'
        }
        $offset += $read
    }
    return $buffer
}

function Send-RconPacket {
    param(
        [System.IO.Stream]$Stream,
        [int]$RequestId,
        [int]$Type,
        [string]$Body
    )

    $bodyBytes = [System.Text.Encoding]::UTF8.GetBytes($Body)
    $payloadLength = 4 + 4 + $bodyBytes.Length + 2
    $packet = [byte[]]::new(4 + $payloadLength)
    [Array]::Copy([BitConverter]::GetBytes($payloadLength), 0, $packet, 0, 4)
    [Array]::Copy([BitConverter]::GetBytes($RequestId), 0, $packet, 4, 4)
    [Array]::Copy([BitConverter]::GetBytes($Type), 0, $packet, 8, 4)
    [Array]::Copy($bodyBytes, 0, $packet, 12, $bodyBytes.Length)
    $Stream.Write($packet, 0, $packet.Length)
    $Stream.Flush()
}

function Read-RconPacketId {
    param([System.IO.Stream]$Stream)

    $lengthBytes = Read-ExactBytes -Stream $Stream -Count 4
    $payloadLength = [BitConverter]::ToInt32($lengthBytes, 0)
    $payload = Read-ExactBytes -Stream $Stream -Count $payloadLength
    return [BitConverter]::ToInt32($payload, 0)
}

function Stop-ServerWithRcon {
    param([string]$Password)

    $client = [System.Net.Sockets.TcpClient]::new()
    try {
        $client.Connect('127.0.0.1', $RconPort)
        $stream = $client.GetStream()
        Send-RconPacket -Stream $stream -RequestId 1 -Type 3 -Body $Password
        if ((Read-RconPacketId -Stream $stream) -ne 1) {
            throw 'RCON authentication failed.'
        }
        Send-RconPacket -Stream $stream -RequestId 2 -Type 2 -Body 'stop'
        Read-RconPacketId -Stream $stream | Out-Null
    } finally {
        $client.Dispose()
    }
}

function Assert-ServerLog {
    param(
        [string]$Label,
        [string]$LogFile
    )

    if (-not (Test-Path -LiteralPath $LogFile)) {
        throw "$Label did not produce a server log."
    }

    $log = Get-Content -LiteralPath $LogFile -Raw
    if ($log -notmatch 'Done \([^)]+\)! For help') {
        throw "$Label did not finish starting the dedicated server."
    }
    if ($log -notmatch 'Item clearing scheduler stopped') {
        throw "$Label did not stop the item clearing scheduler cleanly."
    }
    if ($log -match '(?im)^.*(?:Exception in server tick loop|Failed to start).*$') {
        throw "$Label server log contains a startup failure.`n$($Matches[0])"
    }
    if ($log -notmatch 'Item clearing scheduler started with intervalTicks=') {
        throw "$Label did not start the item clearing scheduler."
    }
}

function Get-GradleInvocation {
    param([string]$VersionRoot)

    $localWrapper = Join-Path $VersionRoot 'gradlew.bat'
    if (Test-Path -LiteralPath $localWrapper) {
        return @{
            Wrapper = $localWrapper
            WorkingDirectory = $VersionRoot
            PrefixArgs = @()
        }
    }

    return @{
        Wrapper = Join-Path $projectRoot 'gradlew.bat'
        WorkingDirectory = $projectRoot
        PrefixArgs = @('-p', "`"$VersionRoot`"")
    }
}

function Invoke-LoomSmoke {
    param(
        [string]$Label,
        [string]$GradleCommand,
        [string]$RunDirectory,
        [string]$VersionRoot = $projectRoot
    )

    $gradle = Get-GradleInvocation -VersionRoot $VersionRoot

    $logFile = Join-Path $RunDirectory 'logs\latest.log'
    $stdoutFile = Join-Path $RunDirectory 'smoke-test.stdout.log'
    $stderrFile = Join-Path $RunDirectory 'smoke-test.stderr.log'
    $stdinFile = Join-Path $RunDirectory 'smoke-test.stdin.txt'
    New-Item -ItemType Directory -Path $RunDirectory -Force | Out-Null
    $modsDirectory = Join-Path $RunDirectory 'mods'
    if (Test-Path -LiteralPath $modsDirectory) {
        Get-ChildItem -LiteralPath $modsDirectory -Filter '*.jar' -File -ErrorAction SilentlyContinue |
            Remove-Item -Force
    }
    [System.IO.File]::WriteAllText((Join-Path $RunDirectory 'eula.txt'), "eula=true`r`n", [System.Text.Encoding]::ASCII)
    [System.IO.File]::WriteAllText($stdinFile, "stop`r`n", [System.Text.Encoding]::ASCII)
    Remove-Item -LiteralPath $logFile, $stdoutFile, $stderrFile -Force -ErrorAction SilentlyContinue

    Write-Host "Starting $Label dedicated server smoke test..."
    $command = "call `"$($gradle.Wrapper)`" $GradleCommand --no-daemon --console=plain < `"$stdinFile`""
    $process = Start-Process -FilePath 'cmd.exe' -ArgumentList '/d', '/c', $command `
        -WorkingDirectory $gradle.WorkingDirectory -RedirectStandardOutput $stdoutFile `
        -RedirectStandardError $stderrFile -WindowStyle Hidden -PassThru

    $deadline = (Get-Date).AddMinutes($TimeoutMinutes)
    $started = $false
    $stopped = $false
    try {
        while ((Get-Date) -lt $deadline) {
            if (Test-Path -LiteralPath $logFile) {
                $log = Get-Content -LiteralPath $logFile -Raw
                $started = $log -match 'Done \([^)]+\)! For help'
                $stopped = $log -match 'Item clearing scheduler stopped'
                if ($started -and $stopped) { break }
            }
            if ($process.HasExited) { break }
            Start-Sleep -Seconds 1
        }
    } finally {
        if (-not $process.HasExited) {
            & taskkill.exe /PID $process.Id /T /F 2>$null | Out-Null
        }
    }

    if (-not ($started -and $stopped)) {
        $details = if (Test-Path -LiteralPath $stderrFile) {
            Get-Content -LiteralPath $stderrFile -Tail 40 | Out-String
        } else {
            'No stderr log was produced.'
        }
        throw "$Label did not complete a clean startup and shutdown.`n$details"
    }

    Assert-ServerLog -Label $Label -LogFile $logFile
    Write-Host "$Label dedicated server started and stopped successfully."
}

function Invoke-RconServerSmoke {
    param(
        [string]$Label,
        [string]$VersionRoot,
        [string]$GradleCommand = 'runServer'
    )

    $runDirectory = Join-Path $VersionRoot 'run'
    $logFile = Join-Path $runDirectory 'logs\latest.log'
    $stdoutFile = Join-Path $runDirectory 'smoke-test.stdout.log'
    $stderrFile = Join-Path $runDirectory 'smoke-test.stderr.log'
    $rconPassword = 'palasitemclear-smoke-test'

    New-Item -ItemType Directory -Path $runDirectory -Force | Out-Null
    $modsDirectory = Join-Path $runDirectory 'mods'
    New-Item -ItemType Directory -Path $modsDirectory -Force | Out-Null
    Get-ChildItem -LiteralPath $modsDirectory -Filter '*.jar' -File -ErrorAction SilentlyContinue |
        Remove-Item -Force
    Set-Content -LiteralPath (Join-Path $runDirectory 'eula.txt') -Value 'eula=true' -Encoding utf8
    Set-Content -LiteralPath (Join-Path $runDirectory 'server.properties') -Encoding ascii -Value @(
        'online-mode=false'
        "server-port=$ServerPort"
        'enable-rcon=true'
        "rcon.port=$RconPort"
        "rcon.password=$rconPassword"
    )
    Remove-Item -LiteralPath $logFile, $stdoutFile, $stderrFile -Force -ErrorAction SilentlyContinue

    Write-Host "Starting $Label dedicated server smoke test..."
    $gradle = Get-GradleInvocation -VersionRoot $VersionRoot
    $command = "call `"$($gradle.Wrapper)`" $GradleCommand --no-daemon --console=plain > `"$stdoutFile`" 2> `"$stderrFile`""
    $startInfo = [System.Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = 'cmd.exe'
    $startInfo.Arguments = "/d /c $command"
    $startInfo.WorkingDirectory = $gradle.WorkingDirectory
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardInput = $true
    $process = [System.Diagnostics.Process]::new()
    $process.StartInfo = $startInfo
    if (-not $process.Start()) {
        throw "Could not start the Gradle process for $Label."
    }

    # Some dev-run plugins (e.g. NeoGradle userdev) do not emit logs/latest.log and
    # write the server log only to the redirected console stream. Fall back to stdout.
    $effectiveLogFile = $logFile

    $deadline = (Get-Date).AddMinutes($TimeoutMinutes)
    $started = $false
    $stopped = $false
    $stopSent = $false
    try {
        while ((Get-Date) -lt $deadline) {
            $activeLog = if (Test-Path -LiteralPath $logFile) { $logFile } elseif (Test-Path -LiteralPath $stdoutFile) { $stdoutFile } else { $null }
            if ($activeLog) {
                $effectiveLogFile = $activeLog
                $log = Get-Content -LiteralPath $activeLog -Raw
                $started = $log -match 'Done \([^)]+\)! For help'
                $stopped = $log -match 'Item clearing scheduler stopped'
                if ($started -and -not $stopSent) {
                    Stop-ServerWithRcon -Password $rconPassword
                    $stopSent = $true
                }
                if ($started -and $stopped) { break }
            }
            if ($process.HasExited) { break }
            Start-Sleep -Seconds 1
        }
    } finally {
        if ($stopped -and -not $process.HasExited) {
            $process.WaitForExit(30000) | Out-Null
        }
        if (-not $process.HasExited) {
            & taskkill.exe /PID $process.Id /T /F 2>$null | Out-Null
        }
    }

    if (-not ($started -and $stopped)) {
        $details = if (Test-Path -LiteralPath $stderrFile) {
            Get-Content -LiteralPath $stderrFile -Tail 40 | Out-String
        } else {
            'No stderr log was produced.'
        }
        throw "$Label did not complete a clean startup and shutdown.`n$details"
    }

    Assert-ServerLog -Label $Label -LogFile $effectiveLogFile
    Write-Host "$Label dedicated server started and stopped successfully."
}

$label = "$BandId/$Loader"
$previousJavaHome = Set-JavaHome -JavaVersion $band.java
try {
    $relativePath = $loaderConfig.path -replace '/', '\'
    if ($BandId -eq '1.20.1' -and $Loader -in @('forge', 'fabric')) {
        $runDirectory = Join-Path $projectRoot "$Loader\run"
        Invoke-LoomSmoke -Label $label -GradleCommand ":${Loader}:runServer" -RunDirectory $runDirectory
        return
    }

    if ($Loader -eq 'neoforge') {
        $versionRoot = Join-Path $projectRoot $relativePath
        Invoke-RconServerSmoke -Label $label -VersionRoot $versionRoot
        return
    }

    if ($Loader -eq 'forge') {
        $versionRoot = Join-Path $projectRoot $relativePath
        Invoke-RconServerSmoke -Label $label -VersionRoot $versionRoot
        return
    }

    $versionRoot = Join-Path $projectRoot $relativePath
    $runDirectory = Join-Path $versionRoot 'run'
    Invoke-LoomSmoke -Label $label -GradleCommand 'runServer' -RunDirectory $runDirectory -VersionRoot $versionRoot
} finally {
    $env:JAVA_HOME = $previousJavaHome
}
