package io.template.shared.utilities;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonMapperUtilityTest {

    @Test
    void rejectsDuplicateKeyWhenReadingTree() {
        String json = "{\"field\": 1, \"field\": 2}";

        assertThrows(
                JsonProcessingException.class,
                () -> JsonMapperUtility.MAPPER.readTree(json)
        );
    }
}
