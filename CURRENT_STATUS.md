# Project Current Status: Job Chef (The Offline-First Agentic Job Matcher)

> **Last Updated:** October 2026  
> **Status:** Active Foundation & Android Client Milestone Complete; Backend & Cloud Services in Planning / Kickoff Phase  
> **Target Release:** v1.0 MVP  

---

## 1. Executive Project Overview

### What is Job Chef?
**Job Chef** is an end-to-end, privacy-focused, offline-first career platform designed specifically for high-intent Software Engineering candidates. It solves modern **"application fatigue"** by structurally decoupling job exploration from application tailoring:

1. **Discovery:** Candidates review live, curated software engineering positions sourced via the Adzuna API in a rapid, tactile mobile gesture deck.
2. **Intent Trigger:** Only jobs explicitly saved by the user (right-swipe) trigger compute-heavy AI processing.
3. **Just-In-Time (JIT) Tailoring:** A cloud-native AI pipeline (Google Gemini 1.5 Flash via Spring AI & Pub/Sub) extracts key skills and automatically crafts custom resume bullet points, cover letters, and compiles ATS-compliant LaTeX PDFs.

### The Three Core Philosophies (Guiding Architectural Pillars)
1. **The "Zero-Cost" OPEX Shield:** 
   The platform is architected to operate at **$0 running cost**. Deterministic local algorithms (regex sanitization, Java/Kotlin dictionary lookups) handle simple data processing. External API calls (Adzuna, Gemini) are cached, heavily rate-limited via Bucket4j, and executed asynchronously only on high-intent actions.
2. **Offline-First & Tactile:** 
   The mobile client is built natively using Jetpack Compose, Room (SQLite), and WorkManager. Candidates can browse, search, and manage dossiers without an active internet connection. Background sync handles batch fetching and status updates seamlessly.
3. **"Executive Tech-Luxury" Dual Aesthetic:** 
   Rejects standard generic startup palettes. Features a **Dual-State Theme**:
   - *Reading / Management (Warm Cream, Deep Slate `#1B263B`, Playfair Display & Inter typography)* for the Resume Vault, Search Brief, and Shortlist.
   - *Active Action / AI Generation (Night Matte Black `#121212`, Frosted Glassmorphism, Crisp White)* for the gesture Swipe Deck and Tailoring Lab.

---

## 2. System Architecture & Tech Stack

```
+----------------------------------------------------------------------------------+
|                                MOBILE CLIENT                                     |
|  Kotlin 2.3.x | Jetpack Compose (BOM) | Room 2.8.4 | Koin 4.2.1 | WorkManager    |
|                                                                                  |
|  [Resume Vault]   [Search Brief]   [Tactile Swipe Deck]   [Tailor Lab / Export]  |
|         │                 │                   │                     │            |
|         ▼                 ▼                   ▼                     ▼            |
|  +────────────────────────────────────────────────────────────────────────────+  |
|  |                 Offline-First Core (Room SQLite Database)                  |  |
|  +────────────────────────────────────────────────────────────────────────────+  |
+--------------------------------─────────┬────────────────────────────────────────+
                                          │ HTTPS (Bearer Auth / Device UUID)
                                          ▼
+----------------------------------------------------------------------------------+
|                            BACKEND (Target Cloud Run)                            |
|             Java 25 LTS (Virtual Threads) | Spring Boot 4.0.6 | Spring AI        |
|                                                                                  |
|  +─────────────────────────+  +──────────────────────────+  +─────────────────+  |
|  | Adzuna Proxy & Caching  |  |  Bucket4j Rate Limiter   |  | Event Publisher |  |
|  +───────────┬─────────────+  +──────────────────────────+  +────────┬────────+  |
+──────────────┼───────────────────────────────────────────────────────┼───────────+
               │                                                       │
      Fetches External Jobs                                   Publishes SAVED Event
               │                                                       │
               ▼                                                       ▼
      +─────────────────+                                     +─────────────────+
      |   Adzuna API    |                                     |  GCP Pub/Sub    |
      +─────────────────+                                     +────────┬────────+
                                                                       │
                                                              Consumes Asynchronously
                                                                       │
                                                                       ▼
                                                              +─────────────────+
                                                              | Gemini 1.5 Flash|
                                                              | (Spring AI JIT) |
                                                              +────────┬────────+
                                                                       │
                                                             Compiles ATS LaTeX PDF
                                                                       │
                                                                       ▼
                                                              +─────────────────+
                                                              | Cloud Storage / |
                                                              | PostgreSQL DB   |
                                                              +─────────────────+
```

### Component Details
| Component | Technology | Version / Spec | Responsibility |
| :--- | :--- | :--- | :--- |
| **Android Language** | Kotlin | 2.3.21 | Native Android development |
| **UI Toolkit** | Jetpack Compose | BOM 2026.04.01 | Declarative tactile user interface |
| **Dependency Injection** | Koin | 4.2.1 | Lightweight Kotlin-first DI |
| **Local Database** | Room | 2.8.4 | Offline-first SQLite persistence & reactive Flows |
| **Networking** | Retrofit + OkHttp | Retrofit 3.0.0 / OkHttp 5.3.2 | HTTP client with mock interceptor & logging |
| **Background Sync** | WorkManager | 2.10.0 | Scheduled job batch syncing |
| **Backend Language** | Java (LTS) | Java 25 | Virtual Threads for high-concurrency external I/O |
| **Backend Framework** | Spring Boot | 4.0.6 (planned) | Event-driven microservice & proxy |
| **Database & Migrations** | PostgreSQL + Flyway | Flyway 12.4 | UUIDv4 schema with JSONB document storage |
| **Cloud Infrastructure** | Google Cloud Platform | Cloud Run, Pub/Sub, Secret Manager | Zero-cost serverless event compute |
| **AI Engine** | Google Gemini 1.5 Flash | Spring AI | Asynchronous resume tailoring & cover letter generation |

---

## 3. Directory Structure & Current Contents

```
D:\Job Chef
├── .agent_skills/               # Engineering guides & reference rules for Android architecture
├── docs/                        # Architectural & product requirements documents
│   ├── CONCEPT.md               # Manifesto, OPEX shield, and design philosophy
│   ├── DESIGN.md                # "Executive Tech-Luxury" styling, typography & motion specs
│   ├── PRD.md                   # Product Requirements Document (V2.0)
│   └── SRS.md                   # Software Requirements Specification (BOM stack)
├── lovable_prototype/           # Initial web prototype & visual design references
│   ├── screenshots/             # Design reference captures of all core screens
│   └── src/                     # React + Vite + Tailwind prototype
├── android/                     # Production Android application (multi-module)
│   ├── app/                     # App entry point, MainActivity, NavHost, BottomNavBar
│   ├── core/
│   │   ├── database/            # Room DB, DAOs, Entities, Mappers, SessionManager, SyncWorker
│   │   ├── design-system/       # Material3 Theme, Colors, Typography, Shapes, ScreenHeader
│   │   ├── domain/              # Clean Architecture domain models & Repository interfaces
│   │   └── network/             # Retrofit API interface, DTOs, MockJobInterceptor
│   └── feature/
│       ├── discovery/           # Search input card & gesture-driven tactile Swipe Deck
│       ├── onboarding/          # Module scaffolded (pending UI)
│       ├── resume/              # Resume Vault screen & upload state management
│       ├── saved/               # Saved Opportunities shortlist screen & filter tabs
│       └── tailor/              # Split-view Tailoring Lab & LaTeX PDF preview export
├── backend/                     # Backend microservice root (currently pending initialization)
├── README.md                    # Repository introduction & architecture summary
└── CURRENT_STATUS.md            # This file
```

---

## 4. What Has Been Accomplished So Far

### A. Documentation & Specifications (100% Complete)
- **PRD & SRS:** Completed and ratified in `docs/PRD.md` and `docs/SRS.md`.
- **Design System Manifesto:** `docs/DESIGN.md` defines the complete color system, typography (Playfair Display + Inter), tactile card physics, and split-screen tailoring UX.
- **Cost & Security Philosophy:** `docs/CONCEPT.md` establishes the Zero-Cost OPEX shield, client-side PII masking, and UUIDv4 IDOR mitigation rules.

### B. Rapid Interactive Prototype (`lovable_prototype`) (100% Complete)
- Fully functioning web prototype built with React, Vite, Tailwind CSS, and Radix UI.
- Validated all 5 primary user workflows: Search Brief, Swipe Deck, Resume Vault, Saved Opportunities list, and Tailor/Export preview.
- High-resolution visual screenshots saved in `lovable_prototype/screenshots/`.

### C. Android Client Implementation (~85% Complete)
- **Architecture & Build Setup:**
  - Modern Gradle Version Catalog (`libs.versions.toml`) configuring AGP 9.2.0, Kotlin 2.3.21, Compose BOM 2026.04.01, and Koin 4.2.1.
  - Multi-module modularization with separation of concerns (`:core:*` vs `:feature:*`).
- **Core Design System (`:core:design-system`):**
  - Custom Material 3 theme (`JobMatcherTheme`) supporting both Warm Cream light mode and Night Matte Black active mode.
  - Typography scale configured for Playfair Display and Inter.
  - Shared UI components: `ScreenHeader`, custom pills, badges, and the centralized `UiNotificationManager` for snackbars.
- **Core Domain Layer (`:core:domain`):**
  - Models: `Job`, `TailoredApplication`, `WorkMode`, `JobStatus`, `GenerationStatus`.
  - Interfaces: `JobRepository`, `TailoredApplicationRepository`.
- **Core Database & Persistence (`:core:database`):**
  - Room database (`JobChefDatabase` v2) with automatic migrations (`Migration1To2` renaming `description` to `snippet`).
  - `JobDao`: Unseen deck query (`LIMIT 20`), Saved query, LIKE keyword search, status update, and cleanup queries.
  - `TailoredApplicationDao`: Persistence and status updates for AI tailoring output.
  - `SyncJobsWorker`: WorkManager worker for background fetching.
  - `SessionManager`: Local persistent zero-auth device UUID generation.
  - Robust type converters and safe enum fallback parsers (`runCatching`).
- **Core Networking (`:core:network`):**
  - `JobChefApiService`: Retrofit endpoints for job discovery, status polling, and generation requests.
  - `MockJobInterceptor`: Realistic local Indian tech market mock payloads (Razorpay, CRED, Zepto, PhonePe, Groww) enabling offline UI/UX validation without live API keys.
- **Feature Modules:**
  - **`:feature:resume` (Resume Vault):** Resume listing, active resume selection, stubbed PDF upload flow with error handling via `UiNotificationManager`.
  - **`:feature:discovery` (Search Brief & Swipe Deck):**
    - Search input card for role, location, keywords, suggested filter chips, and WorkManager sync triggering.
    - Interactive `SwipeDeckScreen` featuring tactile spring physics (`dampingRatio = 0.8f`, `stiffness = 400f`), rotation angles, gesture threshold detection, multi-card layered depth, and action buttons (Reject, Save, Rewind).
  - **`:feature:saved` (Saved Shortlist):** List of saved opportunities with status filters (`All`, `Ready`, `Tailoring`), salary formatting, and click-through navigation.
  - **`:feature:tailor` (Tailoring Lab & Export Preview):**
    - `TailorScreen`: Split-view UI comparing source Job Description with AI-tailored resume bullets and cover letter tabs.
    - `ExportScreen`: High-fidelity A4 aspect ratio preview of LaTeX-compiled resume, action sheet for downloading PDF/Cover letter and deep-linking to the application.
- **App Shell & Navigation (`:app`):**
  - Type-safe Compose Navigation 2.8+ (`JobMatcherNavHost.kt`, `Screen.kt`).
  - Persistent `BottomNavBar` with synchronized active state highlighting.
  - Edge-to-edge layout integration and Scaffold error snackbar observation.

---

## 5. Known Issues, Bugs & Technical Debt (Requires Attention)

The following items were identified during the codebase audit and must be addressed:

| Severity | Issue Description | Affected Files | Impact | Recommended Solution |
| :--- | :--- | :--- | :--- | :--- |
| **High** | **Swipe Deck Card Skipping Bug** | `SwipeDeckViewModel.kt` | Swiping a card increments `currentIndex` while Room concurrently emits a new list that already removes the swiped card. This causes the next card to be skipped. | Track active card IDs in-memory or decouple the swipe index from the reactive Room query. |
| **High** | **Swipe Deck Rewind Inoperable** | `SwipeDeckViewModel.kt`, `JobDao.kt` | Tapping "Rewind" decrements index, but the previous job was already marked `SAVED`/`REJECTED` in Room, so it is missing from `getUnseenJobs()`. | Maintain an in-memory stack of recent session swipes to allow reversing status back to `UNSEEN`. |
| **Medium** | **Inconsistent API Endpoint Prefixes** | `JobChefApiService.kt` | Mix of `api/v1/jobs` vs `v1/api/applications/generate`. | Standardize all endpoints on `api/v1/...` across Android and backend specs. |
| **Medium** | **Saved Opportunities Random Score Shuffling** | `SavedOpportunitiesViewModel.kt` | `matchScore = Random.nextInt(80, 99)` runs inside `map` on every database emission, causing all scores to randomize when any job updates. | Store `matchScore` in `JobEntity` or calculate deterministically from skills match. |
| **Medium** | **Hardcoded Tailoring Status** | `SavedOpportunitiesViewModel.kt` | Status is hardcoded to `JobStatus.READY` instead of observing `TailoredApplicationRepository`. | Combine `JobDao.getSavedJobs()` with `TailoredApplicationDao.getAllApplications()` via Flow zip/combine. |
| **Low** | **Reflection Worker Resolution** | `SearchViewModel.kt` | Uses `Class.forName("com.jobchef.core.database.worker.SyncJobsWorker")` to avoid module coupling. | Move shared worker constants/factory to `:core:domain` or use standard WorkManager request builders. |
| **Low** | **Empty `:feature:onboarding` Module** | `feature/onboarding/` | Directory exists in Gradle settings but has no source files. | Implement first-time onboarding flow or remove module from `settings.gradle.kts` until needed. |

---

## 6. What Needs To Be Done (Future Development Roadmap)

### Phase 1: Android Client Finalization & Polish
1. **Fix Swipe Deck State Management:**
   - Resolve the card skipping and rewind bugs in `SwipeDeckViewModel.kt`.
   - Add card swipe sound/haptic feedback for tactile feel.
2. **Real PDF File Ingestion & Client-Side PII Masking:**
   - Implement Android `ActivityResultContracts.GetContent()` for selecting `.pdf` files.
   - Use Android `PdfRenderer` or Apache PDFBox-Android / Poppler to extract raw text locally.
   - Apply client-side regex to mask emails (`[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}`) and phone numbers before sending to backend.
3. **Connect Tailoring Lab to ViewModels:**
   - Create `TailorViewModel` and `ExportViewModel` wired into Koin.
   - Connect `TailorScreen` to `TailoredApplicationRepository` to load real generated bullets and cover letters from Room/backend.
4. **File Download & PDF Sharing:**
   - Implement native file download and sharing intent for the generated LaTeX PDF in `ExportScreen`.

### Phase 2: Backend Infrastructure & Spring Boot Development (`backend/`)
1. **Initialize Spring Boot Project:**
   - Java 25 LTS, Spring Boot 4.0.6, Spring Web, Spring Data JPA, Spring AI.
   - Parent POM / Gradle build with Docker containerization for GCP Cloud Run.
2. **Database Schemas & Flyway Migrations:**
   - Define PostgreSQL tables: `users` (device UUIDs), `base_resumes` (JSONB), `jobs`, `tailored_applications`.
   - Flyway migration scripts adhering to UUIDv4 primary keys.
3. **Adzuna Sourcing & Ingestion Engine:**
   - Secure server-side call to `GET https://api.adzuna.com/v1/api/jobs/...`.
   - Deterministic Java `HashSet` skill extractor and work mode classifier.
   - Tier-1 PostgreSQL cache to shield Adzuna free-tier limits.
4. **Rate Limiting & Security:**
   - Integrate Bucket4j with Redis to limit requests per device and protect external quotas.
   - Integrate `spring-cloud-gcp-starter-secretmanager` for zero hardcoded secrets.

### Phase 3: Cloud Eventing & Agentic AI Tailoring Pipeline
1. **GCP Pub/Sub Integration:**
   - Publish `JobSavedEvent` when mobile client syncs a right-swiped job.
   - Cloud Run worker subscribes to the topic.
2. **Gemini 1.5 Flash Worker:**
   - Spring AI prompt template combining candidate's parsed JSON resume + full job description.
   - Enforce structured JSON output via `BeanOutputConverter`:
     - Tailored accomplishment bullet points.
     - Customized professional cover letter.
     - Match explanation & keyword alignment.
3. **LaTeX PDF Rendering Engine:**
   - Lightweight headless LaTeX compilation container (or Pandoc/Typst equivalent) generating ATS-compliant single-page PDFs.
   - Store generated PDFs in Google Cloud Storage with temporary pre-signed download URLs.

### Phase 4: DevOps, CI/CD & Production Launch
1. **Cloud Run Deployment:**
   - Configure GCP Cloud Run with "Startup CPU Boost" to mitigate Java JVM cold-start latency.
2. **GitHub Actions CI/CD:**
   - Workload Identity Federation (WIF) setup for secure keyless deployments to GCP.
   - Automated Android unit & instrumentation testing pipelines.
3. **Room FTS5 Transition:**
   - Upgrade SQLite queries in `JobDao` to FTS5 full-text indexing once Room + KSP toolchain compatibility is validated.

---

## 7. Quick Start Guide for New Developers / Agents

### Prerequisites
- **Android Studio:** Ladybug / Meerkat (or newer) supporting Kotlin 2.3+.
- **JDK:** OpenJDK 17 (for Android Gradle builds) and JDK 25 (for upcoming backend development).
- **Android SDK:** `compileSdk = 37`, `minSdk = 29`.

### Building & Running the Android App
```bash
# Navigate to the Android project root
cd "d:\Job Chef\android"

# Run tests
./gradlew test

# Build debug APK
./gradlew assembleDebug
```
*Note: The Android application currently runs with `MockJobInterceptor` enabled by default in `NetworkModule.kt`, allowing instant offline demoing of the discovery deck and navigation flows without requiring a backend server or live API keys.*

### Key Files to Inspect First
- **App Shell:** `android/app/src/main/java/com/jobchef/MainActivity.kt` & `navigation/JobMatcherNavHost.kt`
- **Swipe Interaction:** `android/feature/discovery/src/main/java/com/jobchef/feature/discovery/presentation/SwipeDeckScreen.kt`
- **Local Database:** `android/core/database/src/main/java/com/jobchef/core/database/JobChefDatabase.kt` & `dao/JobDao.kt`
- **Design Tokens:** `android/core/design-system/src/main/java/com/jobchef/core/designsystem/theme/`
- **Architecture Manifesto:** `docs/CONCEPT.md` & `docs/PRD.md`
