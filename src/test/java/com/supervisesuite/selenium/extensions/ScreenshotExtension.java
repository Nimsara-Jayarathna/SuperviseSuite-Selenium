package com.supervisesuite.selenium.extensions;

import io.qameta.allure.Allure;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.ByteArrayInputStream;
import java.util.Optional;

/**
 * JUnit 5 extension that:
 *  - Attaches a browser screenshot to the Allure report whenever a test fails.
 *  - Logs a "PASSED" / "SKIPPED" / "ABORTED" marker as an Allure text attachment
 *    for the other outcomes so the report always shows context.
 */
public class ScreenshotExtension implements TestWatcher {

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        attachScreenshot("Screenshot — FAILED: " + context.getDisplayName());
    }

    @Override
    public void testSuccessful(ExtensionContext context) {
        // Attach a screenshot for the passing state too — useful for demos
        attachScreenshot("Screenshot — PASSED: " + context.getDisplayName());
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        Allure.addAttachment("Aborted", "text/plain", "Test was aborted: " + cause.getMessage());
    }

    @Override
    public void testDisabled(ExtensionContext context, Optional<String> reason) {
        Allure.addAttachment("Disabled", "text/plain",
                "Test disabled: " + reason.orElse("no reason given"));
    }

    // -----------------------------------------------------------------------

    private void attachScreenshot(String name) {
        WebDriver driver = DriverHolder.get();
        if (driver instanceof TakesScreenshot ts) {
            byte[] bytes = ts.getScreenshotAs(OutputType.BYTES);
            Allure.addAttachment(name, "image/png", new ByteArrayInputStream(bytes), "png");
        }
    }
}
