package io.template.execution;

import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.inject.Inject;
import io.template.execution.exceptions.InvalidInputException;
import io.template.execution.models.ApplicationInput;
import io.template.shared.utilities.HibernateValidatorUtility;
import io.template.shared.utilities.JsonMapperUtility;
import jakarta.validation.ConstraintViolation;

/**
 * Handles input sanitization.
 * Deserializes raw input strings into ApplicationInput objects and validates them.
 */
public class InputSanitizer {

    @Inject
    public InputSanitizer() { }

    /**
     * Parses and validates raw input arguments.
     *
     * @param args raw input arguments
     * @return parsed and validated ApplicationInput
     * @throws InvalidInputException if input is missing or invalid
     */
    public ApplicationInput sanitize(String[] args) {
        validateArgumentsStructure(args);
        String jsonString = args[0];

        ApplicationInput applicationInput;
        try {
            applicationInput = JsonMapperUtility.MAPPER.readValue(jsonString, ApplicationInput.class);
        } catch (JsonProcessingException e) {
            throw new InvalidInputException("Invalid input JSON: ", e);
        }

        validateDeserializedInput(applicationInput);
        return applicationInput;
    }

    private void validateArgumentsStructure(String[] args) {
        if (args == null || args.length == 0) {
            throw new InvalidInputException("No input provided");
        }
    }

    private void validateDeserializedInput(ApplicationInput input) {
        Set<ConstraintViolation<ApplicationInput>> violations = HibernateValidatorUtility.VALIDATOR.validate(input);

        if (!violations.isEmpty()) {
            String errors = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining(", "));

            throw new InvalidInputException("Input validation failed: " + errors);
        }
    }
}
