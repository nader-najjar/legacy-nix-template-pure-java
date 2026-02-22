package io.template.samplebusinesslayer;

import java.util.Locale;

import com.google.inject.Inject;
import io.template.samplebusinesslayer.exceptions.CalculationException;
import io.template.samplebusinesslayer.models.CalculationRequest;
import io.template.samplebusinesslayer.models.CalculationResult;

/**
 * Simple calculator service demonstrating business logic.
 */
public class Calculator {

    @Inject
    public Calculator() { }

    public CalculationResult calculate(CalculationRequest request) {
        String operation = request.operation().toUpperCase(Locale.ROOT);
        double result = switch (operation) {
            case "ADD" -> request.operandA() + request.operandB();
            case "SUBTRACT" -> request.operandA() - request.operandB();
            case "MULTIPLY" -> request.operandA() * request.operandB();
            case "DIVIDE" -> {
                if (request.operandB() == 0) {
                    throw new CalculationException("Division by zero");
                }
                yield request.operandA() / request.operandB();
            }
            default -> throw new CalculationException("Unknown operation: " + request.operation());
        };

        return new CalculationResult(result, request.operation());
    }
}
