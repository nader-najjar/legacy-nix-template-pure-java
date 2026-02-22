package io.template;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages application lifecycle and graceful shutdown.
 *
 * <p>Shutdown pattern:
 * <ol>
 *   <li>SIGTERM fires the registered shutdown hook, which sets a flag and waits on a latch.</li>
 *   <li>Long-running executors observe {@link #isShutdownRequested()} at safe boundaries
 *       (e.g., between loop iterations) and stop accepting new work.</li>
 *   <li>Main performs all resource cleanup in its own {@code finally} block, then calls
 *       {@link #signalDone()} to unblock the hook.</li>
 *   <li>The JVM exits after the hook thread returns.</li>
 * </ol>
 *
 * <p>Correctness is guaranteed by idempotency: dying at any point (including SIGKILL,
 * which bypasses hooks entirely) leaves the system in a consistent state. Graceful
 * shutdown only improves efficiency - it is never required for correctness.
 */
public final class LifecycleManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(LifecycleManager.class);
    private static final int SHUTDOWN_GRACE_PERIOD_SECONDS = 10;
    private static final AtomicBoolean SHUTDOWN_REQUESTED = new AtomicBoolean(false);
    private static final CountDownLatch DONE_LATCH = new CountDownLatch(1);

    private LifecycleManager() { }

    /**
     * Registers a JVM shutdown hook that signals main to stop and waits for it to finish.
     */
    public static void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.debug("SIGTERM received - signaling main to stop");
            SHUTDOWN_REQUESTED.set(true);
            try {
                boolean finished = DONE_LATCH.await(SHUTDOWN_GRACE_PERIOD_SECONDS, TimeUnit.SECONDS);
                if (!finished) {
                    LOGGER.warn("Main did not finish within {}s grace period; JVM will exit",
                            SHUTDOWN_GRACE_PERIOD_SECONDS);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.warn("Shutdown hook interrupted");
            }
        }));
    }

    /**
     * Returns {@code true} if SIGTERM has been received.
     * Long-running executors should poll this at safe boundaries to stop cleanly.
     */
    public static boolean isShutdownRequested() {
        return SHUTDOWN_REQUESTED.get();
    }

    /**
     * Signals that main has completed all work and cleanup.
     * Must be called from main's {@code finally} block to unblock the shutdown hook.
     */
    public static void signalDone() {
        DONE_LATCH.countDown();
    }

}
