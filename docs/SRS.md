# Software requirements: Job Chef V1

## Technology and runtime

| Layer | Configuration |
|---|---|
| Android | Kotlin 2.4.21, Android Gradle Plugin 9.3.1, Gradle 9.7.0, Jetpack Compose BOM 2026.09.00, Room 2.8.5, WorkManager 2.12.0, Koin 4.2.2, PDFBox-Android |
| Backend | Java 25 LTS, Spring Boot 4.1.2, Jackson 3.1 LTS, Gradle 9.8.2, PostgreSQL 18.6/Flyway, Redis 8.10.3 |
| Async processing | PostgreSQL outbox -> Pub/Sub -> separate Cloud Run worker; local Pub/Sub emulator |
| Document processing | On-device PDFBox text extraction and Android `PdfDocument` export; backend LaTeX preview compiled with `-no-shell-escape` and a 20-second timeout |
| External services | Adzuna for live job search; Gemini API for structured application drafts; private GCS artifacts in live mode |
| Local development | Docker Compose services: API, worker, PostgreSQL, Redis, Pub/Sub emulator; named artifact volume |

Android compile SDK is 37, target SDK 36, and minimum SDK 29. Configure the local backend URL through Gradle property `jobChefApiBaseUrl` or environment variable `JOB_CHEF_API_BASE_URL`. Debug-only cleartext access is limited to emulator/loopback hosts; live builds must use HTTPS.

## API and ownership

The public API is versioned under `/api/v1`. Device tokens are random, revocable 256-bit opaque values; only their SHA-256 hashes are persisted. Registration requires a limited-use enrollment-code hash. Token refresh rotates the credential. Device-owned resources are always selected with the authenticated device ID.

The OpenAPI contract is `api/v1/openapi.yaml`. Android DTOs and shared Java records must remain synchronized. Sanitized preview PDFs are streamed only after device ownership checks; buckets are not public and raw storage object keys are not returned.

## Data and quotas

PostgreSQL stores devices, enrollment hashes, sanitized resume versions, public job listings, device decisions, applications, generated versions, quotas, outbox records, six-hour search-cache responses, and private preview keys. Redis stores search/global request counters and bounded worker leases. Flyway migrations are append-only for schema evolution.

Default limits are 20 search results, five newly saved applications per device per day, one regeneration per job per day, 20 global generations per day, two concurrent generations, 50,000 resume characters, 20,000 job-description characters, 60,000 total generation-input characters, 24,000 output characters, and 10 outbox records per dispatch batch.

## Failure behavior

Missing credentials/providers produce explicit errors. Redis failure blocks guarded operations rather than bypassing quotas. Pub/Sub publication uses a durable outbox; the worker uses a lease, bounded retries, duplicate-safe version writes, and scheduled stale-lease recovery. Synthetic fixtures are enabled only by `demo`; they are not a live-provider fallback.

## Privacy and retention

Only reviewed sanitized resume text is uploaded. Contact details and source PDFs stay local; generated content is checked for obvious contact data, but no heuristic can prove anonymization. Search-cache entries expire after six hours; successfully published outbox rows are pruned after seven days; live preview artifacts expire after 90 days and are also deleted after application/resume deletion when storage is available. Final personalized PDFs remain on-device unless explicitly shared.

## Verification state

Automated source tests exist but were not run in this checkout. Android/database migration, service integration, PDF layout, emulator, live API, and cloud acceptance are pending as described in `CURRENT_STATUS.md`.
