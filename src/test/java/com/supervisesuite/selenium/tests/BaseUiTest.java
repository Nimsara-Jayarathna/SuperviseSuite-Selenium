package com.supervisesuite.selenium.tests;

import com.supervisesuite.selenium.config.TestConfig;
import com.supervisesuite.selenium.driver.DriverFactory;
import com.supervisesuite.selenium.extensions.DriverHolder;
import com.supervisesuite.selenium.extensions.PreflightHealthExtension;
import com.supervisesuite.selenium.extensions.RetryExtension;
import com.supervisesuite.selenium.extensions.ScreenshotExtension;
import com.supervisesuite.selenium.extensions.TestNamingExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;

import java.time.Duration;

/**
 * Shared Selenium test base with centralized driver + config lifecycle.
 */
@ExtendWith({
        PreflightHealthExtension.class,
        RetryExtension.class,
        TestNamingExtension.class,
        ScreenshotExtension.class
})
public abstract class BaseUiTest {

    protected static WebDriver driver;

    @BeforeAll
    static void setUpDriver() {
        driver = DriverFactory.create(TestConfig.browser());
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(TestConfig.implicitWaitSeconds()));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(TestConfig.pageLoadTimeoutSeconds()));
        driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(TestConfig.scriptTimeoutSeconds()));
        try {
            driver.manage().window().maximize();
        } catch (WebDriverException e) {
            // Safari on macOS may reject maximize() — fall back to an explicit large size
            driver.manage().window().setSize(new Dimension(1440, 900));
        }
        DriverHolder.set(driver);
    }

    @AfterAll
    static void tearDownDriver() {
        if (driver != null) {
            driver.quit();
        }
    }

    protected String baseUrl() {
        return TestConfig.baseUrl();
    }
}
