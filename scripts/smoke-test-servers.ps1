[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$gradleWrapper = Join-Path $projectRoot 'gradlew.bat'

foreach ($loader in @('forge', 'fabric')) {
    $runDirectory = Join-Path $projectRoot "$loader\run"
    $logFile = Join-Path $runDirectory 'logs\latest.log'
    $stdoutFile = Join-Path $runDirectory 'smoke-test.stdout.log'
    $stderrFile = Join-Path $runDirectory 'smoke-test.stderr.log'
    $stdinFile = Join-Path $runDirectory 'smoke-test.stdin.txt'
    New-Item -ItemType Directory -Path $runDirectory -Force | Out-Null
    [System.IO.File]::WriteAllText((Join-Path $runDirectory 'eula.txt'), "eula=true`r`n", [System.Text.Encoding]::ASCII)
    [System.IO.File]::WriteAllText($stdinFile, "stop`r`n", [System.Text.Encoding]::ASCII)
    Remove-Item -LiteralPath $logFile, $stdoutFile, $stderrFile -Force -ErrorAction SilentlyContinue

    Write-Host "Starting the $loader dedicated server smoke test..."
    $command = "call `"$gradleWrapper`" :${loader}:runServer --no-daemon --console=plain < `"$stdinFile`""
    $process = Start-Process -FilePath 'cmd.exe' -ArgumentList '/d', '/c', $command `
        -WorkingDirectory $projectRoot -RedirectStandardOutput $stdoutFile `
        -RedirectStandardError $stderrFile -WindowStyle Hidden -PassThru

    $deadline = (Get-Date).AddMinutes(3)
    $started = $false
    $stopped = $false
    try {
        while ((Get-Date) -lt $deadline) {
            if (Test-Path -LiteralPath $logFile) {
                $log = Get-Content -LiteralPath $logFile -Raw
                $started = $log -match 'Done \([^)]+\)! For help'
                $stopped = $log -match 'Item clearing scheduler stopped'
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
        if (-not $process.HasExited) {
            & taskkill.exe /PID $process.Id /T /F 2>$null | Out-Null
        }
    }

    if (-not ($started -and $stopped)) {
        $details = if (Test-Path -LiteralPath $stderrFile) {
            Get-Content -LiteralPath $stderrFile -Tail 30 | Out-String
        } else {
            'No stderr log was produced.'
        }
        throw "$loader dedicated server did not complete a clean startup and shutdown.`n$details"
    }

    $log = Get-Content -LiteralPath $logFile -Raw
    if ($log -match '(?im)^.*(?:ERROR|Exception in server tick loop|Failed to start).*$') {
        throw "$loader dedicated server log contains an error.`n$($Matches[0])"
    }

    if ($log -notmatch 'Item clearing scheduler started with intervalTicks=') {
        throw "$loader dedicated server did not start the item clearing scheduler."
    }

    Write-Host "$loader dedicated server started and stopped successfully."
}
