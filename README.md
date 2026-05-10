# Job Chef - The Offline-First Agentic Job Matcher

**Version:** 1.0 (Finalized Foundation & Architecture)

The "Agentic Job Matcher" is an end-to-end career-tech platform built for high-intent job seekers, specifically in the Software Engineering domain. It solves "application fatigue" by decoupling the job discovery phase from the tailoring phase. Users browse curated, highly relevant job listings in a tactile mobile interface, while delegating the tedious work of customizing resumes and cover letters to a cloud-native AI worker.

## 🌟 Core Philosophy

1. **The "Zero-Cost" OPEX Shield:** Architected to cost $0 to operate. It prioritizes deterministic local computing over generative AI for simple tasks, relying on caching, rate-limiting, and Just-In-Time (JIT) execution to protect free-tier quotas.
2. **Offline-First & Tactile:** The Android client is a native experience using local Jetpack Compose state management and a Room Database, ensuring seamless swiping and reading even with zero network connectivity.
3. **"Hybrid Tech-Luxury" Aesthetic:** A blend of corporate authority and high-tech depth. It uses a "Quiet Luxury" reading environment (creams, beiges) and a high-tech "Boardroom Slate" glassmorphic interface for active swiping and AI execution.

## 🛠 Architecture & Tech Stack

The application uses a **Native Mobile Client + Event-Driven Microservices** architecture.

### Mobile Client (Android)
* **Language:** Kotlin 2.3.x
* **UI Toolkit:** Jetpack Compose (BOM managed)
* **Local Database:** Room (with FTS4 support)

### Backend Services
* **Language:** Java 25 LTS (utilizing Virtual Threads)
* **Framework:** Spring Boot 4.0.6
* **Database:** PostgreSQL via Hibernate & Flyway versioning
* **Cloud Infrastructure (GCP):** Cloud Run (Compute), Pub/Sub (Eventing), Secret Manager (Security)

### Third-Party APIs
* **Job Sourcing:** Adzuna API (`GET /v1/api/jobs/...`)
* **AI Engine:** Google Gemini 1.5 Flash (via Spring AI)

## 🚀 Key Features & User Flow

1. **The Resume Vault:** Users upload a base PDF resume. Local Android code extracts text, masks PII, and the backend structures it into a strict JSON schema stored in PostgreSQL.
2. **Job Discovery (Adzuna Engine):** The backend queries Adzuna based on user preferences, applying lightweight deduplication and syncing 10-20 raw job summaries to the device.
3. **The Swipe Deck:** A gesture-based UI for reviewing unseen jobs.
   * *Swipe Left (REJECTED):* Job hidden, backend synced, no AI triggered.
   * *Swipe Right (SAVED):* Job moved to "Saved", triggering the Agentic Tailoring workflow.
4. **Just-In-Time (JIT) Agentic Tailoring:** A right-swipe triggers a Pub/Sub event. An AI Worker consumes this event, feeds the Adzuna job description and the user's base JSON resume to Gemini 1.5 Flash, and generates tailored resume bullets and a cover letter.
5. **PDF Export:** Tailored materials sync back to the device, allowing the user to request a backend-generated, ATS-compliant LaTeX PDF.

## 🎨 Design System

**Vibe:** "Executive Tech-Luxury" (Deep Navy/Slate + Creams)

* **Typography:** Playfair Display for headers, Inter for functional UI & body text.
* **Palette:**
  * *Base Theme:* Warm Cream (`#F9F8F6`), Deep Ink (`#1A1A1A`), Boardroom Slate (`#1B263B`).
  * *Active Theme (Swipe/AI):* Night Matte Black (`#121212`), Frosted Glass, Luminous Accents.
* **Interaction:** Heavy, tactile swiping for the deck. Smooth cross-fade transitions from light (reading) to dark (action) modes.

## 🔒 Security & Constraints

* **PII Masking:** Client-side Regex masking ensures contact information never touches the network.
* **Rate Limiting:** Bucket4j protects the backend from abuse and shields the Adzuna API quota using a global application-level bucket.
* **Secret Protection:** Zero hardcoded keys. All credentials injected at runtime via GCP Secret Manager.
* **Authorization:** API requests use Bearer tokens (JWT/API Keys). Database uses UUIDv4 for primary keys to prevent IDOR attacks.
* **Scope Limits:** No universal URL scraping, no in-app messaging, and locked to Adzuna for V1 sourcing.

## 📈 Progress & Milestones Achieved

* **Core Infrastructure & Backend:** Established Spring Boot foundations (Java 25 LTS, Spring Boot 4.0.6), verifying JPA entity modeling, statelessness adherence, and stable docker-compose configurations.
* **Android Architecture:** Successfully implemented Clean Architecture on the Android client (Kotlin 2.3.x), establishing Room database/network layers and fully migrating dependency injection from Hilt to Koin.
* **Performance & Security:** Configured and validated Redis-based horizontal rate limiters (Bucket4j) via JMeter load testing to protect Adzuna API limits, and implemented the Pub/Sub event-driven notification engine for JIT AI.
* **Native Design System:** Translated the "Executive Tech-Luxury" design philosophy into a Jetpack Compose Material 3 theme architecture with defined `Color` (Boardroom Slate, Warm Cream), `Type` (Playfair Display, Inter), and `Theme` foundations.
* **UI Implementation & Polish:** Built and rigorously refactored key screens following the PRD flow:
  * **Resume Vault:** Base resume upload UI with PII masking preparation.
  * **Search Brief:** Target role and location input cards.
  * **Swipe Deck:** The heavy, tactile gesture-based card deck for job review (Light/Dark mode transitions).
  * **Saved Jobs & Tailoring Lab:** The split-view UI for managing saved jobs and reviewing Gemini AI generated materials.
  * Centralized reusable UI elements (e.g., Screen headers, buttons) into the `core:design-system` module.
