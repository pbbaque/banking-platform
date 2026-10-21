$root = Join-Path $PSScriptRoot "backend"
$pidFile = Join-Path $PSScriptRoot ".backend-processes.json"

$services = @(
    "customer-service",
    "identity-service",
    "account-service",
    "transfer-service",
    "audit-service"
)

$startedProcesses = @()

foreach ($service in $services) {
    $servicePath = Join-Path $root $service

    $process = Start-Process powershell `
        -ArgumentList @(
            "-NoExit",
            "-Command",
            "Set-Location '$servicePath'; mvn spring-boot:run '-Dspring-boot.run.profiles=local'"
        ) `
        -PassThru

    $startedProcesses += [PSCustomObject]@{
        Service = $service
        ProcessId = $process.Id
    }

    Write-Host "Iniciado $service en PowerShell PID $($process.Id)"
}

$startedProcesses |
    ConvertTo-Json |
    Set-Content -Path $pidFile -Encoding UTF8

Write-Host ""
Write-Host "Los 5 servicios backend han sido lanzados."
Write-Host "PID guardados en $pidFile"