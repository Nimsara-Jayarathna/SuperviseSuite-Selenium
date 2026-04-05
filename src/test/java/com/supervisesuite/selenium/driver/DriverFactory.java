package com.supervisesuite.selenium.driver;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.safari.SafariDriver;

/**
 * Builds WebDriver instances based on configured browser type.
 */
public final class DriverFactory {

    private DriverFactory() {}

    public static WebDriver create(String browser) {
        return switch (browser) {
            case "chrome" -> buildChromeDriver();
            case "firefox" -> buildFirefoxDriver();
            case "safari" -> buildSafariDriver();
            default -> throw new IllegalArgumentException(
                    "Unsupported browser: " + browser + ". Use chrome, firefox, or safari.");
        };
    }

    private static WebDriver buildChromeDriver() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--no-sandbox", "--disable-dev-shm-usage");
        return new ChromeDriver(options);
    }

    private static WebDriver buildFirefoxDriver() {
        WebDriverManager.firefoxdriver().setup();
        return new FirefoxDriver();
    }

    private static WebDriver buildSafariDriver() {
        return new SafariDriver();
    }
}
