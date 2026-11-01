package com.jobchef.shared.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jobchef.shared.error.ApiException;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class AdzunaJsonCompatibilityTest {
    @Test
    void decodesProviderTreesAndKeepsQuotaErrorsExplicit() throws Exception {
        var status = new AtomicInteger(200);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/api/jobs/gb/search/1", exchange -> {
            byte[] body = """
                    {"results":[
                      {"id":"job-1","title":"Developer","company":{"display_name":"Example"},
                       "location":{"display_name":"Remote"},"description":"Build software",
                       "salary_min":35000.5,"salary_max":null,"redirect_url":"https://example.invalid/job"},
                      {"id":"job-2","title":"Another developer"}
                    ]}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status.get(), body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();
        try {
            var properties = new AdzunaProperties(
                    "synthetic-id", "synthetic-key", "gb", List.of("gb"),
                    "http://127.0.0.1:" + server.getAddress().getPort());
            var provider = new AdzunaJobProvider(RestClient.builder(), properties);

            var jobs = provider.search("gb", "Developer", "", "", 1);

            assertEquals(1, jobs.size());
            assertEquals("Example", jobs.getFirst().company());
            assertEquals(Double.valueOf(35000.5), jobs.getFirst().salaryMin());
            assertNull(jobs.getFirst().salaryMax());
            assertEquals("GBP", jobs.getFirst().currency());

            status.set(429);
            ApiException failure = assertThrows(ApiException.class,
                    () -> provider.search("gb", "Developer", "", "", 1));
            assertEquals("ADZUNA_QUOTA_EXCEEDED", failure.code());
        } finally {
            server.stop(0);
        }
    }
}
