package io.template;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.google.inject.Guice;
import io.template.composition.AWSClientsModule;
import io.template.composition.EnvironmentModule;
import io.template.composition.StrictGuiceModule;
import io.template.execution.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * AWS Lambda entry point.
 * The following classes must not have associated unit tests - smoke tests are used in their place:
 *   - `LambdaHandler.java`
 *   - `composition/*`
 */
public final class LambdaHandler implements RequestStreamHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(LambdaHandler.class);
    private static final String AWS_REQUEST_ID_MDC_KEY = "awsRequestId";

    private final Executor executor = Guice.createInjector(
            com.google.inject.Stage.PRODUCTION,
            new StrictGuiceModule(),
            new EnvironmentModule(),
            new AWSClientsModule()
    ).getInstance(Executor.class);

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        MDC.put(AWS_REQUEST_ID_MDC_KEY, context.getAwsRequestId());
        try {
            String event = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            executor.execute(new String[] {event});
        } catch (Exception exception) {
            LOGGER.error("Technical exception occurred at software entrypoint level: ", exception);
            throw exception;
        } finally {
            MDC.remove(AWS_REQUEST_ID_MDC_KEY);
        }
    }
}
