param(
    [string]$ConfirmDeletion
)

$ErrorActionPreference = "Stop"
function Invoke-Gcloud([string[]]$Arguments) {
    & gcloud @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "gcloud $($Arguments[0]) failed. Review the command output before retrying."
    }
}

$project = [Environment]::GetEnvironmentVariable("GCP_PROJECT_ID")
$region = [Environment]::GetEnvironmentVariable("GCP_REGION")
$bucket = [Environment]::GetEnvironmentVariable("GCP_ARTIFACTS_BUCKET")
$registry = [Environment]::GetEnvironmentVariable("GCP_ARTIFACT_REGISTRY")
if ([string]::IsNullOrWhiteSpace($registry)) { $registry = "jobchef" }
if ([string]::IsNullOrWhiteSpace($project) -or [string]::IsNullOrWhiteSpace($region)) {
    throw "Set GCP_PROJECT_ID and GCP_REGION to the exact resources to remove."
}
if ($ConfirmDeletion -cne "DELETE JOB CHEF RESOURCES FROM $project") {
    throw "Pass -ConfirmDeletion 'DELETE JOB CHEF RESOURCES FROM $project' after reviewing the resource list."
}

Invoke-Gcloud @("config", "set", "project", $project)
Invoke-Gcloud @("scheduler", "jobs", "delete", "jobchef-outbox-dispatch", "--location=$region", "--quiet")
Invoke-Gcloud @("scheduler", "jobs", "delete", "jobchef-generation-recovery", "--location=$region", "--quiet")
Invoke-Gcloud @("pubsub", "subscriptions", "delete", "jobchef-generation-push", "--quiet")
Invoke-Gcloud @("pubsub", "topics", "delete", "jobchef-generation", "--quiet")
Invoke-Gcloud @("pubsub", "topics", "delete", "jobchef-generation-dead-letter", "--quiet")
Invoke-Gcloud @("run", "services", "delete", "jobchef-api", "--region=$region", "--quiet")
Invoke-Gcloud @("run", "services", "delete", "jobchef-worker", "--region=$region", "--quiet")
if (-not [string]::IsNullOrWhiteSpace($bucket)) {
    Write-Host "The private bucket is retained by default. Review its contents, then delete it explicitly with:"
    Write-Host "gcloud storage rm --recursive gs://$bucket"
}
Write-Host "Artifact Registry, secrets, budget alerts, service accounts, external PostgreSQL, and Redis are retained for review."
Write-Host "Delete only resources created for this project; inspect IAM bindings and billing alerts manually."
