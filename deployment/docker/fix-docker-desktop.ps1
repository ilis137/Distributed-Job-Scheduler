# Docker Desktop Fix Script for Windows
# Diagnoses and fixes the "dockerDesktopLinuxEngine named pipe not found" error

param(
    [switch]$Force,
    [switch]$Verbose
)

$ErrorActionPreference = "Stop"

# Colors
function Write-Success { Write-Host "✓ $args" -ForegroundColor Green }
function Write-Error { Write-Host "✗ $args" -ForegroundColor Red }
function Write-Info { Write-Host "ℹ $args" -ForegroundColor Cyan }
function Write-Warning { Write-Host "⚠ $args" -ForegroundColor Yellow }

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Docker Desktop Fix Script" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Check if running as Administrator
$isAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)

if (-not $isAdmin) {
    Write-Warning "Not running as Administrator"
    Write-Info "Some operations may require admin privileges"
    Write-Host ""
}

# Step 2: Check Docker Desktop process
Write-Info "Checking Docker Desktop process..."
$dockerProcess = Get-Process "Docker Desktop" -ErrorAction SilentlyContinue

if ($dockerProcess) {
    Write-Success "Docker Desktop process is running (PID: $($dockerProcess.Id))"
} else {
    Write-Error "Docker Desktop process is NOT running"
    Write-Info "Attempting to start Docker Desktop..."
    
    $dockerPath = "C:\Program Files\Docker\Docker\Docker Desktop.exe"
    if (Test-Path $dockerPath) {
        Start-Process $dockerPath
        Write-Info "Waiting 60 seconds for Docker Desktop to start..."
        Start-Sleep -Seconds 60
    } else {
        Write-Error "Docker Desktop not found at: $dockerPath"
        Write-Info "Please install Docker Desktop from: https://www.docker.com/products/docker-desktop/"
        exit 1
    }
}

# Step 3: Check Docker service
Write-Info "Checking Docker service..."
$dockerService = Get-Service -Name "com.docker.service" -ErrorAction SilentlyContinue

if ($dockerService) {
    if ($dockerService.Status -eq "Running") {
        Write-Success "Docker service is running"
    } else {
        Write-Warning "Docker service is $($dockerService.Status)"
        if ($isAdmin) {
            Write-Info "Attempting to start Docker service..."
            Start-Service -Name "com.docker.service"
            Start-Sleep -Seconds 10
            Write-Success "Docker service started"
        } else {
            Write-Error "Need Administrator privileges to start Docker service"
            Write-Info "Run this script as Administrator or start Docker Desktop manually"
        }
    }
} else {
    Write-Warning "Docker service not found (this may be normal for some Docker Desktop versions)"
}

# Step 4: Check named pipes
Write-Info "Checking Docker named pipes..."
$pipes = Get-ChildItem \\.\pipe\ -ErrorAction SilentlyContinue | Where-Object { $_.Name -like "*docker*" }

if ($pipes) {
    Write-Success "Found Docker named pipes:"
    foreach ($pipe in $pipes) {
        Write-Host "  - $($pipe.Name)" -ForegroundColor Gray
    }
    
    $linuxEnginePipe = $pipes | Where-Object { $_.Name -eq "dockerDesktopLinuxEngine" }
    if ($linuxEnginePipe) {
        Write-Success "dockerDesktopLinuxEngine pipe exists"
    } else {
        Write-Warning "dockerDesktopLinuxEngine pipe NOT found"
        Write-Info "Docker Desktop may still be starting up..."
    }
} else {
    Write-Error "No Docker named pipes found"
    Write-Info "Docker Desktop is not fully initialized"
}

# Step 5: Test Docker CLI
Write-Info "Testing Docker CLI..."
Start-Sleep -Seconds 5  # Give Docker a moment to initialize

try {
    $dockerVersion = docker version 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Success "Docker CLI is working"
        if ($Verbose) {
            Write-Host ""
            docker version
            Write-Host ""
        }
    } else {
        throw "Docker CLI failed"
    }
} catch {
    Write-Error "Docker CLI is NOT working"
    Write-Info "Error: $_"
    Write-Host ""
    Write-Warning "Docker Desktop may still be starting. Please wait 30 seconds and try again."
    Write-Host ""
    Write-Info "If the problem persists, try these steps:"
    Write-Host "  1. Quit Docker Desktop (right-click system tray icon → Quit)"
    Write-Host "  2. Wait 10 seconds"
    Write-Host "  3. Start Docker Desktop again"
    Write-Host "  4. Wait for 'Docker Desktop is running' notification"
    Write-Host "  5. Run this script again"
    Write-Host ""
    exit 1
}

# Step 6: Test Docker daemon
Write-Info "Testing Docker daemon..."
try {
    $containers = docker ps 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Success "Docker daemon is responding"
        $containerCount = (docker ps -q | Measure-Object).Count
        Write-Info "Currently running containers: $containerCount"
    } else {
        throw "Docker daemon failed"
    }
} catch {
    Write-Error "Docker daemon is NOT responding"
    Write-Info "Error: $_"
    exit 1
}

# Step 7: Check WSL2
Write-Info "Checking WSL2 status..."
try {
    $wslList = wsl --list --running 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Success "WSL2 is running"
        if ($Verbose) {
            Write-Host ""
            wsl --list --verbose
            Write-Host ""
        }
    } else {
        Write-Warning "WSL2 may not be running or not installed"
    }
} catch {
    Write-Warning "Could not check WSL2 status"
}

# Step 8: Summary
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$allGood = $true

if ($dockerProcess) {
    Write-Success "Docker Desktop process: Running"
} else {
    Write-Error "Docker Desktop process: Not running"
    $allGood = $false
}

if ($dockerService -and $dockerService.Status -eq "Running") {
    Write-Success "Docker service: Running"
} elseif ($dockerService) {
    Write-Warning "Docker service: $($dockerService.Status)"
} else {
    Write-Info "Docker service: Not applicable"
}

try {
    docker version | Out-Null 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Success "Docker CLI: Working"
    } else {
        Write-Error "Docker CLI: Not working"
        $allGood = $false
    }
} catch {
    Write-Error "Docker CLI: Not working"
    $allGood = $false
}

Write-Host ""

if ($allGood) {
    Write-Success "Docker Desktop is fully operational!"
    Write-Host ""
    Write-Info "You can now run your docker-compose commands:"
    Write-Host "  cd deployment\docker" -ForegroundColor Gray
    Write-Host "  docker-compose build --no-cache" -ForegroundColor Gray
    Write-Host "  docker-compose up -d" -ForegroundColor Gray
} else {
    Write-Error "Docker Desktop is NOT fully operational"
    Write-Host ""
    Write-Info "Recommended actions:"
    Write-Host "  1. Restart Docker Desktop:"
    Write-Host "     - Right-click Docker Desktop icon → Quit"
    Write-Host "     - Wait 10 seconds"
    Write-Host "     - Start Docker Desktop from Start Menu"
    Write-Host "     - Wait for 'Docker Desktop is running' notification"
    Write-Host ""
    Write-Host "  2. If that doesn't work, restart WSL2:"
    Write-Host "     - Run PowerShell as Administrator"
    Write-Host "     - Run: wsl --shutdown"
    Write-Host "     - Wait 10 seconds"
    Write-Host "     - Start Docker Desktop"
    Write-Host ""
    Write-Host "  3. If still not working, see detailed troubleshooting:"
    Write-Host "     - docs\DOCKER_DESKTOP_TROUBLESHOOTING.md"
}

Write-Host ""

