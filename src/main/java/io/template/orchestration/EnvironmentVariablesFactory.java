package io.template.orchestration;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.template.orchestration.exceptions.EnvironmentVariableException;
import io.template.shared.models.EnvironmentVariables;
import io.template.shared.utilities.HibernateValidatorUtility;
import jakarta.validation.ConstraintViolation;

/**
 * Creates {@link EnvironmentVariables} instances from raw environment maps.
 * <p>
 * This class contains all logic for extracting, parsing and validating
 * environment variables. {@link io.template.orchestration.injectionmodules.EnvironmentModule}
 * is intentionally kept thin and delegates to this factory, so the logic
 * can be tested without going through Guice.
 */
public class EnvironmentVariablesFactory {

    private EnvironmentVariablesFactory() { }

    public static EnvironmentVariables from(Map<String, String> environment) {
        EnvironmentVariables environmentVariables = new EnvironmentVariables(
                extractString(environment, "STAGE"),
                extractString(environment, "REGION"),
                extractString(environment, "EXAMPLE_STRING_VAR"),
                extractInt(environment, "EXAMPLE_INT_VAR"),
                extractBoolean(environment, "EXAMPLE_BOOLEAN_VAR")
        );

        validateEnvironmentVariables(environmentVariables);

        return environmentVariables;
    }

    private static void validateEnvironmentVariables(EnvironmentVariables environmentVariables) {
        Set<ConstraintViolation<EnvironmentVariables>> violations = HibernateValidatorUtility.VALIDATOR
                .validate(environmentVariables);

        if (!violations.isEmpty()) {
            String errors = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining(", "));

            throw new EnvironmentVariableException("Environment validation failed: " + errors);
        }
    }

    private static String extractString(Map<String, String> environment, String key) {
        String value = environment.get(key);
        ensureVariableExists(key, value);
        return value;
    }

    private static int extractInt(Map<String, String> environment, String key) {
        String value = environment.get(key);
        ensureVariableExists(key, value);

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            String message = "Environment variable '" + key + "' must be a valid integer, got: " + value;
            throw new EnvironmentVariableException(message, e);
        }
    }

    private static boolean extractBoolean(Map<String, String> environment, String key) {
        String value = environment.get(key);
        ensureVariableExists(key, value);

        return customParseBoolean(key, value);
    }

    private static void ensureVariableExists(String key, String value) {
        if (value == null || value.isBlank()) {
            throw new EnvironmentVariableException("Required environment variable '" + key + "' is not set");
        }
    }

    /**
     * The existing Boolean.parseBoolean() returns {@code false} for any value not equal to {@code "true"}.
     * We want an error thrown if the value is unexpected, and we abide by the JSON standard of expecting
     * lowercase {@code "true"} or {@code "false"}.
     */
    private static boolean customParseBoolean(String key, String value) {
        if ("true".equals(value)) {
            return true;
        }
        if ("false".equals(value)) {
            return false;
        }

        String message = "Environment variable '" + key + "' must be 'true' or 'false', got: " + value;
        throw new EnvironmentVariableException(message);
    }
}

