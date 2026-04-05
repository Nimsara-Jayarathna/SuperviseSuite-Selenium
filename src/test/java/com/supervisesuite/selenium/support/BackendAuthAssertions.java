package com.supervisesuite.selenium.support;

import com.supervisesuite.selenium.config.TestConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Optional backend-side auth assertions for stronger E2E validation.
 */
public final class BackendAuthAssertions {

    private BackendAuthAssertions() {}

    public static void assertLoginRole(String email, String password, String expectedRole) {
        if (!TestConfig.backendRoleAssertionEnabled()) {
            return;
        }

        String body = "{\"email\":\"" + escapeJson(email) + "\",\"password\":\"" + escapeJson(password) + "\"}";
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create(TestConfig.backendBaseUrl() + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .timeout(Duration.ofSeconds(10))
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Backend login assertion failed. status=" + response.statusCode());
            }
            if (!response.body().contains("\"role\":\"" + expectedRole + "\"")) {
                throw new IllegalStateException("Expected role " + expectedRole + " not found in backend response.");
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Backend role assertion failed: " + ex.getMessage(), ex);
        }
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
