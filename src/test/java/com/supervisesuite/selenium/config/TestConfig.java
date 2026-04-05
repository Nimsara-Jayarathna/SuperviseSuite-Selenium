package com.supervisesuite.selenium.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Centralized test configuration from system properties.
 */
public final class TestConfig {

    private static final String DEFAULT_BROWSER = "chrome";
    private static final String DEFAULT_BASE_URL = "http://localhost:5173";
    private static final String DEFAULT_ALLOWED_BROWSERS = "chrome,firefox,safari";
    private static final long DEFAULT_IMPLICIT_WAIT_SECONDS = 5;
    private static final long DEFAULT_PAGE_LOAD_TIMEOUT_SECONDS = 30;
    private static final long DEFAULT_SCRIPT_TIMEOUT_SECONDS = 30;
    private static final long DEFAULT_MODAL_WAIT_SECONDS = 10;
    private static final String DEFAULT_SPEED_PROFILE = "normal";
    private static final int DEFAULT_MAX_RETRIES = 0;
    private static final String DEFAULT_TEST_NAME_PATTERN = "^TC-\\d+.*$";
    private static final Map<String, String> DOT_ENV = loadDotEnv();

    private TestConfig() {}

    public static String browser() {
        return getString("browser", DEFAULT_BROWSER).toLowerCase();
    }

    public static String baseUrl() {
        return getString("base.url", DEFAULT_BASE_URL);
    }

    public static Set<String> allowedBrowsers() {
        String configured = getString("allowed.browsers", DEFAULT_ALLOWED_BROWSERS);
        return Arrays.stream(configured.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());
    }

    public static long implicitWaitSeconds() {
        return parseLong("implicit.wait.seconds", DEFAULT_IMPLICIT_WAIT_SECONDS);
    }

    public static long pageLoadTimeoutSeconds() {
        return parseLong("page.load.timeout.seconds", DEFAULT_PAGE_LOAD_TIMEOUT_SECONDS);
    }

    public static long scriptTimeoutSeconds() {
        return parseLong("script.timeout.seconds", DEFAULT_SCRIPT_TIMEOUT_SECONDS);
    }

    public static long modalWaitSeconds() {
        return parseLong("modal.wait.seconds", DEFAULT_MODAL_WAIT_SECONDS);
    }

    public static long stepDelayMs() {
        String override = getOptional("step.delay.ms");
        if (override != null && !override.isBlank()) {
            return Long.parseLong(override);
        }
        return switch (speedProfile()) {
            case "fast" -> 0;
            case "slow" -> 1200;
            default -> 700;
        };
    }

    public static long charDelayMs() {
        String override = getOptional("char.delay.ms");
        if (override != null && !override.isBlank()) {
            return Long.parseLong(override);
        }
        return switch (speedProfile()) {
            case "fast" -> 0;
            case "slow" -> 80;
            default -> 60;
        };
    }

    public static String speedProfile() {
        return getString("speed.profile", DEFAULT_SPEED_PROFILE).toLowerCase();
    }

    public static String defaultStoryKey() {
        return getString("test.story.key", "UNASSIGNED");
    }

    public static int maxRetries() {
        return Integer.parseInt(getString("retry.max", String.valueOf(DEFAULT_MAX_RETRIES)));
    }

    public static boolean namingValidationEnabled() {
        return Boolean.parseBoolean(getString("naming.validation.enabled", "true"));
    }

    public static String testNamePattern() {
        return getString("test.name.pattern", DEFAULT_TEST_NAME_PATTERN);
    }

    public static boolean preflightEnabled() {
        return Boolean.parseBoolean(getString("preflight.enabled", "true"));
    }

    public static long preflightTimeoutSeconds() {
        return Long.parseLong(getString("preflight.timeout.seconds", "5"));
    }

    public static String backendBaseUrl() {
        return getString("backend.base.url", "http://localhost:8080");
    }

    public static boolean backendRoleAssertionEnabled() {
        return Boolean.parseBoolean(getString("backend.role.assertion.enabled", "false"));
    }

    private static long parseLong(String key, long fallback) {
        String value = getOptional(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return Long.parseLong(value);
    }

    public static String getString(String key, String fallback) {
        String value = getOptional(key);
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private static String getOptional(String key) {
        String systemProperty = System.getProperty(key);
        if (systemProperty != null && !systemProperty.isBlank()) {
            return systemProperty;
        }

        String envDirect = System.getenv(key);
        if (envDirect != null && !envDirect.isBlank()) {
            return envDirect;
        }

        String envMapped = System.getenv(toEnvKey(key));
        if (envMapped != null && !envMapped.isBlank()) {
            return envMapped;
        }

        String dotEnv = DOT_ENV.get(key);
        if (dotEnv != null && !dotEnv.isBlank()) {
            return dotEnv;
        }

        return null;
    }

    private static String toEnvKey(String key) {
        return key.toUpperCase().replace('.', '_');
    }

    private static Map<String, String> loadDotEnv() {
        Map<String, String> values = new HashMap<>();
        Path path = Path.of(".env");
        if (!Files.exists(path)) {
            return values;
        }

        try {
            for (String rawLine : Files.readAllLines(path)) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if (line.startsWith("export ")) {
                    line = line.substring("export ".length()).trim();
                }
                int equalsIndex = line.indexOf('=');
                if (equalsIndex <= 0) {
                    continue;
                }
                String key = line.substring(0, equalsIndex).trim();
                String value = line.substring(equalsIndex + 1).trim();
                values.put(key, unquote(value));
            }
        } catch (IOException ignored) {
            // Ignore .env read failures and continue with system/env/default values.
        }

        return values;
    }

    private static String unquote(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
