# Product requirements: Job Chef V1

## Purpose and boundaries

Job Chef is a college-project Android app that helps one registered device browse a bounded set of job listings and prepare reviewable application documents. It does not submit applications, provide a user account, synchronize a profile between devices, perform OCR, or promise that content is universally ATS-compatible.

## User flow

1. The user registers the installation with a provisioned, one-use code. The returned revocable device token is encrypted locally.
2. The user imports an unlocked, text-based PDF (up to 10 MB, 20 pages, and 50,000 extracted characters), reviews the extracted text and heuristic contact redactions, and selects a local active resume.
3. The app searches a supported Adzuna country for at most 20 jobs. Match explanations are deterministic token overlap, not AI predictions. Six-hour cache entries avoid repeated provider calls for identical search inputs.
4. Saves/rejections/undo are persisted in Room and synchronized through an offline outbox. A save is the explicit request for asynchronous tailoring.
5. The backend uses only reviewed sanitized resume text, saves typed generation output, and renders a private sanitized preview PDF. Android displays the actual rendered PDF pages and builds a personalized export locally with contact details from the exact matching resume version.
6. Users review, edit through the on-device workflow, save/share a resume or cover-letter PDF, or open a validated HTTPS provider listing. The app never reports that an application was submitted.

## Privacy and reliability requirements

- Source PDFs, contact details, and final personalized PDFs remain on-device unless the user shares an export.
- Obvious generated contact details are rejected before backend storage; heuristic sanitization is not a guarantee of anonymization.
- Device requests are authorized independently. No cross-device recovery is supported.
- Saved intent and outbox events are durable before asynchronous dispatch. Duplicate delivery, retries, stale-result isolation, and lease recovery are required.
- Synthetic data and model output are visibly labeled and isolated to the explicit demo profile.
- Provider, quota, storage, and configuration failures remain explicit; there is no automatic paid fallback.

## Fixed V1 bounds

- 20 results per search maximum.
- 5 new saved applications per device per day.
- 1 explicit regeneration per job per day.
- Configurable global daily generation cap (default 20) and concurrent generation cap (default 2).
- 50,000-character sanitized resume; 20,000-character job description; 60,000-character combined input; 24,000-character generated response.
- Local preview artifact is private and persistent; live GCS preview access is device-owned, and cloud artifact lifecycle deletes previews after 90 days.

## Release status

Source implementation and deployment packaging exist, but builds, tests, emulator acceptance, live provider requests, and cloud deployment remain unverified. See `CURRENT_STATUS.md` and `TRANSFER_AND_DEPLOY.md`.
