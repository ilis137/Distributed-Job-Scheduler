# Docker Desktop Quick Fix - Windows

**Error**: `open //./pipe/dockerDesktopLinuxEngine: The system cannot find the file specified`

---

## ⚡ 30-Second Fix

```powershell
# 1. Quit Docker Desktop
# Right-click Docker Desktop icon in system tray → Quit Docker Desktop

# 2. Wait 10 seconds

# 3. Start Docker Desktop
# Start Menu → Docker Desktop

# 4. Wait for "Docker Desktop is running" notification (30-60 seconds)

# 5. Test
docker version
```

**Success**: You should see both Client and Server versions

---

## 🤖 Automated Fix

```powershell
# Run the automated fix script
cd "D:\Projects\Distributed Job Scheduler\deployment\docker"
.\fix-docker-desktop.ps1

# If you get "execution policy" error, run:
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\fix-docker-desktop.ps1
```

---

## 🔍 Quick Diagnostics

### **Is Docker Desktop running?**

```powershell
Get-Process "Docker Desktop" -ErrorAction SilentlyContinue
```

**Expected**: Shows process info  
**If empty**: Docker Desktop is not running → Start it

---

### **Is the named pipe available?**

```powershell
Get-ChildItem \\.\pipe\ | Where-Object { $_.Name -eq "dockerDesktopLinuxEngine" }
```

**Expected**: Shows the pipe  
**If empty**: Docker is not fully started → Wait 30 seconds

---

### **Can Docker CLI connect?**

```powershell
docker version
```

**Expected**: Shows Client and Server versions  
**If error**: Docker is not ready → Restart Docker Desktop

---

## 🛠️ If Quick Fix Doesn't Work

### **Option 1: Restart WSL2**

```powershell
# Run as Administrator
wsl --shutdown
Start-Sleep -Seconds 10
Start-Process "C:\Program Files\Docker\Docker\Docker Desktop.exe"
Start-Sleep -Seconds 60
docker version
```

---

### **Option 2: Restart Docker Service**

```powershell
# Run as Administrator
Stop-Service -Name "com.docker.service" -Force
Start-Sleep -Seconds 5
Start-Service -Name "com.docker.service"
Start-Sleep -Seconds 30
docker version
```

---

## 📝 About the `version` Warning

**Warning**: `WARN[0000] version is obsolete`

**What it means**: Docker Compose v2 no longer requires the `version` field

**Should you remove it?**

- **Keep it**: Backward compatibility, explicit format version
- **Remove it**: Eliminate warning, follow latest best practices

**To remove**:

```yaml
# Before
version: '3.8'
services:
  mysql:
    ...

# After
services:
  mysql:
    ...
```

**Recommendation**: **Keep it for now** - it's harmless and provides clarity

---

## ✅ Verification

After fixing, verify everything works:

```powershell
# 1. Docker is running
docker version

# 2. Docker daemon responds
docker ps

# 3. Build your project
cd "D:\Projects\Distributed Job Scheduler\deployment\docker"
docker-compose build --no-cache

# 4. Start the cluster
docker-compose up -d

# 5. Check all containers
docker-compose ps
```

**Expected**: All containers show "Up" status

---

## 🚨 Prevention

### **Enable Auto-Start**

Docker Desktop Settings → General → ✓ Start Docker Desktop when you log in

### **Wait for Full Startup**

Don't run docker commands immediately after starting Docker Desktop. Wait for the notification: "Docker Desktop is running"

### **Monitor Health**

Add to PowerShell profile (`$PROFILE`):

```powershell
function docker-status {
    try {
        docker version | Out-Null
        Write-Host "✓ Docker is running" -ForegroundColor Green
    } catch {
        Write-Host "✗ Docker is not running" -ForegroundColor Red
    }
}
```

Usage: `docker-status`

---

## 📚 More Help

- **Detailed troubleshooting**: `docs/DOCKER_DESKTOP_TROUBLESHOOTING.md`
- **Automated fix script**: `deployment/docker/fix-docker-desktop.ps1`
- **Docker Desktop docs**: https://docs.docker.com/desktop/troubleshoot/overview/

---

**Last Updated**: 2026-03-12

