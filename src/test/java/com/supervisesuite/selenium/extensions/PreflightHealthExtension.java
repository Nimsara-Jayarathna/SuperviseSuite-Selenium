package com.supervisesuite.selenium.extensions;

import com.supervisesuite.selenium.config.TestConfig;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Fast-fail environment checks before browser startup.
 */
public class PreflightHealthExtension implements BeforeAllCallback {

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        if (!TestConfig.preflightEnabled()) {
            return;
        }

        checkUrlReachable(TestConfig.baseUrl(), "frontend");
        checkUrlReachable(TestConfig.backendBaseUrl(), "backend");
    }

    private void checkUrlReachable(String url, String system) throws Exception {
        Exception firstError = null;
        try {
            probe(url, system);
            return;
        } catch (Exception ex) {
            firstError = ex;
        }

        String fallback = localhostFallback(url);
        if (fallback != null) {
            probe(fallback, system);
            return;
        }

        throw firstError;
    }

    private void probe(String url, String system) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TestConfig.preflightTimeoutSeconds()))
                .build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(TestConfig.preflightTimeoutSeconds()))
                .build();

        HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
        int status = response.statusCode();
        if (status >= 500) {
            throw new IllegalStateException(
                    "Preflight failed for " + system + " url=" + url + ", status=" + status
            );
        }
    }

    private String localhostFallback(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host == null) {
                return null;
            }
            String fallbackHost;
            if ("127.0.0.1".equals(host)) {
                fallbackHost = "localhost";
            } else if ("localhost".equalsIgnoreCase(host)) {
                fallbackHost = "127.0.0.1";
            } else {
                return null;
            }
            return new URI(
                    uri.getScheme(),
                    uri.getUserInfo(),
                    fallbackHost,
                    uri.getPort(),
                    uri.getPath(),
                    uri.getQuery(),
                    uri.getFragment()
            ).toString();
        } catch (URISyntaxException ignored) {
            return null;
        }
    }
}
