# Job Chef backend

The backend is a Java 21 / Spring Boot 3.5.7 multi-project Gradle build. `api` and `worker` are separate deployable applications; `shared` contains versioned API records, authentication, business logic, and Flyway migrations.

- `api`: opaque per-device bearer auth, Adzuna discovery, resume and decision sync, application state, private preview access, and durable outbox dispatch.
- `worker`: authenticated Pub/Sub push handling, bounded generation leases/recovery, Gemini structured output, privacy checks, and escaped LaTeX preview rendering.
- `shared`: PostgreSQL repositories/schema, Redis limits, device identity, and local/GCS private preview storage.

## Synthetic local demo

From the repository root on a machine with Docker Compose:

```powershell
.\backend\scripts\local-demo.ps1
```

The demo profile uses clearly labeled synthetic listings and generated drafts. It does not call Adzuna or Gemini. The script prints a single-use 24-hour enrollment code to the terminal only. Do not save it in the repository or send it in chat.

Run backend tests on a compatible machine with JDK 21 and Gradle 8.14.4:

```powershell
cd backend
gradle :shared:test :api:test :worker:test
```

Run the API and worker containers from the root compose file with `docker compose up --build`; the PowerShell helper performs the same startup plus enrollment provisioning.

Live mode requires external TLS PostgreSQL and Redis, a Pub/Sub topic, a private GCS bucket, Google service accounts/IAM, Secret Manager secret versions, Adzuna app ID/key, Gemini key/model, and one-time enrollment provisioning. Use [`../docs/TRANSFER_AND_DEPLOY.md`](../docs/TRANSFER_AND_DEPLOY.md). Never put secret values in source or send them through chat.
