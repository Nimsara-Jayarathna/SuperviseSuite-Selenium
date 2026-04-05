package com.supervisesuite.selenium.extensions;

import com.supervisesuite.selenium.annotations.UserStory;
import com.supervisesuite.selenium.config.TestConfig;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Severity;
import io.qameta.allure.Story;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;

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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * JUnit 5 extension that:
 *  - Attaches a browser screenshot to the Allure report on every test outcome.
 *  - Writes a proof artifact + rich REPORT.md per browser run, including the
 *    @Description test criteria, severity, and failure reason for each test.
 */
public class ScreenshotExtension implements TestWatcher {
    private static final Path ARTIFACT_ROOT = Path.of("target", "test-artifacts");
    private static final Path PROOFS_ROOT = Path.of(TestConfig.getString("proofs.dir", "proofs"));
    private static final boolean PROOFS_ENABLED = Boolean.parseBoolean(TestConfig.getString("proofs.enabled", "true"));
    private static final String RUN_ID = TestConfig.getString(
            "proofs.run.id",
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
    );
    private static final Pattern UNSAFE = Pattern.compile("[^a-zA-Z0-9._-]");
    private static final Map<String, List<ReportEntry>> REPORT_ENTRIES = new HashMap<>();

    // -----------------------------------------------------------------------
    // Internal data models
    // -----------------------------------------------------------------------

    private record TestMeta(
            String outcome,
            String storyKey,
            String suiteName,
            String browserName,
            String category,
            String testName,
            String displayName,
            String description,
            String severity
    ) {}

    private record ReportEntry(
            String testName,
            String displayName,
            String outcome,
            String category,
            String description,
            String severity,
            String failureReason,
            String timestamp,
            Path relativeImagePath
    ) {}

    // -----------------------------------------------------------------------
    // TestWatcher callbacks
    // -----------------------------------------------------------------------

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        String reason = cause == null ? "" : flattenMessage(cause);
        captureAndPublish(context, "failed", reason);
    }

    @Override
    public void testSuccessful(ExtensionContext context) {
        captureAndPublish(context, "passed", null);
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        String reason = cause == null ? "no cause" : cause.getMessage();
        Allure.addAttachment("Aborted", "text/plain", "Test was aborted: " + reason);
        TestMeta meta = buildMeta(context, "aborted");
        byte[] card = createStatusCard(meta, "ABORTED", reason);
        publishImage(context, meta, card, reason);
    }

    @Override
    public void testDisabled(ExtensionContext context, Optional<String> reason) {
        Allure.addAttachment("Disabled", "text/plain",
                "Test disabled: " + reason.orElse("no reason given"));
        TestMeta meta = buildMeta(context, "disabled");
        byte[] card = createStatusCard(meta, "DISABLED", reason.orElse("No reason"));
        publishImage(context, meta, card, reason.orElse(""));
    }

    // -----------------------------------------------------------------------

    private void captureAndPublish(ExtensionContext context, String outcome, String failureReason) {
        WebDriver driver = DriverHolder.get();
        if (driver instanceof TakesScreenshot ts) {
            byte[] raw = ts.getScreenshotAs(OutputType.BYTES);
            TestMeta meta = buildMeta(context, outcome);
            byte[] formatted = createLabeledPng(raw, meta);
            publishImage(context, meta, formatted, failureReason);
        }
    }

    private void publishImage(ExtensionContext context, TestMeta meta, byte[] imageBytes, String failureReason) {
        String attachmentName = "Screenshot - " + meta.outcome().toUpperCase() + ": " + context.getDisplayName();
        Allure.addAttachment(attachmentName, "image/png", new ByteArrayInputStream(imageBytes), "png");
        Path artifactPath = writeArtifact(imageBytes, meta);
        writeProofArtifactAndReport(imageBytes, meta, artifactPath.getFileName().toString(), failureReason);
    }

    // -----------------------------------------------------------------------
    // Meta extraction
    // -----------------------------------------------------------------------

    private TestMeta buildMeta(ExtensionContext context, String outcome) {
        String displayName = context.getDisplayName();
        String testName = sanitize(displayName);
        String suiteName = context.getTestClass()
                .map(Class::getSimpleName)
                .map(this::sanitize)
                .orElse("unknown_suite");
        String browserName = resolveBrowserName();
        String category = context.getElement()
                .map(el -> el.getAnnotation(Story.class))
                .map(Story::value)
                .map(this::normalizeCategory)
                .orElse("general");
        String storyKey = resolveStoryKey(context);
        String description = context.getElement()
                .map(el -> el.getAnnotation(Description.class))
                .map(Description::value)
                .orElse("");
        String severity = context.getElement()
                .map(el -> el.getAnnotation(Severity.class))
                .map(s -> s.value().name().toLowerCase())
                .orElse("");
        return new TestMeta(outcome, storyKey, suiteName, browserName, category,
                testName, displayName, description, severity);
    }

    // -----------------------------------------------------------------------
    // Image generation
    // -----------------------------------------------------------------------

    private byte[] createLabeledPng(byte[] rawPng, TestMeta meta) {
        try (InputStream in = new ByteArrayInputStream(rawPng);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            BufferedImage screenshot = ImageIO.read(in);
            if (screenshot == null) return rawPng;

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
            String label = meta.outcome().toUpperCase() + " | " + meta.storyKey() + " | "
                    + meta.category().toUpperCase() + " | " + stamp;
            g.drawString(label, 16, 34);
            g.dispose();

            ImageIO.write(canvas, "png", out);
            return out.toByteArray();
        } catch (IOException ex) {
            return rawPng;
        }
    }

    private byte[] createStatusCard(TestMeta meta, String status, String message) {
        int width = 1400;
        int height = 420;
        BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Color bg = switch (meta.outcome()) {
            case "disabled" -> new Color(84, 110, 122);
            case "aborted" -> new Color(255, 143, 0);
            default -> new Color(97, 97, 97);
        };
        g.setColor(bg);
        g.fillRect(0, 0, width, height);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 44));
        g.drawString(status + " | " + meta.storyKey() + " | " + meta.category().toUpperCase(), 44, 90);

        g.setFont(new Font("SansSerif", Font.PLAIN, 28));
        g.drawString("Suite: " + meta.suiteName() + " | Browser: " + meta.browserName(), 44, 150);
        g.drawString("Test: " + meta.testName(), 44, 205);
        g.drawString("Time: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), 44, 260);
        g.drawString("Reason: " + trimForCard(message), 44, 320);
        g.dispose();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(canvas, "png", out);
            return out.toByteArray();
        } catch (IOException ex) {
            return new byte[0];
        }
    }

    // -----------------------------------------------------------------------
    // Artifact writing
    // -----------------------------------------------------------------------

    private Path writeArtifact(byte[] imageBytes, TestMeta meta) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path targetDir = ARTIFACT_ROOT
                .resolve(meta.storyKey())
                .resolve(meta.suiteName())
                .resolve(meta.browserName())
                .resolve(meta.outcome())
                .resolve(meta.category());
        Path targetFile = targetDir.resolve(timestamp + "-" + meta.testName() + ".png");
        try {
            Files.createDirectories(targetDir);
            try (InputStream in = new ByteArrayInputStream(imageBytes)) {
                Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ignored) {}
        return targetFile;
    }

    private void writeProofArtifactAndReport(byte[] imageBytes, TestMeta meta,
                                              String fileName, String failureReason) {
        if (!PROOFS_ENABLED) return;
        Path proofBrowserDir = PROOFS_ROOT
                .resolve(meta.storyKey())
                .resolve(RUN_ID)
                .resolve(meta.suiteName())
                .resolve(meta.browserName());
        Path proofImageDir = proofBrowserDir
                .resolve(meta.outcome())
                .resolve(meta.category());
        Path proofImagePath = proofImageDir.resolve(fileName);
        try {
            Files.createDirectories(proofImageDir);
            try (InputStream in = new ByteArrayInputStream(imageBytes)) {
                Files.copy(in, proofImagePath, StandardCopyOption.REPLACE_EXISTING);
            }
            Path relativeImagePath = proofBrowserDir.relativize(proofImagePath);
            appendReportEntryAndRewrite(meta, relativeImagePath, proofBrowserDir, failureReason);
        } catch (IOException ignored) {}
    }

    // -----------------------------------------------------------------------
    // Report generation
    // -----------------------------------------------------------------------

    private void appendReportEntryAndRewrite(TestMeta meta, Path relativeImagePath,
                                              Path proofBrowserDir, String failureReason) {
        String key = meta.storyKey() + "|" + meta.suiteName() + "|" + meta.browserName();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        synchronized (REPORT_ENTRIES) {
            List<ReportEntry> entries = REPORT_ENTRIES.computeIfAbsent(key, k -> new ArrayList<>());
            entries.add(new ReportEntry(
                    meta.testName(), meta.displayName(), meta.outcome(), meta.category(),
                    meta.description(), meta.severity(),
                    failureReason == null ? "" : failureReason,
                    timestamp, relativeImagePath));
            rewriteReport(meta, proofBrowserDir, entries);
        }
    }

    private void rewriteReport(TestMeta meta, Path proofBrowserDir, List<ReportEntry> entries) {
        List<ReportEntry> sorted = new ArrayList<>(entries);
        sorted.sort(Comparator.comparing(ReportEntry::timestamp).thenComparing(ReportEntry::testName));
        long passed = sorted.stream().filter(e -> "passed".equals(e.outcome())).count();
        long failed = sorted.stream().filter(e -> "failed".equals(e.outcome())).count();

        StringBuilder md = new StringBuilder();

        // ── Header ───────────────────────────────────────────────────────────
        md.append("# Selenium Proof Report\n\n");
        md.append("| | |\n|---|---|\n");
        md.append("| **Story** | `").append(meta.storyKey()).append("` |\n");
        md.append("| **Suite** | `").append(meta.suiteName()).append("` |\n");
        md.append("| **Browser** | `").append(meta.browserName()).append("` |\n");
        md.append("| **Run ID** | `").append(RUN_ID).append("` |\n");
        md.append("| **Commit** | `").append(resolveGitCommit()).append("` |\n");
        md.append("| **Result** | ")
                .append(passed).append(" ✅ passed, ")
                .append(failed).append(" ❌ failed")
                .append(" (").append(sorted.size()).append(" total)")
                .append(" |\n\n");

        // ── Summary table ────────────────────────────────────────────────────
        md.append("## Summary\n\n");
        md.append("| # | Test Case | Outcome | Category | Severity |\n");
        md.append("|---|---|---|---|---|\n");
        for (int i = 0; i < sorted.size(); i++) {
            ReportEntry e = sorted.get(i);
            String emoji = outcomeEmoji(e.outcome());
            String anchor = mdAnchor(e.displayName() + " — " + e.outcome());
            md.append("| ").append(i + 1).append(" | ")
                    .append("[").append(mdEscape(e.displayName())).append("](#").append(anchor).append(") | ")
                    .append(emoji).append(" `").append(e.outcome()).append("` | ")
                    .append("`").append(e.category()).append("` | ")
                    .append(e.severity().isBlank() ? "—" : "`" + e.severity() + "`")
                    .append(" |\n");
        }

        // ── Detailed results ─────────────────────────────────────────────────
        md.append("\n## Test Results\n\n");
        for (ReportEntry e : sorted) {
            String emoji = outcomeEmoji(e.outcome());
            String imageRef = e.relativeImagePath().toString().replace('\\', '/');

            md.append("### ").append(mdEscape(e.displayName())).append(" — ").append(e.outcome()).append("\n\n");
            md.append("| Field | Value |\n|---|---|\n");
            md.append("| **Outcome** | ").append(emoji).append(" `").append(e.outcome().toUpperCase()).append("` |\n");
            md.append("| **Category** | `").append(e.category()).append("` |\n");
            if (!e.severity().isBlank()) {
                md.append("| **Severity** | `").append(e.severity()).append("` |\n");
            }
            md.append("| **Captured** | `").append(e.timestamp()).append("` |\n\n");

            if (!e.description().isBlank()) {
                md.append("**Test criteria:** ").append(e.description()).append("\n\n");
            }

            if (!e.failureReason().isBlank()) {
                String escaped = e.failureReason()
                        .replace("&", "&amp;")
                        .replace("<", "&lt;")
                        .replace(">", "&gt;")
                        .replace("\n", " ")
                        .replace("\r", "");
                md.append("> ❌ **Failure reason:** ").append(escaped).append("\n\n");
            }

            md.append("![").append(e.testName()).append("](").append(imageRef).append(")\n\n");
            md.append("---\n\n");
        }

        Path reportPath = proofBrowserDir.resolve("REPORT.md");
        Path summaryPath = proofBrowserDir.resolve("SUMMARY.json");
        try {
            Files.createDirectories(proofBrowserDir);
            Files.writeString(reportPath, md.toString());
            Files.writeString(summaryPath, buildSummaryJson(meta, sorted, passed, failed));
        } catch (IOException ignored) {}
    }

    // -----------------------------------------------------------------------
    // Utilities
    // -----------------------------------------------------------------------

    private String outcomeEmoji(String outcome) {
        return switch (outcome) {
            case "passed" -> "✅";
            case "failed" -> "❌";
            case "aborted" -> "⚠️";
            case "disabled" -> "⏭️";
            default -> "❓";
        };
    }

    /**
     * Produces a GitHub-compatible Markdown heading anchor from a heading string.
     * Lowercases, removes characters that aren't letters/digits/spaces/hyphens,
     * then replaces spaces with hyphens.
     */
    private String mdAnchor(String heading) {
        return heading.toLowerCase()
                .replaceAll("[^a-z0-9 \\-]", "")
                .trim()
                .replaceAll("\\s+", "-");
    }

    /**
     * Escapes pipe characters in Markdown table cells so they don't break column
     * boundaries.  Other Markdown characters (e.g. →, @) render fine inside tables.
     */
    private String mdEscape(String text) {
        return text == null ? "" : text.replace("|", "\\|");
    }

    /**
     * Extracts the most meaningful single-line message from a throwable.
     * Prefers the deepest cause's message; falls back up the chain if null.
     */
    private String flattenMessage(Throwable t) {
        Throwable deepest = t;
        while (deepest.getCause() != null) {
            deepest = deepest.getCause();
        }
        String msg = deepest.getMessage();
        if (msg == null || msg.isBlank()) {
            msg = t.getMessage();
        }
        if (msg == null || msg.isBlank()) {
            msg = t.getClass().getSimpleName();
        }
        // Keep only the first meaningful line (skip blank / stack lines)
        for (String line : msg.split("\\n")) {
            String trimmed = line.trim();
            if (!trimmed.isBlank() && !trimmed.startsWith("at ")) {
                return trimmed;
            }
        }
        return msg.trim();
    }

    private String trimForCard(String text) {
        if (text == null || text.isBlank()) return "n/a";
        return text.length() > 120 ? text.substring(0, 117) + "..." : text;
    }

    private String buildSummaryJson(TestMeta meta, List<ReportEntry> sorted, long passed, long failed) {
        return "{\n"
                + "  \"story\": \"" + meta.storyKey() + "\",\n"
                + "  \"suite\": \"" + meta.suiteName() + "\",\n"
                + "  \"browser\": \"" + meta.browserName() + "\",\n"
                + "  \"runId\": \"" + RUN_ID + "\",\n"
                + "  \"commit\": \"" + resolveGitCommit() + "\",\n"
                + "  \"captures\": " + sorted.size() + ",\n"
                + "  \"passed\": " + passed + ",\n"
                + "  \"failed\": " + failed + "\n"
                + "}\n";
    }

    private String resolveGitCommit() {
        String fromEnv = TestConfig.getString("git.commit", "");
        if (!fromEnv.isBlank()) return fromEnv;
        Path head = Path.of(".git", "HEAD");
        try {
            if (!Files.exists(head)) return "unknown";
            String headValue = Files.readString(head).trim();
            if (headValue.startsWith("ref:")) {
                String ref = headValue.substring(5).trim();
                Path refPath = Path.of(".git").resolve(ref);
                if (Files.exists(refPath)) return Files.readString(refPath).trim();
            }
            return headValue;
        } catch (IOException ignored) {
            return "unknown";
        }
    }

    private String resolveStoryKey(ExtensionContext context) {
        Optional<UserStory> methodStory = context.getElement()
                .map(el -> el.getAnnotation(UserStory.class));
        if (methodStory.isPresent()) return normalizeStoryKey(methodStory.get().value());

        Optional<UserStory> classStory = context.getTestClass()
                .map(c -> c.getAnnotation(UserStory.class));
        if (classStory.isPresent()) return normalizeStoryKey(classStory.get().value());

        return normalizeStoryKey(TestConfig.defaultStoryKey());
    }

    private String normalizeStoryKey(String raw) {
        String sanitized = sanitize(raw).replaceAll("_+", "_");
        return sanitized.isBlank() ? "UNASSIGNED" : sanitized.toUpperCase();
    }

    private String normalizeCategory(String story) {
        String normalized = sanitize(story).replaceAll("_+", "_");
        return normalized.isBlank() ? "general" : normalized;
    }

    private String sanitize(String value) {
        return UNSAFE.matcher(value).replaceAll("_").toLowerCase();
    }

    private String resolveBrowserName() {
        WebDriver driver = DriverHolder.get();
        if (driver instanceof RemoteWebDriver remoteWebDriver) {
            Capabilities capabilities = remoteWebDriver.getCapabilities();
            if (capabilities != null && capabilities.getBrowserName() != null) {
                String browser = sanitize(capabilities.getBrowserName()).replaceAll("_+", "_");
                return browser.isBlank() ? sanitize(TestConfig.browser()) : browser;
            }
        }
        return sanitize(TestConfig.browser());
    }
}
