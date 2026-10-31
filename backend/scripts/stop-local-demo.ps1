param(
    [switch]$DeleteLocalData
)

$ErrorActionPreference = "Stop"
$repositoryRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
Push-Location $repositoryRoot
try {
    if ($DeleteLocalData) {
        $answer = Read-Host "Delete local PostgreSQL, Redis, and preview-artifact volumes? Type DELETE"
        if ($answer -cne "DELETE") {
            Write-Host "Local data was kept."
            return
        }
        docker compose down --volumes
    }
    else {
        docker compose down
    }
    if ($LASTEXITCODE -ne 0) {
        throw "Docker Compose could not stop the local demo stack."
    }
}
finally {
    Pop-Location
}
