package com.supervisesuite.selenium.pages;

import com.supervisesuite.selenium.config.TestConfig;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
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
 * Cross-browser Safari compatibility (React input filling + form submission)
 * is handled by the BasePage superclass.
 */
public class SupervisorRegisterPage extends BasePage {

    private static final String PATH = "/register/supervisor";
    private static final By SUBMIT_BTN = By.cssSelector("button[type='submit']");

    private final Duration modalWait = Duration.ofSeconds(TestConfig.modalWaitSeconds());

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
        super(driver);
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
    // Field interactions
    // -----------------------------------------------------------------------

    public SupervisorRegisterPage fillFirstName(String value) {
        firstNameInput.clear();
        slowType(firstNameInput, value);
        pause(stepDelayMs);
        return this;
    }

    public SupervisorRegisterPage fillLastName(String value) {
        lastNameInput.clear();
        slowType(lastNameInput, value);
        pause(stepDelayMs);
        return this;
    }

    public SupervisorRegisterPage fillEmail(String value) {
        emailInput.clear();
        slowType(emailInput, value);
        pause(stepDelayMs);
        return this;
    }

    public SupervisorRegisterPage fillPassword(String value) {
        passwordInput.clear();
        slowType(passwordInput, value);
        pause(stepDelayMs);
        return this;
    }

    public SupervisorRegisterPage fillConfirmPassword(String value) {
        confirmPasswordInput.clear();
        slowType(confirmPasswordInput, value);
        pause(stepDelayMs);
        return this;
    }

    public SupervisorRegisterPage clickSubmit() {
        commitActiveInput();
        submitForm(submitButton);
        pause(stepDelayMs);
        return this;
    }

    /** Fills all fields and clicks submit in one call. */
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

    /** Returns all visible field-level error messages (text-red-500 paragraphs). */
    public List<String> getFieldErrorMessages() {
        return fieldErrors.stream()
                .map(WebElement::getText)
                .filter(t -> !t.isBlank())
                .toList();
    }

    /**
     * Waits up to 5 s for a field-level error containing {@code expectedText} to appear.
     * React re-renders validation errors asynchronously; on Safari the latency is higher.
     */
    public boolean isFieldErrorDisplayed(String expectedText) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(d -> getFieldErrorMessages().stream()
                            .anyMatch(msg -> msg.contains(expectedText)));
            return true;
        } catch (Exception e) {
            return false;
        }
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
     * Waits up to modalWait seconds for the modal title to contain
     * "Registration successful".
     *
     * Uses textToBePresentInElementLocated (not visibilityOf) because the loading
     * modal ("Creating supervisor account") appears first on the same selector.
     * We must poll until the text transitions to the success value.
     */
    public boolean isSuccessModalVisible() {
        try {
            new WebDriverWait(driver, modalWait)
                    .until(ExpectedConditions.textToBePresentInElementLocated(
                            By.cssSelector("h2.text-xl.font-semibold"),
                            "Registration successful"));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Returns true if the loading modal ("Creating supervisor account") is visible. */
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
}
