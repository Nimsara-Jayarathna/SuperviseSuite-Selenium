package com.supervisesuite.selenium.extensions;

import io.qameta.allure.Allure;
import io.qameta.allure.Story;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * JUnit 5 extension that:
 *  - Attaches a browser screenshot to the Allure report whenever a test fails.
 *  - Logs a "PASSED" / "SKIPPED" / "ABORTED" marker as an Allure text attachment
 *    for the other outcomes so the report always shows context.
 */
public class ScreenshotExtension implements TestWatcher {
    private static final Path ARTIFACT_ROOT = Path.of("target", "test-artifacts");
    private static final Pattern UNSAFE = Pattern.compile("[^a-z0-9._-]");

    private record TestMeta(String outcome, String category, String testName) {}

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        captureAndPublish(context, "failed");
    }

    @Override
    public void testSuccessful(ExtensionContext context) {
        captureAndPublish(context, "passed");
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

    private void captureAndPublish(ExtensionContext context, String outcome) {
        WebDriver driver = DriverHolder.get();
        if (driver instanceof TakesScreenshot ts) {
            byte[] raw = ts.getScreenshotAs(OutputType.BYTES);
            TestMeta meta = buildMeta(context, outcome);
            byte[] formatted = createLabeledPng(raw, meta);

            String attachmentName = "Screenshot - " + meta.outcome().toUpperCase() + ": " + context.getDisplayName();
            Allure.addAttachment(attachmentName, "image/png", new ByteArrayInputStream(formatted), "png");
            writeArtifact(formatted, meta);
        }
    }

    private TestMeta buildMeta(ExtensionContext context, String outcome) {
        String displayName = context.getDisplayName();
        String testName = sanitize(displayName);
        String category = context.getElement()
                .map(el -> el.getAnnotation(Story.class))
                .map(Story::value)
                .map(this::normalizeCategory)
                .orElse("general");
        return new TestMeta(outcome, category, testName);
    }

    private String normalizeCategory(String story) {
        String normalized = sanitize(story).replaceAll("_+", "_");
        return normalized.isBlank() ? "general" : normalized;
    }

    private String sanitize(String value) {
        return UNSAFE.matcher(value.toLowerCase()).replaceAll("_");
    }

    private byte[] createLabeledPng(byte[] rawPng, TestMeta meta) {
        try (InputStream in = new ByteArrayInputStream(rawPng);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            BufferedImage screenshot = ImageIO.read(in);
            if (screenshot == null) {
                return rawPng;
            }

            int width = screenshot.getWidth();
            int bannerHeight = 56;
            BufferedImage canvas = new BufferedImage(width, screenshot.getHeight() + bannerHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = canvas.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Color bannerColor = switch (meta.outcome()) {
                case "passed" -> new Color(30, 136, 229);
                case "failed" -> new Color(211, 47, 47);
                default -> new Color(97, 97, 97);
            };
            g.setColor(bannerColor);
            g.fillRect(0, 0, width, bannerHeight);
            g.drawImage(screenshot, 0, bannerHeight, null);

            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 18));
            String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String label = meta.outcome().toUpperCase() + " | " + meta.category().toUpperCase() + " | " + stamp;
            g.drawString(label, 16, 34);
            g.dispose();

            ImageIO.write(canvas, "png", out);
            return out.toByteArray();
        } catch (IOException ex) {
            return rawPng;
        }
    }

    private void writeArtifact(byte[] imageBytes, TestMeta meta) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path targetDir = ARTIFACT_ROOT.resolve(meta.outcome()).resolve(meta.category());
        Path targetFile = targetDir.resolve(timestamp + "-" + meta.testName() + ".png");
        try {
            Files.createDirectories(targetDir);
            try (InputStream in = new ByteArrayInputStream(imageBytes)) {
                Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ignored) {
            // Keep test flow uninterrupted even if artifact write fails.
        }
    }
}
