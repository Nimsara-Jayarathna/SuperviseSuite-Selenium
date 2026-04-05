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

/**
 * Page Object for /login.
 *
 * Extends BasePage to inherit cross-browser React input filling and
 * Safari-safe form submission — see BasePage for the technical details.
 */
public class LoginPage extends BasePage {

    private static final String PATH = "/login";
    private final Duration modalWait = Duration.ofSeconds(TestConfig.modalWaitSeconds());

    @FindBy(id = "login-email")
    private WebElement emailInput;

    @FindBy(id = "login-password")
    private WebElement passwordInput;

    @FindBy(css = "button[type='submit']")
    private WebElement submitButton;

    public LoginPage(WebDriver driver) {
        super(driver);
        PageFactory.initElements(driver, this);
    }

    public LoginPage open(String baseUrl) {
        driver.get(baseUrl + PATH);
        return this;
    }

    public LoginPage fillEmail(String email) {
        emailInput.clear();
        slowType(emailInput, email);
        pause(stepDelayMs);
        return this;
    }

    public LoginPage fillPassword(String password) {
        passwordInput.clear();
        slowType(passwordInput, password);
        pause(stepDelayMs);
        return this;
    }

    public LoginPage clickSubmit() {
        commitActiveInput();
        submitForm(submitButton);
        pause(stepDelayMs);
        return this;
    }

    public LoginPage login(String email, String password) {
        return fillEmail(email)
                .fillPassword(password)
                .clickSubmit();
    }

    /**
     * Waits up to modalWait seconds for the URL to contain {@code urlFragment}.
     * Use after login() to confirm the redirect happened.
     */
    public boolean waitForUrlContaining(String urlFragment) {
        try {
            new WebDriverWait(driver, modalWait)
                    .until(ExpectedConditions.urlContains(urlFragment));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns true if an error message containing {@code text} is visible.
     * Waits up to 5 s so Safari's async React renders have time to settle.
     */
    public boolean isErrorDisplayed(String text) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(d -> {
                        try {
                            String body = driver.findElement(
                                    By.cssSelector("p.text-red-500, p.text-red-600")).getText();
                            return body.contains(text);
                        } catch (Exception e) {
                            return false;
                        }
                    });
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }
}
