package com.supervisesuite.selenium.extensions;

import com.supervisesuite.selenium.config.TestConfig;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.regex.Pattern;

/**
 * Enforces deterministic test display-name format.
 */
public class TestNamingExtension implements BeforeEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
        if (!TestConfig.namingValidationEnabled()) {
            return;
        }
        String displayName = context.getDisplayName();
        Pattern pattern = Pattern.compile(TestConfig.testNamePattern());
        if (!pattern.matcher(displayName).matches()) {
            throw new IllegalStateException(
                    "Invalid test display name: '" + displayName + "'. Expected pattern: "
                            + TestConfig.testNamePattern()
            );
        }
    }
}
