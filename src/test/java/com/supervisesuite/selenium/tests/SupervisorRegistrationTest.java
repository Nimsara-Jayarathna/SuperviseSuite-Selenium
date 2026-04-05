package com.supervisesuite.selenium.tests;

import com.supervisesuite.selenium.annotations.UserStory;
import com.supervisesuite.selenium.pages.SupervisorRegisterPage;
import com.supervisesuite.selenium.pages.LoginPage;
import com.supervisesuite.selenium.support.BackendAuthAssertions;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
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
 * Run all tests (slow, visible browser):
 *   mvn test
 *
 * Tune browser speed:
 *   mvn test -Dstep.delay.ms=1200 -Dchar.delay.ms=80    # very slow / demo mode
 *   mvn test -Dstep.delay.ms=0    -Dchar.delay.ms=0     # full speed
 *
 * Generate + open Allure HTML report after the run:
 *   mvn allure:serve
 */
@Epic("Supervisor Registration")
@Feature("Supervisor Registration Form")
@UserStory("US-201")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SupervisorRegistrationTest extends BaseUiTest {

    private SupervisorRegisterPage registerPage;

    // ── Shared valid test data ────────────────────────────────────────────
    private static final String VALID_FIRST    = "Jane";
    private static final String VALID_LAST     = "Smith";
    private static final String VALID_EMAIL    = "jane.smith@sliit.lk";
    private static final String VALID_PASSWORD = "Test@1234";

    @BeforeEach
    void openRegistrationPage() {
        registerPage = new SupervisorRegisterPage(driver);
        registerPage.open(baseUrl());
    }

    // ────────────────────────────────────────────────────────────────────────
    // TC-01 — Happy path
    // ────────────────────────────────────────────────────────────────────────
    @Test
    @Order(1)
    @Story("Happy path")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Fill all fields with valid @sliit.lk credentials. Expect the success modal to appear, then an automatic redirect to /login.")
    @DisplayName("TC-01: Valid data → success modal shown, then redirected to /login")
    void tc01_happyPath_showsSuccessAndRedirects() {
        String uniqueEmail = "test." + UUID.randomUUID().toString().substring(0, 8) + "@sliit.lk";

        registerPage.register(VALID_FIRST, VALID_LAST, uniqueEmail, VALID_PASSWORD, VALID_PASSWORD);

        assertTrue(registerPage.isSuccessModalVisible(),
                "Success modal should appear after valid supervisor registration");

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
    @Story("Client-side validation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Submit the form without filling any field. All five required-field errors must appear.")
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
    @Story("Client-side validation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Enter a gmail.com address. The SLIIT domain validator must reject it.")
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
    @Story("Client-side validation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Enter a @my.sliit.lk student portal address. Supervisors must use @sliit.lk only; the validator must reject the student domain.")
    @DisplayName("TC-04: @my.sliit.lk address → SLIIT institutional email domain error")
    void tc04_studentPortalEmail_showsDomainError() {
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
    @Story("Client-side validation")
    @Severity(SeverityLevel.MINOR)
    @Description("Enter a string that is not a valid email address. The format validator must reject it.")
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
    @Story("Client-side validation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Enter a password shorter than 8 characters. The minimum-length rule must fire.")
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
    @Story("Client-side validation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Enter a password with no uppercase letter. The strength validator must require one.")
    @DisplayName("TC-07: Password with no uppercase letter → uppercase required error")
    void tc07_passwordNoUppercase_showsError() {
        registerPage.register(VALID_FIRST, VALID_LAST, VALID_EMAIL, "test@1234", "test@1234");

        assertTrue(registerPage.isFieldErrorDisplayed("Password must contain an uppercase letter."),
                "Expected uppercase error when password has no uppercase letter");
    }

    // ────────────────────────────────────────────────────────────────────────
    // TC-08 — Password missing lowercase
    // ────────────────────────────────────────────────────────────────────────
    @Test
    @Order(8)
    @Story("Client-side validation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Enter a password with no lowercase letter. The strength validator must require one.")
    @DisplayName("TC-08: Password with no lowercase letter → lowercase required error")
    void tc08_passwordNoLowercase_showsError() {
        registerPage.register(VALID_FIRST, VALID_LAST, VALID_EMAIL, "TEST@1234", "TEST@1234");

        assertTrue(registerPage.isFieldErrorDisplayed("Password must contain a lowercase letter."),
                "Expected lowercase error when password has no lowercase letter");
    }

    // ────────────────────────────────────────────────────────────────────────
    // TC-09 — Password missing digit
    // ────────────────────────────────────────────────────────────────────────
    @Test
    @Order(9)
    @Story("Client-side validation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Enter a password with no numeric digit. The strength validator must require one.")
    @DisplayName("TC-09: Password with no digit → digit required error")
    void tc09_passwordNoDigit_showsError() {
        registerPage.register(VALID_FIRST, VALID_LAST, VALID_EMAIL, "Test@abcd", "Test@abcd");

        assertTrue(registerPage.isFieldErrorDisplayed("Password must contain a digit."),
                "Expected digit error when password has no numeric character");
    }

    // ────────────────────────────────────────────────────────────────────────
    // TC-10 — Password missing special character
    // ────────────────────────────────────────────────────────────────────────
    @Test
    @Order(10)
    @Story("Client-side validation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Enter a password with no special character. The strength validator must require one.")
    @DisplayName("TC-10: Password with no special character → special character required error")
    void tc10_passwordNoSpecialChar_showsError() {
        registerPage.register(VALID_FIRST, VALID_LAST, VALID_EMAIL, "Test12345", "Test12345");

        assertTrue(registerPage.isFieldErrorDisplayed("Password must contain a special character."),
                "Expected special character error when password has no special character");
    }

    // ────────────────────────────────────────────────────────────────────────
    // TC-11 — Passwords do not match
    // ────────────────────────────────────────────────────────────────────────
    @Test
    @Order(11)
    @Story("Client-side validation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Enter different values for password and confirm password. The mismatch error must appear.")
    @DisplayName("TC-11: Mismatched passwords → passwords do not match error")
    void tc11_passwordMismatch_showsError() {
        registerPage.register(VALID_FIRST, VALID_LAST, VALID_EMAIL,
                VALID_PASSWORD, "Different@99");

        assertTrue(registerPage.isFieldErrorDisplayed("Passwords do not match."),
                "Expected mismatch error when password and confirm password differ");
    }

    // ────────────────────────────────────────────────────────────────────────
    // TC-12 — Duplicate email (backend conflict)
    // ────────────────────────────────────────────────────────────────────────
    @Test
    @Order(12)
    @Story("Backend validation")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Register successfully once, then attempt to register again with the same email. The backend must return a conflict error displayed in the general error banner.")
    @DisplayName("TC-12: Duplicate email → backend conflict error shown in general error banner")
    void tc12_duplicateEmail_showsConflictError() {
        String duplicateEmail = "dup." + UUID.randomUUID().toString().substring(0, 6) + "@sliit.lk";

        // First registration — seeds the duplicate in DB
        registerPage.register(VALID_FIRST, VALID_LAST, duplicateEmail, VALID_PASSWORD, VALID_PASSWORD);
        assertTrue(registerPage.isSuccessModalVisible(),
                "First registration should succeed so the duplicate email exists in DB");

        // Navigate back and try the same email again
        driver.get(baseUrl() + "/register/supervisor");
        registerPage = new SupervisorRegisterPage(driver);
        registerPage.register(VALID_FIRST, VALID_LAST, duplicateEmail, VALID_PASSWORD, VALID_PASSWORD);

        // Wait for BE response and the general error banner (text-red-600) to appear
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("p.text-red-600")));

        String errorText = registerPage.getGeneralError();
        assertFalse(errorText.isBlank(),
                "Expected a non-empty conflict error message for duplicate email, got blank");
    }

    // ────────────────────────────────────────────────────────────────────────
    // TC-13 — Registration integrates with authentication and role routing
    // ────────────────────────────────────────────────────────────────────────
    @Test
    @Order(13)
    @Story("Integration with authentication")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Register a supervisor, then log in with the same credentials. Verify authentication succeeds, user role is SUPERVISOR, and app redirects to supervisor dashboard route.")
    @DisplayName("TC-13: Register then login → authenticated as SUPERVISOR and redirected to supervisor dashboard")
    void tc13_registerThenLogin_redirectsToSupervisorDashboardWithSupervisorRole() {
        String uniqueEmail = "flow." + UUID.randomUUID().toString().substring(0, 8) + "@sliit.lk";

        registerPage.register(VALID_FIRST, VALID_LAST, uniqueEmail, VALID_PASSWORD, VALID_PASSWORD);
        assertTrue(registerPage.isSuccessModalVisible(),
                "Success modal should appear after valid supervisor registration");

        new WebDriverWait(driver, Duration.ofSeconds(15))
                .until(ExpectedConditions.urlContains("/login"));

        LoginPage loginPage = new LoginPage(driver);
        loginPage.login(uniqueEmail, VALID_PASSWORD);

        new WebDriverWait(driver, Duration.ofSeconds(20))
                .until(ExpectedConditions.urlContains("/supervisor"));

        assertTrue(driver.getCurrentUrl().contains("/supervisor"),
                "Expected successful login redirect to supervisor route");
        assertEquals("SUPERVISOR", readStoredUserRole(),
                "Expected authenticated user role to be SUPERVISOR");
        BackendAuthAssertions.assertLoginRole(uniqueEmail, VALID_PASSWORD, "SUPERVISOR");
    }

    private String readStoredUserRole() {
        Object value = ((JavascriptExecutor) driver).executeScript(
                "const raw = window.localStorage.getItem('ss_user');"
                        + "if (!raw) return null;"
                        + "try { return JSON.parse(raw).role ?? null; } catch (e) { return null; }"
        );
        return value == null ? null : value.toString();
    }
}
