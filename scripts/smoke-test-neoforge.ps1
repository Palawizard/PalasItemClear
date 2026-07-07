[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$versionRoot = Join-Path $projectRoot 'versions\1.21.1-neoforge'
$runDirectory = Join-Path $versionRoot 'run'
$logFile = Join-Path $runDirectory 'logs\latest.log'
$stdoutFile = Join-Path $runDirectory 'smoke-test.stdout.log'
$stderrFile = Join-Path $runDirectory 'smoke-test.stderr.log'
$gradleWrapper = Join-Path $projectRoot 'gradlew.bat'
$rconPassword = 'palasitemclear-smoke-test'

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
    $client = [System.Net.Sockets.TcpClient]::new()
    try {
        $client.Connect('127.0.0.1', 25576)
        $stream = $client.GetStream()
        Send-RconPacket -Stream $stream -RequestId 1 -Type 3 -Body $rconPassword
        if ((Read-RconPacketId -Stream $stream) -ne 1) {
            throw 'RCON authentication failed.'
        }
        Send-RconPacket -Stream $stream -RequestId 2 -Type 2 -Body 'stop'
        Read-RconPacketId -Stream $stream | Out-Null
    } finally {
        $client.Dispose()
    }
}

New-Item -ItemType Directory -Path $runDirectory -Force | Out-Null
Set-Content -LiteralPath (Join-Path $runDirectory 'eula.txt') -Value 'eula=true' -Encoding utf8
Set-Content -LiteralPath (Join-Path $runDirectory 'server.properties') -Encoding ascii -Value @(
    'online-mode=false'
    'server-port=25566'
    'enable-rcon=true'
    'rcon.port=25576'
    "rcon.password=$rconPassword"
)
Remove-Item -LiteralPath $logFile, $stdoutFile, $stderrFile -Force -ErrorAction SilentlyContinue

Write-Host 'Starting the NeoForge 1.21.1 dedicated server smoke test...'
$command = "call `"$gradleWrapper`" -p `"$versionRoot`" runServer --no-daemon --console=plain > `"$stdoutFile`" 2> `"$stderrFile`""
$startInfo = [System.Diagnostics.ProcessStartInfo]::new()
$startInfo.FileName = 'cmd.exe'
$startInfo.Arguments = "/d /c $command"
$startInfo.WorkingDirectory = $projectRoot
$startInfo.UseShellExecute = $false
$startInfo.CreateNoWindow = $true
$startInfo.RedirectStandardInput = $true
$process = [System.Diagnostics.Process]::new()
$process.StartInfo = $startInfo
if (-not $process.Start()) {
    throw 'Could not start the NeoForge Gradle process.'
}

$deadline = (Get-Date).AddMinutes(4)
$started = $false
$stopped = $false
$stopSent = $false
try {
    while ((Get-Date) -lt $deadline) {
        if (Test-Path -LiteralPath $logFile) {
            $log = Get-Content -LiteralPath $logFile -Raw
            $started = $log -match 'Done \([^)]+\)! For help'
            $stopped = $log -match 'Item clearing scheduler stopped'
            if ($started -and -not $stopSent) {
                Stop-ServerWithRcon
                $stopSent = $true
            }
            if ($started -and $stopped) {
                break
            }
        }

        if ($process.HasExited) {
            break
        }

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
    throw "NeoForge 1.21.1 did not complete a clean startup and shutdown.`n$details"
}

$log = Get-Content -LiteralPath $logFile -Raw
if ($log -match '(?im)^.*(?:ERROR|Exception in server tick loop|Failed to start).*$') {
    throw "NeoForge 1.21.1 server log contains an error.`n$($Matches[0])"
}

Write-Host 'NeoForge 1.21.1 dedicated server started and stopped successfully.'
