$ErrorActionPreference = "Stop"
$repositoryRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
Push-Location $repositoryRoot
$random = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$sha256 = [System.Security.Cryptography.SHA256]::Create()
try {
    $bytes = New-Object byte[] 32
    $random.GetBytes($bytes)
    $code = [Convert]::ToBase64String($bytes).TrimEnd("=").Replace("+", "-").Replace("/", "_")
    $hashBytes = $sha256.ComputeHash([Text.Encoding]::UTF8.GetBytes($code))
    $hash = ([BitConverter]::ToString($hashBytes)).Replace("-", "").ToLowerInvariant()

    docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U jobchef -d jobchef `
        -v "token_hash=$hash" `
        -c "INSERT INTO enrollment_codes(token_hash, remaining_uses, expires_at) VALUES (:'token_hash', 1, now() + interval '24 hours');"
    if ($LASTEXITCODE -ne 0) {
        throw "Could not provision a one-use demo enrollment code."
    }

    Write-Host "One-time enrollment code (valid for 24 hours; do not save in repository files):"
    Write-Host $code
}
finally {
    $random.Dispose()
    $sha256.Dispose()
    Pop-Location
}
