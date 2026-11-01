# Transfer, local acceptance, and optional Cloud Run deployment

## Current handover state

This repository contains source, tests, local Compose configuration, container definitions, and deployment scripts. It has not been built or tested in the current checkout; no APK or container image is included. Before calling the implementation complete, run the checks below on the other device and record their exact results in `CURRENT_STATUS.md`. API keys alone do not provision the database, Redis, GCP identity, Pub/Sub, storage, or Android backend URL.

Do not commit/push this checkout, provision resources from the original device, or deploy before reviewing the account’s current free-tier/billing situation. Existing Git history is intentionally unchanged.

## Move the project safely

Copy the repository and unchanged `.git` directory to the development device. Do not transfer or include `.env`, private resumes, Android app data, `local.properties`, `.jobchef`, build outputs, Gradle caches, or credentials. `.gitignore` excludes local state and filled `.env`; keep the example file unfilled.

Required build-device tools:

- Android Studio with Android SDK compile platform 37, build tools, an emulator/device, and accepted Android SDK licenses.
- JDK 25 LTS for backend and Android Gradle. Install the current security patch for JDK 25 and select it as Android Studio's Gradle JDK; Android Gradle Plugin 9.3.1 and Gradle 9.7.0 are defined under `android/gradle`.
- Gradle 9.8.2 for backend, Docker with Compose v2 for local services, and optionally PostgreSQL `psql` for deployment enrollment provisioning.
- For live mode: authenticated `gcloud`, billing-enabled dedicated GCP project, external TLS PostgreSQL/Redis, and user-owned Adzuna/Gemini credentials.

## Dependency upgrade baseline (November 1, 2026)

Java and Jackson have explicit LTS releases; Gradle, Kotlin, AGP, AndroidX and most application libraries do not have equivalent LTS channels. Use published stable releases and compatible BOMs, not snapshots or release candidates.

| Component | Previous | Selected |
|---|---|---|
| Backend Java / container JRE; Android Gradle JVM | 21 | 25 LTS |
| Backend Gradle | 8.14.4 | 9.8.2 |
| Android Gradle | 9.5.0 | 9.7.0 |
| Spring Boot | 3.5.7 | 4.1.2 |
| Jackson | Boot 3-managed Jackson 2 | Boot 4-managed 3.1.7 LTS |
| Android Gradle Plugin | 9.2.0 | 9.3.1 |
| Kotlin / Compose compiler / serialization plugin | 2.3.21 | 2.4.21 |
| Compose BOM | 2026.04.01 | 2026.09.00 |
| Compose foundation | Individually pinned 1.11.0 | BOM-managed 1.12.1 |
| AndroidX Core KTX | 1.18.0 | 1.19.1 |
| Lifecycle runtime | 2.10.0 | 2.11.0 |
| Navigation Compose | 2.8.5 | 2.10.2 |
| Room | 2.8.4 | 2.8.5 |
| WorkManager | 2.10.0 | 2.12.0 |
| Koin | 4.2.1 | 4.2.2 |
| KSP | 2.3.7 | 2.3.13 |
| OkHttp BOM | 5.3.2 | 5.5.1 |
| Android Gson | Retrofit-transitive 2.13.1 | Explicit 2.15.0 |
| kotlinx.serialization | 1.6.3 | 1.12.0 |
| Google Cloud Storage client | 2.48.0 | 2.76.0 |
| Google Cloud Pub/Sub client | 1.134.0 | 1.158.0 |
| Google Cloud libraries BOM | None | 26.91.0 |
| Local PostgreSQL image | 16-alpine | 18.6-alpine |
| Local Redis image | 7-alpine | 8.10.3-alpine |

The latest AGP found was 9.5.0 and the latest Gradle was 9.8.2. Android deliberately selects AGP 9.3.1 and Gradle 9.7.0 because those are the upper bounds of Kotlin 2.4.21's published fully supported matrix. Newer combinations are not claimed compatible merely because their artifacts exist. The backend does not use Kotlin and can use Gradle 9.8.2. Both Gradle releases support running on JDK 25. Android's Java/Kotlin bytecode target stays 17: updating the host JVM does not make Java 25 bytecode suitable for Android devices. `compileSdk=37`, `targetSdk=36` and `minSdk=29` remain unchanged to avoid unrelated platform behavior changes.

Spring Boot 4's modular starters are used for MVC, RestClient, Jackson, Flyway and OAuth2 resource-server support. Application JSON code now uses Jackson 3's `tools.jackson` packages; the HTTP API version and stored JSON schemas are unchanged. The Boot BOM also upgrades Spring Framework to 7.0.10, Spring Security to 7.1.2, Flyway to 12.4.0, the PostgreSQL driver to 42.7.14, Lettuce to 7.5.2 and backend JUnit Jupiter to 6.0.3. These dependencies stay BOM-managed rather than independently overriding compatible versions. Google's libraries BOM 26.91.0 aligns Storage, Pub/Sub and shared GAX/gRPC/Protobuf/Guava dependencies; individual Google client version overrides were removed.

Retrofit 3.0.0, Activity Compose 1.13.0, PDFBox-Android 2.0.27.0, JUnit 4.13.2, AndroidX test JUnit 1.3.0, Espresso 3.7.0 and the Spring dependency-management plugin 1.1.7 were already at the latest published stable versions found and are unchanged. The Pub/Sub emulator uses Google's maintained rolling `google-cloud-cli:emulators` image; there is no separate emulator LTS pin. TeX Live packages continue to come from the selected Temurin container's supported OS repository.

Release and compatibility sources:

- [Java LTS roadmap](https://www.oracle.com/java/technologies/java-se-support-roadmap.html), [Gradle current release metadata](https://services.gradle.org/versions/current) and [Gradle Java compatibility](https://docs.gradle.org/current/userguide/compatibility.html).
- [Kotlin releases](https://kotlinlang.org/docs/releases.html), [Kotlin's Gradle/AGP matrix](https://kotlinlang.org/docs/gradle-configure-project.html) and [AGP 9.3 compatibility](https://developer.android.com/build/releases/agp-9-3-0-release-notes).
- [Spring Boot release metadata](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-gradle-plugin/maven-metadata.xml), [Boot 4 migration guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide) and [Jackson 3 migration/LTS policy](https://github.com/FasterXML/jackson/blob/main/jackson3/MIGRATING_TO_JACKSON_3.md).
- [Google Maven index](https://dl.google.com/dl/android/maven2/master-index.xml) and [Maven Central](https://repo.maven.apache.org/maven2/): selected stable artifacts/BOMs were checked directly against their published metadata and POMs.
- [Google Cloud's Java 25 support policy](https://docs.cloud.google.com/java/docs/supported-java-versions) and [Google's BOM compatibility guidance](https://docs.cloud.google.com/java/docs/bom).
- [PostgreSQL support/versioning](https://www.postgresql.org/support/versioning/), [PostgreSQL image layout](https://github.com/docker-library/docs/tree/master/postgres) and [Redis 8.10.3 release](https://github.com/redis/redis/releases/tag/8.10.3).

### Java 25 and library compatibility review

| Check | Result / replacement |
|---|---|
| Backend runtime and build tooling | Spring Boot 4.1.2 documents support through Java 27 and Gradle 9.x; Gradle supports Java 25 from 9.1 onward. The selected Java 25 / Gradle 9.8.2 / Boot 4.1.2 combination satisfies those published requirements. |
| JDK APIs removed since Java 21 | Source inspection found no direct use of SecurityManager, stopped/suspended-thread APIs, `sun.*`/`jdk.internal.*`, removed compiler/Nashorn APIs, or obsolete Java EE/JAXB/Servlet packages. Standard JDBC, `java.time`, crypto, process, file and networking APIs used here remain available. This does not prove that every transitive binary is free of internal JDK calls. |
| JSON and web APIs | Replaced `com.fasterxml.jackson.databind` with Jackson 3's `tools.jackson.databind`; Jackson annotations intentionally retain their `com.fasterxml.jackson.annotation` package. Updated Pub/Sub parsing to catch Jackson 3's unchecked `JacksonException`. Added record/nullable-field/ISO-timestamp JSON contract tests for execution on the receiving device. Replaced the old MVC/OAuth2 starters and added explicit Flyway, Jackson and RestClient starters required by Boot 4's modular auto-configuration. |
| Google Cloud clients | Google explicitly tests Java 25. Libraries BOM 26.91.0 selects matching Storage 2.76.0 and Pub/Sub 1.158.0 stacks: GAX 2.88.0, gRPC 1.83.0, Protobuf 4.33.6 and Guava 33.6.0-jre. Neither client requires replacement. The Google clients can still depend internally on Jackson 2; do not globally exclude it simply because application JSON uses Jackson 3's separate namespace. |
| Backend test instrumentation | Boot manages Mockito 5.23.0 and Byte Buddy 1.18.15. Byte Buddy's published compatibility table supports Java 25 from 1.17.0 onward, avoiding the older Java-21-only classfile parsing baseline. |
| Android host versus device runtime | Gradle runs on JDK 25, but Android source/bytecode remains Java 17. Android libraries run on ART, not the backend JVM. No library substitution can make backend Java 25 bytecode universally usable on the supported Android devices. Kotlin 2.4.21 and its serialization/Compose plugins are aligned; serialization 1.12.0 declares Kotlin 2.4.10, which the selected compiler can consume. |
| Android dependency consistency | All 33 catalog dependency coordinates were checked against published POMs, with unversioned Compose, Koin and OkHttp artifacts resolved through their BOMs. Foundation now uses the same 1.12.1 BOM version as Compose UI/runtime rather than a separate older pin. Existing JUnit 4/AndroidX test integration remains on its compatible stable line rather than being replaced with backend JUnit 6. |
| Containers and storage | Official Gradle/JDK 25, Temurin JRE 25, PostgreSQL 18.6 and Redis 8.10.3 tags were verified without pulling images. PostgreSQL 18 uses a different data layout and a new named volume; see the precautions below. |

No Java-25-incompatible direct application dependency without a suitable replacement was identified in this review. Deliberate exceptions are Android's Java 17 bytecode and the AGP/Gradle versions held within Kotlin's compatibility matrix. PDFBox-Android and the Android JUnit 4 integration have no newer compatible stable release required for this JVM upgrade and remain unchanged. Gemini's model is an explicit runtime configuration, not an LTS library pin; model availability remains credential-gated.

Evidence is limited to published compatibility policies, artifact/BOM checks, API source-signature checks, Java/Kotlin syntax parsing, and configuration validation. These are **not** a Gradle dependency graph, Java 25 compilation, executed tests, Android assembly or runtime acceptance. The current device only has Java 11 and no Android SDK/Docker daemon. A bounded attempt to locate a portable JDK 25 was blocked by HTTP 403 and an incomplete release-metadata response; no repeated toolchain downloads were attempted. Run the commands below on the configured device to detect any remaining linkage or runtime issue. For an explicit dependency graph, run `gradle :api:dependencies --configuration runtimeClasspath` and `gradle :worker:dependencies --configuration runtimeClasspath`, plus Android's `.\gradlew.bat :app:dependencies --configuration debugRuntimeClasspath`.

Additional references: [Boot 4.1.2 system requirements](https://github.com/spring-projects/spring-boot/blob/v4.1.2/documentation/spring-boot-docs/src/docs/antora/modules/ROOT/pages/system-requirements.adoc), [JDK 25 migration guide](https://docs.oracle.com/en/java/javase/25/migrate/index.html), and [Byte Buddy Java compatibility](https://github.com/raphw/byte-buddy#java-version-compatibility).

### Follow-up import and code accommodation audit

Published source archives for 77 library artifacts, including Android-specific variants, were inspected. All 294 distinct audited third-party import paths matched package/symbol source evidence: 99 backend and 195 Android paths. Internal `com.jobchef` imports also matched project declarations. Platform/JDK, Kotlin standard-library/coroutine and JUnit imports were reviewed separately; this source matching is not compiler symbol resolution, visibility checking or a complete Gradle classpath. The new loopback HTTP tests use the exported JDK `jdk.httpserver` module, not an internal `sun.*` API.

The module compile-dependency review exposed missing declarations that an artifact-existence check alone did not detect:

- `:core:network` now declares `koin-android`, which supplies its imported `androidContext` extension; `koin-core` alone does not.
- Gson is now explicitly versioned at stable 2.15.0 and declared in network, database and tailor, which use its classes directly. Database and tailor also directly declare Retrofit for `HttpException`; network's `implementation` dependencies do not become those consumers' compile dependencies.
- Database publishes Room runtime through `api`, since its public `JobChefDatabase` extends `RoomDatabase` and the resume feature uses `withTransaction`. Room remains at 2.8.5; no database schema or migration changed.
- Shared backend declares the BOM-managed Servlet API through `compileOnlyApi`, because its public device filter uses Servlet types. Spring Web/RestClient alone does not supply this API. API/worker MVC starters continue to provide the actual servlet-container runtime.
- Removed the unused, independently pinned Coil 2.6.0 dependency from saved opportunities; no source uses Coil or `AsyncImage`, so no replacement image library is needed.
- Android's root build explicitly supplies Kotlin Gradle plugin 2.4.21 on the buildscript classpath for AGP's built-in Kotlin, as recommended by Google's higher-KGP-version guidance. Removed the unused `kotlin-android` plugin alias rather than applying the legacy plugin alongside built-in Kotlin. Compose and serialization plugins remain aligned at 2.4.21.

Jackson 3 tree iteration, `path`/`asText`/numeric accessors, record serialization and mapper builder signatures, Spring RestClient timeouts/HTTP conversion and Security DSL, Google Storage/Pub/Sub builders, Koin ViewModel/worker DSL, Room transactions and current Compose layout calls were checked against published source signatures. No additional application API replacement was identified after the existing Jackson/Boot migration. Added synthetic loopback HTTP tests for Adzuna's Jackson 3 response decoding, result cap and quota error, and Gemini's request encoding/structured-response decoding. Added Android Gson tests for omitted nullable response fields and structured resume round trips used by offline storage/export. These tests do not contact real providers and remain **unexecuted** here.

Source syntax parsing passed for 58 Java files, 71 Kotlin files and 12 Android Kotlin build scripts. Updated TOML, dependency-visibility assertions, internal/external import source matching and whitespace checks passed. Java 25 compilation, annotation processing, resolved Gradle classpaths and device runtime verification remain blocked by the documented environment prerequisites. On the receiving device, also inspect `:core:network:dependencies`, `:core:database:dependencies` and `:feature:tailor:dependencies` with `--configuration debugCompileClasspath`; a resolved classpath and a successful build are the definitive checks for remaining linkage issues.

References: [AGP built-in Kotlin migration](https://developer.android.com/build/migrate-to-built-in-kotlin), [AGP higher-KGP-version configuration](https://developer.android.com/build/releases/agp-9-0-0-release-notes#runtime-dependency-on-kotlin-gradle-plugin), [Gson release metadata](https://repo.maven.apache.org/maven2/com/google/code/gson/gson/maven-metadata.xml), and the selected artifacts' published source archives/POMs in Maven Central and Google Maven. Audit downloads stayed in temporary session artifacts, not the repository or global build-tool installation.

### Existing local database volumes

Do not attach a PostgreSQL 16 data directory to PostgreSQL 18. A major database upgrade needs a dump/restore or `pg_upgrade`; Flyway does not upgrade the database engine. The updated Compose file uses a **new `postgres-18-data` volume**, mounted at `/var/lib/postgresql` as required by the PostgreSQL 18 image. The old `postgres-data` volume is preserved and is not used or deleted automatically. A new demo starts with an empty database and requires new device enrollment; old tokens point to the previous database. If retaining existing demo data, dump it using the old PostgreSQL 16 setup before switching, then restore into the new PostgreSQL 18 database and run Flyway migrations. Keep a verified backup until acceptance passes. Redis persistence should also be backed up before its major upgrade; do not downgrade a volume after Redis 8 has written to it.

`stop-local-demo.ps1 -DeleteLocalData` only deletes volumes referenced by the current Compose file after confirmation. It does not delete the preserved PostgreSQL 16 volume. Inspect any old volumes explicitly before deciding whether to remove them. External managed PostgreSQL/Redis versions are controlled by their providers; this change does not upgrade cloud databases.

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

With JDK 25 and Gradle 9.8.2:

```powershell
cd backend
gradle :shared:test :api:test :worker:test
cd ..\android
.\gradlew.bat :core:network:testDebugUnitTest :feature:resume:testDebugUnitTest :app:testDebugUnitTest
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
