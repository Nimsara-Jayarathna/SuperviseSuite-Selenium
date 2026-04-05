package com.supervisesuite.selenium.extensions;

import org.openqa.selenium.WebDriver;

/**
 * Single static reference to the shared WebDriver instance.
 * Allows JUnit 5 extensions (which have no access to test fields)
 * to reach the driver without reflection.
 */
public final class DriverHolder {

    private static WebDriver driver;

    private DriverHolder() {}

    public static void set(WebDriver d) {
        driver = d;
    }

    public static WebDriver get() {
        return driver;
    }
}
