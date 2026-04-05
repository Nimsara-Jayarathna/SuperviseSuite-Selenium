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
import org.openqa.selenium.WebDriver;

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
        driver.manage().window().maximize();
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
