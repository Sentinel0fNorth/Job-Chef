$ErrorActionPreference = "Stop"
$repositoryRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
Push-Location $repositoryRoot
try {
    docker compose up --build --detach
    if ($LASTEXITCODE -ne 0) {
        throw "Docker Compose could not start the local demo stack."
    }

    $ready = $false
    for ($attempt = 0; $attempt -lt 60; $attempt++) {
        docker compose exec -T api curl --silent --fail http://localhost:8080/actuator/health *> $null
        if ($LASTEXITCODE -eq 0) {
            $ready = $true
            break
        }
        Start-Sleep -Seconds 2
    }
    if (-not $ready) {
        throw "The API did not finish starting. Inspect Docker Compose logs before retrying enrollment."
    }

    & (Join-Path $PSScriptRoot "provision-demo-enrollment.ps1")
    if ($LASTEXITCODE -ne 0) {
        throw "The local demo stack started, but one-time enrollment setup failed."
    }
}
finally {
    Pop-Location
}
