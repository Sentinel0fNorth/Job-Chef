package com.jobchef.worker.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jobchef.shared.config.BackendProperties;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

class GeminiJsonCompatibilityTest {
    @Test
    void writesGenerationRequestAndDecodesStructuredResponseWithJackson3() throws Exception {
        var mapper = JsonMapper.builder().build();
        var resume = new GeminiClient.GeneratedResume(
                "Developer", "Builds software", List.of("Java"), List.of(), List.of());
        String generatedJson = mapper.writeValueAsString(
                new GeminiClient.GeneratedApplication(resume, "A review-ready draft."));
        byte[] response = mapper.writeValueAsString(Map.of(
                "candidates", List.of(Map.of(
                        "content", Map.of("parts", List.of(Map.of("text", generatedJson)))))))
                .getBytes(StandardCharsets.UTF_8);
        var requestBody = new AtomicReference<String>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1beta/models/synthetic-model:generateContent", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (var output = exchange.getResponseBody()) {
                output.write(response);
            }
        });
        server.start();
        try {
            var limits = new BackendProperties(new BackendProperties.Limits(
                    20, 5, 1, 20, 20000, 20000, 40000, 20000, 1, 20, 100000));
            var client = new GeminiClient(RestClient.builder(), mapper, limits,
                    "synthetic-key", "synthetic-model",
                    "http://127.0.0.1:" + server.getAddress().getPort());

            var generated = client.generate(
                    "Developer", "Example", "Build software", mapper.writeValueAsString(resume));

            assertEquals(resume, generated.resumeContent());
            assertEquals("A review-ready draft.", generated.coverLetter());
            var request = mapper.readTree(requestBody.get());
            assertTrue(request.path("contents").isArray());
            assertEquals("application/json",
                    request.path("generationConfig").path("responseMimeType").asText());
            assertTrue(request.path("generationConfig").path("responseSchema").isObject());
        } finally {
            server.stop(0);
        }
    }
}
