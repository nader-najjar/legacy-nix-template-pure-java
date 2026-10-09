package io.template;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import io.template.composition.StrictGuiceModule;
import io.template.environment.models.EnvironmentVariables;
import io.template.execution.Executor;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;

/**
 * Composition test: verifies that a small slice of the application
 * can be assembled and executed through the real dependency injection
 * and configuration layers.
 * <p>
 * General paradigm:
 * <ul>
 *   <li>Use the same wiring mechanism as production (e.g., Guice modules) to obtain
 *       a fully constructed "top" component such as {@link Executor}.</li>
 *   <li>Still avoid creating real non-local resources (databases, AWS clients, etc.);
 *       those should be replaced with mocks or in-memory fakes in the test wiring.</li>
 *   <li>Keep the scope narrow: this is not a full end-to-end test, but a check that
 *       your object graph, modules, and high-level flow are coherent.</li>
 * </ul>
 */
class ApplicationCompositionTest {

    /*
     * Example: overriding external dependencies in composition tests
     *
     * When you introduce external clients (e.g., StorageClient, KinesisClient),
     * you can keep this test pattern but replace those clients with mocks or
     * fakes instead of real networked implementations:
     *
     * The main concept to remember is - you do not want to be instantiating things that are non-local in unit tests.
     * They will likely just fail anyways, but if you happen to have the appropriate credentials cached somewhere
     * on your computer, it may accidentally execute important network calls and impact real data.
     *
     *
     * EnvironmentVariables envVars = mock(EnvironmentVariables.class)
     * StorageClient storageClient = mock(StorageClient.class);
     * KinesisClient kinesisClient = mock(KinesisClient.class);
     *
     * Injector injector = Guice.createInjector(
     *         Stage.PRODUCTION,
     *         new StrictGuiceModule(),
     *         binder -> {
     *             binder.bind(EnvironmentVariables.class).toInstance(envVars)
     *             binder.bind(StorageClient.class).toInstance(storageClient);
     *             binder.bind(KinesisClient.class).toInstance(kinesisClient);
     *         }
     * );
     *
     * Executor executor = injector.getInstance(Executor.class);
     */

    @Test
    void assemblesApplication() {
        assertDoesNotThrow(() -> {
            EnvironmentVariables envVars = mock(EnvironmentVariables.class);
            DynamoDbClient dynamoDBClient = mock(DynamoDbClient.class);

            Injector injector = Guice.createInjector(
                    Stage.PRODUCTION,
                    new StrictGuiceModule(),
                    binder -> {
                        binder.bind(EnvironmentVariables.class).toInstance(envVars);
                        binder.bind(DynamoDbClient.class).toInstance(dynamoDBClient);
                    }
            );

            injector.getInstance(Executor.class);
            // No call to executor.execute(args) here; that behavior is covered by unit tests.
        });
    }
}
