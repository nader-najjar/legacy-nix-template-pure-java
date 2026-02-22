package io.template;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages application lifecycle around shutdown signals.
 *
 * <p>The shutdown hook only sets a flag and waits for the main thread to complete.
 * Resource cleanup is the responsibility of the main thread, not the shutdown hook.
 * Idempotent operations already guarantee correctness under abrupt termination (SIGKILL);
 * signal handling only improves gracefulness of shutdown.</p>
 *
 * <p>For loop-based applications, check {@link #isShutdownRequested()} to exit the loop
 * gracefully. For single-pass applications, the flag can be ignored.</p>
 */
public final class LifecycleManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(LifecycleManager.class);

    private static final int SHUTDOWN_TIMEOUT_SECONDS = 10;

    private static final AtomicBoolean SHUTDOWN_REQUESTED = new AtomicBoolean(false);

    private static final CountDownLatch MAIN_COMPLETED = new CountDownLatch(1);

    private LifecycleManager() { }

    /**
     * Registers a JVM shutdown hook that sets the shutdown flag and waits
     * for the main thread to signal completion before allowing the JVM to exit.
     */
    public static void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.debug("Shutdown signal received, waiting for main to complete...");
            SHUTDOWN_REQUESTED.set(true);
            try {
                boolean completed = MAIN_COMPLETED.await(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (!completed) {
                    LOGGER.warn("Main did not complete within {} seconds of shutdown signal",
                            SHUTDOWN_TIMEOUT_SECONDS);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }));
    }

    /**
     * Returns whether a shutdown signal has been received.
     * Loop-based applications should check this to exit gracefully.
     *
     * @return true if a shutdown signal (e.g., SIGTERM) has been received
     */
    public static boolean isShutdownRequested() {
        return SHUTDOWN_REQUESTED.get();
    }

    /**
     * Signals that the main thread has completed execution and cleanup.
     * This unblocks the shutdown hook, allowing the JVM to exit.
     */
    public static void signalComplete() {
        MAIN_COMPLETED.countDown();
    }
}
