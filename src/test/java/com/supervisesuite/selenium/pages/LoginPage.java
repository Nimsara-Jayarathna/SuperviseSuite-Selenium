package com.supervisesuite.selenium.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

/**
 * Page Object for /login.
 */
public class LoginPage {

    private static final String PATH = "/login";

    private final WebDriver driver;

    @FindBy(id = "login-email")
    private WebElement emailInput;

    @FindBy(id = "login-password")
    private WebElement passwordInput;

    @FindBy(css = "button[type='submit']")
    private WebElement submitButton;

    public LoginPage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    public LoginPage open(String baseUrl) {
        driver.get(baseUrl + PATH);
        return this;
    }

    public LoginPage fillEmail(String email) {
        emailInput.clear();
        emailInput.sendKeys(email);
        return this;
    }

    public LoginPage fillPassword(String password) {
        passwordInput.clear();
        passwordInput.sendKeys(password);
        return this;
    }

    public LoginPage clickSubmit() {
        submitButton.click();
        return this;
    }

    public LoginPage login(String email, String password) {
        return fillEmail(email)
                .fillPassword(password)
                .clickSubmit();
    }
}
