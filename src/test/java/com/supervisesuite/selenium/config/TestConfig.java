package com.supervisesuite.selenium.config;

/**
 * Centralized test configuration from system properties.
 */
public final class TestConfig {

    private static final String DEFAULT_BROWSER = "chrome";
    private static final String DEFAULT_BASE_URL = "http://localhost:5173";
    private static final long DEFAULT_IMPLICIT_WAIT_SECONDS = 5;

    private TestConfig() {}

    public static String browser() {
        return System.getProperty("browser", DEFAULT_BROWSER).toLowerCase();
    }

    public static String baseUrl() {
        return System.getProperty("base.url", DEFAULT_BASE_URL);
    }

    public static long implicitWaitSeconds() {
        String value = System.getProperty("implicit.wait.seconds");
        if (value == null || value.isBlank()) {
            return DEFAULT_IMPLICIT_WAIT_SECONDS;
        }
        return Long.parseLong(value);
    }
}
