package io.template.execution;

import com.google.inject.Inject;
import io.template.environment.models.EnvironmentVariables;
import io.template.environment.models.Stage;
import io.template.execution.models.ApplicationInput;
import io.template.samplebusinesslayer.CalculationResultStore;
import io.template.samplebusinesslayer.Calculator;
import io.template.samplebusinesslayer.models.CalculationRequest;
import io.template.samplebusinesslayer.models.CalculationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;

/**
 * Main business logic executor.
 */
public class Executor {

    private static final Logger LOGGER = LoggerFactory.getLogger(Executor.class);

    private final EnvironmentVariables environmentVariables;
    private final InputSanitizer inputSanitizer;
    private final Calculator calculator;
    private final CalculationResultStore calculationResultStore;

    @Inject
    public Executor(
            EnvironmentVariables environmentVariables,
            InputSanitizer inputSanitizer,
            Calculator calculator,
            CalculationResultStore calculationResultStore
    ) {
        this.environmentVariables = environmentVariables;
        this.inputSanitizer = inputSanitizer;
        this.calculator = calculator;
        this.calculationResultStore = calculationResultStore;
    }

    public void execute(String[] args) {
        Stage stage = environmentVariables.stage();
        Region awsRegion = environmentVariables.awsRegion();
        LOGGER.info("Executing with stage: {}, awsRegion: {}", stage, awsRegion);

        ApplicationInput input = inputSanitizer.sanitize(args);
        LOGGER.info("Sanitized input: {}", input);

        invokeSampleLogic();
    }

    private void invokeSampleLogic() {
        CalculationRequest request = new CalculationRequest(10.0, 5.0, "ADD");
        CalculationResult result = calculator.calculate(request);
        calculationResultStore.save(request, result);
    }
}
