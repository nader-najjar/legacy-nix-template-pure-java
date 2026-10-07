package io.template.orchestration;

import com.google.inject.Inject;
import io.template.orchestration.models.ApplicationInput;
import io.template.samplebusinesslayer.Calculator;
import io.template.samplebusinesslayer.models.CalculationRequest;
import io.template.shared.models.EnvironmentVariables;
import io.template.shared.models.Stage;
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

    @Inject
    public Executor(EnvironmentVariables environmentVariables, InputSanitizer inputSanitizer, Calculator calculator) {
        this.environmentVariables = environmentVariables;
        this.inputSanitizer = inputSanitizer;
        this.calculator = calculator;
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
        calculator.calculate(request);
    }
}
