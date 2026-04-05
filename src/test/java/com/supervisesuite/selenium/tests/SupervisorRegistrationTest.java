package com.supervisesuite.selenium.tests;

import com.supervisesuite.selenium.pages.SupervisorRegisterPage;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end Selenium tests for the Supervisor Registration flow.
 *
 * Prerequisites before running:
 *   1. Frontend running  →  npm run dev  (default: http://localhost:5173)
 *   2. Backend running   →  mvn spring-boot:run  (default: http://localhost:8080)
 *   3. PostgreSQL running and accessible to the backend
 *
 * Run with:  mvn test  (inside SuperviseSuite-Selenium/)
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SupervisorRegistrationTest {

    private static final String BASE_URL = "http://localhost:5173";

    private static WebDriver driver;
    private SupervisorRegisterPage registerPage;

    // ── Shared valid test data ────────────────────────────────────────────
    private static final String VALID_FIRST    = "Jane";
    private static final String VALID_LAST     = "Smith";
    private static final String VALID_EMAIL    = "jane.smith@sliit.lk";
    private static final String VALID_PASSWORD = "Test@1234";

    // ── Driver lifecycle ─────────────────────────────────────────────────

    @BeforeAll
    static void setUpDriver() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--no-sandbox", "--disable-dev-shm-usage");
        driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        driver.manage().window().maximize();
    }

    @AfterAll
    static void tearDownDriver() {
        if (driver != null) {
            driver.quit();
        }
    }

    @BeforeEach
    void openRegistrationPage() {
        registerPage = new SupervisorRegisterPage(driver);
        registerPage.open(BASE_URL);
    }

    // ────────────────────────────────────────────────────────────────────────
    // TC-01 — Happy path
    // ────────────────────────────────────────────────────────────────────────
    @Test
    @Order(1)
    @DisplayName("TC-01: Valid data → success modal shown, then redirected to /login")
    void tc01_happyPath_showsSuccessAndRedirects() {
        // Unique email prevents duplicate-email conflicts across test runs
        String uniqueEmail = "test." + UUID.randomUUID().toString().substring(0, 8) + "@sliit.lk";

        registerPage.register(VALID_FIRST, VALID_LAST, uniqueEmail, VALID_PASSWORD, VALID_PASSWORD);

        assertTrue(registerPage.isSuccessModalVisible(),
                "Success modal should appear after valid supervisor registration");

        // App auto-redirects to /login after ~3 s (useSupervisorRegister hook)
        new WebDriverWait(driver, Duration.ofSeconds(15))
                .until(ExpectedConditions.urlContains("/login"));

        assertTrue(driver.getCurrentUrl().contains("/login"),
                "Should be redirected to /login after successful registration");
    }

    // ────────────────────────────────────────────────────────────────────────
    // TC-02 — All fields empty
    // ────────────────────────────────────────────────────────────────────────
    @Test
    @Order(2)
    @DisplayName("TC-02: Empty form submit → required-field errors for all five fields")
    void tc02_emptyForm_showsAllRequiredErrors() {
        registerPage.clickSubmit();

        assertAll("Required field errors",
                () -> assertTrue(registerPage.isFieldErrorDisplayed("First name is required."),
                        "Expected first name required error"),
                () -> assertTrue(registerPage.isFieldErrorDisplayed("Last name is required."),
                        "Expected last name required error"),
                () -> assertTrue(registerPage.isFieldErrorDisplayed("Email is required."),
                        "Expected email required error"),
                () -> assertTrue(registerPage.isFieldErrorDisplayed("Password is required."),
                        "Expected password required error"),
                () -> assertTrue(registerPage.isFieldErrorDisplayed("Please confirm your password."),
                        "Expected confirm password required error")
        );
    }

    // ────────────────────────────────────────────────────────────────────────
    // TC-03 — Non-SLIIT email domain
    // ────────────────────────────────────────────────────────────────────────
    @Test
    @Order(3)
    @DisplayName("TC-03: Gmail address → SLIIT institutional email domain error")
    void tc03_gmailAddress_showsDomainError() {
        registerPage.register(VALID_FIRST, VALID_LAST,
                "jane@gmail.com", VALID_PASSWORD, VALID_PASSWORD);

        assertTrue(registerPage.isFieldErrorDisplayed(
                "Email must be a valid SLIIT institutional email (@sliit.lk)"),
                "Expected SLIIT domain error for gmail.com address");
    }

    // ────────────────────────────────────────────────────────────────────────
    // TC-04 — Student portal email rejected (@my.sliit.lk)
    // ────────────────────────────────────────────────────────────────────────
    @Test
    @Order(4)
    @DisplayName("TC-04: @my.sliit.lk address → SLIIT institutional email domain error")
    void tc04_studentPortalEmail_showsDomainError() {
        // @my.sliit.lk is a student domain — supervisors must use @sliit.lk only
        registerPage.register(VALID_FIRST, VALID_LAST,
                "jane@my.sliit.lk", VALID_PASSWORD, VALID_PASSWORD);

        assertTrue(registerPage.isFieldErrorDisplayed(
                "Email must be a valid SLIIT institutional email (@sliit.lk)"),
                "Expected SLIIT domain error for @my.sliit.lk address");
    }

    // ────────────────────────────────────────────────────────────────────────
    // TC-05 — Malformed email
    // ────────────────────────────────────────────────────────────────────────
    @Test
    @Order(5)
    @DisplayName("TC-05: Malformed email string → valid email format error")
    void tc05_malformedEmail_showsFormatError() {
        registerPage.register(VALID_FIRST, VALID_LAST,
                "not-an-email", VALID_PASSWORD, VALID_PASSWORD);

        assertTrue(registerPage.isFieldErrorDisplayed("Enter a valid email."),
                "Expected email format error for malformed input");
    }

    // ────────────────────────────────────────────────────────────────────────
    // TC-06 — Password too short
    // ────────────────────────────────────────────────────────────────────────
    @Test
    @Order(6)
    @DisplayName("TC-06: Password < 8 chars → minimum length error")
    void tc06_shortPassword_showsLengthError() {
        registerPage.register(VALID_FIRST, VALID_LAST, VALID_EMAIL, "Ab@1", "Ab@1");

        assertTrue(registerPage.isFieldErrorDisplayed("Password must be at least 8 characters."),
                "Expected minimum length error for short password");
    }

    // ────────────────────────────────────────────────────────────────────────
    // TC-07 — Password missing uppercase
    // ────────────────────────────────────────────────────────────────────────
    @Test
    @Order(7)
    @DisplayName("TC-07: Password with no uppercase letter → uppercase required error")
    void tc07_passwordNoUppercase_showsError() {
        // All lowercase + digit + special char — but no uppercase
        registerPage.register(VALID_FIRST, VALID_LAST, VALID_EMAIL, "test@1234", "test@1234");

        assertTrue(registerPage.isFieldErrorDisplayed("Password must contain an uppercase letter."),
                "Expected uppercase error when password has no uppercase letter");
    }

    // ────────────────────────────────────────────────────────────────────────
    // TC-08 — Password missing lowercase
    // ────────────────────────────────────────────────────────────────────────
    @Test
    @Order(8)
    @DisplayName("TC-08: Password with no lowercase letter → lowercase required error")
    void tc08_passwordNoLowercase_showsError() {
        // All uppercase + digit + special char — but no lowercase
        registerPage.register(VALID_FIRST, VALID_LAST, VALID_EMAIL, "TEST@1234", "TEST@1234");

        assertTrue(registerPage.isFieldErrorDisplayed("Password must contain a lowercase letter."),
                "Expected lowercase error when password has no lowercase letter");
    }

    // ────────────────────────────────────────────────────────────────────────
    // TC-09 — Password missing digit
    // ────────────────────────────────────────────────────────────────────────
    @Test
    @Order(9)
    @DisplayName("TC-09: Password with no digit → digit required error")
    void tc09_passwordNoDigit_showsError() {
        // Mixed case + special char — but no digit
        registerPage.register(VALID_FIRST, VALID_LAST, VALID_EMAIL, "Test@abcd", "Test@abcd");

        assertTrue(registerPage.isFieldErrorDisplayed("Password must contain a digit."),
                "Expected digit error when password has no numeric character");
    }

    // ────────────────────────────────────────────────────────────────────────
    // TC-10 — Password missing special character
    // ────────────────────────────────────────────────────────────────────────
    @Test
    @Order(10)
    @DisplayName("TC-10: Password with no special character → special character required error")
    void tc10_passwordNoSpecialChar_showsError() {
        // Mixed case + digits — but no special character
        registerPage.register(VALID_FIRST, VALID_LAST, VALID_EMAIL, "Test12345", "Test12345");

        assertTrue(registerPage.isFieldErrorDisplayed("Password must contain a special character."),
                "Expected special character error when password has no special character");
    }

    // ────────────────────────────────────────────────────────────────────────
    // TC-11 — Passwords do not match
    // ────────────────────────────────────────────────────────────────────────
    @Test
    @Order(11)
    @DisplayName("TC-11: Mismatched password and confirm password → passwords do not match error")
    void tc11_passwordMismatch_showsError() {
        registerPage.register(VALID_FIRST, VALID_LAST, VALID_EMAIL,
                VALID_PASSWORD, "Different@99");

        assertTrue(registerPage.isFieldErrorDisplayed("Passwords do not match."),
                "Expected mismatch error when password and confirm password differ");
    }

    // ────────────────────────────────────────────────────────────────────────
    // TC-12 — Duplicate email (backend conflict)
    // Requires both FE and BE to be running.
    // ────────────────────────────────────────────────────────────────────────
    @Test
    @Order(12)
    @DisplayName("TC-12: Duplicate email → backend conflict error shown in general error banner")
    void tc12_duplicateEmail_showsConflictError() {
        String duplicateEmail = "dup." + UUID.randomUUID().toString().substring(0, 6) + "@sliit.lk";

        // First registration — must succeed to seed the duplicate
        registerPage.register(VALID_FIRST, VALID_LAST, duplicateEmail, VALID_PASSWORD, VALID_PASSWORD);
        assertTrue(registerPage.isSuccessModalVisible(),
                "First registration should succeed so the duplicate email exists in DB");

        // Navigate back to the registration page fresh
        driver.get(BASE_URL + "/register/supervisor");
        registerPage = new SupervisorRegisterPage(driver);

        // Second registration with same email — backend should return 409 Conflict
        registerPage.register(VALID_FIRST, VALID_LAST, duplicateEmail, VALID_PASSWORD, VALID_PASSWORD);

        // Wait for BE response and general error banner (text-red-600) to appear
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("p.text-red-600")));

        String errorText = registerPage.getGeneralError();
        assertFalse(errorText.isBlank(),
                "Expected a non-empty general error message for duplicate email, but got blank");
    }
}
