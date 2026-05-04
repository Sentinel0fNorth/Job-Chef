# Software Requirements Specification (SRS)

**Project Name:** The Offline-First Agentic Job Matcher  
**Architecture:** Native Mobile Client + Event-Driven Microservices

## 1. Technical Stack (BOM-Managed Strategy)

| Layer | Technology | Target Version | Management Strategy |
| :--- | :--- | :--- | :--- |
| **Mobile Language** | Kotlin | 2.3.x | Managed via Gradle Version Catalogs (`libs.versions.toml`). |
| **Mobile UI/DB** | Jetpack Compose / Room | BOM / 2.8.4 | Compose versions inherited from BOM. Room explicitly declared for FTS4 support. |
| **Backend Language** | Java (LTS) | Java 25 | Unlocks Virtual Threads for concurrent Adzuna/Gemini calls. |
| **Backend Framework** | Spring Boot | 4.0.6 | Fallback to 3.5.14 if milestone dependencies break. Parent POM manages all transitive deps. |
| **Data & Migrations** | Hibernate / Flyway | v7.3 / 12.4 | Hibernate inherited from Spring Boot BOM. Flyway declared for strict schema versioning. |
| **Cloud DevOps** | Google Cloud Platform | N/A | Cloud Run (Compute), Pub/Sub (Eventing), Secret Manager (Security). |

## 2. API Integrations

### 2.1 Adzuna API (Data Sourcing)
* **Endpoint Target:** `GET /v1/api/jobs/{country}/search/1`
* **Query Params:** `app_id`, `app_key`, `what` (Target Title), `where` (Location), `results_per_page`.
* **Mapping Strategy:** Spring Boot will map the Adzuna JSON response natively to our `tailored_applications` table, capturing `redirect_url`, `salary_min`, and `description`.

### 2.2 Google Gemini API via Spring AI
* **Model:** `gemini-1.5-flash`
* **Execution:** Triggered only via Pub/Sub when a job status updates to `SAVED`. Utilizes Spring AI's `BeanOutputConverter` to force strict JSON output for the tailored resume.

## 3. Security & Infrastructure Specs

### 3.1 Rate Limiting (Bucket4j)
* **Implementation:** Bucket4j integrated into the Spring Boot API Gateway / Interceptor layer.
* **Rules:** 
    * **Mobile Ingestion/Sync API:** Limit users to X requests per minute based on IP or User ID to prevent DDoS and database exhaustion.
    * **Adzuna Shield:** Implement a global application-level bucket to ensure the backend never exceeds Adzuna's free-tier hourly request limit, utilizing our Tier 1 Database Cache first.

### 3.2 Secret Protection (GCP Secret Manager)
* **Rule:** No `.env` files or `application.properties` containing raw keys will be committed to GitHub.
* **Implementation:** The Spring Boot application will use the `spring-cloud-gcp-starter-secretmanager` dependency.
* **Properties Mapping:** `application.properties` will reference secrets natively:
```properties
spring.ai.vertex.ai.gemini.api-key=${sm://GEMINI_API_KEY}
adzuna.api.key=${sm://ADZUNA_API_KEY}
```

### 3.3 Authorization & Identity
* **Mobile to Backend:** API requests from the Android client must include a Bearer token (JWT or simple secure API Key) to prevent unauthorized endpoint execution.
* **Database Security:** Auto-incrementing Integers (1, 2, 3) are strictly forbidden. All database primary keys must be UUIDv4 to prevent Insecure Direct Object Reference (IDOR) attacks (e.g., guessing another user's `generation_job_id`).
* **CI/CD Security:** GitHub Actions will deploy to Google Cloud Run using Workload Identity Federation (WIF). No static JSON service accounts will be stored in GitHub Secrets.
