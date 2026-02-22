package io.template.orchestration;

import java.util.Map;

import io.template.orchestration.exceptions.EnvironmentVariableException;
import io.template.shared.models.EnvironmentVariables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.template.testsupport.SampleEnvironmentMaps.validEnvironment;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnvironmentVariablesFactoryTest {

    private Map<String, String> environment;

    @BeforeEach
    void setUp() {
        environment = validEnvironment();
    }

    @Test
    void providesEnvironmentVariablesWithValidValues() {
        EnvironmentVariables result = EnvironmentVariablesFactory.from(environment);

        assertNotNull(result);
        assertEquals("unit", result.stage());
        assertEquals("unit-region", result.region());
        assertEquals("test", result.exampleStringVar());
        assertEquals(1, result.exampleIntVar());
        assertTrue(result.exampleBooleanVar());
    }

    @Test
    void providesEnvironmentVariablesWithFalseBoolean() {
        environment.put("EXAMPLE_BOOLEAN_VAR", "false");

        EnvironmentVariables result = EnvironmentVariablesFactory.from(environment);

        assertFalse(result.exampleBooleanVar());
        assertEquals("unit", result.stage());
        assertEquals(1, result.exampleIntVar());
    }

    @Test
    void throwsExceptionWhenStageIsMissing() {
        environment.remove("STAGE");

        EnvironmentVariableException exception = assertThrows(
                EnvironmentVariableException.class,
                () -> EnvironmentVariablesFactory.from(environment)
        );

        assertTrue(exception.getMessage().contains("STAGE"));
        assertTrue(exception.getMessage().contains("not set"));
    }

    @Test
    void throwsExceptionWhenRegionIsMissing() {
        environment.remove("REGION");

        EnvironmentVariableException exception = assertThrows(
                EnvironmentVariableException.class,
                () -> EnvironmentVariablesFactory.from(environment)
        );

        assertTrue(exception.getMessage().contains("REGION"));
        assertTrue(exception.getMessage().contains("not set"));
    }

    @Test
    void throwsExceptionWhenExampleStringVarIsMissing() {
        environment.remove("EXAMPLE_STRING_VAR");

        EnvironmentVariableException exception = assertThrows(
                EnvironmentVariableException.class,
                () -> EnvironmentVariablesFactory.from(environment)
        );

        assertTrue(exception.getMessage().contains("EXAMPLE_STRING_VAR"));
        assertTrue(exception.getMessage().contains("not set"));
    }

    @Test
    void throwsExceptionWhenExampleIntVarIsMissing() {
        environment.remove("EXAMPLE_INT_VAR");

        EnvironmentVariableException exception = assertThrows(
                EnvironmentVariableException.class,
                () -> EnvironmentVariablesFactory.from(environment)
        );

        assertTrue(exception.getMessage().contains("EXAMPLE_INT_VAR"));
        assertTrue(exception.getMessage().contains("not set"));
    }

    @Test
    void throwsExceptionWhenExampleBooleanVarIsMissing() {
        environment.remove("EXAMPLE_BOOLEAN_VAR");

        EnvironmentVariableException exception = assertThrows(
                EnvironmentVariableException.class,
                () -> EnvironmentVariablesFactory.from(environment)
        );

        assertTrue(exception.getMessage().contains("EXAMPLE_BOOLEAN_VAR"));
        assertTrue(exception.getMessage().contains("not set"));
    }

    @Test
    void throwsExceptionWhenVariableIsBlank() {
        environment.put("STAGE", "");

        EnvironmentVariableException exception = assertThrows(
                EnvironmentVariableException.class,
                () -> EnvironmentVariablesFactory.from(environment)
        );

        assertTrue(exception.getMessage().contains("STAGE"));
        assertTrue(exception.getMessage().contains("not set"));
    }

    @Test
    void throwsExceptionWhenVariableIsWhitespace() {
        environment.put("STAGE", "   ");

        EnvironmentVariableException exception = assertThrows(
                EnvironmentVariableException.class,
                () -> EnvironmentVariablesFactory.from(environment)
        );

        assertTrue(exception.getMessage().contains("STAGE"));
        assertTrue(exception.getMessage().contains("not set"));
    }

    @Test
    void throwsExceptionWhenIntVarIsNotANumber() {
        environment.put("EXAMPLE_INT_VAR", "not-a-number");

        EnvironmentVariableException exception = assertThrows(
                EnvironmentVariableException.class,
                () -> EnvironmentVariablesFactory.from(environment)
        );

        assertTrue(exception.getMessage().contains("EXAMPLE_INT_VAR"));
        assertTrue(exception.getMessage().contains("valid integer"));
        assertTrue(exception.getMessage().contains("not-a-number"));
        assertNotNull(exception.getCause());
    }

    @Test
    void throwsExceptionWhenIntVarIsDecimal() {
        environment.put("EXAMPLE_INT_VAR", "3.14");

        EnvironmentVariableException exception = assertThrows(
                EnvironmentVariableException.class,
                () -> EnvironmentVariablesFactory.from(environment)
        );

        assertTrue(exception.getMessage().contains("EXAMPLE_INT_VAR"));
        assertTrue(exception.getMessage().contains("valid integer"));
    }

    @Test
    void throwsExceptionWhenBooleanVarIsNotTrueOrFalse() {
        environment.put("EXAMPLE_BOOLEAN_VAR", "yes");

        EnvironmentVariableException exception = assertThrows(
                EnvironmentVariableException.class,
                () -> EnvironmentVariablesFactory.from(environment)
        );

        assertTrue(exception.getMessage().contains("EXAMPLE_BOOLEAN_VAR"));
        assertTrue(exception.getMessage().contains("'true' or 'false'"));
        assertTrue(exception.getMessage().contains("yes"));
    }

    @Test
    void throwsExceptionWhenBooleanVarIsTrueWithDifferentCase() {
        environment.put("EXAMPLE_BOOLEAN_VAR", "True");

        EnvironmentVariableException exception = assertThrows(
                EnvironmentVariableException.class,
                () -> EnvironmentVariablesFactory.from(environment)
        );

        assertTrue(exception.getMessage().contains("EXAMPLE_BOOLEAN_VAR"));
        assertTrue(exception.getMessage().contains("'true' or 'false'"));
    }

    @Test
    void throwsExceptionWhenBooleanVarIsFalseWithDifferentCase() {
        environment.put("EXAMPLE_BOOLEAN_VAR", "FALSE");

        EnvironmentVariableException exception = assertThrows(
                EnvironmentVariableException.class,
                () -> EnvironmentVariablesFactory.from(environment)
        );

        assertTrue(exception.getMessage().contains("EXAMPLE_BOOLEAN_VAR"));
        assertTrue(exception.getMessage().contains("'true' or 'false'"));
    }

    @Test
    void throwsExceptionWhenBooleanVarIsOne() {
        environment.put("EXAMPLE_BOOLEAN_VAR", "1");

        EnvironmentVariableException exception = assertThrows(
                EnvironmentVariableException.class,
                () -> EnvironmentVariablesFactory.from(environment)
        );

        assertTrue(exception.getMessage().contains("EXAMPLE_BOOLEAN_VAR"));
        assertTrue(exception.getMessage().contains("'true' or 'false'"));
    }

    @Test
    void handlesNegativeInteger() {
        environment.put("EXAMPLE_INT_VAR", "-42");

        EnvironmentVariables result = EnvironmentVariablesFactory.from(environment);

        assertEquals(-42, result.exampleIntVar());
    }

    @Test
    void handlesZeroInteger() {
        environment.put("EXAMPLE_INT_VAR", "0");

        EnvironmentVariables result = EnvironmentVariablesFactory.from(environment);

        assertEquals(0, result.exampleIntVar());
    }

    @Test
    void handlesLargeInteger() {
        environment.put("EXAMPLE_INT_VAR", "2147483647");

        EnvironmentVariables result = EnvironmentVariablesFactory.from(environment);

        assertEquals(Integer.MAX_VALUE, result.exampleIntVar());
    }

    // Singleton behavior is a Guice concern and is not tested here.
}
