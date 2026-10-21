$ports = @(8081, 8082, 8083, 8084, 8085)
$pidFile = Join-Path $PSScriptRoot ".backend-processes.json"

Write-Host "Deteniendo procesos Java de los microservicios..."

$backendProcessIds = Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue |
    Where-Object { $_.LocalPort -in $ports } |
    Select-Object -ExpandProperty OwningProcess -Unique

foreach ($processId in $backendProcessIds) {
    try {
        $process = Get-Process -Id $processId -ErrorAction Stop

        Write-Host "Deteniendo $($process.ProcessName) (PID $processId)..."
        Stop-Process -Id $processId -Force -ErrorAction Stop
    }
    catch {
        Write-Warning "No se pudo detener el proceso backend con PID $processId."
    }
}

if (Test-Path $pidFile) {
    Write-Host ""
    Write-Host "Cerrando terminales de los microservicios..."

    $terminalProcesses = Get-Content $pidFile |
        ConvertFrom-Json

    foreach ($terminalProcess in $terminalProcesses) {
        try {
            $processId = [int]$terminalProcess.ProcessId
            $service = $terminalProcess.Service

            $process = Get-Process -Id $processId -ErrorAction Stop

            Write-Host "Cerrando terminal de $service (PID $processId)..."
            Stop-Process -Id $processId -Force -ErrorAction Stop
        }
        catch {
            # La terminal puede haberse cerrado manualmente.
        }
    }

    Remove-Item $pidFile -Force
}

Write-Host ""
Write-Host "Backend detenido y terminales cerradas."