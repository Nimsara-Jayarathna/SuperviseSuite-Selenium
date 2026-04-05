package com.supervisesuite.selenium.pages;

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
 */
public class SupervisorRegisterPage {

    private static final String PATH = "/register/supervisor";
    private static final Duration DEFAULT_WAIT = Duration.ofSeconds(10);

    private final WebDriver driver;

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
        firstNameInput.sendKeys(value);
        return this;
    }

    public SupervisorRegisterPage fillLastName(String value) {
        lastNameInput.clear();
        lastNameInput.sendKeys(value);
        return this;
    }

    public SupervisorRegisterPage fillEmail(String value) {
        emailInput.clear();
        emailInput.sendKeys(value);
        return this;
    }

    public SupervisorRegisterPage fillPassword(String value) {
        passwordInput.clear();
        passwordInput.sendKeys(value);
        return this;
    }

    public SupervisorRegisterPage fillConfirmPassword(String value) {
        confirmPasswordInput.clear();
        confirmPasswordInput.sendKeys(value);
        return this;
    }

    public SupervisorRegisterPage clickSubmit() {
        submitButton.click();
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
     * Waits up to {@value DEFAULT_WAIT} seconds for the success modal to appear
     * and returns true when the title reads "Registration successful".
     */
    public boolean isSuccessModalVisible() {
        try {
            new WebDriverWait(driver, DEFAULT_WAIT)
                    .until(ExpectedConditions.visibilityOf(modalTitle));
            return modalTitle.getText().contains("Registration successful");
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
}
