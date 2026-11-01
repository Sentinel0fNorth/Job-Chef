package com.jobchef.shared.service;

import tools.jackson.databind.ObjectMapper;
import com.jobchef.shared.api.ApiModels.JobResponse;
import com.jobchef.shared.api.ApiModels.SearchRequest;
import com.jobchef.shared.api.ApiModels.SearchResponse;
import com.jobchef.shared.config.BackendProperties;
import com.jobchef.shared.error.ApiException;
import com.jobchef.shared.security.TokenHasher;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class JobDiscoveryService {
    private static final Set<String> SKILLS = Set.of(
            "java", "kotlin", "python", "typescript", "javascript", "spring",
            "android", "react", "postgresql", "redis", "docker", "kubernetes",
            "aws", "gcp", "sql", "rest", "graphql");
    private final JobProvider provider;
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final BackendProperties properties;
    private final AdzunaProperties adzunaProperties;

    public JobDiscoveryService(
            JobProvider provider,
            JdbcTemplate jdbc,
            ObjectMapper objectMapper,
            BackendProperties properties,
            AdzunaProperties adzunaProperties) {
        this.provider = provider;
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.adzunaProperties = adzunaProperties;
    }

    public SearchResponse search(UUID deviceId, SearchRequest request) {
        String country = (request.country() == null ? "" : request.country()).toLowerCase(Locale.ROOT);
        if (country.isBlank()) country = adzunaProperties.defaultCountry();
        if (country == null || country.isBlank()) country = "in";
        if (country.length() != 2 || !country.chars().allMatch(Character::isLetter)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_COUNTRY", "Choose a country supported by the configured job provider.");
        }
        int limit = Math.min(request.limit() == null ? 20 : request.limit(), properties.limits().maxSearchResults());
        String role = request.role().trim();
        String location = valueOrEmpty(request.location());
        String keywords = valueOrEmpty(request.keywords());
        String cacheKey = cacheKey(country, role, location, keywords, request.workMode(), limit);
        jdbc.update("DELETE FROM search_cache WHERE expires_at < now()");
        List<SearchResponse> cached = jdbc.query("""
                SELECT response_json::text FROM search_cache
                WHERE cache_key = ? AND expires_at > now()
                """, (rs, row) -> parseCachedResponse(rs.getString(1)), cacheKey);
        if (!cached.isEmpty()) return cached.getFirst();
        List<JobProvider.ExternalJob> results = provider.search(
                country, role, location, keywords, limit);
        List<JobResponse> response = new ArrayList<>(results.size());
        for (JobProvider.ExternalJob job : results) {
            validateExternalJob(job);
            String mode = workMode(job.title() + " " + job.description() + " " + job.location());
            if (request.workMode() != null && !"UNKNOWN".equals(request.workMode())
                    && !request.workMode().equals(mode)) continue;
            UUID id = upsert(job);
            List<String> skills = extractSkills(job.title() + " " + job.description());
            int matchScore = matchScore(request.role() + " " + valueOrEmpty(request.keywords()),
                    job.title() + " " + job.description());
            String matchMethod = "Deterministic token overlap: " + matchScore
                    + "%; not an AI prediction.";
            response.add(new JobResponse(
                    id, job.title(), job.company(), job.location(), job.description(),
                    job.salaryMin(), job.salaryMax(), job.currency(), job.redirectUrl(),
                    skills, mode, matchScore, matchMethod));
        }
        SearchResponse result = new SearchResponse(List.copyOf(response), provider.attribution(), Instant.now());
        try {
            jdbc.update("""
                    INSERT INTO search_cache(cache_key, response_json, expires_at)
                    VALUES (?, ?::jsonb, now() + interval '6 hours')
                    ON CONFLICT (cache_key) DO UPDATE SET
                        response_json = EXCLUDED.response_json,
                        expires_at = EXCLUDED.expires_at,
                        created_at = now()
                    """, cacheKey, objectMapper.writeValueAsString(result));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not cache the bounded job search response.", exception);
        }
        return result;
    }

    private String cacheKey(
            String country, String role, String location, String keywords, String workMode, int limit) {
        try {
            return TokenHasher.sha256(objectMapper.writeValueAsString(
                    List.of(country, role, location, keywords, valueOrEmpty(workMode), limit)));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not create the job search cache key.", exception);
        }
    }

    private SearchResponse parseCachedResponse(String json) {
        try {
            return objectMapper.readValue(json, SearchResponse.class);
        } catch (Exception exception) {
            throw new IllegalStateException("The cached job search response is invalid.", exception);
        }
    }

    private UUID upsert(JobProvider.ExternalJob job) {
        return jdbc.queryForObject("""
                INSERT INTO jobs(provider_id, title, company, location, description,
                                 salary_min, salary_max, currency, redirect_url, work_mode, skills)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)
                ON CONFLICT (provider_id) DO UPDATE SET
                    title = EXCLUDED.title,
                    company = EXCLUDED.company,
                    location = EXCLUDED.location,
                    description = EXCLUDED.description,
                    salary_min = EXCLUDED.salary_min,
                    salary_max = EXCLUDED.salary_max,
                    currency = EXCLUDED.currency,
                    redirect_url = EXCLUDED.redirect_url,
                    refreshed_at = now()
                RETURNING id
                """, UUID.class, job.providerId(), job.title(), job.company(), job.location(),
                job.description(), job.salaryMin(), job.salaryMax(), job.currency(),
                job.redirectUrl(), workMode(job.title() + " " + job.description() + " " + job.location()), skillsJson(job));
    }

    private void validateExternalJob(JobProvider.ExternalJob job) {
        if (job.providerId() == null || job.providerId().isBlank()
                || job.title() == null || job.title().isBlank()
                || job.company() == null || job.company().isBlank()
                || job.description() == null || job.description().length() > properties.limits().maxJobDescriptionCharacters()
                || job.redirectUrl() == null || !isProviderUrl(job.redirectUrl())) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "INVALID_PROVIDER_RESPONSE", "The job provider returned an unsupported listing.");
        }
    }

    private boolean isProviderUrl(String value) {
        try {
            URI uri = URI.create(value);
            String host = uri.getHost();
            String normalizedHost = host == null ? "" : host.toLowerCase(Locale.ROOT);
            return "https".equalsIgnoreCase(uri.getScheme())
                    && host != null
                    && uri.getUserInfo() == null
                    && (uri.getPort() == -1 || uri.getPort() == 443)
                    && (normalizedHost.matches("(?:[a-z0-9-]+\\.)*adzuna\\.(?:com|in|ca|de|fr|nl|sg)")
                        || normalizedHost.matches("(?:[a-z0-9-]+\\.)*adzuna\\.(?:co\\.uk|com\\.au|co\\.nz|co\\.za)"));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private List<String> extractSkills(String text) {
        String normalized = text.toLowerCase(Locale.ROOT);
        return SKILLS.stream().filter(skill -> normalized.contains(skill)).sorted().toList();
    }

    private String workMode(String text) {
        String normalized = text.toLowerCase(Locale.ROOT);
        if (normalized.contains("remote")) return "REMOTE";
        if (normalized.contains("hybrid")) return "HYBRID";
        if (normalized.contains("on-site") || normalized.contains("onsite")) return "ON_SITE";
        return "UNKNOWN";
    }

    private int matchScore(String query, String listing) {
        Set<String> terms = tokenize(query);
        if (terms.isEmpty()) return 0;
        Set<String> listingTerms = tokenize(listing);
        long matching = terms.stream().filter(listingTerms::contains).count();
        return (int) Math.round(matching * 100.0 / terms.size());
    }

    private Set<String> tokenize(String value) {
        return java.util.Arrays.stream(value.toLowerCase(Locale.ROOT).split("[^a-z0-9+#.]+"))
                .filter(term -> term.length() > 1)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private String skillsJson(JobProvider.ExternalJob job) {
        try {
            return objectMapper.writeValueAsString(extractSkills(job.title() + " " + job.description()));
        } catch (Exception ex) {
            throw new IllegalStateException("Could not encode deterministic skills", ex);
        }
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
