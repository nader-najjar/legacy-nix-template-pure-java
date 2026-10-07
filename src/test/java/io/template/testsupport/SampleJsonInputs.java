package io.template.testsupport;

public final class SampleJsonInputs {

    private SampleJsonInputs() { }

    public static String validInput(
            String exampleStringField,
            int exampleIntField,
            boolean exampleBooleanField,
            String exampleTimestampField,
            String... exampleListElements
    ) {
        StringBuilder listBuilder = new StringBuilder();
        for (int i = 0; i < exampleListElements.length; i++) {
            if (i > 0) {
                listBuilder.append(",");
            }
            listBuilder.append("\"").append(exampleListElements[i]).append("\"");
        }

        return """
                { %n\
                  "exampleStringField": "%s",%n\
                  "exampleIntField": %d,%n\
                  "exampleBooleanField": %s,%n\
                  "exampleTimestampField": "%s",%n\
                  "exampleListField": [%s]%n\
                }%n\
                """.formatted(
                exampleStringField,
                exampleIntField,
                exampleBooleanField,
                exampleTimestampField,
                listBuilder.toString()
        );
    }

    public static final String INVALID_JSON_MALFORMED = "{";

    public static final String INVALID_JSON_EMPTY_STRING = "";

    public static final String INVALID_JSON_NOT_JSON = "not json at all";

    public static final String INVALID_JSON_STRING = "invalid-json";

    public static final String INVALID_JSON_WITH_NON_NUMERIC_INT_FIELD = """
            {
              "exampleStringField": "hello",
              "exampleIntField": "not-a-number",
              "exampleBooleanField": true,
              "exampleTimestampField": "2024-01-01T00:00:00Z",
              "exampleListField": ["a","b"]
            }
            """;

    public static final String INVALID_JSON_WITH_INVALID_TIMESTAMP = """
            {
              "exampleStringField": "hello",
              "exampleIntField": 3,
              "exampleBooleanField": true,
              "exampleTimestampField": "invalid-date",
              "exampleListField": ["a","b"]
            }
            """;

    public static final String INVALID_JSON_WITH_NULL_STRING_FIELD = """
            {
              "exampleStringField": null,
              "exampleIntField": 3,
              "exampleBooleanField": true,
              "exampleTimestampField": "2024-01-01T00:00:00Z",
              "exampleListField": ["a","b"]
            }
            """;
}

