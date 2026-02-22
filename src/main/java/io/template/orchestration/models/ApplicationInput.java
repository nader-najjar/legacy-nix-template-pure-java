package io.template.orchestration.models;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Application input model.
 * Represents the deserialized input received by the application.
 * Customize fields based on your application's needs.
 *
 * @param exampleStringField Example string field
 * @param exampleIntField Example integer field
 * @param exampleBooleanField Example boolean field
 * @param exampleTimestampField Example timestamp field
 * @param exampleListField Example list field
 */
public record ApplicationInput(

        @JsonProperty("exampleStringField")
        String exampleStringField,

        @JsonProperty("exampleIntField")
        int exampleIntField,

        @JsonProperty("exampleBooleanField")
        boolean exampleBooleanField,

        @JsonProperty("exampleTimestampField")
        Instant exampleTimestampField,

        @JsonProperty("exampleListField")
        List<String> exampleListField

) { }
