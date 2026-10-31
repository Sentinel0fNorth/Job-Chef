param(
    [switch]$Deploy,
    [string]$ConfirmResourceCreation
)

$ErrorActionPreference = "Stop"
$repositoryRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
Set-Location $repositoryRoot
$environmentFile = Join-Path $repositoryRoot ".env"
if (Test-Path $environmentFile) {
    foreach ($line in Get-Content $environmentFile) {
        $trimmed = $line.Trim()
        if ($trimmed.Length -eq 0 -or $trimmed.StartsWith("#")) { continue }
        $separator = $trimmed.IndexOf("=")
        if ($separator -lt 1) { continue }
        $name = $trimmed.Substring(0, $separator).Trim()
        $value = $trimmed.Substring($separator + 1).Trim()
        if ($name -match "^[A-Z][A-Z0-9_]*$" -and
            [string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name))) {
            [Environment]::SetEnvironmentVariable($name, $value)
        }
    }
}

function Get-RequiredSetting([string]$Name) {
    $value = [Environment]::GetEnvironmentVariable($Name)
    if ([string]::IsNullOrWhiteSpace($value)) {
        throw "Set $Name in the local environment before deployment."
    }
    return $value
}

function Invoke-Gcloud([string[]]$Arguments) {
    & gcloud @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "gcloud $($Arguments[0]) failed. Review the command output before retrying."
    }
}

$project = Get-RequiredSetting "GCP_PROJECT_ID"
$region = Get-RequiredSetting "GCP_REGION"
$billingAccount = Get-RequiredSetting "GCP_BILLING_ACCOUNT"
$bucket = Get-RequiredSetting "GCP_ARTIFACTS_BUCKET"
$registry = [Environment]::GetEnvironmentVariable("GCP_ARTIFACT_REGISTRY")
if ([string]::IsNullOrWhiteSpace($registry)) { $registry = "jobchef" }
$databaseHost = Get-RequiredSetting "POSTGRES_HOST"
$databaseName = Get-RequiredSetting "POSTGRES_DATABASE"
$databaseUser = Get-RequiredSetting "POSTGRES_USERNAME"
$redisHost = Get-RequiredSetting "REDIS_HOST"
$redisPort = [Environment]::GetEnvironmentVariable("REDIS_PORT")
if ([string]::IsNullOrWhiteSpace($redisPort)) { $redisPort = "6379" }
$redisUser = [Environment]::GetEnvironmentVariable("REDIS_USERNAME")
$model = Get-RequiredSetting "GEMINI_MODEL"
$secretNames = @(
    (Get-RequiredSetting "POSTGRES_PASSWORD_SECRET"),
    (Get-RequiredSetting "REDIS_PASSWORD_SECRET"),
    (Get-RequiredSetting "ADZUNA_APP_ID_SECRET"),
    (Get-RequiredSetting "ADZUNA_APP_KEY_SECRET"),
    (Get-RequiredSetting "GEMINI_API_KEY_SECRET")
)

if (-not $Deploy) {
    Write-Host "Preflight complete. No cloud resources were created."
    Write-Host "This script will create Cloud Run, Pub/Sub, Cloud Storage, Artifact Registry, service accounts, IAM bindings, scheduler jobs, and a 500 INR notification budget."
    Write-Host "It will use external PostgreSQL and Redis endpoints supplied in environment variables."
    Write-Host "Run with -Deploy -ConfirmResourceCreation 'CREATE $project' only after reviewing docs/TRANSFER_AND_DEPLOY.md and the current provider costs."
    return
}
if ($ConfirmResourceCreation -cne "CREATE $project") {
    throw "Resource creation confirmation did not match the selected project."
}
if (-not (Get-Command gcloud -ErrorAction SilentlyContinue)) {
    throw "Install and authenticate the Google Cloud CLI on the deployment device first."
}
$billingCurrency = (& gcloud billing accounts describe $billingAccount "--format=value(currencyCode)").Trim()
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($billingCurrency)) {
    throw "Could not verify the selected billing account currency; no cloud resources were created."
}
if ($billingCurrency -cne "INR") {
    throw "The approved notification budget is 500 INR, but this billing account uses $billingCurrency. No cloud resources were created; configure an equivalent budget in the account currency before deployment."
}

$postgresUrl = "jdbc:postgresql://$databaseHost/$databaseName?sslmode=require"
$apiRuntimeAccount = "jobchef-api-runtime"
$workerRuntimeAccount = "jobchef-worker-runtime"
$schedulerAccount = "jobchef-scheduler"
$pushAccount = "jobchef-pubsub-push"
$apiService = "jobchef-api"
$workerService = "jobchef-worker"
$topic = "jobchef-generation"
$deadLetterTopic = "jobchef-generation-dead-letter"
$subscription = "jobchef-generation-push"
$tag = "v1-" + (Get-Date).ToUniversalTime().ToString("yyyyMMddHHmmss")
$imagePrefix = "$region-docker.pkg.dev/$project/$registry"
$apiImage = "$imagePrefix/api:$tag"
$workerImage = "$imagePrefix/worker:$tag"
$apiRuntimeEmail = "$apiRuntimeAccount@$project.iam.gserviceaccount.com"
$workerRuntimeEmail = "$workerRuntimeAccount@$project.iam.gserviceaccount.com"
$schedulerEmail = "$schedulerAccount@$project.iam.gserviceaccount.com"
$pushEmail = "$pushAccount@$project.iam.gserviceaccount.com"

Invoke-Gcloud @("config", "set", "project", $project)
Invoke-Gcloud @("services", "enable",
    "run.googleapis.com",
    "pubsub.googleapis.com",
    "storage.googleapis.com",
    "artifactregistry.googleapis.com",
    "cloudbuild.googleapis.com",
    "secretmanager.googleapis.com",
    "cloudscheduler.googleapis.com",
    "billingbudgets.googleapis.com")

Invoke-Gcloud @("artifacts", "repositories", "create", $registry,
    "--location=$region", "--repository-format=docker", "--description=Job Chef V1 containers")
Invoke-Gcloud @("artifacts", "repositories", "set-cleanup-policies", $registry,
    "--location=$region", "--policy=backend/deploy/artifact-registry-cleanup.json")
Invoke-Gcloud @("storage", "buckets", "create", "gs://$bucket",
    "--location=$region", "--uniform-bucket-level-access", "--public-access-prevention")
Invoke-Gcloud @("storage", "buckets", "update", "gs://$bucket",
    "--lifecycle-file=backend/deploy/gcs-lifecycle.json")
Invoke-Gcloud @("pubsub", "topics", "create", $topic)
Invoke-Gcloud @("pubsub", "topics", "create", $deadLetterTopic)

foreach ($account in @($apiRuntimeAccount, $workerRuntimeAccount, $schedulerAccount, $pushAccount)) {
    Invoke-Gcloud @("iam", "service-accounts", "create", $account,
        "--display-name=Job Chef $account runtime")
}
Invoke-Gcloud @("storage", "buckets", "add-iam-policy-binding", "gs://$bucket",
    "--member=serviceAccount:$apiRuntimeEmail", "--role=roles/storage.objectAdmin")
Invoke-Gcloud @("storage", "buckets", "add-iam-policy-binding", "gs://$bucket",
    "--member=serviceAccount:$workerRuntimeEmail", "--role=roles/storage.objectAdmin")
Invoke-Gcloud @("pubsub", "topics", "add-iam-policy-binding", $topic,
    "--member=serviceAccount:$apiRuntimeEmail", "--role=roles/pubsub.publisher")

foreach ($secret in @($secretNames[0], $secretNames[1], $secretNames[2], $secretNames[3])) {
    Invoke-Gcloud @("secrets", "versions", "describe", "latest",
        "--secret=$secret", "--project=$project")
    Invoke-Gcloud @("secrets", "add-iam-policy-binding", $secret,
        "--member=serviceAccount:$apiRuntimeEmail", "--role=roles/secretmanager.secretAccessor")
}
foreach ($secret in @($secretNames[0], $secretNames[1], $secretNames[4])) {
    Invoke-Gcloud @("secrets", "versions", "describe", "latest",
        "--secret=$secret", "--project=$project")
    Invoke-Gcloud @("secrets", "add-iam-policy-binding", $secret,
        "--member=serviceAccount:$workerRuntimeEmail", "--role=roles/secretmanager.secretAccessor")
}

Invoke-Gcloud @("builds", "submit", "--config=backend/deploy/cloudbuild-api.yaml",
    "--substitutions=_IMAGE=$apiImage", ".")
Invoke-Gcloud @("builds", "submit", "--config=backend/deploy/cloudbuild-worker.yaml",
    "--substitutions=_IMAGE=$workerImage", ".")

$apiEnvironment = @(
    "SPRING_PROFILES_ACTIVE=live",
    "JOB_CHEF_MODE=live",
    "SPRING_DATASOURCE_URL=$postgresUrl",
    "SPRING_DATASOURCE_USERNAME=$databaseUser",
    "DB_POOL_SIZE=5",
    "REDIS_HOST=$redisHost",
    "REDIS_PORT=$redisPort",
    "REDIS_USERNAME=$redisUser",
    "REDIS_SSL=true",
    "GCP_PROJECT_ID=$project",
    "PUBSUB_TOPIC=$topic",
    "JOB_CHEF_ARTIFACTS_BUCKET=$bucket",
    "JOB_CHEF_INTERNAL_AUDIENCE=https://pending-audience.invalid",
    "SCHEDULER_SERVICE_ACCOUNT=$schedulerEmail",
    "ADZUNA_DEFAULT_COUNTRY=in",
    "ADZUNA_SUPPORTED_COUNTRIES=in",
    "GEMINI_MODEL=$model",
    "MAX_SEARCH_RESULTS=20",
    "MAX_NEW_APPLICATIONS_PER_DEVICE_PER_DAY=5",
    "MAX_REGENERATIONS_PER_JOB_PER_DAY=1",
    "MAX_GLOBAL_GENERATIONS_PER_DAY=20",
    "MAX_CONCURRENT_GENERATIONS=2"
)
$workerEnvironment = @(
    "SPRING_PROFILES_ACTIVE=live",
    "JOB_CHEF_MODE=live",
    "SPRING_DATASOURCE_URL=$postgresUrl",
    "SPRING_DATASOURCE_USERNAME=$databaseUser",
    "DB_POOL_SIZE=3",
    "REDIS_HOST=$redisHost",
    "REDIS_PORT=$redisPort",
    "REDIS_USERNAME=$redisUser",
    "REDIS_SSL=true",
    "JOB_CHEF_ARTIFACTS_BUCKET=$bucket",
    "JOB_CHEF_INTERNAL_AUDIENCE=https://pending-audience.invalid",
    "SCHEDULER_SERVICE_ACCOUNT=$schedulerEmail",
    "PUBSUB_PUSH_SERVICE_ACCOUNT=$pushEmail",
    "GEMINI_MODEL=$model",
    "MAX_GLOBAL_GENERATIONS_PER_DAY=20",
    "MAX_GLOBAL_TOKENS_PER_DAY=100000",
    "MAX_CONCURRENT_GENERATIONS=1"
)
$apiSecrets = @(
    "SPRING_DATASOURCE_PASSWORD=$($secretNames[0]):latest",
    "REDIS_PASSWORD=$($secretNames[1]):latest",
    "ADZUNA_APP_ID=$($secretNames[2]):latest",
    "ADZUNA_APP_KEY=$($secretNames[3]):latest"
)
$workerSecrets = @(
    "SPRING_DATASOURCE_PASSWORD=$($secretNames[0]):latest",
    "REDIS_PASSWORD=$($secretNames[1]):latest",
    "GEMINI_API_KEY=$($secretNames[4]):latest"
)

Invoke-Gcloud @("run", "deploy", $apiService, "--image=$apiImage", "--region=$region",
    "--service-account=$apiRuntimeEmail", "--allow-unauthenticated", "--min=0", "--max=2",
    "--concurrency=10", "--cpu=1", "--memory=512Mi", "--timeout=60",
    "--set-env-vars=$($apiEnvironment -join ',')", "--set-secrets=$($apiSecrets -join ',')")
Invoke-Gcloud @("run", "deploy", $workerService, "--image=$workerImage", "--region=$region",
    "--service-account=$workerRuntimeEmail", "--no-allow-unauthenticated", "--min=0", "--max=1",
    "--concurrency=1", "--cpu=1", "--memory=1Gi", "--timeout=300",
    "--set-env-vars=$($workerEnvironment -join ',')", "--set-secrets=$($workerSecrets -join ',')")

$apiUrl = (& gcloud run services describe $apiService "--region=$region" "--format=value(status.url)").Trim()
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($apiUrl)) {
    throw "Could not read the deployed API service URL."
}
$workerUrl = (& gcloud run services describe $workerService "--region=$region" "--format=value(status.url)").Trim()
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($workerUrl)) {
    throw "Could not read the deployed worker service URL."
}
Invoke-Gcloud @("run", "services", "update", $apiService, "--region=$region",
    "--update-env-vars=JOB_CHEF_INTERNAL_AUDIENCE=$apiUrl")
Invoke-Gcloud @("run", "services", "update", $workerService, "--region=$region",
    "--update-env-vars=JOB_CHEF_INTERNAL_AUDIENCE=$workerUrl")

Invoke-Gcloud @("run", "services", "add-iam-policy-binding", $workerService,
    "--region=$region", "--member=serviceAccount:$pushEmail", "--role=roles/run.invoker")
Invoke-Gcloud @("run", "services", "add-iam-policy-binding", $apiService,
    "--region=$region", "--member=serviceAccount:$schedulerEmail", "--role=roles/run.invoker")
Invoke-Gcloud @("run", "services", "add-iam-policy-binding", $workerService,
    "--region=$region", "--member=serviceAccount:$schedulerEmail", "--role=roles/run.invoker")

$projectNumber = (& gcloud projects describe $project "--format=value(projectNumber)").Trim()
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($projectNumber)) {
    throw "Could not read the selected project number for Pub/Sub push authentication."
}
$pubsubAgent = "service-$projectNumber@gcp-sa-pubsub.iam.gserviceaccount.com"
Invoke-Gcloud @("pubsub", "topics", "add-iam-policy-binding", $deadLetterTopic,
    "--member=serviceAccount:$pubsubAgent", "--role=roles/pubsub.publisher")
Invoke-Gcloud @("iam", "service-accounts", "add-iam-policy-binding", $pushEmail,
    "--member=serviceAccount:$pubsubAgent", "--role=roles/iam.serviceAccountTokenCreator")
Invoke-Gcloud @("pubsub", "subscriptions", "create", $subscription,
    "--topic=$topic",
    "--push-endpoint=$workerUrl/internal/pubsub/generate",
    "--push-auth-service-account=$pushEmail",
    "--push-auth-token-audience=$workerUrl",
    "--dead-letter-topic=$deadLetterTopic",
    "--max-delivery-attempts=5",
    "--ack-deadline=600")

Invoke-Gcloud @("scheduler", "jobs", "create", "http", "jobchef-outbox-dispatch",
    "--location=$region", "--schedule=*/1 * * * *",
    "--uri=$apiUrl/api/v1/internal/outbox/dispatch", "--http-method=POST",
    "--oidc-service-account=$schedulerEmail", "--oidc-token-audience=$apiUrl")
Invoke-Gcloud @("scheduler", "jobs", "create", "http", "jobchef-generation-recovery",
    "--location=$region", "--schedule=*/5 * * * *",
    "--uri=$workerUrl/internal/recovery", "--http-method=POST",
    "--oidc-service-account=$schedulerEmail", "--oidc-token-audience=$workerUrl")

Invoke-Gcloud @("billing", "budgets", "create", "--billing-account=$billingAccount",
    "--display-name=Job Chef 500 INR notification budget", "--budget-amount=500INR",
    "--threshold-rule=percent=0.5,basis=CURRENT_SPEND",
    "--threshold-rule=percent=0.8,basis=CURRENT_SPEND",
    "--threshold-rule=percent=1.0,basis=CURRENT_SPEND")

Write-Host "Initial deployment steps completed. API URL: $apiUrl"
Write-Host "Worker URL: $workerUrl (authenticated; do not expose publicly)."
Write-Host "Set ANDROID_BACKEND_URL=$apiUrl and configure one-time device enrollment before testing."
Write-Host "Budget alerts are notifications, not a hard spending cap. Perform the documented live acceptance and cost review."
