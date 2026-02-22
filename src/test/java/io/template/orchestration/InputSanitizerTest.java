package io.template.orchestration;

import java.util.Set;

import io.template.orchestration.exceptions.InvalidInputException;
import io.template.orchestration.models.ApplicationInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.template.testsupport.SampleJsonInputs.INVALID_JSON_EMPTY_STRING;
import static io.template.testsupport.SampleJsonInputs.INVALID_JSON_MALFORMED;
import static io.template.testsupport.SampleJsonInputs.INVALID_JSON_NOT_JSON;
import static io.template.testsupport.SampleJsonInputs.INVALID_JSON_WITH_INVALID_TIMESTAMP;
import static io.template.testsupport.SampleJsonInputs.INVALID_JSON_WITH_NON_NUMERIC_INT_FIELD;
import static io.template.testsupport.SampleJsonInputs.VALID_JSON_WITH_NULL_STRING_FIELD;
import static io.template.testsupport.SampleJsonInputs.validInput;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputSanitizerTest {

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
    void handlesJsonWithNullStringField() {
        String json = VALID_JSON_WITH_NULL_STRING_FIELD;

        ApplicationInput input = sanitizer.sanitize(new String[]{json});

        assertNotNull(input);
        // null string field is allowed (no validation constraints)
    }
}
