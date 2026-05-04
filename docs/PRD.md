# Product Requirements Document (PRD)

**Project Name:** The Offline-First Agentic Job Matcher  
**Document Version:** 2.0 (Finalized Architecture & JIT Generation)

## 1. Executive Summary

**Objective:** To build an end-to-end career-tech platform that sources live job postings via Adzuna and utilizes Just-In-Time (JIT) AI generation to dynamically tailor a candidate's resume and cover letter only for the roles they explicitly express interest in.

**The Solution:** An event-driven, asynchronous backend architecture that decouples job discovery from AI processing. Users review raw/summarized job listings in a native Android swipe interface. A right-swipe triggers a background Pub/Sub event, enqueuing the Gemini AI to generate customized application materials natively in the cloud.

## 2. Core Features & User Flow

### 2.1 The "Resume Vault" (Onboarding)
**Feature:** Users upload a base resume PDF. Local Android code extracts text and masks PII (emails/phones). A one-time backend AI call structures this text into a strict JSON schema and saves it to PostgreSQL.

### 2.2 Job Discovery (The Adzuna Engine)
**Feature:** The user submits a target role (e.g., "SDE Intern"). The backend queries the Adzuna API, applies a lightweight deduplication/normalization filter, and syncs 10-20 raw job summaries (Title, Company, Salary, Snippet) to the user's mobile device as UNSEEN jobs.

### 2.3 The "Swipe" Discovery Deck (UI/UX)
**Feature:** A gesture-based Compose card deck displaying UNSEEN jobs.
* **Swipe Left (REJECTED):** Job is hidden. Backend syncs state. No AI triggered.
* **Swipe Right (SAVED):** Job moves to "Saved" list. This triggers the Agentic Tailoring workflow.

### 2.4 Just-In-Time Agentic Tailoring (The AI Worker)
**Feature:** When the backend receives a SAVED state from the sync API, it publishes an event to Pub/Sub. The AI Worker consumes this, feeds the full Adzuna job description and the user's JSON base resume to Gemini 1.5 Flash, and generates tailored resume bullets and a cover letter.

### 2.5 Offline-First Synchronization & PDF Export
**Feature:** Room DB and WorkManager ensure the user can swipe and read through jobs offline. Once tailored materials are synced back to the device, the user can request a backend-generated, ATS-compliant LaTeX PDF of their tailored resume.

## 3. Security, Privacy & Limits

* **PII Masking:** Client-side Regex masks contact info before any data hits the network.
* **Rate Limiting:** The backend enforces strict rate limiting to protect the Adzuna API quota from abuse.
* **Secret Protection:** Zero hardcoded keys. All credentials are injected at runtime via GCP Secret Manager.
