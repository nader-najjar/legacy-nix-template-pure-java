package io.template;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.template.orchestration.Executor;
import io.template.orchestration.injectionmodules.EnvironmentModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application entry point.
 * The following classes must not have associated unit tests - smoke tests are used in their place:
 *   - `Main.java`
 *   - `LifecycleManager.java`
 *   - `orchestration/injectionmodules/*`
 */
public final class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private Main() { }

    public static void main(String[] args) {
        LifecycleManager.registerShutdownHook();
        Injector injector;
        int exitCode = 0;

        try {
            injector = Guice.createInjector(
                    new EnvironmentModule()
            );
            Executor executor = injector.getInstance(Executor.class);
            executor.execute(args);
        } catch (Exception exception) {
            LOGGER.error("Technical exception occurred at software entrypoint level: ", exception);
            exitCode = 1;
        } finally {
            // Close any AutoCloseable resources here before the JVM exits.
            // Resources requiring cleanup should be declared before the try block so they are in scope.
            LifecycleManager.signalDone();
        }

        System.exit(exitCode);
    }
}
