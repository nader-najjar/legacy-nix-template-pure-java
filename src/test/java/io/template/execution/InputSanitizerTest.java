package io.template.execution;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.template.execution.exceptions.InvalidInputException;
import io.template.execution.models.ApplicationInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static io.template.testsupport.SampleJsonInputs.INVALID_JSON_EMPTY_STRING;
import static io.template.testsupport.SampleJsonInputs.INVALID_JSON_MALFORMED;
import static io.template.testsupport.SampleJsonInputs.INVALID_JSON_NOT_JSON;
import static io.template.testsupport.SampleJsonInputs.INVALID_JSON_WITH_INVALID_TIMESTAMP;
import static io.template.testsupport.SampleJsonInputs.INVALID_JSON_WITH_NON_NUMERIC_INT_FIELD;
import static io.template.testsupport.SampleJsonInputs.INVALID_JSON_WITH_NULL_STRING_FIELD;
import static io.template.testsupport.SampleJsonInputs.validInput;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputSanitizerTest {

    private static final Map<String, String> VALID_FIELDS = Map.of(
            "exampleStringField", "\"a\"",
            "exampleIntField", "5",
            "exampleBooleanField", "true",
            "exampleTimestampField", "\"2026-01-01T00:00:00Z\"",
            "exampleListField", "[\"x\",\"y\"]");

    private static final ApplicationInput EXPECTED = new ApplicationInput(
            "a", 5, true, Instant.parse("2026-01-01T00:00:00Z"), List.of("x", "y"));

    private InputSanitizer sanitizer;

    @BeforeEach
    void setUp() {
        sanitizer = new InputSanitizer();
    }

    @Test
    void sanitizesValidInput() {
        String json = validInput(
                "hello",
                3,
                true,
                "2024-01-01T00:00:00Z",
                "a",
                "b"
        );

        ApplicationInput input = sanitizer.sanitize(new String[]{json});

        assertEquals("hello", input.exampleStringField());
        assertEquals(3, input.exampleIntField());
        assertTrue(input.exampleBooleanField());
        assertEquals("2024-01-01T00:00:00Z", input.exampleTimestampField().toString());
        assertEquals(Set.of("a", "b"), Set.copyOf(input.exampleListField()));
    }

    @Test
    void sanitizesInputWithFalseBoolean() {
        String json = validInput(
                "test",
                0,
                false,
                "2024-01-01T00:00:00Z"
        );

        ApplicationInput input = sanitizer.sanitize(new String[]{json});

        assertFalse(input.exampleBooleanField());
        assertEquals(0, input.exampleIntField());
        assertTrue(input.exampleListField().isEmpty());
    }

    @Test
    void rejectsNullArgs() {
        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> sanitizer.sanitize(null)
        );

        assertTrue(exception.getMessage().contains("No input provided"));
    }

    @Test
    void rejectsEmptyArgs() {
        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> sanitizer.sanitize(new String[]{})
        );

        assertTrue(exception.getMessage().contains("No input provided"));
    }

    @Test
    void rejectsInvalidJson() {
        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> sanitizer.sanitize(new String[]{INVALID_JSON_MALFORMED})
        );

        assertTrue(exception.getMessage().contains("Invalid input JSON"));
        assertNotNull(exception.getCause());
    }

    @Test
    void rejectsEmptyJsonString() {
        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> sanitizer.sanitize(new String[]{INVALID_JSON_EMPTY_STRING})
        );

        assertTrue(exception.getMessage().contains("Invalid input JSON"));
    }

    @Test
    void rejectsMalformedJson() {
        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> sanitizer.sanitize(new String[]{INVALID_JSON_NOT_JSON})
        );

        assertTrue(exception.getMessage().contains("Invalid input JSON"));
    }

    @Test
    void rejectsJsonWithInvalidFieldTypes() {
        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> sanitizer.sanitize(new String[]{INVALID_JSON_WITH_NON_NUMERIC_INT_FIELD})
        );

        assertTrue(exception.getMessage().contains("Invalid input JSON"));
        assertNotNull(exception.getCause());
    }

    @Test
    void rejectsJsonWithInvalidTimestampFormat() {
        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> sanitizer.sanitize(new String[]{INVALID_JSON_WITH_INVALID_TIMESTAMP})
        );

        assertTrue(exception.getMessage().contains("Invalid input JSON"));
        assertNotNull(exception.getCause());
    }

    @Test
    void usesFirstArgWhenMultipleArgsProvided() {
        String json1 = validInput(
                "first",
                1,
                true,
                "2024-01-01T00:00:00Z",
                "a"
        );
        String json2 = validInput(
                "second",
                2,
                false,
                "2024-01-01T00:00:00Z",
                "b"
        );

        ApplicationInput input = sanitizer.sanitize(new String[]{json1, json2});

        assertEquals("first", input.exampleStringField());
        assertEquals(1, input.exampleIntField());
    }

    @Test
    void rejectsJsonWithNullStringField() {
        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> sanitizer.sanitize(new String[]{INVALID_JSON_WITH_NULL_STRING_FIELD})
        );

        assertTrue(exception.getMessage().contains("Input validation failed"));
    }

    @Test
    void ignoresUnknownScalarProperty() {
        Map<String, String> fields = new LinkedHashMap<>(VALID_FIELDS);
        fields.put("unknownField", "1");

        assertEquals(EXPECTED, sanitizer.sanitize(new String[]{toJson(fields)}));
    }

    @Test
    void ignoresUnknownNestedProperty() {
        Map<String, String> fields = new LinkedHashMap<>(VALID_FIELDS);
        fields.put("unknownObject", "{\"nested\":[1,{\"deep\":true}],\"label\":\"z\"}");

        assertEquals(EXPECTED, sanitizer.sanitize(new String[]{toJson(fields)}));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "exampleStringField",
        "exampleIntField",
        "exampleBooleanField",
        "exampleTimestampField",
        "exampleListField"
    })
    void rejectsMissingProperty(String missingField) {
        Map<String, String> fields = new LinkedHashMap<>(VALID_FIELDS);
        fields.remove(missingField);
        String json = toJson(fields);

        assertThrows(InvalidInputException.class, () -> sanitizer.sanitize(new String[]{json}));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "exampleStringField",
        "exampleIntField",
        "exampleBooleanField",
        "exampleTimestampField",
        "exampleListField"
    })
    void rejectsNullProperty(String nullField) {
        Map<String, String> fields = new LinkedHashMap<>(VALID_FIELDS);
        fields.put(nullField, "null");
        String json = toJson(fields);

        assertThrows(InvalidInputException.class, () -> sanitizer.sanitize(new String[]{json}));
    }

    @Test
    void rejectsDuplicateProperty() {
        String json = toJson(VALID_FIELDS).replaceFirst("\\{", "{\"exampleStringField\":\"b\",");

        assertThrows(InvalidInputException.class, () -> sanitizer.sanitize(new String[]{json}));
    }

    private static String toJson(Map<String, String> fields) {
        return fields.entrySet().stream()
                .map(field -> "\"" + field.getKey() + "\":" + field.getValue())
                .collect(Collectors.joining(",", "{", "}"));
    }
}
