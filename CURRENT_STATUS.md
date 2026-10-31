# Current implementation and verification status

## Readiness

| Area | Source status | Verified status |
|---|---|---|
| Android device enrollment, local resume vault, reviewed sanitization, offline decision/resume/application-deletion outboxes, search/swipe/saved states, editable PDF save/share | Implemented in source | Not built or emulator-tested |
| Spring API, PostgreSQL/Flyway, Redis limits, Adzuna adapter, device identity, decision/application state | Implemented in source | Not compiled or integration-tested |
| Separate Pub/Sub worker, Gemini adapter, local fixtures, idempotent claims/recovery, LaTeX sanitized preview | Implemented in source | Not compiled or exercised |
| Docker Compose, API/worker images, private local/GCS artifact adapters, Cloud Run deployment/teardown scripts | Added | Docker/build/deployment not run |
| API contract and transfer/privacy/cost documentation | Updated | Static files not yet externally validated |

This is **implementation handover readiness, not a verified release**. No successful Android/backend build, automated test run, local-stack exercise, live Adzuna/Gemini request, APK, cloud deployment, or cost measurement is claimed. Do not copy a fabricated APK or infer verification from source files.

## Environment constraint

The earlier bounded tool inventory found Java 11 instead of the backend’s configured Java 21, no Android SDK, no installed Gradle executable, and no available Docker daemon. An earlier Gradle-wrapper distribution download timed out before compilation. Per the approved validation policy, further tool downloads/build retries were not allowed to block implementation.

## Implemented safeguards and configured limits

- Device identity is a revocable random token; the backend stores only its SHA-256 hash. Android encrypts the token with Android Keystore and refreshes it after an API 401. There is no account login or cross-device recovery.
- Original PDFs, source text, and detected contact fields remain on-device. Only the user-reviewed sanitized text is uploaded. Sanitization is heuristic, so users must inspect it; it does not prove anonymization.
- Search results are capped at 20 and cached for six hours. API search is rate-limited per device and globally.
- New saved applications are capped at five per device per day; explicit regeneration is capped at one per job per day; the live worker has a configurable global daily cap and concurrency leases.
- Saved intent and outbox records are persisted before dispatch. Worker claims are leased/idempotent; stale work is recoverable. A withdrawn save is checked again before output is committed.
- Sanitized resume versions are parsed once into structured PostgreSQL content; generated output is checked for apparent contact details. Provider calls reserve a durable daily call/token budget before requests. Private sanitized preview PDFs are produced with escaped LaTeX, shell escape disabled, and a bounded compile time. Final personalized PDFs are built and previewed on-device with contact details injected from local fields, outside the editable generated draft.
- Live provider errors remain errors; local synthetic fixtures are limited to the explicit `demo` profile. There is no automatic paid provider fallback.

## Tests and checks

Meaningful source tests now cover the Android reviewed-text sanitizer, fixture provider, device-token hashing, resume privacy checks, decision-batch rejection, local artifact storage/path traversal, LaTeX escaping with common Unicode punctuation, and the device-revocation HTTP contract. They have **not** been executed here. The Room migration, Android ViewModel/WorkManager flows, real PostgreSQL/Redis/Pub/Sub recovery, generated PDF text/layout, and UI acceptance remain to be tested on the configured development device.

Use the commands and manual acceptance checklist in [`docs/TRANSFER_AND_DEPLOY.md`](docs/TRANSFER_AND_DEPLOY.md). Record each result as passed, failed, blocked, or credential-gated; never describe a demo fixture or static inspection as live verification.

## Remaining verification before calling V1 complete

1. On a device with JDK 21, Gradle 8.14.4, Android SDK/NDK requirements, and Docker Compose, run backend unit tests, Android unit tests, Android debug assembly, and the local Compose stack.
2. Exercise enrollment, text-PDF import/review, search, save/undo, offline outbox replay, duplicate Pub/Sub delivery, quota exhaustion/retry, crash recovery, deletion, and regeneration.
3. Inspect saved/share PDFs with a text extractor and confirm the preview is the actual rendered PDF; test overflow, multiple pages, Unicode, and large text.
4. With user-supplied local secrets only, test Adzuna/Gemini quota and error behavior. Live provider and deployed Cloud Run behavior remain unverified until explicitly configured and run elsewhere.
5. Confirm current free-tier eligibility, costs, budget alerts, and teardown resources in the deployment account. GCP budget alerts are notifications, not a spending cutoff.
