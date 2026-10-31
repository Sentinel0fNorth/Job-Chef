# Transfer, local acceptance, and optional Cloud Run deployment

## Current handover state

This repository contains source, tests, local Compose configuration, container definitions, and deployment scripts. It has not been built or tested in the current checkout; no APK or container image is included. Before calling the implementation complete, run the checks below on the other device and record their exact results in `CURRENT_STATUS.md`. API keys alone do not provision the database, Redis, GCP identity, Pub/Sub, storage, or Android backend URL.

Do not commit/push this checkout, provision resources from the original device, or deploy before reviewing the account’s current free-tier/billing situation. Existing Git history is intentionally unchanged.

## Move the project safely

Copy the repository and unchanged `.git` directory to the development device. Do not transfer or include `.env`, private resumes, Android app data, `local.properties`, `.jobchef`, build outputs, Gradle caches, or credentials. `.gitignore` excludes local state and filled `.env`; keep the example file unfilled.

Required build-device tools:

- Android Studio with Android SDK compile platform 37, build tools, an emulator/device, and accepted Android SDK licenses.
- JDK 21 for backend and Android Gradle; Android Gradle Plugin 9.2.0 and wrapper requirements are defined under `android/gradle`.
- Gradle 8.14.4 for backend, Docker with Compose v2 for local services, and optionally PostgreSQL `psql` for deployment enrollment provisioning.
- For live mode: authenticated `gcloud`, billing-enabled dedicated GCP project, external TLS PostgreSQL/Redis, and user-owned Adzuna/Gemini credentials.

## Run the synthetic local stack

From repository root using PowerShell:

```powershell
.\backend\scripts\local-demo.ps1
docker compose ps
docker compose logs --tail 100 api worker
```

The script builds API/worker images, starts PostgreSQL, Redis and Pub/Sub emulator, waits for the API health endpoint, then inserts only a SHA-256 hash for a generated one-use enrollment code. The code appears in the terminal once and expires in 24 hours. Demo mode uses synthetic listings and drafts only; do not use synthetic output for real applications.

Build/install the Android debug app against the local API:

```powershell
cd android
.\gradlew.bat :app:assembleDebug -PjobChefApiBaseUrl=http://10.0.2.2:8081/
```

Use `10.0.2.2` from the standard Android emulator to reach host port 8081. Debug cleartext is limited to loopback/emulator hosts; release builds keep HTTPS-only networking. Enter the printed enrollment code on first launch. The app has no default production URL or bundled provider key.

Stop without data loss:

```powershell
.\backend\scripts\stop-local-demo.ps1
```

To deliberately erase named PostgreSQL, Redis, and preview-artifact volumes:

```powershell
.\backend\scripts\stop-local-demo.ps1 -DeleteLocalData
```

## Automated and manual acceptance

With JDK 21 and Gradle 8.14.4:

```powershell
cd backend
gradle :shared:test :api:test :worker:test
cd ..\android
.\gradlew.bat :feature:resume:testDebugUnitTest :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug -PjobChefApiBaseUrl=http://10.0.2.2:8081/
```

Then run the compose demo and verify each item:

1. Register once; restart the app and confirm the encrypted device token persists. Confirm a second registration using the same one-use code is rejected.
2. Import an unlocked text PDF, inspect the extracted/sanitized text, edit missed identifying details, activate it, and confirm the original file/contact fields never appear in backend request logs or PostgreSQL.
3. Search a matching demo role and an irrelevant role. Confirm listing attribution is explicitly synthetic, result count never exceeds 20, and saved/rejected/undo decisions survive offline/restart and replay once online.
4. Save a job with and without an active resume; verify `WAITING_FOR_RESUME`, queued, processing, ready, failed and quota states rather than optimistic success. Exercise duplicate Pub/Sub delivery and stale-lease recovery.
5. Inspect resume and cover-letter previews as rendered pages of the actual generated PDF. Save/share both. Extract saved text with a PDF text tool and confirm local contacts are restored only in the final device PDF, with no content clipped at page boundaries.
6. Reject/undo while queued and while processing, delete a resume/application, retry an expired/failed item, hit five new saves/device/day and one regeneration/job/day, and verify limits are explicit and cannot be bypassed by duplicate delivery.
7. Test malformed/encrypted/scanned/oversized PDFs, missing URLs/provider credentials, Adzuna/Gemini 429/5xx, Redis unavailable, database restart, and token rotation/revocation. Use only synthetic inputs in demo mode.

Test exact PDFs with extraction/layout checks, not just the Compose screen. A generated PDF that cannot be read, contains clipped content, or contains remote contact data is a failure.

Live Adzuna and Gemini smoke tests are opt-in and credential-gated; never enable them in ordinary tests or silently spend a paid quota. Record passed, failed, blocked, and credential-gated results separately.

## Prepare a live environment

1. Review current Adzuna and Gemini terms, quotas, regions, and pricing. Obtain an Adzuna app ID/key and a Gemini API key/model with suitable quota. Do not assume Google AI Pro provides blanket Gemini API or GCP hosting entitlement; verify any Google Developer Programme credit, eligibility, activation, expiry, and covered services on the target account.
2. Select a supported Adzuna country. The default is `in`; confirm Adzuna currently serves it for your account. Configure `ADZUNA_SUPPORTED_COUNTRIES` only with country codes verified for the account.
3. Choose hosted PostgreSQL and Redis free-tier candidates using [`PRIVACY_AND_COST.md`](PRIVACY_AND_COST.md). Confirm PostgreSQL TLS, migrations, connection limits and sleep behavior, and Redis TLS/RESP/Lua/sorted-set support. Job Chef does not provision either database and does not use Cloud SQL or Memorystore.
4. Create/select a dedicated GCP project and region. Confirm billing currency and cost estimates. Cloud Run must be allowed to scale to zero. A 500 INR budget creates notifications only; it cannot enforce a lifetime spending ceiling.
5. Create five Secret Manager secrets and add secret versions for PostgreSQL password, Redis password, Adzuna app ID, Adzuna app key, and Gemini API key. Use Secret Manager UI or a protected local mechanism; do not put values in command history, source files, or chat. Secret *names* go into `.env`.
6. Copy `.env.example` to `.env` and fill non-secret deployment settings plus the Secret Manager names. Set `GCP_PROJECT_ID`, `GCP_REGION`, `GCP_BILLING_ACCOUNT`, `GCP_ARTIFACTS_BUCKET`, `GCP_ARTIFACT_REGISTRY`, PostgreSQL host/database/user, Redis host/port/user, `GEMINI_MODEL`, and the five secret-name variables. The deployment script expects the external PostgreSQL host to accept TLS and the external Redis provider to support TLS and the required commands.
7. From repository root, run `.\backend\deploy\deploy-cloudrun.ps1` once as a preflight. Review every resource it will create. Only an explicit later invocation with `-Deploy -ConfirmResourceCreation 'CREATE <project-id>'` performs cloud changes. Before creating resources, the script verifies that the selected billing account uses INR; if it uses another currency, deployment stops and you must configure a reviewed equivalent budget before proceeding. The script separates API and worker service accounts and limits Gemini-secret access to the worker. The script is an opt-in starter, not a substitute for checking current GCP CLI permissions, provider pricing, secret versions, or project-specific policies. It is not safe to blindly rerun after partial failure; inspect what exists and continue deliberately.

The deployment script creates Artifact Registry and cleanup policy, two service accounts, a private uniform-access GCS bucket with 90-day object lifecycle, Pub/Sub generation/dead-letter topics and authenticated push subscription, public Cloud Run API (device auth still required), private Cloud Run worker, outbox/recovery Scheduler jobs, IAM bindings, and a 500 INR notification budget. PostgreSQL/Redis remain external. Both Cloud Run services use minimum instances zero; API max is two instances and worker max one/concurrency one. TeX Live makes the worker image large and its registry/build cost should be checked.

`backend/deploy/teardown-cloudrun.ps1` removes Scheduler jobs, Pub/Sub subscription/topics, and Cloud Run services only after an exact project-name confirmation. It intentionally retains the GCS bucket, images/repository, secrets, budget, service accounts, and external databases for review. Inspect contents and delete only project-owned resources. Deleting a database/Redis instance may affect unrelated data; perform that manually only after backup/ownership checks.

## Provision device enrollment in live mode

Set local PostgreSQL connection variables on the deployment device (`PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD`, and `PGSSLMODE=require`) and install `psql`, then run:

```powershell
.\backend\scripts\provision-enrollment.ps1
```

The script inserts a random code’s hash with one use and a 24-hour expiry, then prints the code locally. It never writes the plaintext code to a repository file. Provide it only to the intended test device. Registration has a global daily Redis rate limit. If a device token is revoked/expired or the app is reinstalled, issue a new enrollment code; old local source resumes and device identity are not recoverable on another installation.

## Configure and verify Android for live backend

Build with the deployed HTTPS API URL and keep provider/database keys out of the APK:

```powershell
cd android
.\gradlew.bat :app:assembleRelease -PjobChefApiBaseUrl=https://<api-service-url>/
```

No APK is produced or signed by this repository handover. Sign/package on the receiving device only after successful acceptance, and distribute the enrollment code separately and securely.

## Post-demo shutdown

1. Revoke test device credentials and disable/scale down the API and worker if not actively needed.
2. Pause/delete the Pub/Sub push subscription and delete recovery/outbox Scheduler jobs.
3. Delete project-owned Cloud Run services, artifact images/repository, private preview bucket, secrets, service accounts/IAM grants, and budget notifications after checking dependencies and preserving only sanitized showcase assets.
4. Stop/delete external PostgreSQL and Redis resources only after verifying ownership and deleting/retaining data intentionally.
5. Remove local Compose containers and, when appropriate, named volumes and local `.jobchef` artifacts.
6. Review Cloud Logging retention, outstanding billing, egress, and provider/API quotas. Alerts are delayed notifications, not a shutdown automation.
