# Privacy, retention, and cost controls

## Data flow

| Data | Location and handling |
|---|---|
| Original PDF, extracted source text, contact fields, and local resume version | Android device only. No OCR is used. |
| Reviewed sanitized resume text | Sent to the API only after explicit user review; stored under the device ID in PostgreSQL. Regex redaction is heuristic and is not proof of anonymization. |
| Search role/location/keywords and Adzuna listings | Sent to Job Chef/Adzuna. The backend stores normalized public listings and a six-hour response cache keyed by a SHA-256 digest of normalized search parameters. |
| Generated resume JSON and cover letter | Stored in PostgreSQL as device-owned application output. The live worker checks for apparent email/phone/address patterns before persistence. |
| Sanitized preview PDF | Stored in a private local volume in demo or private GCS bucket in live mode; accessed only through a device-authenticated, ownership-checked API route. The bucket is never made public. |
| Final personalized PDF | Generated and previewed locally from the selected output and the matching local resume version. It is copied/shared only when the user chooses. |
| Device credential | Random opaque token encrypted with Android Keystore; backend stores only its SHA-256 hash. Never include a token in logs, examples, screenshots, or chat. |

Names, links, location, employment history, and narrative may still identify a person after email/phone masking. Always inspect the sanitized text. Gemini unpaid-service terms can permit provider use of inputs/outputs to improve products, depending on terms, account, and region. Use synthetic data until you have reviewed the current terms and are comfortable sending the reviewed text.

## Retention and deletion

- PostgreSQL resume, application, and generated-version data remain until the user deletes the relevant resume/application or the external database is removed.
- Live sanitized preview objects are private and lifecycle-deleted after 90 days. The API stops advertising/serving expired previews. Local preview artifacts are deleted when the relevant application/resume is deleted; the local named volume can be explicitly removed with `stop-local-demo.ps1 -DeleteLocalData`.
- Deleting a generated application removes its local draft immediately and persists a server-deletion event for offline replay. Resume deletion removes stored generated versions/previews and requeues still-saved applications only against the remaining active resume.
- Per-job daily regeneration usage is retained independently from application output, so deleting and saving the same job again does not reset that job's daily regeneration allowance.
- Successfully published outbox rows are pruned after seven days; failed/unpublished rows remain for retry/recovery.
- Search-cache responses expire after six hours and expired entries are pruned during later search requests.
- The project does not claim to undo data already processed by Adzuna or Gemini. Storage deletion cannot undo a job application submitted outside the app.
- Cloud Run logs use the project’s configured Cloud Logging bucket/retention. Review that retention in the deployment project; do not change a shared project’s global log bucket just for this project. A dedicated project makes cost and retention review safer.

## Free-tier-first deployment candidates

No provider/account or current free-tier eligibility was verified from this checkout. Candidate hosted services to evaluate on deployment day:

- **Neon PostgreSQL**: PostgreSQL wire protocol with TLS. Confirm current free-plan sleep/compute/storage limits, pooled/direct endpoint behavior, migration support, region, connection caps, and egress before use. Keep Hikari pools small (API 5, worker 3 maximum).
- **Upstash Redis**: TLS Redis-compatible endpoint. Before relying on its free plan, verify standard RESP access and support for `INCR`, `EXPIRE`, `EVAL`, `ZADD NX`, `ZCARD`, `ZREMRANGEBYSCORE`, `ZREM`, and `PEXPIRE`; Job Chef uses a Lua script and sorted-set leases. A REST-only endpoint or unsupported command subset is not compatible.

These are candidates, not promises of current pricing, eligibility, or compatibility. Free tiers may sleep, cap commands/storage/connections, require payment verification, or change. Keep database and Redis regions close to Cloud Run where practical; cross-provider traffic can add latency and egress charges. Do not substitute paid Cloud SQL or Memorystore.

## Budget and kill switch

INR 500 is the total additional infrastructure budget, not a recurring allowance. The deployment script creates a 500 INR Cloud Billing **notification** budget with 50%, 80%, and 100% current-spend thresholds. Notifications are delayed estimates, not hard caps; billing lag, minimum charges, taxes, and exchange rates can exceed the target. Confirm the billing currency and current cost calculator before deployment.

Cloud Run is configured to scale to zero, with API maximum two instances and worker maximum one; the worker concurrency is one. Generation and search quotas are application-level controls, not spending caps. Other costs include external PostgreSQL/Redis, Google Cloud Storage, Artifact Registry, Cloud Build, Cloud Logging, Pub/Sub, Cloud Scheduler, internet egress, and Gemini/Adzuna provider quotas. The worker image includes TeX Live, which is materially larger than the API image.

Pause/stop the app by disabling the Android URL or revoking device credentials. Operational kill switches: disable Cloud Run API/worker services, pause the Pub/Sub subscription, and disable the recovery/outbox Scheduler jobs. Do not assume disabling one service stops all billable resources.

## Credential handling

Never put live secret values in the repository, `.env.example`, a built APK, screenshots, or chat. The ignored local `.env` contains only deployment setting values/secret names as needed; actual API/database/Redis passwords belong in Secret Manager or local environment variables. Cloud Run reads secret versions by reference. `provision-enrollment.ps1` prints one random single-use code to the local terminal and stores only its hash; send that code directly to the intended test device owner, not to this chat.
