package io.template;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.template.bootstrap.injectionmodules.EnvironmentModule;
import io.template.bootstrap.logic.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application entry point.
 * The following classes must not have associated unit tests - smoke tests are used in their place:
 *   - {@code Main.java}
 *   - {@code LifecycleManager.java}
 *   - {@code bootstrap/injectionmodules/*}
 */
public final class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private Main() { }

    public static void main(String[] args) {
        LifecycleManager.registerShutdownHook();

        int exitCode = 0;
        Injector injector = null;
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
            cleanupResources(injector);
            LifecycleManager.signalComplete();
        }

        System.exit(exitCode);
    }

    private static void cleanupResources(Injector injector) {
        if (injector == null) {
            return;
        }
        try {
            // Pull resources from the injector and close them here to prevent resource leaks
        } catch (Exception e) {
            LOGGER.error("Error during resource cleanup", e);
        }
    }
}
