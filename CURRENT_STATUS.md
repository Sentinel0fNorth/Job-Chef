# Current implementation and verification status

## Readiness

| Area | Source status | Verified status |
|---|---|---|
| Android device enrollment, local resume vault, reviewed sanitization, offline decision/resume/application-deletion outboxes, search/swipe/saved states, editable PDF save/share | Implemented in source | Not built or emulator-tested |
| Spring API, PostgreSQL/Flyway, Redis limits, Adzuna adapter, device identity, decision/application state | Implemented in source | Not compiled or integration-tested |
| Separate Pub/Sub worker, Gemini adapter, local fixtures, idempotent claims/recovery, LaTeX sanitized preview | Implemented in source | Not compiled or exercised |
| Docker Compose, API/worker images, private local/GCS artifact adapters, Cloud Run deployment/teardown scripts | Added | Docker/build/deployment not run |
| API contract and transfer/privacy/cost documentation | Updated | Static configuration parsed; dependency artifact availability checked; runtime acceptance pending |

This is **implementation handover readiness, not a verified release**. No successful Android/backend build, automated test run, local-stack exercise, live Adzuna/Gemini request, APK, cloud deployment, or cost measurement is claimed. Do not copy a fabricated APK or infer verification from source files.

## Environment constraint

The bounded tool inventory found Java 11 instead of the now-configured JDK 25, no Android SDK, no installed Gradle executable, and no available Docker daemon. An earlier Gradle-wrapper distribution download timed out before compilation. Per the approved validation policy, further tool downloads/build retries must not block implementation.

The dependency upgrade was researched against official release pages and Maven repository metadata on November 1, 2026. The backend now targets Java 25 LTS, Gradle 9.8.2, Spring Boot 4.1.2 and Jackson 3.1.7 LTS; Android uses JDK 25 to run Gradle 9.7.0 while retaining Java 17 bytecode for Android compatibility. Version selection, compatibility exceptions, sources, and PostgreSQL volume migration precautions are recorded in `docs/TRANSFER_AND_DEPLOY.md`. Artifact availability and syntax checks do not establish a successful upgraded build.

The compatibility review verified 33 Android catalog and 17 backend dependency coordinates, both Gradle distribution checksums, BOM selections and official container tags. Syntax parsing passed for 56 Java and 70 Kotlin source files; TOML/YAML, Compose, Android XML, PowerShell and whitespace checks passed. Google Cloud dependencies are aligned with libraries BOM 26.91.0. These checks do not replace Java 25 compilation or Gradle's resolved dependency graph. The JDK 25 build preflight is blocked by the installed Java 11; bounded toolchain metadata requests failed with HTTP 403/incomplete response. JSON compatibility tests were added but not executed.

The follow-up import audit matched all 294 audited third-party import paths against 77 published source archives and matched internal imports against project declarations. It corrected missing Android Koin/Retrofit/Gson/Room compile dependencies and the shared backend's Servlet compile API, explicitly aligned AGP's built-in Kotlin compiler classpath, upgraded Android Gson to 2.15.0 and removed unused Coil. Syntax parsing now passes for 58 Java files, 71 Kotlin files and 12 Android build scripts. Dependency-visibility assertions, TOML and whitespace checks passed. New Adzuna/Gemini loopback HTTP and Android Gson regression tests are source-only and unexecuted; this is still not a successful build or resolved dependency graph. Details and receiving-device commands are in `docs/TRANSFER_AND_DEPLOY.md`.

## Implemented safeguards and configured limits

- Device identity is a revocable random token; the backend stores only its SHA-256 hash. Android encrypts the token with Android Keystore and refreshes it after an API 401. There is no account login or cross-device recovery.
- Original PDFs, source text, and detected contact fields remain on-device. Only the user-reviewed sanitized text is uploaded. Sanitization is heuristic, so users must inspect it; it does not prove anonymization.
- Search results are capped at 20 and cached for six hours. API search is rate-limited per device and globally.
- New saved applications are capped at five per device per day; explicit regeneration is capped at one per job per day; the live worker has a configurable global daily cap and concurrency leases.
- Saved intent and outbox records are persisted before dispatch. Worker claims are leased/idempotent; stale work is recoverable. A withdrawn save is checked again before output is committed.
- Sanitized resume versions are parsed once into structured PostgreSQL content; generated output is checked for apparent contact details. Provider calls reserve a durable daily call/token budget before requests. Private sanitized preview PDFs are produced with escaped LaTeX, shell escape disabled, and a bounded compile time. Final personalized PDFs are built and previewed on-device with contact details injected from local fields, outside the editable generated draft.
- Live provider errors remain errors; local synthetic fixtures are limited to the explicit `demo` profile. There is no automatic paid provider fallback.

## Tests and checks

Meaningful source tests now cover the Android reviewed-text sanitizer, fixture provider, device-token hashing, resume privacy checks, decision-batch rejection, local artifact storage/path traversal, LaTeX escaping with common Unicode punctuation, the device-revocation HTTP contract, and Jackson 3 record/nullable-field/timestamp JSON compatibility. They have **not** been executed here. The Room migration, Android ViewModel/WorkManager flows, real PostgreSQL/Redis/Pub/Sub recovery, generated PDF text/layout, and UI acceptance remain to be tested on the configured development device.

Use the commands and manual acceptance checklist in [`docs/TRANSFER_AND_DEPLOY.md`](docs/TRANSFER_AND_DEPLOY.md). Record each result as passed, failed, blocked, or credential-gated; never describe a demo fixture or static inspection as live verification.

## Remaining verification before calling V1 complete

1. On a device with JDK 25, Gradle 9.8.2 for backend, the Android Gradle 9.7.0 wrapper, Android SDK requirements, and Docker Compose, run backend unit tests, Android unit tests, Android debug assembly, and the local Compose stack.
2. Exercise enrollment, text-PDF import/review, search, save/undo, offline outbox replay, duplicate Pub/Sub delivery, quota exhaustion/retry, crash recovery, deletion, and regeneration.
3. Inspect saved/share PDFs with a text extractor and confirm the preview is the actual rendered PDF; test overflow, multiple pages, Unicode, and large text.
4. With user-supplied local secrets only, test Adzuna/Gemini quota and error behavior. Live provider and deployed Cloud Run behavior remain unverified until explicitly configured and run elsewhere.
5. Confirm current free-tier eligibility, costs, budget alerts, and teardown resources in the deployment account. GCP budget alerts are notifications, not a spending cutoff.
