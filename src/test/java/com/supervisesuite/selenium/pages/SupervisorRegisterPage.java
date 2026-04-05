package com.supervisesuite.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * Page Object for /register/supervisor using Selenium PageFactory.
 *
 * Element IDs sourced directly from RegisterForm.tsx:
 *   reg-first-name, reg-last-name, reg-email, reg-password, reg-confirm-password
 *
 * The submit button has no id in the source — located by CSS [type=submit].
 * Error paragraphs have no ids — located by their Tailwind colour classes.
 * The success/loading modal title is an <h2> — located by CSS + text content.
 *
 * Slow mode:
 *   Set system property  step.delay.ms  to control the pause (ms) injected after
 *   each user-facing action (field fill, button click). Default: 700 ms.
 *   Set  char.delay.ms  to control per-character typing speed. Default: 60 ms.
 *
 *   Examples:
 *     mvn test -Dstep.delay.ms=1200 -Dchar.delay.ms=80   # slow demo
 *     mvn test -Dstep.delay.ms=0    -Dchar.delay.ms=0    # full speed
 */
public class SupervisorRegisterPage {

    private static final String PATH = "/register/supervisor";
    private static final Duration DEFAULT_WAIT = Duration.ofSeconds(10);

    /**
     * Pause injected after each field interaction (fill / click).
     * Override with -Dstep.delay.ms=NNN on the mvn command line.
     */
    private static final long STEP_DELAY_MS =
            Long.parseLong(System.getProperty("step.delay.ms", "700"));

    /**
     * Delay between individual keystrokes for visible slow typing.
     * Override with -Dchar.delay.ms=NNN on the mvn command line.
     */
    private static final long CHAR_DELAY_MS =
            Long.parseLong(System.getProperty("char.delay.ms", "60"));

    private final WebDriver driver;
    private final Actions actions;

    // -----------------------------------------------------------------------
    // Form fields  (all have stable id attributes in RegisterForm.tsx)
    // -----------------------------------------------------------------------

    @FindBy(id = "reg-first-name")
    private WebElement firstNameInput;

    @FindBy(id = "reg-last-name")
    private WebElement lastNameInput;

    @FindBy(id = "reg-email")
    private WebElement emailInput;

    @FindBy(id = "reg-password")
    private WebElement passwordInput;

    @FindBy(id = "reg-confirm-password")
    private WebElement confirmPasswordInput;

    // Submit button — no id in source; unique on the page via type=submit
    @FindBy(css = "button[type='submit']")
    private WebElement submitButton;

    // -----------------------------------------------------------------------
    // Validation feedback elements  (no ids — Tailwind class selectors)
    // -----------------------------------------------------------------------

    // <p class="text-xs text-red-500"> — inline field-level errors
    @FindBy(css = "p.text-red-500")
    private List<WebElement> fieldErrors;

    // <p class="... text-red-600"> — general error banner (e.g. duplicate email from BE)
    @FindBy(css = "p.text-red-600")
    private WebElement generalErrorBanner;

    // -----------------------------------------------------------------------
    // RequestStateModal — rendered via React portal into document.body
    // Title is an <h2 class="... text-xl font-semibold ...">
    // -----------------------------------------------------------------------
    @FindBy(css = "h2.text-xl.font-semibold")
    private WebElement modalTitle;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    public SupervisorRegisterPage(WebDriver driver) {
        this.driver = driver;
        this.actions = new Actions(driver);
        PageFactory.initElements(driver, this);
    }

    // -----------------------------------------------------------------------
    // Navigation
    // -----------------------------------------------------------------------

    public SupervisorRegisterPage open(String baseUrl) {
        driver.get(baseUrl + PATH);
        return this;
    }

    // -----------------------------------------------------------------------
    // Field interactions — each field fill uses slow typing + a post-action pause
    // -----------------------------------------------------------------------

    public SupervisorRegisterPage fillFirstName(String value) {
        firstNameInput.clear();
        slowType(firstNameInput, value);
        pause(STEP_DELAY_MS);
        return this;
    }

    public SupervisorRegisterPage fillLastName(String value) {
        lastNameInput.clear();
        slowType(lastNameInput, value);
        pause(STEP_DELAY_MS);
        return this;
    }

    public SupervisorRegisterPage fillEmail(String value) {
        emailInput.clear();
        slowType(emailInput, value);
        pause(STEP_DELAY_MS);
        return this;
    }

    public SupervisorRegisterPage fillPassword(String value) {
        passwordInput.clear();
        slowType(passwordInput, value);
        pause(STEP_DELAY_MS);
        return this;
    }

    public SupervisorRegisterPage fillConfirmPassword(String value) {
        confirmPasswordInput.clear();
        slowType(confirmPasswordInput, value);
        pause(STEP_DELAY_MS);
        return this;
    }

    public SupervisorRegisterPage clickSubmit() {
        submitButton.click();
        pause(STEP_DELAY_MS);
        return this;
    }

    /**
     * Convenience method — fills all fields and clicks submit in one call.
     */
    public SupervisorRegisterPage register(String firstName, String lastName,
                                           String email, String password,
                                           String confirmPassword) {
        return fillFirstName(firstName)
                .fillLastName(lastName)
                .fillEmail(email)
                .fillPassword(password)
                .fillConfirmPassword(confirmPassword)
                .clickSubmit();
    }

    // -----------------------------------------------------------------------
    // Assertions / state queries
    // -----------------------------------------------------------------------

    /**
     * Returns all visible field-level error messages (text-red-500 paragraphs).
     */
    public List<String> getFieldErrorMessages() {
        return fieldErrors.stream()
                .map(WebElement::getText)
                .filter(t -> !t.isBlank())
                .toList();
    }

    /**
     * Returns true if any field-level error contains {@code expectedText}.
     */
    public boolean isFieldErrorDisplayed(String expectedText) {
        return getFieldErrorMessages().stream()
                .anyMatch(msg -> msg.contains(expectedText));
    }

    /**
     * Returns the text of the general error banner (text-red-600),
     * or an empty string if the banner is not present.
     */
    public String getGeneralError() {
        try {
            return generalErrorBanner.getText();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Waits up to {@link #DEFAULT_WAIT} seconds for the modal title to contain
     * "Registration successful".
     *
     * Why not visibilityOf(modalTitle)?
     * The loading modal ("Creating supervisor account") renders first using the
     * same h2 selector. visibilityOf() fires immediately on the loading state,
     * then getText() returns the loading text → false. We must poll until the
     * text itself transitions to the success value.
     */
    public boolean isSuccessModalVisible() {
        try {
            new WebDriverWait(driver, DEFAULT_WAIT)
                    .until(ExpectedConditions.textToBePresentInElementLocated(
                            By.cssSelector("h2.text-xl.font-semibold"),
                            "Registration successful"));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns true if the loading modal ("Creating supervisor account") is visible.
     */
    public boolean isLoadingModalVisible() {
        try {
            return modalTitle.isDisplayed()
                    && modalTitle.getText().contains("Creating supervisor account");
        } catch (Exception e) {
            return false;
        }
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    // -----------------------------------------------------------------------
    // Slow-mode helpers
    // -----------------------------------------------------------------------

    /**
     * Types {@code text} one character at a time with {@link #CHAR_DELAY_MS}
     * between keystrokes so the typing is visible in the browser.
     */
    private void slowType(WebElement element, String text) {
        element.click();
        for (char c : text.toCharArray()) {
            element.sendKeys(String.valueOf(c));
            pause(CHAR_DELAY_MS);
        }
    }

    /**
     * Sleeps for {@code ms} milliseconds. Swallows InterruptedException
     * (restoring the interrupt flag) so callers stay clean.
     */
    private void pause(long ms) {
        if (ms <= 0) return;
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
