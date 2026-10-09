package io.template.environment;

import java.util.Map;
import java.util.stream.Stream;

import io.template.environment.exceptions.EnvironmentVariableException;
import io.template.environment.models.EnvironmentVariables;
import io.template.environment.models.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.regions.Region;

import static io.template.testsupport.SampleEnvironmentMaps.validEnvironment;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnvironmentVariablesFactoryTest {

    private Map<String, String> environment;

    @BeforeEach
    void setUp() {
        environment = validEnvironment();
    }

    static Stream<String> requiredKeys() {
        return Stream.of(
                "STAGE",
                "AWS_REGION",
                "CALCULATION_RESULTS_TABLE_NAME",
                "EXAMPLE_STRING_VAR",
                "EXAMPLE_INT_VAR",
                "EXAMPLE_BOOLEAN_VAR"
        );
    }

    @Test
    void providesEnvironmentVariablesWithValidValues() {
        EnvironmentVariables result = EnvironmentVariablesFactory.from(environment);

        assertNotNull(result);
        assertEquals(Stage.BETA, result.stage());
        assertEquals(Region.US_EAST_1, result.awsRegion());
        assertEquals("CalculationResults", result.calculationResultsTableName());
        assertEquals("test", result.exampleStringVar());
        assertEquals(1, result.exampleIntVar());
        assertTrue(result.exampleBooleanVar());
    }

    @Test
    void providesEnvironmentVariablesWithFalseBoolean() {
        environment.put("EXAMPLE_BOOLEAN_VAR", "false");

        EnvironmentVariables result = EnvironmentVariablesFactory.from(environment);

        assertFalse(result.exampleBooleanVar());
        assertEquals(Stage.BETA, result.stage());
        assertEquals(1, result.exampleIntVar());
    }

    @ParameterizedTest
    @MethodSource("requiredKeys")
    void throwsExceptionWhenVariableIsMissing(String key) {
        environment.remove(key);

        EnvironmentVariableException exception = assertThrows(
                EnvironmentVariableException.class,
                () -> EnvironmentVariablesFactory.from(environment)
        );

        assertTrue(exception.getMessage().contains(key));
        assertTrue(exception.getMessage().contains("not set"));
    }

    @ParameterizedTest
    @MethodSource("requiredKeys")
    void throwsExceptionWhenVariableIsBlank(String key) {
        environment.put(key, "");

        EnvironmentVariableException exception = assertThrows(
                EnvironmentVariableException.class,
                () -> EnvironmentVariablesFactory.from(environment)
        );

        assertTrue(exception.getMessage().contains(key));
        assertTrue(exception.getMessage().contains("not set"));
    }

    @ParameterizedTest
    @MethodSource("requiredKeys")
    void throwsExceptionWhenVariableIsWhitespace(String key) {
        environment.put(key, "   ");

        EnvironmentVariableException exception = assertThrows(
                EnvironmentVariableException.class,
                () -> EnvironmentVariablesFactory.from(environment)
        );

        assertTrue(exception.getMessage().contains(key));
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

    @ParameterizedTest
    @EnumSource(Stage.class)
    void providesEnvironmentVariablesWithEachStage(Stage stage) {
        environment.put("STAGE", stage.name());

        EnvironmentVariables result = EnvironmentVariablesFactory.from(environment);

        assertEquals(stage, result.stage());
    }

    static Stream<Region> awsRegions() {
        return Region.regions().stream();
    }

    @ParameterizedTest
    @MethodSource("awsRegions")
    void providesEnvironmentVariablesWithEachAWSRegion(Region awsRegion) {
        environment.put("AWS_REGION", awsRegion.id());

        EnvironmentVariables result = EnvironmentVariablesFactory.from(environment);

        assertEquals(awsRegion, result.awsRegion());
    }

    @Test
    void throwsExceptionWhenAWSRegionIsWellFormedButUnknownEvenThoughRegionOfAcceptsAnyId() {
        String unknownRegionId = "us-fake-1";
        assertDoesNotThrow(() -> Region.of(unknownRegionId));
        environment.put("AWS_REGION", unknownRegionId);

        EnvironmentVariableException exception = assertThrows(
                EnvironmentVariableException.class,
                () -> EnvironmentVariablesFactory.from(environment)
        );

        assertTrue(exception.getMessage().contains("AWS_REGION"));
        assertTrue(exception.getMessage().contains("must be one of"));
        assertTrue(exception.getMessage().contains("got: " + unknownRegionId));
    }

    @ParameterizedTest
    @ValueSource(strings = {"US-EAST-1", "US_EAST_1", " us-east-1", "us-east-1 "})
    void throwsExceptionWhenAWSRegionIsNotAnExactRegionId(String value) {
        environment.put("AWS_REGION", value);

        EnvironmentVariableException exception = assertThrows(
                EnvironmentVariableException.class,
                () -> EnvironmentVariablesFactory.from(environment)
        );

        assertTrue(exception.getMessage().contains("AWS_REGION"));
        assertTrue(exception.getMessage().contains("must be one of"));
        assertTrue(exception.getMessage().contains("got: " + value));
    }

    @ParameterizedTest
    @CsvSource({
        "STAGE, dev",
        "STAGE, Production",
        "STAGE, ' PRODUCTION'"
    })
    void throwsExceptionWhenEnumValueIsNotAnExactConstantName(String key, String value) {
        environment.put(key, value);

        EnvironmentVariableException exception = assertThrows(
                EnvironmentVariableException.class,
                () -> EnvironmentVariablesFactory.from(environment)
        );

        assertTrue(exception.getMessage().contains(key));
        assertTrue(exception.getMessage().contains("must be one of"));
        assertTrue(exception.getMessage().contains("got: " + value));
        assertInstanceOf(IllegalArgumentException.class, exception.getCause());
    }

    // Singleton behavior is a Guice concern and is not tested here.
}
