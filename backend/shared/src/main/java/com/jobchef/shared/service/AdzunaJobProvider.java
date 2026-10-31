package com.jobchef.shared.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.time.Duration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import com.jobchef.shared.error.ApiException;

@Component
@ConditionalOnProperty(name = "jobchef.mode", havingValue = "live", matchIfMissing = true)
public class AdzunaJobProvider implements JobProvider {
    private final RestClient restClient;
    private final AdzunaProperties properties;

    public AdzunaJobProvider(RestClient.Builder builder, AdzunaProperties properties) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(20));
        this.restClient = builder.requestFactory(requestFactory).baseUrl(properties.baseUrl()).build();
        this.properties = properties;
    }

    @Override
    public List<ExternalJob> search(
            String country, String role, String location, String keywords, int limit) {
        if (properties.appId() == null || properties.appId().isBlank()
                || properties.appKey() == null || properties.appKey().isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "ADZUNA_NOT_CONFIGURED",
                    "Live job search is unavailable because Adzuna credentials are not configured.");
        }
        if (!properties.supportedCountries().contains(country)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_COUNTRY",
                    "The requested country is not enabled for this Adzuna account.");
        }
        try {
            JsonNode root = restClient.get()
                    .uri(builder -> {
                        builder.path("/v1/api/jobs/{country}/search/1")
                                .queryParam("app_id", properties.appId())
                                .queryParam("app_key", properties.appKey())
                                .queryParam("what", role)
                                .queryParam("results_per_page", limit);
                        if (!location.isBlank()) builder.queryParam("where", location);
                        if (!keywords.isBlank()) builder.queryParam("what_and", keywords);
                        return builder.build(country);
                    })
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode results = root == null ? null : root.path("results");
            if (results == null || !results.isArray()) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "INVALID_PROVIDER_RESPONSE",
                        "Adzuna returned an invalid response.");
            }
            List<ExternalJob> jobs = new ArrayList<>();
            for (JsonNode item : results) {
                if (jobs.size() >= limit) break;
                jobs.add(map(item, country));
            }
            return List.copyOf(jobs);
        } catch (ApiException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            if (status == 429) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "ADZUNA_QUOTA_EXCEEDED",
                        "Adzuna rejected the search because its configured quota was reached.");
            }
            if (status == 401 || status == 403) {
                throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "ADZUNA_CREDENTIALS_INVALID",
                        "Live job search is unavailable because Adzuna rejected the configured credentials.");
            }
            throw new ApiException(HttpStatus.BAD_GATEWAY, "JOB_PROVIDER_UNAVAILABLE",
                    "Adzuna could not complete the search. No fixture listings were substituted.");
        } catch (RestClientException ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "JOB_PROVIDER_UNAVAILABLE",
                    "Adzuna could not complete the search. No fixture listings were substituted.");
        }
    }

    private ExternalJob map(JsonNode item, String country) {
        return new ExternalJob(
                item.path("id").asText(""),
                item.path("title").asText(""),
                item.path("company").path("display_name").asText(""),
                item.path("location").path("display_name").asText(""),
                item.path("description").asText(""),
                nullableDouble(item.path("salary_min")),
                nullableDouble(item.path("salary_max")),
                currency(country),
                item.path("redirect_url").asText(""));
    }

    private static Double nullableDouble(JsonNode node) {
        return node.isNumber() ? node.doubleValue() : null;
    }

    private static String currency(String country) {
        return switch (country) {
            case "gb" -> "GBP";
            case "us" -> "USD";
            case "in" -> "INR";
            case "au" -> "AUD";
            case "ca" -> "CAD";
            case "de", "fr", "nl" -> "EUR";
            case "nz" -> "NZD";
            case "sg" -> "SGD";
            case "za" -> "ZAR";
            default -> null;
        };
    }
}
