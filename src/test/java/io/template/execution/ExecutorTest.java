package io.template.execution;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.template.environment.models.EnvironmentVariables;
import io.template.environment.models.Stage;
import io.template.execution.exceptions.InvalidInputException;
import io.template.execution.models.ApplicationInput;
import io.template.samplebusinesslayer.CalculationResultStore;
import io.template.samplebusinesslayer.Calculator;
import io.template.samplebusinesslayer.models.CalculationRequest;
import io.template.samplebusinesslayer.models.CalculationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.regions.Region;

import static io.template.testsupport.SampleApplicationInputs.exampleApplicationInput;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExecutorTest {

    @Mock
    private InputSanitizer inputSanitizer;

    @Mock
    private Calculator calculator;

    @Mock
    private CalculationResultStore calculationResultStore;

    @Mock
    private EnvironmentVariables environmentVariables;

    private Executor executor;

    @BeforeEach
    void setUp() {
        when(environmentVariables.stage()).thenReturn(Stage.BETA);
        when(environmentVariables.awsRegion()).thenReturn(Region.US_EAST_1);

        executor = new Executor(environmentVariables, inputSanitizer, calculator, calculationResultStore);
    }

    @Test
    void executesWithValidInput() {
        String[] args = new String[]{"opaque-input"};
        ApplicationInput mockInput = exampleApplicationInput();

        CalculationResult calculationResult = new CalculationResult(15.0, "ADD");

        when(inputSanitizer.sanitize(args)).thenReturn(mockInput);
        when(calculator.calculate(any(CalculationRequest.class))).thenReturn(calculationResult);

        executor.execute(args);

        // Verify environment variables are accessed
        verify(environmentVariables).stage();
        verify(environmentVariables).awsRegion();

        // Verify input sanitization
        verify(inputSanitizer).sanitize(args);

        // Verify calculator is called with the exact expected request
        verify(calculator).calculate(argThat(request ->
                request.operandA() == 10.0 &&
                request.operandB() == 5.0 &&
                "ADD".equals(request.operation())
        ));

        verify(calculationResultStore).save(new CalculationRequest(10.0, 5.0, "ADD"), calculationResult);
    }

    @SuppressFBWarnings(
            value = "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT",
            justification = "Mockito in-order verification; return values are irrelevant"
    )
    @Test
    void executesInCorrectOrder() {
        String[] args = new String[]{"opaque-input"};
        ApplicationInput mockInput = exampleApplicationInput();

        CalculationResult calculationResult = new CalculationResult(15.0, "ADD");

        when(inputSanitizer.sanitize(args)).thenReturn(mockInput);
        when(calculator.calculate(any(CalculationRequest.class))).thenReturn(calculationResult);

        executor.execute(args);

        // Verify the order of operations
        InOrder inOrder = inOrder(environmentVariables, inputSanitizer, calculator, calculationResultStore);
        inOrder.verify(environmentVariables).stage();
        inOrder.verify(environmentVariables).awsRegion();
        inOrder.verify(inputSanitizer).sanitize(args);
        inOrder.verify(calculator).calculate(argThat(request ->
                request.operandA() == 10.0 &&
                request.operandB() == 5.0 &&
                "ADD".equals(request.operation())
        ));
        inOrder.verify(calculationResultStore).save(new CalculationRequest(10.0, 5.0, "ADD"), calculationResult);
    }

    @Test
    void propagatesExceptionWhenInputSanitizerFails() {
        String[] args = new String[]{"invalid-input"};
        InvalidInputException exception = new InvalidInputException("Invalid input JSON");

        when(inputSanitizer.sanitize(args)).thenThrow(exception);

        assertThrows(
                InvalidInputException.class,
                () -> executor.execute(args)
        );

        // Verify environment variables are still accessed (logging happens before sanitization)
        verify(environmentVariables).stage();
        verify(environmentVariables).awsRegion();
        verify(inputSanitizer).sanitize(args);
        // Verify calculator is never called when sanitization fails
        verify(calculator, never()).calculate(any(CalculationRequest.class));
        verify(calculationResultStore, never()).save(any(CalculationRequest.class), any(CalculationResult.class));
    }
}
