package com.supervisesuite.selenium.extensions;

import com.supervisesuite.selenium.config.TestConfig;
import io.qameta.allure.Allure;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Retries flaky tests up to configured max retries.
 */
public class RetryExtension implements InvocationInterceptor {

    @Override
    public void interceptTestMethod(Invocation<Void> invocation,
                                    ReflectiveInvocationContext<Method> invocationContext,
                                    ExtensionContext extensionContext) throws Throwable {
        int maxRetries = TestConfig.maxRetries();
        int attempts = 0;
        Throwable last = null;
        AtomicBoolean flakyRecovered = new AtomicBoolean(false);

        while (attempts <= maxRetries) {
            try {
                invocation.proceed();
                if (flakyRecovered.get()) {
                    Allure.addAttachment(
                            "Flaky Recovery",
                            "text/plain",
                            "Test passed after retry. retriesUsed=" + attempts
                    );
                }
                return;
            } catch (Throwable t) {
                last = t;
                if (attempts >= maxRetries) {
                    throw last;
                }
                attempts++;
                flakyRecovered.set(true);
                Allure.addAttachment(
                        "Retry Attempt",
                        "text/plain",
                        "Retrying test after failure. attempt=" + attempts + " of " + maxRetries
                                + ", reason=" + t.getClass().getSimpleName() + ": " + t.getMessage()
                );
            }
        }
        if (last != null) {
            throw last;
        }
    }
}
