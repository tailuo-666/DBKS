param(
    [int]$BackendPort = 8080,
    [int]$FrontendPort = 5173,
    [int]$StartTimeoutSec = 90,
    [switch]$KeepRunning
)

$ErrorActionPreference = 'Stop'

$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$BackendDir = Join-Path $ProjectRoot 'dbksh'
$FrontendDir = Join-Path $ProjectRoot 'dbksf'
$LogDir = Join-Path $ProjectRoot '.bridge-test-logs'
$CategoryQuery = '%E4%BA%8C%E6%89%8B%E4%B9%A6'

$backendProcess = $null
$frontendProcess = $null

function Write-Step {
    param([string]$Message)

    Write-Host "[bridge] $Message" -ForegroundColor Cyan
}

function Write-Info {
    param([string]$Message)

    Write-Host "[bridge] $Message"
}

function Get-ExecutablePath {
    param([string]$Name)

    $candidates = @($Name)
    if ($env:OS -eq 'Windows_NT') {
        $candidates = @("$Name.cmd", "$Name.exe", $Name)
    }

    foreach ($candidate in $candidates) {
        $command = Get-Command $candidate -ErrorAction SilentlyContinue
        if ($command) {
            return $command.Source
        }
    }

    throw "Command not found: $Name"
}

function Test-TcpPort {
    param(
        [string]$HostName,
        [int]$Port
    )

    $client = New-Object System.Net.Sockets.TcpClient

    try {
        $asyncResult = $client.BeginConnect($HostName, $Port, $null, $null)
        $connected = $asyncResult.AsyncWaitHandle.WaitOne(1500, $false)

        if (-not $connected) {
            return $false
        }

        $client.EndConnect($asyncResult)
        return $true
    } catch {
        return $false
    } finally {
        $client.Dispose()
    }
}

function Invoke-JsonRequest {
    param([string]$Url)

    $response = Invoke-WebRequest -UseBasicParsing -Uri $Url -TimeoutSec 8
    return $response.Content | ConvertFrom-Json
}

function Get-BackendProbeUrl {
    param([int]$Port)

    return "http://127.0.0.1:$Port/shop/products/category?category=$CategoryQuery"
}

function Test-BackendApi {
    param([int]$Port)

    try {
        $result = Invoke-JsonRequest -Url (Get-BackendProbeUrl -Port $Port)
        if ($result.code -eq 1) {
            return $result
        }
    } catch {
        return $null
    }

    return $null
}

function Test-FrontendRoot {
    param([int]$Port)

    try {
        $indexResponse = Invoke-WebRequest -UseBasicParsing -Uri "http://127.0.0.1:$Port/index.html" -TimeoutSec 8
        $viteClientResponse = Invoke-WebRequest -UseBasicParsing -Uri "http://127.0.0.1:$Port/@vite/client" -TimeoutSec 8

        if (
            $indexResponse.StatusCode -eq 200 -and
            $indexResponse.Content.ToLower().Contains('<html') -and
            $viteClientResponse.StatusCode -eq 200
        ) {
            return $true
        }
    } catch {
        return $false
    }

    return $false
}

function Test-FrontendBridge {
    param([int]$Port)

    try {
        $result = Invoke-JsonRequest -Url (Get-BackendProbeUrl -Port $Port)
        if ($result.code -eq 1) {
            return $result
        }
    } catch {
        return $null
    }

    return $null
}

function Wait-UntilReady {
    param(
        [string]$Name,
        [scriptblock]$Probe,
        [int]$TimeoutSec
    )

    $startedAt = Get-Date

    while ((New-TimeSpan -Start $startedAt -End (Get-Date)).TotalSeconds -lt $TimeoutSec) {
        $probeResult = & $Probe
        if ($probeResult) {
            return $probeResult
        }

        Start-Sleep -Seconds 2
    }

    throw "$Name start timed out after ${TimeoutSec}s."
}

function Start-ManagedProcess {
    param(
        [string]$Name,
        [string]$FilePath,
        [string[]]$Arguments,
        [string]$WorkingDirectory,
        [string]$StdOutLog,
        [string]$StdErrLog
    )

    Write-Step "Starting $Name"

    return Start-Process `
        -FilePath $FilePath `
        -ArgumentList $Arguments `
        -WorkingDirectory $WorkingDirectory `
        -RedirectStandardOutput $StdOutLog `
        -RedirectStandardError $StdErrLog `
        -PassThru
}

function Show-LogTail {
    param(
        [string]$Path,
        [int]$Lines = 80
    )

    if (Test-Path $Path) {
        Write-Host "----- $Path -----" -ForegroundColor DarkYellow
        Get-Content $Path -Tail $Lines
    }
}

function Cleanup-StartedProcesses {
    if ($KeepRunning) {
        return
    }

    foreach ($proc in @($frontendProcess, $backendProcess)) {
        if ($null -ne $proc) {
            try {
                Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
            } catch {
            }
        }
    }
}

if (-not (Test-Path $BackendDir)) {
    throw "Backend directory not found: $BackendDir"
}

if (-not (Test-Path $FrontendDir)) {
    throw "Frontend directory not found: $FrontendDir"
}

if (-not (Test-Path (Join-Path $FrontendDir 'node_modules'))) {
    throw "Missing dbksf/node_modules. Install frontend dependencies first."
}

$npmPath = Get-ExecutablePath -Name 'npm'
$mvnPath = Get-ExecutablePath -Name 'mvn'

if (-not (Test-TcpPort -HostName '127.0.0.1' -Port 3306)) {
    throw 'MySQL port 3306 is not reachable.'
}

if (-not (Test-TcpPort -HostName '127.0.0.1' -Port 6379)) {
    throw 'Redis port 6379 is not reachable.'
}

New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

$backendOutLog = Join-Path $LogDir 'backend.out.log'
$backendErrLog = Join-Path $LogDir 'backend.err.log'
$frontendOutLog = Join-Path $LogDir 'frontend.out.log'
$frontendErrLog = Join-Path $LogDir 'frontend.err.log'

Remove-Item $backendOutLog, $backendErrLog, $frontendOutLog, $frontendErrLog -ErrorAction SilentlyContinue

try {
    Write-Step "Checking backend on port $BackendPort"
    $backendDirectResult = Test-BackendApi -Port $BackendPort

    if ($backendDirectResult) {
        Write-Info "Found existing backend: http://127.0.0.1:$BackendPort"
    } else {
        $backendArgs = @('-DskipTests', 'spring-boot:run')
        if ($BackendPort -ne 8080) {
            $backendArgs += "-Dspring-boot.run.arguments=--server.port=$BackendPort"
        }

        $backendProcess = Start-ManagedProcess `
            -Name 'dbksh' `
            -FilePath $mvnPath `
            -Arguments $backendArgs `
            -WorkingDirectory $BackendDir `
            -StdOutLog $backendOutLog `
            -StdErrLog $backendErrLog

        $backendDirectResult = Wait-UntilReady `
            -Name 'dbksh' `
            -TimeoutSec $StartTimeoutSec `
            -Probe { Test-BackendApi -Port $BackendPort }
    }

    Write-Step "Checking frontend on port $FrontendPort"
    $frontendRootReady = Test-FrontendRoot -Port $FrontendPort

    if ($frontendRootReady) {
        Write-Info "Found existing frontend: http://127.0.0.1:$FrontendPort"
    } else {
        $frontendArgs = @('run', 'dev', '--', '--host', '127.0.0.1', '--port', "$FrontendPort")

        $frontendProcess = Start-ManagedProcess `
            -Name 'dbksf' `
            -FilePath $npmPath `
            -Arguments $frontendArgs `
            -WorkingDirectory $FrontendDir `
            -StdOutLog $frontendOutLog `
            -StdErrLog $frontendErrLog

        Wait-UntilReady `
            -Name 'dbksf' `
            -TimeoutSec $StartTimeoutSec `
            -Probe { Test-FrontendRoot -Port $FrontendPort } | Out-Null
    }

    Write-Step 'Checking frontend proxy bridge'
    $frontendBridgeResult = Wait-UntilReady `
        -Name 'frontend proxy bridge' `
        -TimeoutSec 20 `
        -Probe { Test-FrontendBridge -Port $FrontendPort }

    $backendCount = @($backendDirectResult.data).Count
    $frontendCount = @($frontendBridgeResult.data).Count

    Write-Host ''
    Write-Host 'Bridge test passed.' -ForegroundColor Green
    Write-Host "backend : http://127.0.0.1:$BackendPort"
    Write-Host "frontend: http://127.0.0.1:$FrontendPort"
    Write-Host "direct  : /shop/products/category?category=<encoded> -> $backendCount item(s)"
    Write-Host "proxy   : /shop/products/category?category=<encoded> -> $frontendCount item(s)"
    Write-Host "logs    : $LogDir"

    if ($KeepRunning) {
        Write-Host 'mode    : keep-running'
    } else {
        Write-Host 'mode    : auto-cleanup'
    }
} catch {
    Write-Host ''
    Write-Host "Bridge test failed: $($_.Exception.Message)" -ForegroundColor Red

    Show-LogTail -Path $backendOutLog
    Show-LogTail -Path $backendErrLog
    Show-LogTail -Path $frontendOutLog
    Show-LogTail -Path $frontendErrLog

    throw
} finally {
    Cleanup-StartedProcesses
}
