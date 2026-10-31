$ErrorActionPreference = "Stop"
foreach ($name in @("PGHOST", "PGDATABASE", "PGUSER", "PGPASSWORD")) {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name))) {
        throw "Set $name locally before provisioning a device code; values are not requested in chat."
    }
}
if (-not (Get-Command psql -ErrorAction SilentlyContinue)) {
    throw "Install the PostgreSQL client tools on the deployment device."
}

$random = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$sha256 = [System.Security.Cryptography.SHA256]::Create()
try {
    $bytes = New-Object byte[] 32
    $random.GetBytes($bytes)
    $code = [Convert]::ToBase64String($bytes).TrimEnd("=").Replace("+", "-").Replace("/", "_")
    $hashBytes = $sha256.ComputeHash([Text.Encoding]::UTF8.GetBytes($code))
    $hash = ([BitConverter]::ToString($hashBytes)).Replace("-", "").ToLowerInvariant()
    $port = [Environment]::GetEnvironmentVariable("PGPORT")
    if ([string]::IsNullOrWhiteSpace($port)) { $port = "5432" }

    psql --no-psqlrc --set ON_ERROR_STOP=1 --host=$env:PGHOST --port=$port `
        --username=$env:PGUSER --dbname=$env:PGDATABASE `
        --set "token_hash=$hash" `
        --command "INSERT INTO enrollment_codes(token_hash, remaining_uses, expires_at) VALUES (:'token_hash', 1, now() + interval '24 hours');"
    if ($LASTEXITCODE -ne 0) {
        throw "Could not provision a one-use device enrollment code."
    }
    Write-Host "One-time device enrollment code (valid for 24 hours; do not save in repository files):"
    Write-Host $code
}
finally {
    $random.Dispose()
    $sha256.Dispose()
}
