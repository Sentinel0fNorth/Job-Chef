# Job Chef

Job Chef is an offline-first Android college project for discovering a small batch of job listings, saving decisions locally, and reviewing synthetic or Gemini-generated application drafts. It is not an application-submission service and does not apply to jobs for the user.

## What is included

- **Android:** Kotlin, Jetpack Compose, Room, WorkManager, encrypted device credentials, text-based PDF import, reviewed on-device sanitization, offline decisions, and locally generated resume/cover-letter PDFs.
- **Backend:** Java 21 / Spring Boot 3.5, a device-authenticated API, a separate Pub/Sub generation worker, PostgreSQL/Flyway, Redis quotas, Adzuna search, Gemini structured output, and private sanitized PDF previews.
- **Local demo:** Docker Compose with PostgreSQL, Redis, the Pub/Sub emulator, API, worker, a private artifact volume, and visibly synthetic listings/generated drafts.
- **Transfer/deployment:** separate API/worker images, opt-in Cloud Run packaging, bounded quotas and retention, secrets-by-reference examples, and teardown guidance.

The source implementation has **not been built or runtime-verified** in this checkout. No APK is included. See [`CURRENT_STATUS.md`](CURRENT_STATUS.md) for the exact tool limits and deferred checks; do not treat source presence as a successful build, live integration, or deployment.

## Local demonstration

Prerequisites: Docker Compose, Android Studio/SDK for the configured compile SDK, JDK 21, and the Android Gradle wrapper.

From the repository root in PowerShell:

```powershell
.\backend\scripts\local-demo.ps1
```

The script starts only local synthetic services, waits for the API, and prints a one-use enrollment code. On an Android emulator build the debug app with the API URL pointed at the host emulator bridge:

```powershell
cd android
.\gradlew.bat :app:assembleDebug -PjobChefApiBaseUrl=http://10.0.2.2:8081/
```

The debug manifest permits cleartext only to local loopback/emulator hosts; release builds continue to require HTTPS. The app’s default backend URL is intentionally invalid. Stop services while retaining local demo data with `.\backend\scripts\stop-local-demo.ps1`; add `-DeleteLocalData` only when intentionally deleting the local database, Redis, and preview-artifact volumes.

For live-provider use, deployment and acceptance, read [`docs/TRANSFER_AND_DEPLOY.md`](docs/TRANSFER_AND_DEPLOY.md) and [`docs/PRIVACY_AND_COST.md`](docs/PRIVACY_AND_COST.md) first. Live operation needs external PostgreSQL and Redis, GCP resources/identity, Adzuna credentials, Gemini API access, secret versions, and a one-time enrollment code; API keys alone are not sufficient.

No commit, push, branch switch, cloud resource creation, or deployment is part of this checkout’s work.
