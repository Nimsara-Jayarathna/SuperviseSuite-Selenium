package com.supervisesuite.selenium.pages;

import com.supervisesuite.selenium.config.TestConfig;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.interactions.MoveTargetOutOfBoundsException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Abstract base for all page objects.
 *
 * Centralises two cross-browser workarounds that affect every page on Safari:
 *
 * 1. React controlled-input filling (fillReact / slowType)
 *    Safari's safaridriver does not always fire the synthetic 'input' event that
 *    React's onChange handler listens to when a bulk sendKeys() is used, so React
 *    state stays empty even though the DOM looks filled.
 *    Fix: use the native HTMLInputElement.prototype value setter + a bubbling
 *    'input' event dispatch (the same technique React Testing Library uses).
 *
 * 2. Form submission (submitForm)
 *    Safari's safaridriver fires the DOM 'click' event on a submit button but
 *    does NOT reliably complete the click → form-submit event chain, especially
 *    when no fields were interacted with beforehand.
 *    Fix: dispatch a SubmitEvent directly on the <form> element.  React's
 *    delegated root listeners catch the bubbled event, call e.preventDefault(),
 *    validate their own state, and either show errors or call the API — exactly
 *    what a real user click would trigger.
 *
 * Both fixes are transparent to Chrome and Firefox: the JS APIs used are
 * standard W3C and behave identically across all modern browsers.
 */
public abstract class BasePage {

    protected final WebDriver driver;
    protected final Actions actions;

    protected final long stepDelayMs;
    protected final long charDelayMs;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.actions = new Actions(driver);
        this.stepDelayMs = TestConfig.stepDelayMs();
        this.charDelayMs = TestConfig.charDelayMs();
    }

    // -----------------------------------------------------------------------
    // React-safe input filling
    // -----------------------------------------------------------------------

    /**
     * Fills a React controlled {@code <input>} reliably on all browsers.
     *
     * <p>Fast mode (charDelayMs == 0): uses the native HTMLInputElement prototype
     * setter + a bubbling {@code input} event so that React's onChange fires even
     * in Safari where a bulk sendKeys() may not trigger the synthetic event.
     *
     * <p>Slow mode (charDelayMs &gt; 0): types character-by-character so the
     * action is visible in the browser window. A minimum 16 ms gap per keystroke
     * (≈ one 60 fps frame) prevents React from batching adjacent events.
     */
    protected void slowType(WebElement element, String text) {
        element.click();
        if (charDelayMs <= 0) {
            if (driver instanceof JavascriptExecutor js) {
                js.executeScript(
                        "var s = Object.getOwnPropertyDescriptor(" +
                                "window.HTMLInputElement.prototype, 'value').set;" +
                                "s.call(arguments[0], arguments[1]);" +
                                "arguments[0].dispatchEvent(new Event('input', {bubbles:true}));",
                        element, text);
            } else {
                element.sendKeys(text);
            }
        } else {
            long gap = Math.max(charDelayMs, 16);
            for (char c : text.toCharArray()) {
                element.sendKeys(String.valueOf(c));
                pause(gap);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Safari-safe form submission
    // -----------------------------------------------------------------------

    /**
     * Submits the form that contains {@code button} in a way that reliably calls
     * React's {@code onSubmit} handler on all browsers including Safari.
     *
     * <p>Approach: dispatch a bubbling, cancelable {@code SubmitEvent} directly
     * on the {@code <form>} element. React's delegated root listener catches it,
     * calls {@code e.preventDefault()}, validates its state, and either shows
     * validation errors or makes the API call — identical to what a real user
     * click would produce.
     *
     * <p>Falls back to progressively weaker click strategies if the JS dispatch
     * fails (e.g., the button is not inside a {@code <form>} for some reason):
     * native WebDriver click → Actions move+click → JS element click.
     *
     * @param button the {@code button[type="submit"]} element to submit through
     */
    protected void submitForm(WebElement button) {
        scrollIntoViewCenter(button);
        // Brief settle so Safari's compositing layer is ready to receive the event
        pause(120);

        if (submitViaFormEvent(button)) return;

        try {
            button.click();
            return;
        } catch (Exception ignored) {}

        try {
            actions.moveToElement(button).click().perform();
            return;
        } catch (MoveTargetOutOfBoundsException ignored) {
        } catch (Exception ignored) {}

        jsClick(button);
    }

    /**
     * Waits for the submit button identified by {@code buttonSelector} to be
     * clickable, then calls {@link #submitForm(WebElement)}.
     *
     * <p>Use this overload when submitting before the button is guaranteed to be
     * in the DOM (e.g., right after page navigation).
     */
    protected void submitForm(By buttonSelector, Duration timeout) {
        WebElement button = new WebDriverWait(driver, timeout)
                .until(ExpectedConditions.elementToBeClickable(buttonSelector));
        submitForm(button);
    }

    /**
     * Blurs the currently focused input and dispatches a {@code change} event so
     * React processes the final value before the form is submitted. Call this
     * immediately before {@link #submitForm} when the last filled field might
     * still be focused.
     */
    protected void commitActiveInput() {
        if (driver instanceof JavascriptExecutor js) {
            js.executeScript(
                    "var el = document.activeElement;" +
                            "if (!el) return;" +
                            "if (typeof el.blur === 'function') el.blur();" +
                            "el.dispatchEvent(new Event('change', {bubbles:true}));");
        }
    }

    // -----------------------------------------------------------------------
    // Low-level helpers (private — use the public API above)
    // -----------------------------------------------------------------------

    private boolean submitViaFormEvent(WebElement button) {
        if (!(driver instanceof JavascriptExecutor js)) return false;
        try {
            js.executeScript(
                    "var btn = arguments[0];" +
                            "var form = btn.closest('form');" +
                            "if (!form) return;" +
                            "var evt;" +
                            "try {" +
                            "  evt = new SubmitEvent('submit',{bubbles:true,cancelable:true,submitter:btn});" +
                            "} catch(e) {" +
                            "  evt = new Event('submit',{bubbles:true,cancelable:true});" +
                            "}" +
                            "form.dispatchEvent(evt);",
                    button);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void scrollIntoViewCenter(WebElement element) {
        if (driver instanceof JavascriptExecutor js) {
            js.executeScript(
                    "arguments[0].scrollIntoView({block:'center',inline:'center',behavior:'instant'});",
                    element);
        }
    }

    private void jsClick(WebElement element) {
        if (driver instanceof JavascriptExecutor js) {
            js.executeScript("arguments[0].click();", element);
        } else {
            element.click();
        }
    }

    protected void pause(long ms) {
        if (ms <= 0) return;
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
